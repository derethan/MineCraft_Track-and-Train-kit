using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Drawing.Text;
using System.IO;
using System.IO.Compression;

namespace HdTexturePackGenerator {
    public class Program {
        public static void Main(string[] args) {
            Console.WriteLine("==========================================================");
            Console.WriteLine("  Track and Train Kit - Super HD 2K LabPBR 1.3 Pack Generator");
            Console.WriteLine("==========================================================");

            string repoRoot = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "../../../../../"));
            string packDir = Path.Combine(repoRoot, "resourcepacks", "Track_and_Train_HD_PBR");
            string zipPath = Path.Combine(repoRoot, "resourcepacks", "Track_and_Train_HD_PBR.zip");
            string curseForgeZip = @"C:\Users\swack\curseforge\minecraft\Instances\26.3\resourcepacks\Track_and_Train_HD_PBR.zip";

            if (Directory.Exists(packDir)) {
                Directory.Delete(packDir, true);
            }
            Directory.CreateDirectory(packDir);

            string entityDir = Path.Combine(packDir, "assets", "miningtools", "textures", "entity");
            string blockDir = Path.Combine(packDir, "assets", "miningtools", "textures", "block");
            string cmodelsDir = Path.Combine(blockDir, "cmodels");

            Directory.CreateDirectory(entityDir);
            Directory.CreateDirectory(blockDir);
            Directory.CreateDirectory(cmodelsDir);

            string baseEntityDir = Path.Combine(repoRoot, "src", "main", "resources", "assets", "miningtools", "textures", "entity");

            // 1. Generate Vehicles & Rolling Stock
            Console.WriteLine("\n[1/4] Generating 2K Vehicles & Rolling Stock...");
            VehicleGenerators.GenerateAllVehicles(baseEntityDir, entityDir);

            // 2. Generate Rails, Tracks & Signals
            Console.WriteLine("\n[2/4] Generating 2K Rails, Tracks & Signals...");
            TrackGenerators.GenerateStraightRail(blockDir);
            TrackGenerators.GenerateCurvedRail(blockDir);
            TrackGenerators.GenerateSignalsAndSwitches(blockDir);
            TrackGenerators.GenerateBlockRails(blockDir);

            // 3. Metadata & Configuration
            Console.WriteLine("\n[3/4] Writing Pack Metadata & Shader Properties...");
            string mcmeta = @"{
  ""pack"": {
    ""pack_format"": 68.0,
    ""min_format"": 65.0,
    ""max_format"": 99.0,
    ""description"": ""§6§lTrack & Train Kit §b§lSuper HD PBR§r\n§7Crisp 2K LabPBR 1.3, True POM Depth & Reflections""
  }
}";
            File.WriteAllText(Path.Combine(packDir, "pack.mcmeta"), mcmeta);

            string texProps = @"format=lab-pbr
version=1.3
";
            string optifineDir1 = Path.Combine(packDir, "assets", "minecraft", "optifine");
            string optifineDir2 = Path.Combine(packDir, "assets", "miningtools", "optifine");
            Directory.CreateDirectory(optifineDir1);
            Directory.CreateDirectory(optifineDir2);
            File.WriteAllText(Path.Combine(optifineDir1, "texture.properties"), texProps);
            File.WriteAllText(Path.Combine(optifineDir2, "texture.properties"), texProps);

            // Generate Pack Icon
            using (Bitmap icon = new Bitmap(512, 512, PixelFormat.Format32bppArgb))
            using (Graphics g = Graphics.FromImage(icon)) {
                g.SmoothingMode = SmoothingMode.AntiAlias;
                g.TextRenderingHint = TextRenderingHint.AntiAliasGridFit;
                g.Clear(Color.FromArgb(255, 18, 20, 24));

                // Gold border
                using (Pen pen = new Pen(Color.FromArgb(255, 230, 180, 45), 12)) {
                    g.DrawRectangle(pen, 10, 10, 492, 492);
                }

                // Locomotive silhouette / banner
                using (SolidBrush header = new SolidBrush(Color.FromArgb(255, 245, 190, 40)))
                using (SolidBrush sub = new SolidBrush(Color.FromArgb(255, 100, 210, 255)))
                using (Font fontH = new Font(FontFamily.GenericSansSerif, 38, FontStyle.Bold))
                using (Font fontS = new Font(FontFamily.GenericSansSerif, 28, FontStyle.Bold)) {
                    g.DrawString("TRACK & TRAIN", fontH, header, 40, 180);
                    g.DrawString("SUPER HD PBR", fontS, sub, 75, 250);
                }

                icon.Save(Path.Combine(packDir, "pack.png"), ImageFormat.Png);
            }

            // 4. Archive into ZIP with normalized forward slashes
            Console.WriteLine("\n[4/4] Creating ZIP archive with normalized forward slashes...");
            if (File.Exists(zipPath)) {
                File.Delete(zipPath);
            }

            using (ZipArchive zip = ZipFile.Open(zipPath, ZipArchiveMode.Create)) {
                string[] allFiles = Directory.GetFiles(packDir, "*.*", SearchOption.AllDirectories);
                foreach (string file in allFiles) {
                    string rel = Path.GetRelativePath(packDir, file).Replace('\\', '/');
                    zip.CreateEntryFromFile(file, rel, CompressionLevel.Optimal);
                }
            }

            Console.WriteLine($"Successfully built ZIP ({new FileInfo(zipPath).Length / 1024 / 1024} MB) at:\n  {zipPath}");

            // Deploy to CurseForge instance
            if (Directory.Exists(Path.GetDirectoryName(curseForgeZip))) {
                File.Copy(zipPath, curseForgeZip, true);
                Console.WriteLine($"\n[DEPLOYED] Copied directly to CurseForge resourcepacks directory:\n  {curseForgeZip}");
            }

            Console.WriteLine("\n=== SUPER HD LABPBR GENERATION COMPLETE ===");
        }
    }
}
