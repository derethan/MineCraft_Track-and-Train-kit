using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;

public class TextureGenerator {
    private string outputRoot;
    private string srcAssetsRoot;

    public TextureGenerator(string outputRoot, string srcAssetsRoot) {
        this.outputRoot = outputRoot;
        this.srcAssetsRoot = srcAssetsRoot;
    }

    public void GenerateAll() {
        Console.WriteLine("Generating Super HD LabPBR / POM Textures...");

        GenerateRailsAndTrackPieces();
        GenerateSignalsAndSwitches();
        GenerateBufferStops();
        GenerateTrackBlocks();
        GenerateAutomationBlocks();
        GenerateLocomotives();
        GenerateRollingStock();
        GenerateItemIcons();
        GeneratePackMetaAndIcon();

        Console.WriteLine("All HD LabPBR / POM textures generated successfully!");
    }

    private void GenerateRailsAndTrackPieces() {
        Console.WriteLine("Generating Rails & Track Pieces...");
        string blockDir = Path.Combine(outputRoot, "assets/miningtools/textures/block");
        string cmodelsDir = Path.Combine(blockDir, "cmodels");

        // 1. Straight Rail (cmodels/rail) - 1024x1024
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            // Crushed granite ballast gravel bed background
            buf.DrawAggregateLumps(0, 0, 1024, 1024, Color.FromArgb(255, 90, 88, 86), 40, 59, 1001);

            // Heavy creosoted wooden sleepers (ties) - 4 ties across 1024 height
            for (int t = 0; t < 4; t++) {
                int ty = t * 256 + 18;
                int th = 175;
                buf.DrawWoodPlanks(40, ty, 944, th, th / 2, Color.FromArgb(255, 48, 36, 26), 1010 + t);

                // Cast iron tie plates under left and right rails
                int[] tiePlateX = new int[] { 170, 770 };
                foreach (int tpx in tiePlateX) {
                    buf.DrawBrushedSteel(tpx - 45, ty - 5, 90, th + 10, Color.FromArgb(255, 45, 42, 40), 180, 140, false, 200 + t);
                    // 4 heavy spikes/bolts per tie plate
                    buf.DrawHexBolt(tpx - 28, ty + 20, 8);
                    buf.DrawHexBolt(tpx + 28, ty + 20, 8);
                    buf.DrawHexBolt(tpx - 28, ty + th - 20, 8);
                    buf.DrawHexBolt(tpx + 28, ty + th - 20, 8);
                }
            }

            // Left & Right Steel Rails
            int[] railX = new int[] { 170, 770 };
            foreach (int rx in railX) {
                // Rail base flange (wide, weathered steel)
                buf.DrawBrushedSteel(rx - 32, 0, 64, 1024, Color.FromArgb(255, 75, 70, 68), 215, 130, false, 301);
                // Rail vertical web (shadowed steel)
                buf.DrawBrushedSteel(rx - 14, 0, 28, 1024, Color.FromArgb(255, 55, 52, 50), 235, 145, false, 302);
                // Rail head / crown (mirror-polished cold-rolled steel from wheel wear!)
                buf.DrawBrushedSteel(rx - 18, 0, 36, 1024, Color.FromArgb(255, 195, 200, 208), 255, 245, false, 303);
                // High-polish center contact line
                buf.DrawBrushedSteel(rx - 8, 0, 16, 1024, Color.FromArgb(255, 225, 230, 238), 255, 252, false, 304);
            }

            buf.Save(Path.Combine(cmodelsDir, "rail"));
        }

        // 2. Curved Rail (cmodels/rail_curve) - 1024x1024
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            buf.DrawAggregateLumps(0, 0, 1024, 1024, Color.FromArgb(255, 90, 88, 86), 40, 59, 1002);

            // Radial ties
            for (int i = 0; i < 6; i++) {
                double angle = (i * 18.0) * Math.PI / 180.0;
                int cx = (int)(Math.Cos(angle) * 550.0);
                int cy = (int)(Math.Sin(angle) * 550.0);
                buf.DrawWoodPlanks(Math.Max(0, cx - 70), Math.Max(0, cy - 70), 200, 140, 70, Color.FromArgb(255, 48, 36, 26), 2000 + i);
            }

            // Curved rails (inner and outer radius)
            float rInner = 400f;
            float rOuter = 820f;
            for (int y = 0; y < 1024; y++) {
                for (int x = 0; x < 1024; x++) {
                    float dist = (float)Math.Sqrt(x * x + y * y);
                    bool isInnerRail = Math.Abs(dist - rInner) <= 24f;
                    bool isOuterRail = Math.Abs(dist - rOuter) <= 24f;

                    if (isInnerRail || isOuterRail) {
                        float dRail = isInnerRail ? Math.Abs(dist - rInner) : Math.Abs(dist - rOuter);
                        float crown = 1f - (dRail / 24f);

                        int r = TextureUtils.Clamp((int)(120 + crown * 115f), 0, 255);
                        int g = TextureUtils.Clamp((int)(125 + crown * 115f), 0, 255);
                        int b = TextureUtils.Clamp((int)(132 + crown * 115f), 0, 255);

                        byte h = (byte)(200 + crown * 55f);
                        byte sm = (byte)(140 + crown * 105f);

                        buf.SetPbr(x, y, Color.FromArgb(255, r, g, b), h, sm, 230, 0, 255);
                    }
                }
            }

            buf.Save(Path.Combine(cmodelsDir, "rail_curve"));
            buf.Save(Path.Combine(cmodelsDir, "rail_curve_open"));
        }

        // 3. Switch Actuator Box (cmodels/box_center & box_center_on) - 512x512
        for (int state = 0; state <= 1; state++) {
            using (PbrBuffer buf = new PbrBuffer(512, 512)) {
                // Cast iron housing with beveled plate edges
                buf.DrawBrushedSteel(20, 20, 472, 472, Color.FromArgb(255, 42, 44, 48), 180, 130, true, 401);
                // Heavy perimeter hex bolts
                for (int i = 0; i < 8; i++) {
                    buf.DrawHexBolt(50 + i * 58, 45, 8);
                    buf.DrawHexBolt(50 + i * 58, 467, 8);
                    buf.DrawHexBolt(45, 50 + i * 58, 8);
                    buf.DrawHexBolt(467, 50 + i * 58, 8);
                }
                // Central mechanical gear & linkage cover
                buf.DrawBrushedSteel(120, 120, 272, 272, Color.FromArgb(255, 60, 64, 70), 220, 160, false, 402);
                buf.DrawRivets(140, 140, 6, 46, true, 4);
                buf.DrawRivets(140, 372, 6, 46, true, 4);

                // Status Indicator Lamp
                Color lampCol = (state == 1) ? Color.FromArgb(255, 50, 255, 80) : Color.FromArgb(255, 255, 180, 20);
                buf.DrawEmissiveLamp(256, 256, 45, lampCol);

                string name = (state == 1) ? "box_center_on" : "box_center";
                buf.Save(Path.Combine(cmodelsDir, name));
            }
        }

        // 4. Power Rails & Third-Rail Conductors (cmodels/wire_off & wire_on & wire_black) - 512x512
        for (int state = 0; state <= 1; state++) {
            using (PbrBuffer buf = new PbrBuffer(512, 512)) {
                // Ceramic porcelain insulator base (bracket)
                buf.FillRect(180, 40, 152, 432, Color.FromArgb(255, 180, 175, 165), 160, 210, 59, 0, 255);

                if (state == 0) {
                    // Cold copper/brass third-rail power bar
                    buf.DrawBrushedSteel(216, 0, 80, 512, Color.FromArgb(255, 215, 135, 50), 235, 210, false, 501);
                    // Central wear contact strip
                    buf.DrawBrushedSteel(236, 0, 40, 512, Color.FromArgb(255, 245, 180, 80), 245, 235, false, 502);
                } else {
                    // Energized glowing plasma power bar!
                    buf.DrawBrushedSteel(216, 0, 80, 512, Color.FromArgb(255, 255, 200, 100), 240, 240, false, 503);
                    // Core electric arc line (Emissive = 0 for brilliant glow!)
                    buf.FillRect(236, 0, 40, 512, Color.FromArgb(255, 180, 240, 255), 255, 255, 60, 0, 0 /* Emissive MAX */);
                    // Radiant halo
                    buf.FillRect(226, 0, 10, 512, Color.FromArgb(255, 100, 200, 255), 245, 250, 60, 0, 10);
                    buf.FillRect(276, 0, 10, 512, Color.FromArgb(255, 100, 200, 255), 245, 250, 60, 0, 10);
                }

                string name = (state == 1) ? "wire_on" : "wire_off";
                buf.Save(Path.Combine(cmodelsDir, name));
            }
        }

        // wire_black & activator_rail_line
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            buf.DrawBrushedSteel(0, 0, 512, 512, Color.FromArgb(255, 28, 30, 33), 160, 120, false, 555);
            buf.Save(Path.Combine(cmodelsDir, "wire_black"));
        }
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            buf.FillRect(0, 0, 512, 512, Color.FromArgb(255, 220, 40, 30), 200, 170, 59, 10, 255);
            buf.FillRect(216, 0, 80, 512, Color.FromArgb(255, 255, 220, 50), 230, 240, 231 /* Gold trigger */, 0, 255);
            buf.Save(Path.Combine(cmodelsDir, "activator_rail_line"));
        }
    }

    private void GenerateSignalsAndSwitches() {
        Console.WriteLine("Generating Signals & Switches...");
        string blockDir = Path.Combine(outputRoot, "assets/miningtools/textures/block");

        // 1. Signal Closed (Red) - 512x512
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            // Dark cast-iron bezel
            buf.DrawBrushedSteel(0, 0, 512, 512, Color.FromArgb(255, 32, 34, 38), 180, 140, true, 601);
            // Heavy perimeter flange bolts
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4.0;
                int bx = (int)(256 + Math.Cos(a) * 215);
                int by = (int)(256 + Math.Sin(a) * 215);
                buf.DrawHexBolt(bx, by, 10);
            }
            // Circular signal spectacle housing
            for (int y = 0; y < 512; y++) {
                for (int x = 0; x < 512; x++) {
                    float dist = (float)Math.Sqrt((x - 256) * (x - 256) + (y - 256) * (y - 256));
                    if (dist < 180f) {
                        float ring = (float)Math.Sin(dist * 0.35f);
                        int r = TextureUtils.Clamp((int)(160 + ring * 40f), 0, 255);
                        buf.SetPbr(x, y, Color.FromArgb(255, r, 15, 15), (byte)(160 + ring * 30f), 245, 60, 0, 255);
                    }
                }
            }
            // Red glowing emissive core lamp
            buf.DrawEmissiveLamp(256, 256, 110, Color.FromArgb(255, 255, 30, 20));
            buf.Save(Path.Combine(blockDir, "track_switch_front_closed"));
        }

        // 2. Signal Open (Green) - 512x512
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            buf.DrawBrushedSteel(0, 0, 512, 512, Color.FromArgb(255, 32, 34, 38), 180, 140, true, 602);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4.0;
                int bx = (int)(256 + Math.Cos(a) * 215);
                int by = (int)(256 + Math.Sin(a) * 215);
                buf.DrawHexBolt(bx, by, 10);
            }
            for (int y = 0; y < 512; y++) {
                for (int x = 0; x < 512; x++) {
                    float dist = (float)Math.Sqrt((x - 256) * (x - 256) + (y - 256) * (y - 256));
                    if (dist < 180f) {
                        float ring = (float)Math.Sin(dist * 0.35f);
                        int g = TextureUtils.Clamp((int)(160 + ring * 40f), 0, 255);
                        buf.SetPbr(x, y, Color.FromArgb(255, 15, g, 30), (byte)(160 + ring * 30f), 245, 60, 0, 255);
                    }
                }
            }
            buf.DrawEmissiveLamp(256, 256, 110, Color.FromArgb(255, 40, 255, 90));
            buf.Save(Path.Combine(blockDir, "track_switch_front_open"));
        }

        // 3. Switch Stand Metal Body (track_switch_metal) - 512x512
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            buf.DrawBrushedSteel(0, 0, 512, 512, Color.FromArgb(255, 45, 48, 52), 170, 130, false, 603);
            buf.DrawRivets(40, 40, 8, 60, true, 5);
            buf.DrawRivets(40, 472, 8, 60, true, 5);
            // Yellow & black hazard stripes on switch lever counterweight
            buf.DrawCorrugatedSteel(120, 120, 272, 272, 40, Color.FromArgb(255, 230, 180, 25), 604);
            buf.Save(Path.Combine(blockDir, "track_switch_metal"));
        }
    }

    private void GenerateBufferStops() {
        Console.WriteLine("Generating Buffer Stops...");
        string blockDir = Path.Combine(outputRoot, "assets/miningtools/textures/block");

        // 1. buffer_stop_metal - 512x512
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            buf.DrawBrushedSteel(0, 0, 512, 512, Color.FromArgb(255, 50, 52, 58), 180, 140, false, 701);
            buf.DrawRivets(30, 30, 10, 50, true, 5);
            buf.DrawRivets(30, 482, 10, 50, true, 5);
            buf.DrawHexBolt(256, 128, 12);
            buf.DrawHexBolt(256, 384, 12);
            buf.Save(Path.Combine(blockDir, "buffer_stop_metal"));
        }

        // 2. buffer_stop_wood - 512x512
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            buf.DrawWoodPlanks(0, 0, 512, 512, 128, Color.FromArgb(255, 75, 52, 32), 702);
            // Heavy steel strapping bands
            buf.DrawBrushedSteel(60, 0, 36, 512, Color.FromArgb(255, 40, 42, 46), 210, 160, false, 703);
            buf.DrawBrushedSteel(416, 0, 36, 512, Color.FromArgb(255, 40, 42, 46), 210, 160, false, 704);
            buf.DrawHexBolt(78, 128, 8);
            buf.DrawHexBolt(78, 384, 8);
            buf.DrawHexBolt(434, 128, 8);
            buf.DrawHexBolt(434, 384, 8);
            buf.Save(Path.Combine(blockDir, "buffer_stop_wood"));
        }

        // 3. buffer_stop_pad - 512x512 (heavy ribbed rubber buffer)
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            buf.FillRect(0, 0, 512, 512, Color.FromArgb(255, 25, 26, 28), 170, 50, 59, 20, 255);
            for (int y = 0; y < 512; y++) {
                for (int x = 0; x < 512; x++) {
                    float dist = (float)Math.Sqrt((x - 256) * (x - 256) + (y - 256) * (y - 256));
                    if (dist < 230f) {
                        float ring = (float)Math.Cos(dist * 0.18f);
                        int c = TextureUtils.Clamp((int)(35 + ring * 18f), 0, 255);
                        byte h = (byte)(180 + ring * 45f);
                        buf.SetPbr(x, y, Color.FromArgb(255, c, c, c), h, 65, 59, 20, 255);
                    }
                }
            }
            buf.Save(Path.Combine(blockDir, "buffer_stop_pad"));
        }

        // 4. buffer_stop_sign - 512x512 (red & white retro-reflective warning sign)
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            buf.FillRect(0, 0, 512, 512, Color.FromArgb(255, 235, 30, 25), 200, 210, 60, 0, 255);
            // White reflective border & center cross
            buf.FillRect(40, 40, 432, 30, Color.FromArgb(255, 245, 245, 248), 210, 220, 60, 0, 255);
            buf.FillRect(40, 442, 432, 30, Color.FromArgb(255, 245, 245, 248), 210, 220, 60, 0, 255);
            buf.FillRect(40, 40, 30, 432, Color.FromArgb(255, 245, 245, 248), 210, 220, 60, 0, 255);
            buf.FillRect(442, 40, 30, 432, Color.FromArgb(255, 245, 245, 248), 210, 220, 60, 0, 255);

            // Center reflective disc
            for (int y = 0; y < 512; y++) {
                for (int x = 0; x < 512; x++) {
                    float dist = (float)Math.Sqrt((x - 256) * (x - 256) + (y - 256) * (y - 256));
                    if (dist < 110f) {
                        buf.SetPbr(x, y, Color.FromArgb(255, 250, 250, 252), 225, 240, 60, 0, 255);
                    }
                }
            }
            buf.Save(Path.Combine(blockDir, "buffer_stop_sign"));
        }
    }

    private void GenerateTrackBlocks() {
        Console.WriteLine("Generating Track Rail Blocks...");
        string blockDir = Path.Combine(outputRoot, "assets/miningtools/textures/block");

        // 1. Braking Rail - 512x512
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            buf.DrawAggregateLumps(0, 0, 512, 512, Color.FromArgb(255, 90, 88, 86), 40, 59, 801);
            // Heavy friction brake shoes with yellow/black hazard stripes
            buf.DrawBrushedSteel(50, 0, 80, 512, Color.FromArgb(255, 60, 55, 50), 220, 110, false, 802);
            buf.DrawBrushedSteel(382, 0, 80, 512, Color.FromArgb(255, 60, 55, 50), 220, 110, false, 803);
            for (int y = 0; y < 512; y++) {
                bool isYellow = ((y / 40) % 2) == 0;
                Color sc = isYellow ? Color.FromArgb(255, 235, 185, 20) : Color.FromArgb(255, 25, 25, 28);
                for (int x = 50; x < 130; x++) buf.SetPbr(x, y, sc, 220, 150, 59, 10, 255);
                for (int x = 382; x < 462; x++) buf.SetPbr(x, y, sc, 220, 150, 59, 10, 255);
            }
            // Steel rails
            buf.DrawBrushedSteel(150, 0, 30, 512, Color.FromArgb(255, 200, 205, 212), 255, 245, false, 804);
            buf.DrawBrushedSteel(332, 0, 30, 512, Color.FromArgb(255, 200, 205, 212), 255, 245, false, 805);
            buf.Save(Path.Combine(blockDir, "braking_rail"));
        }

        // 2. Horn Rail & horn_rail_on - 512x512
        for (int state = 0; state <= 1; state++) {
            using (PbrBuffer buf = new PbrBuffer(512, 512)) {
                buf.DrawAggregateLumps(0, 0, 512, 512, Color.FromArgb(255, 90, 88, 86), 40, 59, 811);
                buf.DrawBrushedSteel(90, 0, 32, 512, Color.FromArgb(255, 195, 200, 208), 255, 245, false, 812);
                buf.DrawBrushedSteel(390, 0, 32, 512, Color.FromArgb(255, 195, 200, 208), 255, 245, false, 813);

                // Central acoustic transducer / air horn pickup diaphragm
                buf.DrawBrushedSteel(206, 156, 100, 200, Color.FromArgb(255, 215, 140, 45), 210, 210, true, 814);
                Color beacon = (state == 1) ? Color.FromArgb(255, 255, 230, 30) : Color.FromArgb(255, 180, 110, 20);
                buf.DrawEmissiveLamp(256, 256, 35, beacon);

                string name = (state == 1) ? "horn_rail_on" : "horn_rail";
                buf.Save(Path.Combine(blockDir, name));
            }
        }

        // 3. Control Rail & control_rail_on - 512x512
        for (int state = 0; state <= 1; state++) {
            using (PbrBuffer buf = new PbrBuffer(512, 512)) {
                buf.DrawAggregateLumps(0, 0, 512, 512, Color.FromArgb(255, 90, 88, 86), 40, 59, 821);
                buf.DrawBrushedSteel(90, 0, 32, 512, Color.FromArgb(255, 195, 200, 208), 255, 245, false, 822);
                buf.DrawBrushedSteel(390, 0, 32, 512, Color.FromArgb(255, 195, 200, 208), 255, 245, false, 823);

                // Central inductive loop sensor & digital status LED strip
                buf.DrawBrushedSteel(216, 0, 80, 512, Color.FromArgb(255, 38, 40, 44), 190, 150, false, 824);
                for (int i = 0; i < 6; i++) {
                    Color ledCol = (state == 1) ? Color.FromArgb(255, 40, 255, 90) : Color.FromArgb(255, 255, 40, 30);
                    buf.DrawEmissiveLamp(256, 50 + i * 80, 16, ledCol);
                }

                string name = (state == 1) ? "control_rail_on" : "control_rail";
                buf.Save(Path.Combine(blockDir, name));
            }
        }

        // 4. Electric Rail & electric_rail_on - 512x512
        for (int state = 0; state <= 1; state++) {
            using (PbrBuffer buf = new PbrBuffer(512, 512)) {
                buf.DrawAggregateLumps(0, 0, 512, 512, Color.FromArgb(255, 90, 88, 86), 40, 59, 831);
                buf.DrawBrushedSteel(90, 0, 32, 512, Color.FromArgb(255, 195, 200, 208), 255, 245, false, 832);
                buf.DrawBrushedSteel(390, 0, 32, 512, Color.FromArgb(255, 195, 200, 208), 255, 245, false, 833);

                // Central energized third rail conductor
                if (state == 1) {
                    buf.FillRect(226, 0, 60, 512, Color.FromArgb(255, 255, 230, 80), 250, 250, 234, 0, 0 /* Emissive MAX */);
                } else {
                    buf.DrawBrushedSteel(226, 0, 60, 512, Color.FromArgb(255, 195, 120, 40), 220, 190, false, 834);
                }

                string name = (state == 1) ? "electric_rail_on" : "electric_rail";
                buf.Save(Path.Combine(blockDir, name));
            }
        }

        // 5. Junction & Turnout Rails (turnout_rail_0..3 & junction_rail & wye_rail_0..1) - 512x512
        string[] railItems = new string[] {
            "junction_rail", "turnout_rail_0", "turnout_rail_1", "turnout_rail_2", "turnout_rail_3", "wye_rail_0", "wye_rail_1"
        };
        foreach (string rname in railItems) {
            using (PbrBuffer buf = new PbrBuffer(512, 512)) {
                buf.DrawAggregateLumps(0, 0, 512, 512, Color.FromArgb(255, 90, 88, 86), 40, 59, 850);
                // Straight through rails
                buf.DrawBrushedSteel(90, 0, 32, 512, Color.FromArgb(255, 195, 200, 208), 255, 245, false, 851);
                buf.DrawBrushedSteel(390, 0, 32, 512, Color.FromArgb(255, 195, 200, 208), 255, 245, false, 852);
                // Heavy cast frog casting in center
                buf.DrawBrushedSteel(180, 180, 152, 152, Color.FromArgb(255, 120, 125, 130), 240, 210, true, 853);
                buf.DrawHexBolt(210, 210, 8);
                buf.DrawHexBolt(302, 210, 8);
                buf.DrawHexBolt(210, 302, 8);
                buf.DrawHexBolt(302, 302, 8);

                buf.Save(Path.Combine(blockDir, rname));
            }
        }
    }

    private void GenerateAutomationBlocks() {
        Console.WriteLine("Generating Automation Blocks & Machinery...");
        string blockDir = Path.Combine(outputRoot, "assets/miningtools/textures/block");

        // Loaders / Unloaders / Fuelers
        string[] prefixes = new string[] {
            "cargo_loader", "cargo_unloader", "fluid_loader", "fluid_unloader", "coal_fueler", "diesel_fueler", "entity_loader", "entity_unloader"
        };
        string[] faces = new string[] { "top", "bottom", "front", "side" };

        foreach (string pfx in prefixes) {
            Color mainCol = pfx.Contains("cargo") ? Color.FromArgb(255, 52, 60, 68) :
                            pfx.Contains("fluid") ? Color.FromArgb(255, 38, 55, 75) :
                            pfx.Contains("diesel") ? Color.FromArgb(255, 75, 45, 35) :
                            pfx.Contains("coal") ? Color.FromArgb(255, 40, 42, 45) :
                            Color.FromArgb(255, 55, 68, 55);

            foreach (string face in faces) {
                using (PbrBuffer buf = new PbrBuffer(512, 512)) {
                    buf.DrawBrushedSteel(0, 0, 512, 512, mainCol, 180, 140, true, 901);
                    buf.DrawRivets(30, 30, 8, 64, true, 5);
                    buf.DrawRivets(30, 482, 8, 64, true, 5);

                    if (face == "front") {
                        // Industrial intake chute / port & dials
                        buf.DrawDiamondTread(128, 128, 256, 256, Color.FromArgb(255, 45, 48, 52), 902);
                        buf.DrawEmissiveLamp(256, 80, 24, Color.FromArgb(255, 50, 230, 255));
                    } else if (face == "top") {
                        buf.DrawLouverGrille(100, 100, 312, 312, 28, Color.FromArgb(255, 40, 42, 46));
                    }

                    buf.Save(Path.Combine(blockDir, pfx + "_" + face));
                }
            }
        }

        // Chunk Loader (_top, _bottom, _front, _side, _top_on, _front_on) - 512x512
        string[] clFaces = new string[] { "top", "bottom", "front", "side", "top_on", "front_on" };
        foreach (string cf in clFaces) {
            using (PbrBuffer buf = new PbrBuffer(512, 512)) {
                buf.DrawBrushedSteel(0, 0, 512, 512, Color.FromArgb(255, 30, 34, 42), 190, 150, true, 951);
                buf.DrawRivets(35, 35, 8, 62, true, 5);
                buf.DrawRivets(35, 477, 8, 62, true, 5);

                bool isOn = cf.Contains("on");
                if (cf.Contains("top") || cf.Contains("front")) {
                    // Pulsing dimensional chronometer ring
                    for (int y = 0; y < 512; y++) {
                        for (int x = 0; x < 512; x++) {
                            float dist = (float)Math.Sqrt((x - 256) * (x - 256) + (y - 256) * (y - 256));
                            if (dist > 120f && dist < 170f) {
                                float ring = (float)Math.Sin(dist * 0.2f);
                                byte h = (byte)(190 + ring * 40f);
                                buf.SetPbr(x, y, Color.FromArgb(255, 20, 160, 210), h, 240, 60, 0, 255);
                            }
                        }
                    }

                    Color coreCol = isOn ? Color.FromArgb(255, 0, 245, 255) : Color.FromArgb(255, 10, 80, 110);
                    buf.DrawEmissiveLamp(256, 256, 75, coreCol);
                }

                buf.Save(Path.Combine(blockDir, "chunk_loader_" + cf));
            }
        }

        // Surveyor's Charge (_top, _side, _bottom) - 512x512
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            // Dynamite bundle top with brass blasting caps and fuse
            buf.FillRect(0, 0, 512, 512, Color.FromArgb(255, 160, 35, 25), 180, 110, 59, 20, 255);
            buf.DrawHexBolt(256, 256, 25);
            buf.DrawEmissiveLamp(256, 256, 15, Color.FromArgb(255, 255, 200, 30));
            buf.Save(Path.Combine(blockDir, "surveyors_charge_top"));
        }
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            // Dynamite sticks side with warning stencils
            buf.DrawCorrugatedSteel(0, 0, 512, 512, 85, Color.FromArgb(255, 175, 38, 28), 981);
            buf.DrawBrushedSteel(0, 200, 512, 112, Color.FromArgb(255, 240, 235, 220), 200, 90, true, 982);
            buf.Save(Path.Combine(blockDir, "surveyors_charge_side"));
        }
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            buf.FillRect(0, 0, 512, 512, Color.FromArgb(255, 140, 30, 20), 160, 90, 59, 30, 255);
            buf.Save(Path.Combine(blockDir, "surveyors_charge_bottom"));
        }
    }

    private void GenerateLocomotives() {
        Console.WriteLine("Generating HD Locomotives...");
        string entDir = Path.Combine(outputRoot, "assets/miningtools/textures/entity");
        string srcEntDir = Path.Combine(srcAssetsRoot, "textures/entity");

        // 1. Basic Steam Locomotive - 1024x512
        using (PbrBuffer buf = new PbrBuffer(1024, 512)) {
            string srcPath = Path.Combine(srcEntDir, "basic_locomotive.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            } else {
                buf.DrawBrushedSteel(0, 0, 1024, 512, Color.FromArgb(255, 45, 48, 52), 170, 140, true, 1101);
            }

            // Boiler riveted seams
            buf.DrawRivets(50, 40, 16, 55, true, 4);
            buf.DrawRivets(50, 180, 16, 55, true, 4);
            // Polished brass boiler bands & bell (Metal ID 234)
            buf.DrawBrushedSteel(300, 0, 40, 240, Color.FromArgb(255, 225, 165, 45), 230, 220, false, 1102);
            buf.DrawBrushedSteel(600, 0, 40, 240, Color.FromArgb(255, 225, 165, 45), 230, 220, false, 1103);
            // Cab windshield
            buf.DrawGlassPane(750, 60, 180, 110, Color.FromArgb(255, 160, 190, 220));
            // Firebox glowing grate (Emissive = 0)
            buf.DrawEmissiveLamp(450, 350, 40, Color.FromArgb(255, 255, 120, 20));
            // Headlight lantern
            buf.DrawEmissiveLamp(100, 100, 35, Color.FromArgb(255, 255, 240, 150));

            buf.Save(Path.Combine(entDir, "basic_locomotive"));
        }

        // 2. Diesel Locomotive (Canadian National Road Freight) - 1024x1024 (Upgraded 8x from 128x128!)
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            string srcPath = Path.Combine(srcEntDir, "diesel_locomotive.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            }

            // High-detail Canadian National freight styling:
            // Walkway deck (0..304, 0..128) - Diamond tread anti-slip steel
            buf.DrawDiamondTread(0, 0, 304, 128, Color.FromArgb(255, 42, 45, 50), 1201);
            // Red safety cab (0..200, 176..368)
            buf.DrawBrushedSteel(0, 176, 200, 192, Color.FromArgb(255, 195, 30, 25), 180, 160, true, 1202);
            buf.DrawRivets(10, 186, 8, 22, false, 3);
            // Cab windshields
            buf.DrawGlassPane(20, 210, 60, 50, Color.FromArgb(255, 140, 180, 215));
            buf.DrawGlassPane(90, 210, 60, 50, Color.FromArgb(255, 140, 180, 215));
            // Machinery hood (0..450, 576..768) - Matte charcoal brushed steel
            buf.DrawBrushedSteel(0, 576, 450, 192, Color.FromArgb(255, 32, 34, 38), 170, 130, true, 1203);
            // Radiator cooling grilles / louvers (480..700, 576..768)
            buf.DrawLouverGrille(480, 576, 220, 192, 16, Color.FromArgb(255, 25, 26, 28));
            // Twin high-intensity nose headlights (Emissive = 0!)
            buf.DrawEmissiveLamp(680, 220, 25, Color.FromArgb(255, 255, 250, 180));
            // Ditch lights on front walkway pilot
            buf.DrawEmissiveLamp(680, 300, 18, Color.FromArgb(255, 255, 250, 180));
            buf.DrawEmissiveLamp(760, 300, 18, Color.FromArgb(255, 255, 250, 180));
            // Heavy 6-axle cast steel trucks (336..600, 768..1024)
            buf.DrawBrushedSteel(336, 768, 264, 256, Color.FromArgb(255, 40, 42, 46), 185, 140, false, 1204);
            buf.DrawHexBolt(380, 820, 10);
            buf.DrawHexBolt(520, 820, 10);

            buf.Save(Path.Combine(entDir, "diesel_locomotive"));
        }

        // 3. Electric High-Speed Aerodynamic Locomotive - 1024x1024 (Upgraded 8x!)
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            string srcPath = Path.Combine(srcEntDir, "electric_locomotive.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            }

            // Streamlined bullet train aerodynamic body
            buf.DrawBrushedSteel(0, 160, 500, 200, Color.FromArgb(255, 235, 238, 242), 200, 220, true, 1301);
            // Wraparound tinted aerodynamic cockpit glass
            buf.DrawGlassPane(0, 368, 200, 150, Color.FromArgb(255, 45, 65, 85));
            // High-intensity LED headlight bar (Emissive = 0!)
            buf.FillRect(752, 368, 80, 30, Color.FromArgb(255, 235, 245, 255), 255, 255, 60, 0, 0);
            // Pantograph copper collector bar (Metal ID 234)
            buf.DrawBrushedSteel(400, 640, 180, 40, Color.FromArgb(255, 235, 145, 50), 240, 230, true, 1302);

            buf.Save(Path.Combine(entDir, "electric_locomotive"));
        }
    }

    private void GenerateRollingStock() {
        Console.WriteLine("Generating HD Rolling Stock & Carts...");
        string entDir = Path.Combine(outputRoot, "assets/miningtools/textures/entity");
        string srcEntDir = Path.Combine(srcAssetsRoot, "textures/entity");

        // 1. Caboose - 1024x1024
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            string srcPath = Path.Combine(srcEntDir, "caboose.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            }
            buf.DrawWoodPlanks(0, 0, 1024, 512, 32, Color.FromArgb(255, 175, 30, 25), 1401);
            buf.DrawGlassPane(200, 150, 120, 80, Color.FromArgb(255, 160, 190, 220));
            buf.DrawGlassPane(600, 150, 120, 80, Color.FromArgb(255, 160, 190, 220));
            // Rear observation marker lantern (Emissive Red)
            buf.DrawEmissiveLamp(900, 250, 30, Color.FromArgb(255, 255, 30, 20));
            buf.Save(Path.Combine(entDir, "caboose"));
        }

        // 2. Container Cart - 1024x1024 (Corrugated ISO container)
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            string srcPath = Path.Combine(srcEntDir, "container_cart.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            }
            buf.DrawCorrugatedSteel(0, 0, 1024, 600, 32, Color.FromArgb(255, 35, 80, 145), 1402);
            // Corner castings and door rods
            buf.DrawBrushedSteel(0, 0, 60, 600, Color.FromArgb(255, 30, 32, 36), 210, 160, false, 1403);
            buf.DrawBrushedSteel(964, 0, 60, 600, Color.FromArgb(255, 30, 32, 36), 210, 160, false, 1404);
            buf.Save(Path.Combine(entDir, "container_cart"));
        }

        // 3. Generator Cart - 1024x1024
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            string srcPath = Path.Combine(srcEntDir, "generator_cart.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            }
            buf.DrawBrushedSteel(0, 0, 1024, 1024, Color.FromArgb(255, 45, 50, 56), 180, 140, true, 1405);
            buf.DrawLouverGrille(320, 100, 384, 300, 24, Color.FromArgb(255, 30, 32, 35));
            // Gauges & status beacon
            buf.DrawEmissiveLamp(200, 200, 35, Color.FromArgb(255, 50, 220, 255));
            buf.DrawEmissiveLamp(800, 200, 35, Color.FromArgb(255, 255, 180, 30));
            buf.Save(Path.Combine(entDir, "generator_cart"));
        }

        // 4. Hopper Carts (empty, coal, ore, gravel, sand, red_sand, dirt) - 1024x1024
        string[] hopperTypes = new string[] {
            "hopper_cart_empty", "hopper_cart_coal", "hopper_cart_ore", "hopper_cart_gravel",
            "hopper_cart_sand", "hopper_cart_red_sand", "hopper_cart_dirt"
        };
        foreach (string ht in hopperTypes) {
            using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
                string srcPath = Path.Combine(srcEntDir, ht + ".png");
                if (File.Exists(srcPath)) {
                    using (Bitmap src = new Bitmap(srcPath)) {
                        buf.UpscaleFromSource(src);
                    }
                }
                // Slanted reinforced steel hopper bin
                buf.DrawBrushedSteel(0, 0, 1024, 1024, Color.FromArgb(255, 55, 58, 64), 180, 140, true, 1501);
                buf.DrawRivets(40, 40, 16, 60, true, 5);
                buf.DrawRivets(40, 984, 16, 60, true, 5);

                // Specific aggregate contents on top hopper opening
                if (ht.Contains("coal")) {
                    buf.DrawAggregateLumps(200, 200, 624, 624, Color.FromArgb(255, 22, 22, 24), 120, 59, 1502);
                } else if (ht.Contains("ore")) {
                    buf.DrawAggregateLumps(200, 200, 624, 624, Color.FromArgb(255, 95, 92, 88), 100, 230 /* Metallic sparkles */, 1503);
                } else if (ht.Contains("gravel")) {
                    buf.DrawAggregateLumps(200, 200, 624, 624, Color.FromArgb(255, 125, 120, 115), 50, 59, 1504);
                } else if (ht.Contains("red_sand")) {
                    buf.DrawAggregateLumps(200, 200, 624, 624, Color.FromArgb(255, 185, 75, 35), 40, 59, 1505);
                } else if (ht.Contains("sand")) {
                    buf.DrawAggregateLumps(200, 200, 624, 624, Color.FromArgb(255, 215, 195, 140), 40, 59, 1506);
                } else if (ht.Contains("dirt")) {
                    buf.DrawAggregateLumps(200, 200, 624, 624, Color.FromArgb(255, 95, 65, 42), 30, 59, 1507);
                }

                buf.Save(Path.Combine(entDir, ht));
            }
        }

        // 5. Ingot Cart - 1024x1024
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            string srcPath = Path.Combine(srcEntDir, "ingot_cart.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            }
            // Flatbed frame
            buf.DrawBrushedSteel(0, 0, 1024, 1024, Color.FromArgb(255, 45, 48, 52), 170, 140, true, 1601);
            // Stacks of high-purity forged steel ingots (Metal ID 230, Smoothness 215)
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 3; col++) {
                    int ix = 120 + col * 270;
                    int iy = 120 + row * 200;
                    buf.DrawBrushedSteel(ix, iy, 240, 170, Color.FromArgb(255, 185, 190, 198), 240, 220, true, 1602 + row * 3 + col);
                    buf.DrawHexBolt(ix + 120, iy + 85, 12);
                }
            }
            buf.Save(Path.Combine(entDir, "ingot_cart"));
        }

        // 6. Liquid Tank Cart - 1024x1024
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            string srcPath = Path.Combine(srcEntDir, "liquid_cart.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            }
            // Cylindrical welded tank barrel
            buf.DrawBrushedSteel(0, 0, 1024, 1024, Color.FromArgb(255, 110, 115, 122), 210, 180, true, 1701);
            // Circumferential welded plate seams
            buf.DrawRivets(256, 0, 16, 64, false, 5);
            buf.DrawRivets(768, 0, 16, 64, false, 5);
            // Brass valves & manway hatch
            buf.DrawHexBolt(512, 512, 28);
            buf.Save(Path.Combine(entDir, "liquid_cart"));
        }

        // 7. Livestock Cart - 1024x1024
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            string srcPath = Path.Combine(srcEntDir, "livestock_cart.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            }
            buf.DrawWoodPlanks(0, 0, 1024, 1024, 48, Color.FromArgb(255, 115, 85, 55), 1801);
            // Vertical iron reinforcement posts
            buf.DrawBrushedSteel(200, 0, 48, 1024, Color.FromArgb(255, 42, 45, 50), 220, 150, false, 1802);
            buf.DrawBrushedSteel(500, 0, 48, 1024, Color.FromArgb(255, 42, 45, 50), 220, 150, false, 1803);
            buf.DrawBrushedSteel(800, 0, 48, 1024, Color.FromArgb(255, 42, 45, 50), 220, 150, false, 1804);
            buf.Save(Path.Combine(entDir, "livestock_cart"));
        }

        // 8. Logging & Wood Cart - 1024x1024
        string[] woodCartNames = new string[] { "logging_cart", "wood_cart" };
        foreach (string wc in woodCartNames) {
            using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
                string srcPath = Path.Combine(srcEntDir, wc + ".png");
                if (File.Exists(srcPath)) {
                    using (Bitmap src = new Bitmap(srcPath)) {
                        buf.UpscaleFromSource(src);
                    }
                }
                // Giant timber logs with deep bark
                buf.DrawTreeBark(0, 0, 1024, 1024, Color.FromArgb(255, 85, 60, 42), 1901);
                // Log cross-cut ends with concentric tree rings
                buf.DrawTreeLogRings(256, 256, 180, Color.FromArgb(255, 180, 140, 95), 1902);
                buf.DrawTreeLogRings(768, 256, 180, Color.FromArgb(255, 180, 140, 95), 1903);
                buf.Save(Path.Combine(entDir, wc));
            }
        }

        // 9. Ore Cart - 1024x1024
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            string srcPath = Path.Combine(srcEntDir, "ore_cart.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            }
            buf.DrawBrushedSteel(0, 0, 1024, 1024, Color.FromArgb(255, 50, 52, 58), 180, 130, true, 2001);
            buf.DrawAggregateLumps(150, 150, 724, 724, Color.FromArgb(255, 80, 78, 75), 90, 230, 2002);
            buf.Save(Path.Combine(entDir, "ore_cart"));
        }

        // 10. Tunnel Bore - 1024x1024
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            string srcPath = Path.Combine(srcEntDir, "tunnel_bore.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            }
            buf.DrawBrushedSteel(0, 0, 1024, 1024, Color.FromArgb(255, 48, 50, 56), 180, 140, true, 2101);
            // Tungsten carbide cutterhead disc teeth
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6.0;
                int tx = (int)(512 + Math.Cos(a) * 320);
                int ty = (int)(512 + Math.Sin(a) * 320);
                buf.DrawHexBolt(tx, ty, 24);
            }
            buf.DrawEmissiveLamp(512, 512, 60, Color.FromArgb(255, 255, 180, 20));
            buf.Save(Path.Combine(entDir, "tunnel_bore"));
        }

        // 11. Turret Cart - 1024x1024
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            string srcPath = Path.Combine(srcEntDir, "turret_cart.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            }
            buf.DrawBrushedSteel(0, 0, 1024, 1024, Color.FromArgb(255, 60, 68, 62), 190, 150, true, 2201);
            buf.DrawHexBolt(256, 256, 20);
            buf.DrawHexBolt(768, 256, 20);
            buf.DrawEmissiveLamp(512, 512, 45, Color.FromArgb(255, 255, 50, 30));
            buf.Save(Path.Combine(entDir, "turret_cart"));
        }

        // 12. Track Paver - 1024x1024
        using (PbrBuffer buf = new PbrBuffer(1024, 1024)) {
            string srcPath = Path.Combine(srcEntDir, "track_paver.png");
            if (File.Exists(srcPath)) {
                using (Bitmap src = new Bitmap(srcPath)) {
                    buf.UpscaleFromSource(src);
                }
            }
            buf.DrawDiamondTread(0, 0, 1024, 1024, Color.FromArgb(255, 230, 175, 20), 2301);
            buf.DrawEmissiveLamp(512, 256, 40, Color.FromArgb(255, 255, 240, 120));
            buf.Save(Path.Combine(entDir, "track_paver"));
        }
    }

    private void GenerateItemIcons() {
        Console.WriteLine("Generating HD Item Icons...");
        string itemDir = Path.Combine(outputRoot, "assets/miningtools/textures/item");
        string srcItemDir = Path.Combine(srcAssetsRoot, "textures/item");

        if (Directory.Exists(srcItemDir)) {
            foreach (string file in Directory.GetFiles(srcItemDir, "*.png")) {
                string fileName = Path.GetFileNameWithoutExtension(file);
                using (Bitmap src = new Bitmap(file)) {
                    // Create clean 64x64 HD icon
                    using (PbrBuffer buf = new PbrBuffer(64, 64)) {
                        buf.UpscaleFromSource(src);
                        buf.Save(Path.Combine(itemDir, fileName));
                    }
                }
            }
        }
    }

    private void GeneratePackMetaAndIcon() {
        Console.WriteLine("Generating pack.mcmeta and pack.png...");

        // pack.mcmeta with version ranges
        string mcmeta = @"{
  ""pack"": {
    ""pack_format"": 68.0,
    ""min_format"": 65.0,
    ""max_format"": 99.0,
    ""description"": ""§6§lTrack & Train Kit §b§lSuper HD PBR§r\n§7Full LabPBR 1.3 + POM Depth & Reflections""
  }
}";
        File.WriteAllText(Path.Combine(outputRoot, "pack.mcmeta"), mcmeta);

        // texture.properties for OptiFine & Iris shaders
        string texProps = @"format=lab-pbr
version=1.3
";
        string optifineDir1 = Path.Combine(outputRoot, "assets/minecraft/optifine");
        string optifineDir2 = Path.Combine(outputRoot, "assets/miningtools/optifine");
        Directory.CreateDirectory(optifineDir1);
        Directory.CreateDirectory(optifineDir2);
        File.WriteAllText(Path.Combine(optifineDir1, "texture.properties"), texProps);
        File.WriteAllText(Path.Combine(optifineDir2, "texture.properties"), texProps);

        // pack.png - 512x512 stunning logo icon
        using (PbrBuffer buf = new PbrBuffer(512, 512)) {
            buf.DrawBrushedSteel(0, 0, 512, 512, Color.FromArgb(255, 20, 24, 30), 180, 160, true, 9999);
            // Radiant glowing tracks
            buf.DrawBrushedSteel(90, 0, 32, 512, Color.FromArgb(255, 210, 220, 235), 255, 250, false, 9991);
            buf.DrawBrushedSteel(390, 0, 32, 512, Color.FromArgb(255, 210, 220, 235), 255, 250, false, 9992);
            // Center electric power beam
            buf.FillRect(236, 0, 40, 512, Color.FromArgb(255, 40, 220, 255), 255, 255, 60, 0, 0);
            // Emissive locomotive headlight beacon
            buf.DrawEmissiveLamp(256, 256, 80, Color.FromArgb(255, 255, 240, 180));

            // Save pack.png
            using (Bitmap bmp = new Bitmap(512, 512, PixelFormat.Format32bppArgb)) {
                for (int y = 0; y < 512; y++) {
                    for (int x = 0; x < 512; x++) {
                        bmp.SetPixel(x, y, buf.Albedo[x, y]);
                    }
                }
                bmp.Save(Path.Combine(outputRoot, "pack.png"), ImageFormat.Png);
            }
        }
    }
}
