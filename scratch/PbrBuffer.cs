using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;

public class PbrBuffer : IDisposable {
    public int Width { get; set; }
    public int Height { get; set; }
    public Color[,] Albedo;
    public byte[,] HeightMap;
    public byte[,] Smoothness;
    public byte[,] MetalId;
    public byte[,] Porosity;
    public byte[,] Emissive;

    public PbrBuffer(int w, int h) {
        Width = w;
        Height = h;
        Albedo = new Color[w, h];
        HeightMap = new byte[w, h];
        Smoothness = new byte[w, h];
        MetalId = new byte[w, h];
        Porosity = new byte[w, h];
        Emissive = new byte[w, h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Albedo[x, y] = Color.FromArgb(0, 0, 0, 0);
                HeightMap[x, y] = 128;
                Smoothness[x, y] = 60;
                MetalId[x, y] = 59; // default dielectric F0
                Porosity[x, y] = 0;
                Emissive[x, y] = 255; // 255 = emission OFF in LabPBR
            }
        }
    }

    public void SetPbr(int x, int y, Color color, byte h, byte smooth, byte metal, byte porous, byte emissive) {
        if (x < 0 || x >= Width || y < 0 || y >= Height) return;
        Albedo[x, y] = color;
        HeightMap[x, y] = h;
        Smoothness[x, y] = smooth;
        MetalId[x, y] = metal;
        Porosity[x, y] = porous;
        Emissive[x, y] = emissive;
    }

    public void FillRect(int rx, int ry, int rw, int rh, Color color, byte h, byte smooth, byte metal, byte porous, byte emissive) {
        int x2 = Math.Min(Width, rx + rw);
        int y2 = Math.Min(Height, ry + rh);
        int x1 = Math.Max(0, rx);
        int y1 = Math.Max(0, ry);

        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                SetPbr(x, y, color, h, smooth, metal, porous, emissive);
            }
        }
    }

    public void DrawBrushedSteel(int rx, int ry, int rw, int rh, Color baseColor, byte baseH, byte smooth, bool horizontal, int seed) {
        int x2 = Math.Min(Width, rx + rw);
        int y2 = Math.Min(Height, ry + rh);
        int x1 = Math.Max(0, rx);
        int y1 = Math.Max(0, ry);

        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                float streak = horizontal ? TextureUtils.Hash(y, 0, seed) : TextureUtils.Hash(x, 0, seed);
                float fine = TextureUtils.Hash(x, y, seed + 1);
                float noise = (streak * 0.7f + fine * 0.3f - 0.5f) * 0.12f;

                int r = TextureUtils.Clamp((int)(baseColor.R * (1f + noise)), 0, 255);
                int g = TextureUtils.Clamp((int)(baseColor.G * (1f + noise)), 0, 255);
                int b = TextureUtils.Clamp((int)(baseColor.B * (1f + noise)), 0, 255);

                byte h = (byte)TextureUtils.Clamp((int)(baseH + noise * 30f), 1, 255);
                byte sm = (byte)TextureUtils.Clamp((int)(smooth + noise * 40f), 0, 255);

                SetPbr(x, y, Color.FromArgb(baseColor.A, r, g, b), h, sm, 230 /* Iron metal */, 5, 255);
            }
        }
    }

    public void DrawRivets(int rx, int ry, int count, int spacing, bool horizontal, int radius) {
        for (int i = 0; i < count; i++) {
            int cx = horizontal ? rx + i * spacing : rx;
            int cy = horizontal ? ry : ry + i * spacing;

            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    float dist = (float)Math.Sqrt(dx * dx + dy * dy);
                    if (dist <= radius) {
                        float dome = (float)Math.Cos(dist / radius * Math.PI * 0.5f);
                        int px = cx + dx;
                        int py = cy + dy;
                        if (px >= 0 && px < Width && py >= 0 && py < Height) {
                            Color cur = Albedo[px, py];
                            int cr = TextureUtils.Clamp((int)(cur.R * (0.8f + dome * 0.4f)), 0, 255);
                            int cg = TextureUtils.Clamp((int)(cur.G * (0.8f + dome * 0.4f)), 0, 255);
                            int cb = TextureUtils.Clamp((int)(cur.B * (0.8f + dome * 0.4f)), 0, 255);
                            byte h = (byte)TextureUtils.Clamp((int)(HeightMap[px, py] + dome * 35f), 1, 255);
                            byte sm = (byte)TextureUtils.Clamp((int)(Smoothness[px, py] + dome * 40f), 0, 255);
                            SetPbr(px, py, Color.FromArgb(cur.A, cr, cg, cb), h, sm, 230, 0, 255);
                        }
                    }
                }
            }
        }
    }

    public void DrawHexBolt(int cx, int cy, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                float q2x = Math.Abs(dx);
                float q2y = Math.Abs(dy);
                if (q2x * 0.5f + q2y * 0.866f <= radius && q2x <= radius) {
                    int px = cx + dx;
                    int py = cy + dy;
                    if (px >= 0 && px < Width && py >= 0 && py < Height) {
                        Color cur = Albedo[px, py];
                        float bevel = (dx == -radius || dx == radius || dy == -radius || dy == radius) ? 0.7f : 1.2f;
                        int cr = TextureUtils.Clamp((int)(cur.R * bevel), 0, 255);
                        int cg = TextureUtils.Clamp((int)(cur.G * bevel), 0, 255);
                        int cb = TextureUtils.Clamp((int)(cur.B * bevel), 0, 255);
                        byte h = (byte)TextureUtils.Clamp((int)(HeightMap[px, py] + 28), 1, 255);
                        SetPbr(px, py, Color.FromArgb(cur.A, cr, cg, cb), h, 190, 230, 0, 255);
                    }
                }
            }
        }
    }

    public void DrawWoodPlanks(int rx, int ry, int rw, int rh, int plankHeight, Color woodColor, int seed) {
        int x2 = Math.Min(Width, rx + rw);
        int y2 = Math.Min(Height, ry + rh);
        int x1 = Math.Max(0, rx);
        int y1 = Math.Max(0, ry);

        for (int y = y1; y < y2; y++) {
            int plankIdx = (y - ry) / plankHeight;
            int plankLocalY = (y - ry) % plankHeight;
            bool isSeam = (plankLocalY == 0 || plankLocalY == plankHeight - 1);

            for (int x = x1; x < x2; x++) {
                if (isSeam) {
                    SetPbr(x, y, Color.FromArgb(255, 18, 14, 10), 60, 20, 59, 60, 255);
                    continue;
                }

                float grain = TextureUtils.SmoothNoise(x * 0.2f, y * 0.03f + plankIdx * 17f, seed);
                float fine = TextureUtils.Hash(x, y, seed + plankIdx);
                float variation = (grain * 0.75f + fine * 0.25f - 0.5f) * 0.35f;

                int r = TextureUtils.Clamp((int)(woodColor.R * (1f + variation)), 0, 255);
                int g = TextureUtils.Clamp((int)(woodColor.G * (1f + variation)), 0, 255);
                int b = TextureUtils.Clamp((int)(woodColor.B * (1f + variation)), 0, 255);

                byte h = (byte)TextureUtils.Clamp((int)(140 + variation * 50f), 80, 210);
                byte sm = (byte)TextureUtils.Clamp((int)(35 + variation * 20f), 10, 60);

                SetPbr(x, y, Color.FromArgb(255, r, g, b), h, sm, 59, 50, 255);
            }
        }
    }

    public void DrawCorrugatedSteel(int rx, int ry, int rw, int rh, int period, Color paintColor, int seed) {
        int x2 = Math.Min(Width, rx + rw);
        int y2 = Math.Min(Height, ry + rh);
        int x1 = Math.Max(0, rx);
        int y1 = Math.Max(0, ry);

        for (int x = x1; x < x2; x++) {
            float phase = ((x - rx) % period) / (float)period;
            float wave = (float)Math.Sin(phase * Math.PI * 2.0);

            for (int y = y1; y < y2; y++) {
                float n = (TextureUtils.Hash(x, y, seed) - 0.5f) * 0.08f;
                float light = 1f + wave * 0.22f + n;

                int r = TextureUtils.Clamp((int)(paintColor.R * light), 0, 255);
                int g = TextureUtils.Clamp((int)(paintColor.G * light), 0, 255);
                int b = TextureUtils.Clamp((int)(paintColor.B * light), 0, 255);

                byte h = (byte)TextureUtils.Clamp((int)(160 + wave * 75f), 10, 250);
                byte sm = (byte)TextureUtils.Clamp((int)(150 + wave * 30f), 80, 210);

                SetPbr(x, y, Color.FromArgb(255, r, g, b), h, sm, 60, 5, 255);
            }
        }
    }

    public void DrawDiamondTread(int rx, int ry, int rw, int rh, Color metalColor, int seed) {
        int x2 = Math.Min(Width, rx + rw);
        int y2 = Math.Min(Height, ry + rh);
        int x1 = Math.Max(0, rx);
        int y1 = Math.Max(0, ry);

        int grid = 12;
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                int gx = (x - rx) % grid;
                int gy = (y - ry) % grid;
                bool isDiamond = (Math.Abs(gx - grid / 2) + Math.Abs(gy - grid / 2)) <= (grid / 4);

                float n = (TextureUtils.Hash(x, y, seed) - 0.5f) * 0.1f;
                float factor = isDiamond ? (1.3f + n) : (0.85f + n);

                int r = TextureUtils.Clamp((int)(metalColor.R * factor), 0, 255);
                int g = TextureUtils.Clamp((int)(metalColor.G * factor), 0, 255);
                int b = TextureUtils.Clamp((int)(metalColor.B * factor), 0, 255);

                byte h = (byte)(isDiamond ? 220 : 140);
                byte sm = (byte)(isDiamond ? 190 : 120);

                SetPbr(x, y, Color.FromArgb(255, r, g, b), h, sm, 230, 10, 255);
            }
        }
    }

    public void DrawLouverGrille(int rx, int ry, int rw, int rh, int slatHeight, Color metalColor) {
        int x2 = Math.Min(Width, rx + rw);
        int y2 = Math.Min(Height, ry + rh);
        int x1 = Math.Max(0, rx);
        int y1 = Math.Max(0, ry);

        for (int y = y1; y < y2; y++) {
            int localY = (y - ry) % slatHeight;
            float t = localY / (float)slatHeight;
            float factor = (t < 0.2f) ? 0.3f : (0.7f + t * 0.5f);

            for (int x = x1; x < x2; x++) {
                int r = TextureUtils.Clamp((int)(metalColor.R * factor), 0, 255);
                int g = TextureUtils.Clamp((int)(metalColor.G * factor), 0, 255);
                int b = TextureUtils.Clamp((int)(metalColor.B * factor), 0, 255);

                byte h = (byte)TextureUtils.Clamp((int)(80 + t * 140f), 20, 240);
                byte sm = 150;

                SetPbr(x, y, Color.FromArgb(255, r, g, b), h, sm, 230, 0, 255);
            }
        }
    }

    public void DrawGlassPane(int rx, int ry, int rw, int rh, Color tint) {
        int x2 = Math.Min(Width, rx + rw);
        int y2 = Math.Min(Height, ry + rh);
        int x1 = Math.Max(0, rx);
        int y1 = Math.Max(0, ry);

        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                bool isBorder = (x - rx < 2 || x2 - 1 - x < 2 || y - ry < 2 || y2 - 1 - y < 2);
                if (isBorder) {
                    SetPbr(x, y, Color.FromArgb(255, 30, 32, 35), 180, 50, 59, 10, 255);
                } else {
                    float glare = ((x - rx) + (y - ry)) / (float)(rw + rh);
                    int r = TextureUtils.Clamp((int)(tint.R + glare * 35f), 0, 255);
                    int g = TextureUtils.Clamp((int)(tint.G + glare * 45f), 0, 255);
                    int b = TextureUtils.Clamp((int)(tint.B + glare * 55f), 0, 255);

                    SetPbr(x, y, Color.FromArgb(tint.A, r, g, b), 200, 248, 60, 0, 255);
                }
            }
        }
    }

    public void DrawEmissiveLamp(int cx, int cy, int radius, Color glowColor) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                float dist = (float)Math.Sqrt(dx * dx + dy * dy);
                if (dist <= radius) {
                    int px = cx + dx;
                    int py = cy + dy;
                    if (px >= 0 && px < Width && py >= 0 && py < Height) {
                        float intensity = 1.0f - (dist / radius);
                        intensity = intensity * intensity;

                        int r = TextureUtils.Clamp((int)(glowColor.R * 0.7f + 255f * 0.3f * intensity), 0, 255);
                        int g = TextureUtils.Clamp((int)(glowColor.G * 0.7f + 255f * 0.3f * intensity), 0, 255);
                        int b = TextureUtils.Clamp((int)(glowColor.B * 0.7f + 255f * 0.3f * intensity), 0, 255);

                        byte emissiveVal = (byte)TextureUtils.Clamp((int)((1.0f - intensity) * 80f), 0, 254);
                        byte h = (byte)(210 + intensity * 40f);

                        SetPbr(px, py, Color.FromArgb(255, r, g, b), h, 250, 60, 0, emissiveVal);
                    }
                }
            }
        }
    }

    public void DrawAggregateLumps(int rx, int ry, int rw, int rh, Color baseColor, byte baseSmooth, byte metalId, int seed) {
        int x2 = Math.Min(Width, rx + rw);
        int y2 = Math.Min(Height, ry + rh);
        int x1 = Math.Max(0, rx);
        int y1 = Math.Max(0, ry);

        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                float n1 = TextureUtils.SmoothNoise(x * 0.15f, y * 0.15f, seed);
                float n2 = TextureUtils.SmoothNoise(x * 0.4f, y * 0.4f, seed + 1);
                float facet = (n1 * 0.6f + n2 * 0.4f);

                float specN = TextureUtils.Hash(x, y, seed + 2);
                bool hasSparkle = (metalId != 59) && (specN > 0.88f);

                int r = TextureUtils.Clamp((int)(baseColor.R * (0.6f + facet * 0.8f)), 0, 255);
                int g = TextureUtils.Clamp((int)(baseColor.G * (0.6f + facet * 0.8f)), 0, 255);
                int b = TextureUtils.Clamp((int)(baseColor.B * (0.6f + facet * 0.8f)), 0, 255);

                if (hasSparkle) {
                    r = Math.Min(255, r + 90);
                    g = Math.Min(255, g + 80);
                    b = Math.Min(255, b + 40);
                }

                byte h = (byte)TextureUtils.Clamp((int)(110 + facet * 130f), 30, 250);
                byte sm = (byte)(hasSparkle ? 220 : TextureUtils.Clamp((int)(baseSmooth + (facet - 0.5f) * 40f), 10, 180));
                byte m = (byte)(hasSparkle ? metalId : 59);

                SetPbr(x, y, Color.FromArgb(255, r, g, b), h, sm, m, 40, 255);
            }
        }
    }

    public void DrawTreeBark(int rx, int ry, int rw, int rh, Color barkColor, int seed) {
        int x2 = Math.Min(Width, rx + rw);
        int y2 = Math.Min(Height, ry + rh);
        int x1 = Math.Max(0, rx);
        int y1 = Math.Max(0, ry);

        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                float furrow = (float)Math.Sin(x * 0.35f + TextureUtils.SmoothNoise(x * 0.1f, y * 0.05f, seed) * 4.0f);
                float n = TextureUtils.SmoothNoise(x * 0.2f, y * 0.1f, seed + 1);
                float light = 0.8f + furrow * 0.25f + n * 0.2f;

                int r = TextureUtils.Clamp((int)(barkColor.R * light), 0, 255);
                int g = TextureUtils.Clamp((int)(barkColor.G * light), 0, 255);
                int b = TextureUtils.Clamp((int)(barkColor.B * light), 0, 255);

                byte h = (byte)TextureUtils.Clamp((int)(150 + furrow * 70f + n * 30f), 20, 245);
                SetPbr(x, y, Color.FromArgb(255, r, g, b), h, 30, 59, 55, 255);
            }
        }
    }

    public void DrawTreeLogRings(int cx, int cy, int radius, Color woodColor, int seed) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                float dist = (float)Math.Sqrt(dx * dx + dy * dy);
                if (dist <= radius) {
                    int px = cx + dx;
                    int py = cy + dy;
                    if (px >= 0 && px < Width && py >= 0 && py < Height) {
                        float ring = (float)Math.Sin(dist * 0.7f + TextureUtils.SmoothNoise(px * 0.1f, py * 0.1f, seed) * 1.5f);
                        float angle = (float)Math.Atan2(dy, dx);
                        float crack = (float)Math.Sin(angle * 5f + TextureUtils.Hash((int)(dist * 0.5f), 0, seed) * 2f);
                        bool isRadialCrack = (crack > 0.94f && dist > radius * 0.25f);

                        if (isRadialCrack) {
                            SetPbr(px, py, Color.FromArgb(255, 25, 18, 12), 40, 20, 59, 60, 255);
                        } else {
                            float factor = 0.85f + ring * 0.18f;
                            int r = TextureUtils.Clamp((int)(woodColor.R * factor), 0, 255);
                            int g = TextureUtils.Clamp((int)(woodColor.G * factor), 0, 255);
                            int b = TextureUtils.Clamp((int)(woodColor.B * factor), 0, 255);

                            byte h = (byte)TextureUtils.Clamp((int)(160 + ring * 35f), 80, 215);
                            SetPbr(px, py, Color.FromArgb(255, r, g, b), h, 40, 59, 45, 255);
                        }
                    }
                }
            }
        }
    }

    public void UpscaleFromSource(Bitmap src) {
        float scaleX = (float)src.Width / Width;
        float scaleY = (float)src.Height / Height;

        for (int y = 0; y < Height; y++) {
            int srcY = Math.Min(src.Height - 1, (int)(y * scaleY));
            for (int x = 0; x < Width; x++) {
                int srcX = Math.Min(src.Width - 1, (int)(x * scaleX));
                Color sc = src.GetPixel(srcX, srcY);
                if (sc.A == 0) continue;

                float fineNoise = (TextureUtils.Hash(x, y, 999) - 0.5f) * 0.06f;
                int r = TextureUtils.Clamp((int)(sc.R * (1f + fineNoise)), 0, 255);
                int g = TextureUtils.Clamp((int)(sc.G * (1f + fineNoise)), 0, 255);
                int b = TextureUtils.Clamp((int)(sc.B * (1f + fineNoise)), 0, 255);

                float lum = (r * 0.299f + g * 0.587f + b * 0.114f) / 255f;
                bool isGray = Math.Abs(r - g) < 22 && Math.Abs(g - b) < 22;
                bool isYellow = r > 160 && g > 130 && b < 80;
                bool isBrightRed = r > 150 && g < 70 && b < 70;
                bool isBrightGreen = g > 150 && r < 70 && b < 70;
                bool isCyanGlow = b > 180 && g > 150 && r < 100;

                byte h = (byte)TextureUtils.Clamp((int)(90 + lum * 140f + fineNoise * 30f), 20, 245);
                byte sm = (byte)(isGray ? 160 : 130);
                byte m = (byte)(isGray ? 230 /* Iron */ : 59 /* Dielectric */);
                byte em = 255;

                if (isYellow && lum > 0.7f) {
                    em = 10;
                    sm = 240;
                } else if (isBrightRed && lum > 0.45f) {
                    em = 15;
                    sm = 240;
                } else if (isBrightGreen && lum > 0.45f) {
                    em = 15;
                    sm = 240;
                } else if (isCyanGlow) {
                    em = 0;
                    sm = 255;
                }

                SetPbr(x, y, Color.FromArgb(sc.A, r, g, b), h, sm, m, 5, em);
            }
        }
    }

    public void Save(string basePath) {
        string dir = Path.GetDirectoryName(basePath);
        if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir)) {
            Directory.CreateDirectory(dir);
        }

        // 1. Albedo
        using (Bitmap bmpA = new Bitmap(Width, Height, PixelFormat.Format32bppArgb)) {
            for (int y = 0; y < Height; y++) {
                for (int x = 0; x < Width; x++) {
                    bmpA.SetPixel(x, y, Albedo[x, y]);
                }
            }
            bmpA.Save(basePath + ".png", ImageFormat.Png);
        }

        // 2. Normal + AO + POM Height
        using (Bitmap bmpN = new Bitmap(Width, Height, PixelFormat.Format32bppArgb)) {
            for (int y = 0; y < Height; y++) {
                for (int x = 0; x < Width; x++) {
                    int x0 = Math.Max(0, x - 1);
                    int x1 = Math.Min(Width - 1, x + 1);
                    int y0 = Math.Max(0, y - 1);
                    int y1 = Math.Min(Height - 1, y + 1);

                    float hL = HeightMap[x0, y];
                    float hR = HeightMap[x1, y];
                    float hU = HeightMap[x, y0];
                    float hD = HeightMap[x, y1];

                    float dx = (hR - hL) * 0.05f;
                    float dy = (hD - hU) * 0.05f;
                    float dz = 1.0f;

                    float len = (float)Math.Sqrt(dx * dx + dy * dy + dz * dz);
                    float nx = dx / len;
                    float ny = dy / len;

                    int r = Math.Max(0, Math.Min(255, (int)((nx * 0.5f + 0.5f) * 255f)));
                    int g = Math.Max(0, Math.Min(255, (int)((ny * 0.5f + 0.5f) * 255f)));

                    float laplacian = (hL + hR + hU + hD - 4f * HeightMap[x, y]);
                    float aoVal = 255f + laplacian * 0.9f;
                    int b = Math.Max(35, Math.Min(255, (int)aoVal));

                    int a = Math.Max(1, (int)HeightMap[x, y]);

                    if (Albedo[x, y].A == 0) {
                        bmpN.SetPixel(x, y, Color.FromArgb(0, 128, 128, 255));
                    } else {
                        bmpN.SetPixel(x, y, Color.FromArgb(a, r, g, b));
                    }
                }
            }
            bmpN.Save(basePath + "_n.png", ImageFormat.Png);
        }

        // 3. Specular Map (R = Smoothness, G = Metal ID, B = Porosity, A = Emissive)
        using (Bitmap bmpS = new Bitmap(Width, Height, PixelFormat.Format32bppArgb)) {
            for (int y = 0; y < Height; y++) {
                for (int x = 0; x < Width; x++) {
                    if (Albedo[x, y].A == 0) {
                        bmpS.SetPixel(x, y, Color.FromArgb(0, 0, 0, 0));
                    } else {
                        bmpS.SetPixel(x, y, Color.FromArgb(
                            Emissive[x, y],
                            Smoothness[x, y],
                            MetalId[x, y],
                            Porosity[x, y]
                        ));
                    }
                }
            }
            bmpS.Save(basePath + "_s.png", ImageFormat.Png);
        }
    }

    public void Dispose() {}
}
