using System;
using System.Drawing;
using System.IO;

namespace HdTexturePackGenerator {
    public static class TrackGenerators {

        // 1. STRAIGHT RAIL (cmodels/rail.png) - 2048x2048
        public static void GenerateStraightRail(string outDir) {
            Console.WriteLine("  -> Generating Straight Rail (2048x2048 LabPBR 1.3 + POM)...");
            int W = 2048;
            int H = 2048;
            using (PbrCanvas c = new PbrCanvas(W, H)) {
                // 1. Crushed Granite Ballast Gravel Bed
                DrawUtils.DrawAggregates(c, 0, 0, W, H, "gravel", 201);

                // 2. Heavy Creosoted Wooden Sleepers (Ties) - 4 ties across height
                int numTies = 4;
                int tiePitch = H / numTies;
                Color creosoteWood = Color.FromArgb(255, 48, 38, 28);

                for (int t = 0; t < numTies; t++) {
                    int ty = t * tiePitch + 35;
                    int th = 350;
                    DrawUtils.DrawWoodPlanks(c, 60, ty, W - 120, th, creosoteWood, 48, true, 210 + t);

                    // Cast iron tie plates under left and right rails
                    int[] tiePlateX = new int[] { 340, 1540 };
                    foreach (int tpx in tiePlateX) {
                        DrawUtils.DrawBrushedBareMetal(c, tpx - 90, ty - 10, 180, th + 20, Color.FromArgb(255, 42, 40, 38), 180, 160, false, 220 + t);
                        // 4 heavy hex spikes/bolts per plate
                        DrawUtils.DrawHexBolt(c, tpx - 55, ty + 40, 16);
                        DrawUtils.DrawHexBolt(c, tpx + 55, ty + 40, 16);
                        DrawUtils.DrawHexBolt(c, tpx - 55, ty + th - 40, 16);
                        DrawUtils.DrawHexBolt(c, tpx + 55, ty + th - 40, 16);
                    }
                }

                // 3. Left and Right Steel Rails
                int[] railCenters = new int[] { 340, 1540 };
                foreach (int rx in railCenters) {
                    // Rail base flange (Width 160)
                    DrawUtils.DrawBrushedBareMetal(c, rx - 80, 0, 160, H, Color.FromArgb(255, 60, 62, 65), 190, 180, false, 231);
                    // Rail web (Width 70)
                    DrawUtils.DrawBrushedBareMetal(c, rx - 35, 0, 70, H, Color.FromArgb(255, 75, 78, 82), 220, 200, false, 232);
                    // Mirror-polished rail running crown (Width 50) - Maximum reflection & smoothness
                    // Smoothness 252, Metal 230, Height 255
                    for (int y = 0; y < H; y++) {
                        for (int dx = -25; dx <= 25; dx++) {
                            int px = rx + dx;
                            float crownBevel = 1f - (Math.Abs(dx) / 25f) * 0.25f;
                            int r = DrawUtils.Clamp((int)(230 * crownBevel), 0, 255);
                            int g = DrawUtils.Clamp((int)(232 * crownBevel), 0, 255);
                            int b = DrawUtils.Clamp((int)(235 * crownBevel), 0, 255);
                            byte h = (byte)DrawUtils.Clamp((int)(255 - Math.Abs(dx) * 0.8f), 235, 255);
                            c.SetPixel(px, y, Color.FromArgb(255, r, g, b), h, 252, 230, 0);
                        }
                    }
                }

                c.Save(Path.Combine(outDir, "cmodels/rail"));
            }
        }

        // 2. CURVED RAIL (cmodels/rail_curve.png) - 2048x2048
        public static void GenerateCurvedRail(string outDir) {
            Console.WriteLine("  -> Generating Curved Rail (2048x2048 LabPBR 1.3 + POM)...");
            int W = 2048;
            int H = 2048;
            using (PbrCanvas c = new PbrCanvas(W, H)) {
                // Ballast
                DrawUtils.DrawAggregates(c, 0, 0, W, H, "gravel", 241);

                // Radial Wooden Ties
                Color creosoteWood = Color.FromArgb(255, 48, 38, 28);
                float centerX = 0;
                float centerY = H;
                int numRadialTies = 12;

                for (int i = 0; i <= numRadialTies; i++) {
                    float angle = (float)(i * (Math.PI * 0.5) / numRadialTies);
                    float cos = (float)Math.Cos(angle);
                    float sin = (float)Math.Sin(angle);

                    for (float r = 500; r < 2000; r += 20) {
                        int tx = (int)(centerX + r * cos);
                        int ty = (int)(centerY - r * sin);
                        for (int dw = -30; dw <= 30; dw++) {
                            int px = (int)(tx - dw * sin);
                            int py = (int)(ty - dw * cos);
                            if (px >= 0 && px < W && py >= 0 && py < H) {
                                c.SetPixel(px, py, creosoteWood, 175, 45, 0, 55);
                            }
                        }
                    }
                }

                // Curved Steel Rails (Inner Radius: 850, Outer Radius: 1650)
                float[] radii = new float[] { 850f, 1650f };
                for (int y = 0; y < H; y++) {
                    float dy = centerY - y;
                    for (int x = 0; x < W; x++) {
                        float dx = x - centerX;
                        float dist = (float)Math.Sqrt(dx * dx + dy * dy);

                        foreach (float r0 in radii) {
                            float diff = Math.Abs(dist - r0);
                            if (diff <= 30f) {
                                float t = 1f - (diff / 30f);
                                int r = DrawUtils.Clamp((int)(230 * (0.8f + t * 0.2f)), 0, 255);
                                int g = DrawUtils.Clamp((int)(232 * (0.8f + t * 0.2f)), 0, 255);
                                int b = DrawUtils.Clamp((int)(235 * (0.8f + t * 0.2f)), 0, 255);
                                byte h = (byte)DrawUtils.Clamp((int)(220 + t * 35f), 180, 255);
                                byte sm = (byte)DrawUtils.Clamp((int)(210 + t * 42f), 150, 252);
                                c.SetPixel(x, y, Color.FromArgb(255, r, g, b), h, sm, 230, 0);
                            }
                        }
                    }
                }

                c.Save(Path.Combine(outDir, "cmodels/rail_curve"));
            }
        }

        // 3. SIGNALS, SWITCHES & BUFFER STOPS - 1024x1024
        public static void GenerateSignalsAndSwitches(string blockDir) {
            Console.WriteLine("  -> Generating Track Signals & Switches (1024x1024 LabPBR 1.3)...");
            int S = 1024;

            // A. Track Switch Stand Metal (Cast Iron Housing)
            using (PbrCanvas c = new PbrCanvas(S, S)) {
                DrawUtils.DrawBrushedBareMetal(c, 0, 0, S, S, Color.FromArgb(255, 40, 42, 45), 200, 210, true, 261);
                DrawUtils.DrawHexBolt(c, 256, 256, 64);
                DrawUtils.DrawHexBolt(c, 768, 256, 64);
                DrawUtils.DrawHexBolt(c, 256, 768, 64);
                DrawUtils.DrawHexBolt(c, 768, 768, 64);
                c.Save(Path.Combine(blockDir, "track_switch_metal"));
            }

            // B. Switch Targets (Open = Green, Closed = Red)
            using (PbrCanvas c = new PbrCanvas(S, S)) {
                DrawUtils.DrawBeveledPanel(c, 0, 0, S, S, Color.FromArgb(255, 30, 32, 34), 180, 180, 0, 20, 262);
                DrawUtils.DrawEmissiveLamp(c, 512, 512, 350, Color.FromArgb(255, 30, 235, 40), 0);
                c.Save(Path.Combine(blockDir, "track_switch_front_open"));
            }

            using (PbrCanvas c = new PbrCanvas(S, S)) {
                DrawUtils.DrawBeveledPanel(c, 0, 0, S, S, Color.FromArgb(255, 30, 32, 34), 180, 180, 0, 20, 263);
                DrawUtils.DrawEmissiveLamp(c, 512, 512, 350, Color.FromArgb(255, 245, 30, 20), 0);
                c.Save(Path.Combine(blockDir, "track_switch_front_closed"));
            }

            // C. Buffer Stop Wood & Metal
            using (PbrCanvas c = new PbrCanvas(S, S)) {
                DrawUtils.DrawWoodPlanks(c, 0, 0, S, S, Color.FromArgb(255, 80, 55, 35), 128, true, 264);
                DrawUtils.DrawRivets(c, 128, 200, 4, 256, true, 32);
                c.Save(Path.Combine(blockDir, "buffer_stop_wood"));
            }

            using (PbrCanvas c = new PbrCanvas(S, S)) {
                DrawUtils.DrawBeveledPanel(c, 0, 0, S, S, Color.FromArgb(255, 195, 30, 25), 200, 210, 0, 32, 265);
                // Yellow safety chevrons
                for (int y = 0; y < S; y++) {
                    for (int x = 0; x < S; x++) {
                        if (((x + y) / 64) % 2 == 0) {
                            c.SetPixel(x, y, Color.FromArgb(255, 235, 190, 25), 210, 210, 0);
                        }
                    }
                }
                c.Save(Path.Combine(blockDir, "buffer_stop_pad"));
            }

            // D. Box Center Signal (Active = Green, Inactive = Red)
            using (PbrCanvas c = new PbrCanvas(S, S)) {
                DrawUtils.DrawBeveledPanel(c, 0, 0, S, S, Color.FromArgb(255, 35, 38, 42), 190, 190, 230, 24, 266);
                DrawUtils.DrawEmissiveLamp(c, 512, 512, 300, Color.FromArgb(255, 240, 30, 20), 0);
                c.Save(Path.Combine(blockDir, "cmodels/box_center"));
            }

            using (PbrCanvas c = new PbrCanvas(S, S)) {
                DrawUtils.DrawBeveledPanel(c, 0, 0, S, S, Color.FromArgb(255, 35, 38, 42), 190, 190, 230, 24, 267);
                DrawUtils.DrawEmissiveLamp(c, 512, 512, 300, Color.FromArgb(255, 30, 240, 40), 0);
                c.Save(Path.Combine(blockDir, "cmodels/box_center_on"));
            }

            // E. Electric Wires (Off = Black insulated copper, On = Glowing cyan plasma arc)
            using (PbrCanvas c = new PbrCanvas(S, S)) {
                DrawUtils.DrawBrushedBareMetal(c, 0, 0, S, S, Color.FromArgb(255, 28, 28, 30), 190, 140, true, 268);
                c.Save(Path.Combine(blockDir, "cmodels/wire_off"));
            }

            using (PbrCanvas c = new PbrCanvas(S, S)) {
                DrawUtils.DrawBrushedBareMetal(c, 0, 0, S, S, Color.FromArgb(255, 80, 200, 255), 245, 252, true, 269);
                // Glowing cyan plasma core
                for (int y = 0; y < S; y++) {
                    for (int x = 0; x < S; x++) {
                        c.SetPixel(x, y, Color.FromArgb(255, 140, 230, 255), 255, 255, 0, 0, 0);
                    }
                }
                c.Save(Path.Combine(blockDir, "cmodels/wire_on"));
            }
        }

        // 4. BLOCK OVERLAY RAILS (turnout_rail, electric_rail, etc.) - 512x512
        public static void GenerateBlockRails(string blockDir) {
            Console.WriteLine("  -> Generating Functional Rails (512x512 LabPBR 1.3)...");
            int S = 512;
            string[] rails = new string[] {
                "braking_rail", "control_rail", "control_rail_on", "electric_rail",
                "electric_rail_on", "horn_rail", "horn_rail_on", "junction_rail",
                "turnout_rail_0", "turnout_rail_1", "turnout_rail_2", "turnout_rail_3"
            };

            foreach (string rName in rails) {
                using (PbrCanvas c = new PbrCanvas(S, S)) {
                    DrawUtils.DrawAggregates(c, 0, 0, S, S, "gravel", 281);
                    // Ties
                    for (int t = 0; t < 3; t++) {
                        DrawUtils.DrawWoodPlanks(c, 20, t * 170 + 20, S - 40, 90, Color.FromArgb(255, 50, 40, 30), 24, true, 282 + t);
                    }
                    // Left and right rails
                    int r1 = 100;
                    int r2 = 412;
                    c.FillRect(r1 - 15, 0, 30, S, Color.FromArgb(255, 225, 228, 232), 255, 252, 230);
                    c.FillRect(r2 - 15, 0, 30, S, Color.FromArgb(255, 225, 228, 232), 255, 252, 230);

                    if (rName.Contains("_on")) {
                        // Radiant electric line
                        c.FillRect(S / 2 - 8, 0, 16, S, Color.FromArgb(255, 80, 220, 255), 250, 252, 0, 0, 0);
                    }

                    c.Save(Path.Combine(blockDir, rName));
                }
            }
        }
    }
}
