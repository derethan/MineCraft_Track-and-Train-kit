using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;

public class TrueHdLabPbrGenerator {
    private string srcDir;
    private string outDir;

    public TrueHdLabPbrGenerator(string srcDir, string outDir) {
        this.srcDir = srcDir;
        this.outDir = outDir;
    }

    public void ProcessAll() {
        Console.WriteLine("Processing all authentic mod textures into Super HD LabPBR 1.3 + POM...");

        // Process all textures in textures/entity, textures/block, and textures/item
        ProcessDirectory("textures/entity", 1024, 1024);
        ProcessDirectory("textures/block", 512, 512);
        ProcessDirectory("textures/item", 64, 64);

        // Also process block/cmodels
        ProcessDirectory("textures/block/cmodels", 512, 512);

        // Write pack.mcmeta with Minecraft 26.3 required keys
        string mcmeta = @"{
  ""pack"": {
    ""pack_format"": 68.0,
    ""min_format"": 65.0,
    ""max_format"": 99.0,
    ""description"": ""§6§lTrack & Train Kit §b§lSuper HD PBR§r\n§7Authentic LabPBR 1.3 + POM Depth & Reflections""
  }
}";
        File.WriteAllText(Path.Combine(outDir, "pack.mcmeta"), mcmeta);

        // Write texture.properties for OptiFine & Iris shaders
        string texProps = @"format=lab-pbr
version=1.3
";
        string optifineDir1 = Path.Combine(outDir, "assets/minecraft/optifine");
        string optifineDir2 = Path.Combine(outDir, "assets/miningtools/optifine");
        Directory.CreateDirectory(optifineDir1);
        Directory.CreateDirectory(optifineDir2);
        File.WriteAllText(Path.Combine(optifineDir1, "texture.properties"), texProps);
        File.WriteAllText(Path.Combine(optifineDir2, "texture.properties"), texProps);

        // Copy pack.png if present, or generate an HD one
        string packPngSrc = Path.Combine(srcDir, "../logo.png");
        string packPngDst = Path.Combine(outDir, "pack.png");
        if (File.Exists(packPngSrc)) {
            using (Bitmap logo = new Bitmap(packPngSrc))
            using (Bitmap packIcon = new Bitmap(512, 512, PixelFormat.Format32bppArgb))
            using (Graphics g = Graphics.FromImage(packIcon)) {
                g.InterpolationMode = InterpolationMode.HighQualityBicubic;
                g.DrawImage(logo, 0, 0, 512, 512);
                packIcon.Save(packPngDst, ImageFormat.Png);
            }
        }

        Console.WriteLine("All textures processed successfully!");
    }

    private void ProcessDirectory(string subPath, int targetW, int targetH) {
        string inPath = Path.Combine(srcDir, subPath);
        string targetPath = Path.Combine(outDir, "assets/miningtools", subPath);
        if (!Directory.Exists(inPath)) return;

        Directory.CreateDirectory(targetPath);

        foreach (string file in Directory.GetFiles(inPath, "*.png")) {
            string fileName = Path.GetFileName(file);
            // Skip existing _n and _s files from source
            if (fileName.EndsWith("_n.png") || fileName.EndsWith("_s.png")) continue;

            string baseName = Path.GetFileNameWithoutExtension(file);

            using (Bitmap srcBmp = new Bitmap(file)) {
                int outW = Math.Max(srcBmp.Width, targetW);
                int outH = Math.Max(srcBmp.Height, targetH);

                // Preserve aspect ratio for non-square textures (e.g. basic_locomotive 1024x512)
                if (srcBmp.Width == srcBmp.Height * 2) {
                    outH = outW / 2;
                } else if (srcBmp.Height == srcBmp.Width * 2) {
                    outW = outH / 2;
                }

                ProcessSingleTexture(srcBmp, Path.Combine(targetPath, baseName), outW, outH);
            }
        }
    }

    private void ProcessSingleTexture(Bitmap src, string outBasePath, int width, int height) {
        // High quality bicubic resize for base albedo
        Bitmap albedo = new Bitmap(width, height, PixelFormat.Format32bppArgb);
        using (Graphics g = Graphics.FromImage(albedo)) {
            g.InterpolationMode = (src.Width < 64) ? InterpolationMode.NearestNeighbor : InterpolationMode.HighQualityBicubic;
            g.PixelOffsetMode = PixelOffsetMode.HighQuality;
            g.DrawImage(src, 0, 0, width, height);
        }

        Bitmap normalBmp = new Bitmap(width, height, PixelFormat.Format32bppArgb);
        Bitmap specBmp = new Bitmap(width, height, PixelFormat.Format32bppArgb);

        // 1. First pass: compute height, smoothness, metal ID, and emissive for each pixel
        float[,] rawHeight = new float[width, height];
        byte[,] smoothMap = new byte[width, height];
        byte[,] metalMap = new byte[width, height];
        byte[,] porousMap = new byte[width, height];
        byte[,] emissiveMap = new byte[width, height];

        float scaleX = (float)src.Width / width;
        float scaleY = (float)src.Height / height;

        for (int y = 0; y < height; y++) {
            int srcY = Math.Min(src.Height - 1, (int)(y * scaleY));
            for (int x = 0; x < width; x++) {
                int srcX = Math.Min(src.Width - 1, (int)(x * scaleX));

                Color c = albedo.GetPixel(x, y);
                if (c.A == 0) {
                    rawHeight[x, y] = 0;
                    smoothMap[x, y] = 0;
                    metalMap[x, y] = 0;
                    porousMap[x, y] = 0;
                    emissiveMap[x, y] = 255;
                    continue;
                }

                // Analyze pixel color & luminance
                float r = c.R / 255f;
                float g = c.G / 255f;
                float b = c.B / 255f;
                float lum = 0.299f * r + 0.587f * g + 0.114f * b;

                // Color properties
                float maxC = Math.Max(r, Math.Max(g, b));
                float minC = Math.Min(r, Math.Min(g, b));
                float sat = (maxC == 0) ? 0 : (maxC - minC) / maxC;

                bool isGray = sat < 0.18f;
                bool isWarmWood = (r > g) && (g > b) && (r < 0.65f) && (b < 0.35f);
                bool isYellowLamp = (r > 0.75f && g > 0.65f && b < 0.4f);
                bool isRedLight = (r > 0.7f && g < 0.3f && b < 0.3f && lum > 0.4f);
                bool isGreenLight = (g > 0.7f && r < 0.3f && b < 0.4f && lum > 0.4f);
                bool isCyanGlow = (b > 0.7f && g > 0.6f && r < 0.4f);
                bool isGlass = (b > 0.6f && g > 0.5f && r > 0.4f && sat > 0.1f && sat < 0.45f && lum > 0.45f);

                // Micro-surface noise (for rich photorealistic texture)
                float micro = (TextureUtils.Hash(x, y, 777) - 0.5f) * 0.05f;

                // Height base
                float hVal = 170f + (lum - 0.5f) * 60f + micro * 30f;

                byte sm = 150;
                byte metal = 59; // dielectric default
                byte porous = 5;
                byte em = 255; // emission OFF

                if (isYellowLamp || isRedLight || isGreenLight || isCyanGlow) {
                    // Emissive light / indicator / lantern / signal
                    em = (byte)Math.Max(0, Math.Min(30, (int)((1f - lum) * 50f)));
                    sm = 250;
                    metal = 59;
                    hVal = 240f;
                } else if (isGlass) {
                    // Cab glass / windshields
                    sm = 248; // mirror smooth
                    metal = 60; // dielectric glass
                    porous = 0;
                    hVal = 190f;
                } else if (isWarmWood) {
                    // Wood planks / timber logs
                    float woodGrain = (float)Math.Sin(x * 0.4f + y * 0.05f) * 0.15f;
                    sm = (byte)TextureUtils.Clamp((int)(35 + woodGrain * 20f), 15, 60);
                    metal = 59;
                    porous = 50;
                    hVal = 140f + woodGrain * 40f;
                } else if (isGray) {
                    // Bare metal / iron / steel
                    if (lum > 0.7f) {
                        // Polished rail crown / chrome / bright metal
                        metal = 230; // Iron/Steel ID
                        sm = (byte)TextureUtils.Clamp((int)(225 + lum * 25f), 210, 252);
                        porous = 0;
                        hVal = 245f;
                    } else if (lum < 0.15f) {
                        // Deep shadows / crevices / underside
                        metal = 230;
                        sm = 110;
                        porous = 15;
                        hVal = 60f;
                    } else {
                        // Heavy cast iron / brushed industrial steel
                        metal = 230; // Iron/Steel ID
                        sm = (byte)TextureUtils.Clamp((int)(160 + lum * 40f), 130, 195);
                        porous = 5;
                        hVal = 185f;
                    }
                } else {
                    // Painted metal (Canadian National red, container blue/green, warning yellow)
                    metal = 59; // dielectric paint
                    sm = (byte)TextureUtils.Clamp((int)(165 + lum * 35f), 140, 200); // satin enamel finish
                    porous = 5;
                    hVal = 185f;
                }

                rawHeight[x, y] = TextureUtils.Clamp(hVal, 1f, 255f);
                smoothMap[x, y] = sm;
                metalMap[x, y] = metal;
                porousMap[x, y] = porous;
                emissiveMap[x, y] = em;
            }
        }

        // 2. Second pass: Sobel filter for DirectX normal map + Ambient Occlusion + Height
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = albedo.GetPixel(x, y);
                if (c.A == 0) {
                    normalBmp.SetPixel(x, y, Color.FromArgb(0, 128, 128, 255));
                    specBmp.SetPixel(x, y, Color.FromArgb(0, 0, 0, 0));
                    continue;
                }

                int x0 = Math.Max(0, x - 1);
                int x1 = Math.Min(width - 1, x + 1);
                int y0 = Math.Max(0, y - 1);
                int y1 = Math.Min(height - 1, y + 1);

                float hL = rawHeight[x0, y];
                float hR = rawHeight[x1, y];
                float hU = rawHeight[x, y0];
                float hD = rawHeight[x, y1];

                // DirectX Normal: Red = +X (right), Green = +Y (down)
                float dx = (hR - hL) * 0.06f;
                float dy = (hD - hU) * 0.06f;
                float dz = 1.0f;

                float len = (float)Math.Sqrt(dx * dx + dy * dy + dz * dz);
                float nx = dx / len;
                float ny = dy / len;

                int nr = Math.Max(0, Math.Min(255, (int)((nx * 0.5f + 0.5f) * 255f)));
                int ng = Math.Max(0, Math.Min(255, (int)((ny * 0.5f + 0.5f) * 255f)));

                // Ambient Occlusion from curvature
                float laplacian = (hL + hR + hU + hD - 4f * rawHeight[x, y]);
                float ao = 255f + laplacian * 1.1f;
                int nb = Math.Max(40, Math.Min(255, (int)ao));

                // Alpha = POM Height (1..255)
                int na = Math.Max(1, Math.Min(255, (int)rawHeight[x, y]));

                normalBmp.SetPixel(x, y, Color.FromArgb(na, nr, ng, nb));

                // Specular: R = Smoothness, G = Metal ID, B = Porosity, A = Emissive
                specBmp.SetPixel(x, y, Color.FromArgb(
                    emissiveMap[x, y],
                    smoothMap[x, y],
                    metalMap[x, y],
                    porousMap[x, y]
                ));
            }
        }

        // Save the 3 maps
        albedo.Save(outBasePath + ".png", ImageFormat.Png);
        normalBmp.Save(outBasePath + "_n.png", ImageFormat.Png);
        specBmp.Save(outBasePath + "_s.png", ImageFormat.Png);

        albedo.Dispose();
        normalBmp.Dispose();
        specBmp.Dispose();
    }
}
