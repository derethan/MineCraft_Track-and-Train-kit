using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Drawing.Text;

namespace HdTexturePackGenerator {
    public static class DrawUtils {
        // Fast deterministic pseudo-random hash
        public static float Hash(int x, int y, int seed) {
            int n = x + y * 57 + seed * 131;
            n = (n << 13) ^ n;
            return ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 2147483648.0f;
        }

        public static float Noise(float x, float y, int seed) {
            int ix = (int)Math.Floor(x);
            int iy = (int)Math.Floor(y);
            float fx = x - ix;
            float fy = y - iy;
            float s = fx * fx * (3f - 2f * fx);
            float t = fy * fy * (3f - 2f * fy);
            float h00 = Hash(ix, iy, seed);
            float h10 = Hash(ix + 1, iy, seed);
            float h01 = Hash(ix, iy + 1, seed);
            float h11 = Hash(ix + 1, iy + 1, seed);
            return (h00 * (1f - s) + h10 * s) * (1f - t) + (h01 * (1f - s) + h11 * s) * t;
        }

        public static int Clamp(int val, int min, int max) => Math.Clamp(val, min, max);

        // 1. Heavy Beveled Painted/Coated Steel Panel
        public static void DrawBeveledPanel(PbrCanvas c, int rx, int ry, int rw, int rh, Color paintColor, byte baseH = 190, byte smooth = 200, byte metal = 0, int bevel = 4, int seed = 101) {
            int x1 = Math.Max(0, rx);
            int y1 = Math.Max(0, ry);
            int x2 = Math.Min(c.Width, rx + rw);
            int y2 = Math.Min(c.Height, ry + rh);

            for (int y = y1; y < y2; y++) {
                int distY = Math.Min(y - ry, (ry + rh - 1) - y);
                for (int x = x1; x < x2; x++) {
                    int distX = Math.Min(x - rx, (rx + rw - 1) - x);
                    int edgeDist = Math.Min(distX, distY);

                    // Micro enamel texture
                    float micro = (Hash(x, y, seed) - 0.5f) * 0.05f;

                    if (edgeDist < bevel) {
                        // Sloped bevel edge
                        float t = (float)edgeDist / bevel;
                        float shade = (x - rx < bevel || y - ry < bevel) ? 1.15f : 0.75f;
                        int cr = Clamp((int)(paintColor.R * shade * (1f + micro)), 0, 255);
                        int cg = Clamp((int)(paintColor.G * shade * (1f + micro)), 0, 255);
                        int cb = Clamp((int)(paintColor.B * shade * (1f + micro)), 0, 255);
                        byte h = (byte)Clamp((int)(baseH - (1f - t) * 60f), 20, 255);
                        c.SetPixel(x, y, Color.FromArgb(255, cr, cg, cb), h, (byte)(smooth - 20), metal, 5);
                    } else {
                        // Flat panel face with subtle metallic / enamel highlights
                        int cr = Clamp((int)(paintColor.R * (1f + micro)), 0, 255);
                        int cg = Clamp((int)(paintColor.G * (1f + micro)), 0, 255);
                        int cb = Clamp((int)(paintColor.B * (1f + micro)), 0, 255);
                        c.SetPixel(x, y, Color.FromArgb(255, cr, cg, cb), baseH, smooth, metal, 5);
                    }
                }
            }
        }

        // 2. Brushed Bare Steel / Cast Iron (Metal ID 230)
        public static void DrawBrushedBareMetal(PbrCanvas c, int rx, int ry, int rw, int rh, Color baseColor, byte baseH = 200, byte smooth = 235, bool horizontal = true, int seed = 202) {
            int x1 = Math.Max(0, rx);
            int y1 = Math.Max(0, ry);
            int x2 = Math.Min(c.Width, rx + rw);
            int y2 = Math.Min(c.Height, ry + rh);

            for (int y = y1; y < y2; y++) {
                for (int x = x1; x < x2; x++) {
                    float streak = horizontal ? Hash(0, y, seed) : Hash(x, 0, seed);
                    float fine = Hash(x, y, seed + 1);
                    float var = (streak * 0.7f + fine * 0.3f - 0.5f) * 0.14f;

                    int cr = Clamp((int)(baseColor.R * (1f + var)), 0, 255);
                    int cg = Clamp((int)(baseColor.G * (1f + var)), 0, 255);
                    int cb = Clamp((int)(baseColor.B * (1f + var)), 0, 255);

                    byte h = (byte)Clamp((int)(baseH + var * 25f), 10, 255);
                    byte sm = (byte)Clamp((int)(smooth + var * 30f), 100, 255);

                    c.SetPixel(x, y, Color.FromArgb(255, cr, cg, cb), h, sm, 230 /* Iron/Steel */, 2);
                }
            }
        }

        // 3. Spherical Dome Rivets with full 3D POM extrusion
        public static void DrawRivets(PbrCanvas c, int cx, int cy, int count, int spacing, bool horizontal, int radius = 6) {
            for (int i = 0; i < count; i++) {
                int px = horizontal ? cx + i * spacing : cx;
                int py = horizontal ? cy : cy + i * spacing;

                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        float dist = (float)Math.Sqrt(dx * dx + dy * dy);
                        if (dist <= radius) {
                            float dome = (float)Math.Cos(dist / radius * Math.PI * 0.5);
                            int tx = px + dx;
                            int ty = py + dy;
                            if (tx >= 0 && tx < c.Width && ty >= 0 && ty < c.Height) {
                                int idx = ty * c.Width + tx;
                                Color cur = c.Albedo[idx];
                                float light = 0.8f + (dx - dy) * 0.15f / radius + dome * 0.4f;
                                int cr = Clamp((int)(cur.R * light), 0, 255);
                                int cg = Clamp((int)(cur.G * light), 0, 255);
                                int cb = Clamp((int)(cur.B * light), 0, 255);

                                byte h = (byte)Clamp((int)(c.HeightMap[idx] + dome * 50f), 20, 255);
                                byte sm = (byte)Clamp((int)(c.Smoothness[idx] + dome * 35f), 80, 252);
                                c.SetPixel(tx, ty, Color.FromArgb(255, cr, cg, cb), h, sm, 230, 0);
                            }
                        }
                    }
                }
            }
        }

        // 4. Hexagonal Machine Bolts
        public static void DrawHexBolt(PbrCanvas c, int cx, int cy, int radius = 8) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    float q2x = Math.Abs(dx);
                    float q2y = Math.Abs(dy);
                    if (q2x * 0.5f + q2y * 0.866f <= radius && q2x <= radius) {
                        int tx = cx + dx;
                        int ty = cy + dy;
                        if (tx >= 0 && tx < c.Width && ty >= 0 && ty < c.Height) {
                            int idx = ty * c.Width + tx;
                            Color cur = c.Albedo[idx];
                            bool isEdge = (Math.Abs(dx) >= radius - 1 || Math.Abs(dy) >= radius - 1);
                            float shade = isEdge ? 0.65f : 1.25f;
                            int cr = Clamp((int)(cur.R * shade), 0, 255);
                            int cg = Clamp((int)(cur.G * shade), 0, 255);
                            int cb = Clamp((int)(cur.B * shade), 0, 255);

                            byte h = (byte)Clamp((int)(c.HeightMap[idx] + (isEdge ? 20 : 45)), 20, 255);
                            c.SetPixel(tx, ty, Color.FromArgb(255, cr, cg, cb), h, 210, 230, 0);
                        }
                    }
                }
            }
        }

        // 5. Corrugated Shipping Container Steel (Trapezoidal waves)
        public static void DrawCorrugatedPanels(PbrCanvas c, int rx, int ry, int rw, int rh, Color paintColor, int wavePeriod = 32, int seed = 303) {
            int x1 = Math.Max(0, rx);
            int y1 = Math.Max(0, ry);
            int x2 = Math.Min(c.Width, rx + rw);
            int y2 = Math.Min(c.Height, ry + rh);

            for (int x = x1; x < x2; x++) {
                float phase = ((x - rx) % wavePeriod) / (float)wavePeriod;
                // Trapezoidal wave profile: -1 to +1
                float wave;
                if (phase < 0.25f) wave = -1f + phase * 8f; // up slope
                else if (phase < 0.5f) wave = 1f;           // top flat
                else if (phase < 0.75f) wave = 1f - (phase - 0.5f) * 8f; // down slope
                else wave = -1f;                             // bottom flat

                for (int y = y1; y < y2; y++) {
                    float micro = (Hash(x, y, seed) - 0.5f) * 0.06f;
                    float light = 1.0f + wave * 0.20f + micro;

                    int cr = Clamp((int)(paintColor.R * light), 0, 255);
                    int cg = Clamp((int)(paintColor.G * light), 0, 255);
                    int cb = Clamp((int)(paintColor.B * light), 0, 255);

                    byte h = (byte)Clamp((int)(160 + wave * 80f), 35, 250);
                    byte sm = (byte)Clamp((int)(195 + wave * 25f), 140, 225);

                    c.SetPixel(x, y, Color.FromArgb(255, cr, cg, cb), h, sm, 0, 5);
                }
            }
        }

        // 6. Diamond Tread Walkway Plate
        public static void DrawDiamondTread(PbrCanvas c, int rx, int ry, int rw, int rh, Color metalColor, int gridSize = 16, int seed = 404) {
            int x1 = Math.Max(0, rx);
            int y1 = Math.Max(0, ry);
            int x2 = Math.Min(c.Width, rx + rw);
            int y2 = Math.Min(c.Height, ry + rh);

            int halfGrid = gridSize / 2;
            for (int y = y1; y < y2; y++) {
                int gy = (y - ry) % gridSize;
                for (int x = x1; x < x2; x++) {
                    int gx = (x - rx) % gridSize;
                    bool isDiamond = (Math.Abs(gx - halfGrid) + Math.Abs(gy - halfGrid)) <= (gridSize / 4);

                    float n = (Hash(x, y, seed) - 0.5f) * 0.08f;
                    float factor = isDiamond ? (1.35f + n) : (0.8f + n);

                    int cr = Clamp((int)(metalColor.R * factor), 0, 255);
                    int cg = Clamp((int)(metalColor.G * factor), 0, 255);
                    int cb = Clamp((int)(metalColor.B * factor), 0, 255);

                    byte h = (byte)(isDiamond ? 245 : 120);
                    byte sm = (byte)(isDiamond ? 220 : 130);

                    c.SetPixel(x, y, Color.FromArgb(255, cr, cg, cb), h, sm, 230, 8);
                }
            }
        }

        // 7. Angled Cooling Louver Vents
        public static void DrawLouvers(PbrCanvas c, int rx, int ry, int rw, int rh, Color metalColor, int slatHeight = 12) {
            int x1 = Math.Max(0, rx);
            int y1 = Math.Max(0, ry);
            int x2 = Math.Min(c.Width, rx + rw);
            int y2 = Math.Min(c.Height, ry + rh);

            for (int y = y1; y < y2; y++) {
                int localY = (y - ry) % slatHeight;
                float t = localY / (float)slatHeight;
                // Deep cut at the top of the slat, slope down to bottom
                float factor = (t < 0.25f) ? 0.35f : (0.75f + t * 0.45f);
                byte h = (byte)Clamp((int)(40 + t * 180f), 30, 245);

                for (int x = x1; x < x2; x++) {
                    int cr = Clamp((int)(metalColor.R * factor), 0, 255);
                    int cg = Clamp((int)(metalColor.G * factor), 0, 255);
                    int cb = Clamp((int)(metalColor.B * factor), 0, 255);

                    c.SetPixel(x, y, Color.FromArgb(255, cr, cg, cb), h, 160, 230, 2);
                }
            }
        }

        // 8. Tongue-and-Groove Wooden Planks (Deep wood grain, split checking & joints)
        public static void DrawWoodPlanks(PbrCanvas c, int rx, int ry, int rw, int rh, Color woodColor, int plankHeight = 32, bool horizontal = true, int seed = 505) {
            int x1 = Math.Max(0, rx);
            int y1 = Math.Max(0, ry);
            int x2 = Math.Min(c.Width, rx + rw);
            int y2 = Math.Min(c.Height, ry + rh);

            for (int y = y1; y < y2; y++) {
                for (int x = x1; x < x2; x++) {
                    int coord1 = horizontal ? (y - ry) : (x - rx);
                    int coord2 = horizontal ? (x - rx) : (y - ry);
                    int pIdx = coord1 / plankHeight;
                    int pOffset = coord1 % plankHeight;
                    bool isSeam = (pOffset == 0 || pOffset == plankHeight - 1);
                    if (isSeam) {
                        // Deep gap seam between planks
                        c.SetPixel(x, y, Color.FromArgb(255, 20, 16, 12), 35, 20, 0, 70);
                        continue;
                    }

                    float fiber = Noise(coord2 * 0.12f, coord1 * 0.02f + pIdx * 13f, seed);
                    float fine = Hash(x, y, seed + pIdx);
                    float var = (fiber * 0.7f + fine * 0.3f - 0.5f) * 0.35f;

                    int cr = Clamp((int)(woodColor.R * (1f + var)), 0, 255);
                    int cg = Clamp((int)(woodColor.G * (1f + var)), 0, 255);
                    int cb = Clamp((int)(woodColor.B * (1f + var)), 0, 255);

                    byte h = (byte)Clamp((int)(165 + var * 50f), 70, 220);
                    byte sm = (byte)Clamp((int)(40 + var * 20f), 15, 75);

                    c.SetPixel(x, y, Color.FromArgb(255, cr, cg, cb), h, sm, 0, 55);
                }
            }
        }

        // 9. Glass Windshield / Cab Window (Reflective glass + rubber seal)
        public static void DrawGlassWindow(PbrCanvas c, int rx, int ry, int rw, int rh, Color glassColor, int borderSize = 4) {
            int x1 = Math.Max(0, rx);
            int y1 = Math.Max(0, ry);
            int x2 = Math.Min(c.Width, rx + rw);
            int y2 = Math.Min(c.Height, ry + rh);

            for (int y = y1; y < y2; y++) {
                bool borderY = (y - ry < borderSize) || ((ry + rh - 1) - y < borderSize);
                for (int x = x1; x < x2; x++) {
                    bool borderX = (x - rx < borderSize) || ((rx + rw - 1) - x < borderSize);
                    if (borderX || borderY) {
                        // Black rubber weather gasket seal
                        c.SetPixel(x, y, Color.FromArgb(255, 22, 22, 24), 160, 40, 0, 20);
                    } else {
                        // High gloss mirror-smooth cab glass pane
                        float diag = (float)(x - rx + y - ry) / (rw + rh);
                        float reflectionStreak = (float)Math.Sin(diag * Math.PI * 3.0) * 0.12f;
                        int cr = Clamp((int)(glassColor.R * (1f + reflectionStreak)), 0, 255);
                        int cg = Clamp((int)(glassColor.G * (1f + reflectionStreak)), 0, 255);
                        int cb = Clamp((int)(glassColor.B * (1f + reflectionStreak)), 0, 255);

                        c.SetPixel(x, y, Color.FromArgb(255, cr, cg, cb), 150, 252 /* max glass gloss */, 0, 0);
                    }
                }
            }
        }

        // 10. Emissive Lamp / Lens (Headlight / Signal / Tail Lamp)
        public static void DrawEmissiveLamp(PbrCanvas c, int cx, int cy, int radius, Color glowColor, byte emissiveVal = 0) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    float dist = (float)Math.Sqrt(dx * dx + dy * dy);
                    if (dist <= radius) {
                        int px = cx + dx;
                        int py = cy + dy;
                        if (px >= 0 && px < c.Width && py >= 0 && py < c.Height) {
                            float t = dist / radius;
                            float core = 1f - t * t;
                            if (dist > radius - 2) {
                                // Brass / Chrome reflector ring rim
                                c.SetPixel(px, py, Color.FromArgb(255, 180, 180, 185), 230, 245, 233, 0, 255);
                            } else {
                                // Radiant emissive lens
                                int cr = Clamp((int)(glowColor.R * (0.8f + core * 0.4f)), 0, 255);
                                int cg = Clamp((int)(glowColor.G * (0.8f + core * 0.4f)), 0, 255);
                                int cb = Clamp((int)(glowColor.B * (0.8f + core * 0.4f)), 0, 255);
                                byte em = (byte)Math.Clamp((int)(emissiveVal + (1f - core) * 15f), 0, 20);
                                c.SetPixel(px, py, Color.FromArgb(255, cr, cg, cb), 245, 254, 0, 0, em);
                            }
                        }
                    }
                }
            }
        }

        // 11. Stenciled Railroad Identification Decal Text
        public static void DrawDecalText(PbrCanvas c, string text, int x, int y, int fontSize, Color textColor, bool vertical = false) {
            using (Bitmap temp = new Bitmap(c.Width, c.Height, PixelFormat.Format32bppArgb))
            using (Graphics g = Graphics.FromImage(temp)) {
                g.TextRenderingHint = TextRenderingHint.AntiAliasGridFit;
                using (Font font = new Font(FontFamily.GenericSansSerif, fontSize, FontStyle.Bold, GraphicsUnit.Pixel))
                using (SolidBrush brush = new SolidBrush(textColor)) {
                    if (vertical) {
                        g.TranslateTransform(x, y);
                        g.RotateTransform(90);
                        g.DrawString(text, font, brush, 0, 0);
                    } else {
                        g.DrawString(text, font, brush, x, y);
                    }
                }

                for (int py = 0; py < c.Height; py++) {
                    for (int px = 0; px < c.Width; px++) {
                        Color tc = temp.GetPixel(px, py);
                        if (tc.A > 30) {
                            int idx = py * c.Width + px;
                            Color cur = c.Albedo[idx];
                            float alpha = tc.A / 255f;
                            int nr = (int)(cur.R * (1f - alpha) + tc.R * alpha);
                            int ng = (int)(cur.G * (1f - alpha) + tc.G * alpha);
                            int nb = (int)(cur.B * (1f - alpha) + tc.B * alpha);
                            // Decals have crisp painted texture with slight raised height
                            byte h = (byte)Clamp((int)(c.HeightMap[idx] + 8), 10, 255);
                            c.SetPixel(px, py, Color.FromArgb(255, nr, ng, nb), h, 195, c.MetalId[idx], c.Porosity[idx], c.Emissive[idx]);
                        }
                    }
                }
            }
        }

        // 12. Loose Aggregate Deposits (Coal, Ore, Gravel, Sand, Dirt)
        public static void DrawAggregates(PbrCanvas c, int rx, int ry, int rw, int rh, string type, int seed = 606) {
            int x1 = Math.Max(0, rx);
            int y1 = Math.Max(0, ry);
            int x2 = Math.Min(c.Width, rx + rw);
            int y2 = Math.Min(c.Height, ry + rh);

            for (int y = y1; y < y2; y++) {
                for (int x = x1; x < x2; x++) {
                    switch (type) {
                        case "coal": {
                            // Glossy faceted anthracite coal lumps
                            float facet = Hash(x / 12, y / 12, seed);
                            float subFacet = Hash(x / 4, y / 4, seed + 1);
                            float shade = 0.08f + facet * 0.14f + subFacet * 0.06f;
                            int val = Clamp((int)(shade * 255f), 15, 65);
                            byte h = (byte)Clamp((int)(110 + facet * 110f + subFacet * 35f), 20, 255);
                            byte sm = (byte)Clamp((int)(120 + facet * 70f), 60, 200);
                            c.SetPixel(x, y, Color.FromArgb(255, val, val, val + 2), h, sm, 0, 30);
                            break;
                        }
                        case "ore": {
                            // Raw iron ore chunk with sparkling hematite veins
                            float rock = Hash(x / 14, y / 14, seed);
                            float vein = Hash(x / 3, y / 3, seed + 2);
                            bool isSparkle = (vein > 0.88f);
                            if (isSparkle) {
                                c.SetPixel(x, y, Color.FromArgb(255, 215, 190, 160), 245, 240, 230 /* Iron metal */, 5);
                            } else {
                                int r = Clamp((int)(95 * (0.8f + rock * 0.4f)), 0, 255);
                                int g = Clamp((int)(75 * (0.8f + rock * 0.4f)), 0, 255);
                                int b = Clamp((int)(68 * (0.8f + rock * 0.4f)), 0, 255);
                                byte h = (byte)Clamp((int)(100 + rock * 120f), 20, 245);
                                c.SetPixel(x, y, Color.FromArgb(255, r, g, b), h, 60, 0, 45);
                            }
                            break;
                        }
                        case "gravel": {
                            // Angular granite ballast pebbles
                            float pebble = Hash(x / 8, y / 8, seed);
                            float tone = 0.35f + pebble * 0.35f;
                            int val = Clamp((int)(tone * 255f), 70, 190);
                            byte h = (byte)Clamp((int)(90 + pebble * 130f), 20, 245);
                            c.SetPixel(x, y, Color.FromArgb(255, val, (int)(val * 0.96f), (int)(val * 0.92f)), h, 50, 0, 50);
                            break;
                        }
                        case "sand": {
                            // Fine silica sand
                            float dune = (float)Math.Sin(x * 0.05f + y * 0.02f) * 0.1f;
                            float grain = (Hash(x, y, seed) - 0.5f) * 0.08f;
                            int r = Clamp((int)(215 * (1f + dune + grain)), 0, 255);
                            int g = Clamp((int)(190 * (1f + dune + grain)), 0, 255);
                            int b = Clamp((int)(135 * (1f + dune + grain)), 0, 255);
                            byte h = (byte)Clamp((int)(150 + dune * 60f + grain * 20f), 80, 220);
                            c.SetPixel(x, y, Color.FromArgb(255, r, g, b), h, 30, 0, 75);
                            break;
                        }
                        case "red_sand": {
                            float dune = (float)Math.Sin(x * 0.05f + y * 0.02f) * 0.1f;
                            float grain = (Hash(x, y, seed) - 0.5f) * 0.08f;
                            int r = Clamp((int)(190 * (1f + dune + grain)), 0, 255);
                            int g = Clamp((int)(100 * (1f + dune + grain)), 0, 255);
                            int b = Clamp((int)(50 * (1f + dune + grain)), 0, 255);
                            byte h = (byte)Clamp((int)(150 + dune * 60f + grain * 20f), 80, 220);
                            c.SetPixel(x, y, Color.FromArgb(255, r, g, b), h, 30, 0, 75);
                            break;
                        }
                        case "dirt":
                        default: {
                            float soil = Hash(x / 6, y / 6, seed);
                            int r = Clamp((int)(90 * (0.8f + soil * 0.4f)), 0, 255);
                            int g = Clamp((int)(62 * (0.8f + soil * 0.4f)), 0, 255);
                            int b = Clamp((int)(40 * (0.8f + soil * 0.4f)), 0, 255);
                            byte h = (byte)Clamp((int)(120 + soil * 80f), 60, 210);
                            c.SetPixel(x, y, Color.FromArgb(255, r, g, b), h, 25, 0, 70);
                            break;
                        }
                    }
                }
            }
        }

        // =========================================================================
        // AUTHENTIC 2-AXLE RAILROAD BOGIE TRUCK & FLANGED WHEEL RENDERING
        // =========================================================================

        /// <summary>
        /// Draws an authentic railroad flanged wheel with mirror-polished steel tread,
        /// recessed dished web, raised cast hub, and center axle pin / retention nut.
        /// </summary>
        public static void DrawRailWheel(PbrCanvas c, int cx, int cy, int radius, int seed = 89) {
            int r2 = radius * radius;
            int rTread = (int)(radius * 0.82f);
            int rHub = (int)(radius * 0.44f);
            int rNut = (int)(radius * 0.22f);

            for (int dy = -radius; dy <= radius; dy++) {
                int py = cy + dy;
                if (py < 0 || py >= c.Height) continue;

                for (int dx = -radius; dx <= radius; dx++) {
                    int px = cx + dx;
                    if (px < 0 || px >= c.Width) continue;

                    float distSq = dx * dx + dy * dy;
                    if (distSq > r2) continue;

                    float dist = (float)Math.Sqrt(distSq);

                    // Concentric lathe machining grooves
                    float lathe = (float)Math.Sin(dist * 1.8f) * 0.04f;

                    if (dist >= rTread) {
                        // 1. Polished Rail-Contact Tread Ring (LabPBR mirror reflection)
                        float t = (dist - rTread) / (radius - rTread);
                        int cr = Clamp((int)(218 * (1f + lathe) + t * 10f), 180, 255);
                        int cg = Clamp((int)(224 * (1f + lathe) + t * 10f), 180, 255);
                        int cb = Clamp((int)(230 * (1f + lathe) + t * 10f), 180, 255);

                        byte h = (byte)Clamp((int)(245 + (1f - Math.Abs(t - 0.5f) * 2f) * 10f), 200, 255);
                        byte sm = 252; // Ultra-polished mirror steel
                        c.SetPixel(px, py, Color.FromArgb(255, cr, cg, cb), h, sm, 230 /* Steel */, 0);
                    } else if (dist >= rHub) {
                        // 2. Concave Dished Web (Recessed cast iron with brake dust patina)
                        float t = (dist - rHub) / (rTread - rHub);
                        float dishProfile = (float)Math.Sin(t * Math.PI); // deep valley
                        float micro = (Hash(px, py, seed) - 0.5f) * 0.06f;

                        int cr = Clamp((int)(50 * (1f + micro) + dishProfile * 15f), 30, 90);
                        int cg = Clamp((int)(52 * (1f + micro) + dishProfile * 15f), 30, 90);
                        int cb = Clamp((int)(56 * (1f + micro) + dishProfile * 15f), 35, 100);

                        byte h = (byte)Clamp((int)(80 + (1f - dishProfile) * 40f), 40, 160);
                        byte sm = 165; // Matte cast finish
                        c.SetPixel(px, py, Color.FromArgb(255, cr, cg, cb), h, sm, 230, 25);
                    } else if (dist >= rNut) {
                        // 3. Raised Wheel Hub Boss
                        float t = (dist - rNut) / (rHub - rNut);
                        float dome = (float)Math.Cos(t * Math.PI * 0.5f);
                        int cr = Clamp((int)(85 + dome * 25f), 60, 130);
                        int cg = Clamp((int)(88 + dome * 25f), 60, 130);
                        int cb = Clamp((int)(92 + dome * 25f), 65, 135);

                        byte h = (byte)Clamp((int)(200 + dome * 35f), 180, 240);
                        byte sm = 205;
                        c.SetPixel(px, py, Color.FromArgb(255, cr, cg, cb), h, sm, 230, 10);
                    } else {
                        // 4. Center Axle Pin & Retention Hex Nut
                        bool isNutEdge = (Math.Abs(dx) >= rNut - 1 || Math.Abs(dy) >= rNut - 1);
                        float shade = isNutEdge ? 0.75f : 1.25f;

                        int cr = Clamp((int)(160 * shade), 90, 230);
                        int cg = Clamp((int)(165 * shade), 90, 230);
                        int cb = Clamp((int)(172 * shade), 95, 240);

                        byte h = 255;
                        byte sm = 240;
                        c.SetPixel(px, py, Color.FromArgb(255, cr, cg, cb), h, sm, 230, 0);
                    }
                }
            }
        }

        /// <summary>
        /// Draws a Bettendorf cast-steel bogie sideframe bracket with central coil spring packet
        /// and end journal boxes.
        /// </summary>
        public static void DrawBogieSideframe(PbrCanvas c, int rx, int ry, int rw, int rh, int seed = 88) {
            Color castSteel = Color.FromArgb(255, 38, 40, 44);
            int x1 = Math.Max(0, rx);
            int y1 = Math.Max(0, ry);
            int x2 = Math.Min(c.Width, rx + rw);
            int y2 = Math.Min(c.Height, ry + rh);

            // Base cast steel frame
            for (int y = y1; y < y2; y++) {
                int distY = Math.Min(y - ry, (ry + rh - 1) - y);
                for (int x = x1; x < x2; x++) {
                    float relX = (float)(x - rx) / rw;
                    float micro = (Hash(x, y, seed) - 0.5f) * 0.05f;

                    // Flanged structural ribs along top and bottom
                    bool isRib = (distY < rh * 0.22f);
                    float shade = isRib ? 1.15f : 0.85f;

                    int cr = Clamp((int)(castSteel.R * shade * (1f + micro)), 0, 255);
                    int cg = Clamp((int)(castSteel.G * shade * (1f + micro)), 0, 255);
                    int cb = Clamp((int)(castSteel.B * shade * (1f + micro)), 0, 255);

                    byte h = (byte)(isRib ? 225 : 180);
                    byte sm = (byte)(isRib ? 195 : 170);

                    // Central Spring Pocket (relX: 0.38 to 0.62)
                    if (relX >= 0.38f && relX <= 0.62f && distY >= rh * 0.15f) {
                        float coilT = (relX - 0.38f) / 0.24f;
                        float springX = (coilT < 0.5f) ? (coilT / 0.5f) : ((coilT - 0.5f) / 0.5f);
                        float coilSine = (float)Math.Sin((y - ry) * 0.8f);

                        if (coilSine > 0.1f) {
                            // Bright metallic spring coil crest
                            cr = Clamp((int)(160 + coilSine * 50f), 0, 255);
                            cg = Clamp((int)(165 + coilSine * 50f), 0, 255);
                            cb = Clamp((int)(172 + coilSine * 50f), 0, 255);
                            h = 245;
                            sm = 230;
                        } else {
                            // Deep shadowed recess between coils
                            cr = 20; cg = 22; cb = 24;
                            h = 50;
                            sm = 140;
                        }
                    }

                    c.SetPixel(x, y, Color.FromArgb(255, cr, cg, cb), h, sm, 230, 10);
                }
            }

            // End Journal Bearing Boxes (left at 0.12, right at 0.88)
            int jRad = (int)(rh * 0.38f);
            int jx1 = rx + (int)(rw * 0.12f);
            int jx2 = rx + (int)(rw * 0.88f);
            int jy = ry + rh / 2;

            DrawHexBolt(c, jx1, jy, Math.Max(4, jRad));
            DrawHexBolt(c, jx2, jy, Math.Max(4, jRad));
        }

        /// <summary>
        /// Draws full HD Bogie Truck & Wheel textures for standard 128x128 train cart models (at scale S).
        /// U=54, V=20: Bettendorf sideframe brackets (cast steel, springs, journal boxes).
        /// U=80, V=20: Flanged rail wheels (mirror-polished tread, dished web, hub nut).
        /// U=104, V=20: Bolster cradle & steel cross-axles.
        /// </summary>
        public static void DrawBogieTruckSet128(PbrCanvas c, int S = 16) {
            // 1. Bolster Beam & Axles (texOffs 104, 20)
            DrawBrushedBareMetal(c, 104 * S, 20 * S, 24 * S, 14 * S, Color.FromArgb(255, 42, 45, 50), 200, 215, true, 87);
            for (int b = 0; b < 2; b++) {
                DrawRivets(c, 106 * S + b * 10 * S, 22 * S, 4, 3 * S, true, 4);
            }

            // 2. Sideframe Brackets (texOffs 54, 20)
            // Model box: 7.4 x 1.6 x 0.6. Total UV footprint ~16x3 units
            DrawBogieSideframe(c, (int)((54 + 0.6f) * S), (int)((20 + 0.6f) * S), (int)(7.4f * S), (int)(1.6f * S), 88);
            // Reverse / back sideframe face
            DrawBogieSideframe(c, (int)((54 + 0.6f + 7.4f + 0.6f) * S), (int)((20 + 0.6f) * S), (int)(7.4f * S), (int)(1.6f * S), 89);
            // Top/bottom edge metal
            DrawBrushedBareMetal(c, 54 * S, 20 * S, 16 * S, (int)(0.6f * S), Color.FromArgb(255, 38, 40, 44), 180, 200, true, 90);

            // 3. Flanged Rail Wheels (texOffs 80, 20)
            // Model box: 2.4 x 2.4 x 0.8.
            // Tread contact rim across top and side flaps (ultra-polished mirror reflection):
            DrawBrushedBareMetal(c, 80 * S, 20 * S, (int)(0.8f * S), (int)(3.2f * S), Color.FromArgb(255, 215, 222, 228), 245, 252, true, 91);
            DrawBrushedBareMetal(c, (int)((80 + 0.8f + 2.4f) * S), 20 * S, (int)(0.8f * S), (int)(3.2f * S), Color.FromArgb(255, 215, 222, 228), 245, 252, true, 92);

            // Wheel Outer Circular Face (centered at U = 80 + 0.8 + 1.2 = 82.0, V = 20 + 0.8 + 1.2 = 22.0)
            int wheelRadius = (int)(1.18f * S);
            int cx1 = (int)(82.0f * S);
            int cy1 = (int)(22.0f * S);
            DrawRailWheel(c, cx1, cy1, wheelRadius, 93);

            // Wheel Inner Face
            int cx2 = (int)((80 + 0.8f + 2.4f + 0.8f + 1.2f) * S);
            DrawRailWheel(c, cx2, cy1, wheelRadius, 94);
        }

        /// <summary>
        /// Draws full HD Bogie Truck & Wheel textures for 1024x1024 models (Caboose and Tankers at scale S).
        /// </summary>
        public static void DrawBogieTruckSet1024(PbrCanvas c, int bolsterU, int bolsterV, int sfU, int sfV1, int sfV2,
                                                int wU, int wV1, int wV2, int S = 2, int seed = 95) {
            // Bolster Mount (16 x 6 x 48)
            DrawBrushedBareMetal(c, bolsterU * S, bolsterV * S, 48 * S, 16 * S, Color.FromArgb(255, 42, 45, 50), 200, 215, true, seed);

            // Sideframe 1 & 2 (48 x 8 x 2)
            DrawBogieSideframe(c, sfU * S, sfV1 * S, 48 * S, 8 * S, seed + 1);
            DrawBogieSideframe(c, sfU * S, sfV2 * S, 48 * S, 8 * S, seed + 2);

            // Wheels: 16 x 16 x 4 each.
            // Tread polished rim:
            DrawBrushedBareMetal(c, wU * S, wV1 * S, 4 * S, 20 * S, Color.FromArgb(255, 215, 222, 228), 245, 252, true, seed + 3);
            DrawBrushedBareMetal(c, (wU + 44) * S, wV1 * S, 4 * S, 20 * S, Color.FromArgb(255, 215, 222, 228), 245, 252, true, seed + 4);
            DrawBrushedBareMetal(c, (wU + 88) * S, wV1 * S, 4 * S, 20 * S, Color.FromArgb(255, 215, 222, 228), 245, 252, true, seed + 5);
            DrawBrushedBareMetal(c, (wU + 132) * S, wV1 * S, 4 * S, 20 * S, Color.FromArgb(255, 215, 222, 228), 245, 252, true, seed + 6);

            // Circular Wheel Faces:
            int wRad = 7 * S;
            DrawRailWheel(c, (wU + 12) * S, (wV1 + 12) * S, wRad, seed + 7);
            DrawRailWheel(c, (wU + 44 + 12) * S, (wV1 + 12) * S, wRad, seed + 8);
            DrawRailWheel(c, (wU + 88 + 12) * S, (wV1 + 12) * S, wRad, seed + 9);
            DrawRailWheel(c, (wU + 132 + 12) * S, (wV1 + 12) * S, wRad, seed + 10);

            if (wV2 != wV1) {
                DrawRailWheel(c, (wU + 12) * S, (wV2 + 12) * S, wRad, seed + 11);
                DrawRailWheel(c, (wU + 44 + 12) * S, (wV2 + 12) * S, wRad, seed + 12);
                DrawRailWheel(c, (wU + 88 + 12) * S, (wV2 + 12) * S, wRad, seed + 13);
                DrawRailWheel(c, (wU + 132 + 12) * S, (wV2 + 12) * S, wRad, seed + 14);
            }
        }

        /// <summary>
        /// Draws the authentic bright silver industrial flatbed deck (diamond plate, perimeter bevels,
        /// rivets, dark undercarriage sill, couplers, and standard 2-axle bogie trucks).
        /// Matches the flatbed seen on wood_cart and cargo flatbeds.
        /// </summary>
        public static void DrawSilverFlatbedDeck(PbrCanvas c, int S = 16) {
            Color silverSteel = Color.FromArgb(255, 215, 222, 228);
            Color darkSill = Color.FromArgb(255, 36, 38, 42);
            Color couplerMetal = Color.FromArgb(255, 28, 30, 32);

            // 1. Diamond Plate Flatbed Deck: texOffs(0, 0), box 24 x 2 x 16 (U=0..64, V=0..18)
            DrawDiamondTread(c, 0, 0, 64 * S, 18 * S, silverSteel, 16);

            // Perimeter Beveled Frame Edges (Height 240, Smoothness 230)
            for (int y = 0; y < 18 * S; y++) {
                for (int x = 0; x < 2 * S; x++) {
                    c.SetPixel(x, y, Color.FromArgb(255, 180, 185, 192), 240, 230, 230, 0);
                }
                for (int x = 62 * S; x < 64 * S; x++) {
                    c.SetPixel(x, y, Color.FromArgb(255, 180, 185, 192), 240, 230, 230, 0);
                }
            }
            for (int x = 0; x < 64 * S; x++) {
                for (int y = 0; y < 2 * S; y++) {
                    c.SetPixel(x, y, Color.FromArgb(255, 180, 185, 192), 240, 230, 230, 0);
                }
                for (int y = 16 * S; y < 18 * S; y++) {
                    c.SetPixel(x, y, Color.FromArgb(255, 180, 185, 192), 240, 230, 230, 0);
                }
            }

            // Outer Structural Rivets along edges
            for (int rx = 3 * S; rx < 62 * S; rx += 4 * S) {
                DrawRivets(c, rx, S, 1, 0, true, 5);
                DrawRivets(c, rx, 17 * S, 1, 0, true, 5);
            }

            // 2. Undercarriage Center Beam: texOffs(0, 20), box 18 x 2 x 8 (U=0..52, V=20..30)
            DrawBrushedBareMetal(c, 0, 20 * S, 52 * S, 10 * S, darkSill, 180, 215, true, 71);

            // 3. Couplers: texOffs(0, 32), box 2 x 2 x 4 (U=0..12, V=32..38)
            DrawBrushedBareMetal(c, 0, 32 * S, 12 * S, 6 * S, couplerMetal, 210, 220, true, 72);

            // 4. Standard 2-Axle Bogie Trucks & Flanged Wheels
            DrawBogieTruckSet128(c, S);
        }
    }
}
