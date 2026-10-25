using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;

public class True2kPbrEngine {
    private string srcDir;
    private string outDir;

    public True2kPbrEngine(string srcDir, string outDir) {
        this.srcDir = srcDir;
        this.outDir = outDir;
    }

    public void Generate2kPack() {
        Console.WriteLine("=== Starting 2K Super HD LabPBR 1.3 + POM Engine ===");

        // 1. Process all entity textures at 2048x2048 (2K)
        ProcessCategory("textures/entity", 2048, 2048);

        // 2. Process block models (cmodels rails & tracks) at 2048x2048
        ProcessCategory("textures/block/cmodels", 2048, 2048);

        // 3. Process block textures (switches, signals, buffer stops, rails) at 1024x1024
        ProcessCategory("textures/block", 1024, 1024);

        // 4. Process item icons at 128x128
        ProcessCategory("textures/item", 128, 128);

        // 5. Pack metadata & configuration
        WriteMetadata();

        Console.WriteLine("=== 2K Super HD LabPBR Generation Complete ===");
    }

    private void ProcessCategory(string relPath, int targetW, int targetH) {
        string inFolder = Path.Combine(srcDir, relPath);
        string outFolder = Path.Combine(outDir, "assets/miningtools", relPath);

        if (!Directory.Exists(inFolder)) return;
        Directory.CreateDirectory(outFolder);

        foreach (string file in Directory.GetFiles(inFolder, "*.png")) {
            string fileName = Path.GetFileName(file);
            if (fileName.EndsWith("_n.png") || fileName.EndsWith("_s.png")) continue;

            string baseName = Path.GetFileNameWithoutExtension(file);

            using (Bitmap srcBmp = new Bitmap(file)) {
                int outW = targetW;
                int outH = targetH;

                // Respect 2:1 aspect ratio textures like basic_locomotive (1024x512 -> 2048x1024)
                if (srcBmp.Width == srcBmp.Height * 2) {
                    outH = outW / 2;
                } else if (srcBmp.Height == srcBmp.Width * 2) {
                    outW = outH / 2;
                } else if (srcBmp.Width > targetW || srcBmp.Height > targetH) {
                    outW = srcBmp.Width;
                    outH = srcBmp.Height;
                }

                Synthesize2kTexture(srcBmp, Path.Combine(outFolder, baseName), outW, outH, baseName);
            }
        }
    }

    private void Synthesize2kTexture(Bitmap src, string outBasePath, int width, int height, string name) {
        // High quality bicubic upscaled base
        Bitmap albedo = new Bitmap(width, height, PixelFormat.Format32bppArgb);
        using (Graphics g = Graphics.FromImage(albedo)) {
            g.InterpolationMode = (src.Width <= 32) ? InterpolationMode.NearestNeighbor : InterpolationMode.HighQualityBicubic;
            g.PixelOffsetMode = PixelOffsetMode.HighQuality;
            g.DrawImage(src, 0, 0, width, height);
        }

        Bitmap normalBmp = new Bitmap(width, height, PixelFormat.Format32bppArgb);
        Bitmap specBmp = new Bitmap(width, height, PixelFormat.Format32bppArgb);

        float[,] heightMap = new float[width, height];
        byte[,] smoothMap = new byte[width, height];
        byte[,] metalMap = new byte[width, height];
        byte[,] porousMap = new byte[width, height];
        byte[,] emissiveMap = new byte[width, height];

        float scaleX = (float)src.Width / width;
        float scaleY = (float)src.Height / height;

        // Pass 1: Pixel classification and physical material parameter assignment
        for (int y = 0; y < height; y++) {
            int srcY = Math.Min(src.Height - 1, (int)(y * scaleY));
            for (int x = 0; x < width; x++) {
                int srcX = Math.Min(src.Width - 1, (int)(x * scaleX));

                Color c = albedo.GetPixel(x, y);
                if (c.A == 0) {
                    heightMap[x, y] = 0;
                    smoothMap[x, y] = 0;
                    metalMap[x, y] = 0;
                    porousMap[x, y] = 0;
                    emissiveMap[x, y] = 255;
                    continue;
                }

                float r = c.R / 255f;
                float g = c.G / 255f;
                float b = c.B / 255f;
                float lum = 0.299f * r + 0.587f * g + 0.114f * b;

                float maxC = Math.Max(r, Math.Max(g, b));
                float minC = Math.Min(r, Math.Min(g, b));
                float sat = (maxC == 0) ? 0 : (maxC - minC) / maxC;

                // Feature detection
                bool isLamp = (r > 0.8f && g > 0.7f && b < 0.4f) || (lum > 0.88f && sat < 0.2f && (name.Contains("locomotive") || name.Contains("lamp") || name.Contains("light")));
                bool isSignalRed = (r > 0.65f && g < 0.25f && b < 0.25f && lum > 0.35f && (name.Contains("switch") || name.Contains("signal") || name.Contains("caboose")));
                bool isSignalGreen = (g > 0.65f && r < 0.25f && b < 0.35f && lum > 0.35f && (name.Contains("switch") || name.Contains("signal") || name.Contains("box_center")));
                bool isElectricArc = (b > 0.75f && g > 0.7f && r < 0.5f && name.Contains("wire_on"));
                bool isFirebox = (r > 0.7f && g > 0.3f && b < 0.2f && name.Contains("basic_locomotive"));

                bool isGlass = (b > 0.5f && g > 0.5f && r > 0.4f && sat > 0.08f && sat < 0.35f && lum > 0.45f && lum < 0.85f);
                bool isWood = (r > g) && (g > b) && (r < 0.7f) && (b < 0.4f) && (sat > 0.25f) && (name.Contains("wood") || name.Contains("caboose") || name.Contains("log") || name.Contains("rail") || name.Contains("livestock"));
                bool isCoal = (lum < 0.18f && sat < 0.1f && name.Contains("coal"));
                bool isOreSparkle = (name.Contains("ore") && lum > 0.6f && sat < 0.2f);
                bool isGrayMetal = (sat < 0.15f);

                // Photorealistic micro-surface texturing (brushed metal, wood grain, paint micro-texture)
                float microNoise = (TextureUtils.Hash(x, y, 101) - 0.5f) * 0.08f;
                float brushedStreak = (float)Math.Sin(x * 0.8f + TextureUtils.Hash(0, y, 102) * 5.0f) * 0.04f;

                float h = 185f;
                byte sm = 175;
                byte metal = 0;
                byte porous = 5;
                byte em = 255; // 255 = off

                if (isLamp || isSignalRed || isSignalGreen || isElectricArc || isFirebox) {
                    // Radiant emissive lighting (LabPBR emissive: 0..20 = max glow)
                    em = (byte)Math.Max(0, Math.Min(25, (int)((1f - lum) * 40f)));
                    sm = 252;
                    metal = 0;
                    porous = 0;
                    h = 245f;
                } else if (isGlass) {
                    // Cab glass / windshields
                    sm = 250; // high gloss glass reflection
                    metal = 0; // dielectric
                    porous = 0;
                    h = 170f;
                } else if (isWood) {
                    // Timber sleepers / planks / logs
                    float woodFiber = (float)Math.Sin(x * 0.25f + y * 0.02f) * 0.15f;
                    sm = (byte)TextureUtils.Clamp((int)(35 + woodFiber * 20f), 15, 60);
                    metal = 0;
                    porous = 55;
                    h = 135f + woodFiber * 50f + microNoise * 30f;
                } else if (isCoal) {
                    // Glossy faceted anthracite coal
                    float facet = (TextureUtils.Hash(x / 4, y / 4, 303) - 0.5f) * 0.4f;
                    sm = (byte)TextureUtils.Clamp((int)(120 + facet * 60f), 60, 180);
                    metal = 0;
                    porous = 35;
                    h = 110f + facet * 100f;
                } else if (isOreSparkle) {
                    // Sparkling metallic ore flecks
                    sm = 230;
                    metal = 230; // Iron
                    porous = 10;
                    h = 220f;
                } else if (isGrayMetal) {
                    // Bare steel & cast iron
                    if (lum > 0.65f) {
                        // Polished rail crown / wheel tread / chrome trim
                        metal = 230; // Iron/Steel
                        sm = (byte)TextureUtils.Clamp((int)(235 + lum * 18f), 220, 252);
                        porous = 0;
                        h = 248f + brushedStreak * 25f;
                    } else if (lum < 0.18f) {
                        // Undercarriage / deep crevices / brake shoes
                        metal = 230;
                        sm = 110;
                        porous = 15;
                        h = 45f;
                    } else {
                        // Industrial brushed sheet steel / cast iron
                        metal = 230; // Iron/Steel
                        sm = (byte)TextureUtils.Clamp((int)(165 + lum * 45f + brushedStreak * 30f), 130, 210);
                        porous = 5;
                        h = 185f + brushedStreak * 35f + microNoise * 20f;
                    }
                } else {
                    // Painted industrial body panels (CN Red, container paint, hazard yellow)
                    metal = 0; // dielectric paint
                    sm = (byte)TextureUtils.Clamp((int)(180 + lum * 30f + microNoise * 20f), 150, 215); // satin gloss
                    porous = 5;
                    h = 190f + microNoise * 20f;
                }

                heightMap[x, y] = TextureUtils.Clamp(h, 1f, 255f);
                smoothMap[x, y] = sm;
                metalMap[x, y] = metal;
                porousMap[x, y] = porous;
                emissiveMap[x, y] = em;

                // Add subtle realistic micro-contrast to Albedo for HD photo-realism without altering the colors
                if (em == 255) {
                    float contrastFactor = 1f + microNoise * 0.6f + brushedStreak * 0.4f;
                    int nr = TextureUtils.Clamp((int)(c.R * contrastFactor), 0, 255);
                    int ng = TextureUtils.Clamp((int)(c.G * contrastFactor), 0, 255);
                    int nb = TextureUtils.Clamp((int)(c.B * contrastFactor), 0, 255);
                    albedo.SetPixel(x, y, Color.FromArgb(c.A, nr, ng, nb));
                }
            }
        }

        // Pass 2: Edge detection, tangent normal calculation, ambient occlusion, and POM height map
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

                float hL = heightMap[x0, y];
                float hR = heightMap[x1, y];
                float hU = heightMap[x, y0];
                float hD = heightMap[x, y1];

                // DirectX Normal: Red = +X (right), Green = +Y (down)
                float dx = (hR - hL) * 0.08f;
                float dy = (hD - hU) * 0.08f;
                float dz = 1.0f;

                float len = (float)Math.Sqrt(dx * dx + dy * dy + dz * dz);
                float nx = dx / len;
                float ny = dy / len;

                int nr = Math.Max(0, Math.Min(255, (int)((nx * 0.5f + 0.5f) * 255f)));
                int ng = Math.Max(0, Math.Min(255, (int)((ny * 0.5f + 0.5f) * 255f)));

                // Ambient Occlusion from curvature Laplacian
                float laplacian = (hL + hR + hU + hD - 4f * heightMap[x, y]);
                float ao = 255f + laplacian * 1.4f;
                int nb = Math.Max(30, Math.Min(255, (int)ao));

                // Alpha = POM Height (1..255)
                int na = Math.Max(1, Math.Min(255, (int)heightMap[x, y]));

                normalBmp.SetPixel(x, y, Color.FromArgb(na, nr, ng, nb));

                // Specular Map
                specBmp.SetPixel(x, y, Color.FromArgb(
                    emissiveMap[x, y],
                    smoothMap[x, y],
                    metalMap[x, y],
                    porousMap[x, y]
                ));
            }
        }

        // Save maps
        albedo.Save(outBasePath + ".png", ImageFormat.Png);
        normalBmp.Save(outBasePath + "_n.png", ImageFormat.Png);
        specBmp.Save(outBasePath + "_s.png", ImageFormat.Png);

        albedo.Dispose();
        normalBmp.Dispose();
        specBmp.Dispose();
    }

    private void WriteMetadata() {
        string mcmeta = @"{
  ""pack"": {
    ""pack_format"": 68.0,
    ""min_format"": 65.0,
    ""max_format"": 99.0,
    ""description"": ""§6§lTrack & Train Kit §b§lSuper HD PBR (2K)§r\n§72K Photorealistic LabPBR 1.3 + Full POM Depth""
  }
}";
        File.WriteAllText(Path.Combine(outDir, "pack.mcmeta"), mcmeta);

        string texProps = @"format=lab-pbr
version=1.3
";
        string optifineDir1 = Path.Combine(outDir, "assets/minecraft/optifine");
        string optifineDir2 = Path.Combine(outDir, "assets/miningtools/optifine");
        Directory.CreateDirectory(optifineDir1);
        Directory.CreateDirectory(optifineDir2);
        File.WriteAllText(Path.Combine(optifineDir1, "texture.properties"), texProps);
        File.WriteAllText(Path.Combine(optifineDir2, "texture.properties"), texProps);

        // Copy pack icon
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
    }
}
