using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;

namespace HdTexturePackGenerator {
    public static class VehicleGenerators {

        public static void GenerateAllVehicles(string baseEntityDir, string outDir) {
            Console.WriteLine("  -> Processing all authentic vehicle textures from mod assets to 2K LabPBR 1.3...");
            string[] files = Directory.GetFiles(baseEntityDir, "*.png");

            foreach (string file in files) {
                string name = Path.GetFileNameWithoutExtension(file);
                if (name.EndsWith("_n") || name.EndsWith("_s")) continue;

                ProcessEntityBaseTexture(file, outDir);
            }
        }

        public static void ProcessEntityBaseTexture(string srcFile, string outDir, int targetWidth = 2048, int targetHeight = 2048) {
            string fileNameNoExt = Path.GetFileNameWithoutExtension(srcFile);
            Console.WriteLine($"     * Enhancing {fileNameNoExt} to 2K LabPBR...");

            bool isPaintedVehicle = fileNameNoExt.StartsWith("container_cart") ||
                                    fileNameNoExt.StartsWith("caboose") ||
                                    fileNameNoExt.StartsWith("diesel") ||
                                    fileNameNoExt.StartsWith("electric") ||
                                    fileNameNoExt.StartsWith("basic_locomotive") ||
                                    fileNameNoExt.StartsWith("hopper_cart") ||
                                    fileNameNoExt.StartsWith("liquid_cart") ||
                                    fileNameNoExt.StartsWith("track_paver");

            using (Bitmap srcBmp = new Bitmap(srcFile)) {
                int w = targetWidth;
                int h = (srcBmp.Width == srcBmp.Height * 2) ? (targetWidth / 2) : targetHeight;

                using (Bitmap scaledBmp = new Bitmap(w, h, PixelFormat.Format32bppArgb)) {
                    using (Graphics g = Graphics.FromImage(scaledBmp)) {
                        // HighQualityBicubic for 1024+ textures to produce smooth anti-aliased text typography;
                        // NearestNeighbor for low-res pixel art.
                        if (srcBmp.Width >= 1024) {
                            g.InterpolationMode = InterpolationMode.HighQualityBicubic;
                            g.SmoothingMode = SmoothingMode.HighQuality;
                            g.PixelOffsetMode = PixelOffsetMode.HighQuality;
                        } else {
                            g.InterpolationMode = InterpolationMode.NearestNeighbor;
                            g.PixelOffsetMode = PixelOffsetMode.Half;
                        }
                        g.DrawImage(srcBmp, 0, 0, w, h);
                    }

                    using (PbrCanvas c = new PbrCanvas(w, h)) {
                        BitmapData data = scaledBmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
                        unsafe {
                            byte* ptr = (byte*)data.Scan0;
                            for (int y = 0; y < h; y++) {
                                byte* row = ptr + y * data.Stride;
                                for (int x = 0; x < w; x++) {
                                    int px = x * 4;
                                    byte b = row[px + 0];
                                    byte gCol = row[px + 1];
                                    byte r = row[px + 2];
                                    byte a = row[px + 3];

                                    if (a == 0) continue; // transparent

                                    float lum = 0.299f * r + 0.587f * gCol + 0.114f * b;
                                    int max = Math.Max(r, Math.Max(gCol, b));
                                    int min = Math.Min(r, Math.Min(gCol, b));
                                    float sat = (max == 0) ? 0f : (max - min) / (float)max;

                                    byte height = 190;
                                    byte smooth = 210;
                                    byte metal = 0;
                                    byte porous = 0;
                                    byte emissive = 255;
                                    bool isText = false;

                                    // 1. Emissive Check (Headlights, red marker lamps, glowing firebox)
                                    if (r > 210 && gCol > 160 && b < 60 && sat > 0.6f) {
                                        emissive = (byte)Math.Clamp(255 - (int)(lum * 0.9f), 0, 254);
                                        smooth = 245;
                                        height = 230;
                                    } else if (r > 190 && gCol < 50 && b < 50 && sat > 0.7f && lum > 100) {
                                        emissive = 10;
                                        smooth = 230;
                                        height = 220;
                                    }
                                    // 2. Glass / Windows (cyan/translucent blue tint)
                                    else if (b > 150 && gCol > 130 && r < 140 && sat > 0.15f && sat < 0.5f) {
                                        smooth = 252;
                                        height = 175;
                                    }
                                    // 3. Brass / Gold / Copper
                                    else if (r > 170 && gCol > 120 && b < 70 && sat > 0.5f) {
                                        metal = 234;
                                        smooth = 230;
                                        height = 225;
                                    }
                                    // 4. Wood Planks / Logs (warm brown with distinct R > G > B)
                                    else if (r > gCol && gCol > b && sat > 0.22f && r < 170 && b < 90 && !fileNameNoExt.StartsWith("caboose")) {
                                        smooth = 85;
                                        porous = 45;
                                        float grain = (DrawUtils.Hash(x, y, 77) - 0.5f) * 0.08f;
                                        height = (byte)Math.Clamp((int)(120 + (lum - 70) * 0.9f + grain * 40f), 50, 215);
                                    }
                                    // 5. Painted Text Stencils / Decals (e.g. "MINING TOOLS", "CHUNK ANCHOR", container numbers)
                                    // Must have EXACT SAME height (190) and smooth paint finish as panel so POM & normal maps
                                    // remain completely flat, sharp, and easy to read without depth cliffs or specular distortion!
                                    else if (isPaintedVehicle && lum >= 150 && sat < 0.25f) {
                                        metal = 0;      // Dielectric white paint, NOT bare chrome
                                        smooth = 210;   // Smooth enamel finish matching panel
                                        height = 190;   // Completely flat with surrounding panel (zero depth distortion)
                                        isText = true;
                                    }
                                    // 6. Bare Steel / Chrome / Polished Metal (low sat, bright)
                                    else if (!isPaintedVehicle && sat < 0.12f && lum >= 155) {
                                        metal = 230;
                                        smooth = 245;
                                        height = (byte)Math.Clamp((int)(190 + lum * 0.25f), 180, 250);
                                    }
                                    // 7. Dark Cast Iron / Chassis Sill / Frame (low sat, dark)
                                    else if (sat < 0.15f && lum < 120) {
                                        metal = 230;
                                        smooth = 185;
                                        height = (byte)Math.Clamp((int)(140 + lum * 0.4f), 90, 205);
                                    }
                                    // 8. Painted Steel Body (dielectric paint finish)
                                    else {
                                        metal = 0;
                                        smooth = 210;
                                        height = 190;
                                    }

                                    // Micro-surface noise for hyper realism (skipped on text for razor-sharp typography)
                                    float micro = isText ? 0f : (DrawUtils.Hash(x, y, 101) - 0.5f) * 0.03f;
                                    int cr = Math.Clamp((int)(r * (1f + micro)), 0, 255);
                                    int cg = Math.Clamp((int)(gCol * (1f + micro)), 0, 255);
                                    int cb = Math.Clamp((int)(b * (1f + micro)), 0, 255);

                                    c.SetPixel(x, y, Color.FromArgb(a, cr, cg, cb), height, smooth, metal, porous, emissive);
                                }
                            }
                        }
                        scaledBmp.UnlockBits(data);

                        // Procedural enhancements:
                        int S16 = 16;
                        // For all flatbed-based carts, ensure the silver flatbed deck with diamond plate & rivets is embossed:
                        if (fileNameNoExt.StartsWith("container_cart") || fileNameNoExt == "wood_cart" ||
                            fileNameNoExt == "logging_cart" || fileNameNoExt == "ore_cart" || fileNameNoExt == "ingot_cart") {
                            DrawUtils.DrawSilverFlatbedDeck(c, S16);
                        }

                        // For all hopper carts, ensure the lower frame is solid black and wheels match wood_cart:
                        if (fileNameNoExt.StartsWith("hopper_cart")) {
                            Color blackFrame = Color.FromArgb(255, 22, 24, 26);
                            DrawUtils.DrawBrushedBareMetal(c, 0, 0, 52 * S16, 8 * S16, blackFrame, 180, 210, true, 151);
                            DrawUtils.DrawBrushedBareMetal(c, 0, 10 * S16, 12 * S16, 6 * S16, blackFrame, 210, 220, true, 152);
                            DrawUtils.DrawBrushedBareMetal(c, 0, 18 * S16, 60 * S16, 11 * S16, blackFrame, 180, 210, true, 153);
                            DrawUtils.DrawBogieTruckSet128(c, S16);
                        }

                        // For other 128x128 carts with standard wheels at U=80, V=20:
                        if (w == 2048 && h == 2048 && !fileNameNoExt.Contains("caboose") && !fileNameNoExt.Contains("liquid") &&
                            !fileNameNoExt.Contains("diesel") && !fileNameNoExt.Contains("electric") &&
                            !fileNameNoExt.Contains("track_paver") && !fileNameNoExt.Contains("tunnel_bore")) {
                            int testIdx = (22 * S16) * w + (82 * S16);
                            if (c.Albedo[testIdx].A > 0) {
                                DrawUtils.DrawBogieTruckSet128(c, S16);
                            }
                        }

                        c.Save(Path.Combine(outDir, fileNameNoExt));
                    }
                }
            }
        }
    }
}
