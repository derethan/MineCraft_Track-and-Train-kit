using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;

using System.Runtime.CompilerServices;

namespace HdTexturePackGenerator {
    public class PbrCanvas : IDisposable {
        public int Width { get; }
        public int Height { get; }

        public Color[] Albedo;
        public byte[] HeightMap;
        public byte[] Smoothness;
        public byte[] MetalId;
        public byte[] Porosity;
        public byte[] Emissive;

        public PbrCanvas(int width, int height) {
            Width = width;
            Height = height;
            int total = width * height;
            Albedo = new Color[total];
            HeightMap = new byte[total];
            Smoothness = new byte[total];
            MetalId = new byte[total];
            Porosity = new byte[total];
            Emissive = new byte[total];

            for (int i = 0; i < total; i++) {
                Albedo[i] = Color.FromArgb(0, 0, 0, 0);
                HeightMap[i] = 128;
                Smoothness[i] = 40;
                MetalId[i] = 0;
                Porosity[i] = 0;
                Emissive[i] = 255; // 255 = Off in LabPBR
            }
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void SetPixel(int x, int y, Color color, byte h, byte smooth, byte metal, byte porous = 0, byte em = 255) {
            if (x < 0 || x >= Width || y < 0 || y >= Height) return;
            int idx = y * Width + x;
            Albedo[idx] = color;
            HeightMap[idx] = h;
            Smoothness[idx] = smooth;
            MetalId[idx] = metal;
            Porosity[idx] = porous;
            Emissive[idx] = em;
        }

        public void FillRect(int rx, int ry, int rw, int rh, Color color, byte h, byte smooth, byte metal, byte porous = 0, byte em = 255) {
            int x1 = Math.Max(0, rx);
            int y1 = Math.Max(0, ry);
            int x2 = Math.Min(Width, rx + rw);
            int y2 = Math.Min(Height, ry + rh);

            for (int y = y1; y < y2; y++) {
                int row = y * Width;
                for (int x = x1; x < x2; x++) {
                    int idx = row + x;
                    Albedo[idx] = color;
                    HeightMap[idx] = h;
                    Smoothness[idx] = smooth;
                    MetalId[idx] = metal;
                    Porosity[idx] = porous;
                    Emissive[idx] = em;
                }
            }
        }

        public void Save(string basePath) {
            Directory.CreateDirectory(Path.GetDirectoryName(basePath)!);

            // 1. Albedo diffuse
            using (Bitmap diffBmp = new Bitmap(Width, Height, PixelFormat.Format32bppArgb)) {
                BitmapData data = diffBmp.LockBits(new Rectangle(0, 0, Width, Height), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
                unsafe {
                    byte* ptr = (byte*)data.Scan0;
                    for (int y = 0; y < Height; y++) {
                        byte* row = ptr + y * data.Stride;
                        int rowIdx = y * Width;
                        for (int x = 0; x < Width; x++) {
                            Color c = Albedo[rowIdx + x];
                            int px = x * 4;
                            row[px + 0] = c.B;
                            row[px + 1] = c.G;
                            row[px + 2] = c.R;
                            row[px + 3] = c.A;
                        }
                    }
                }
                diffBmp.UnlockBits(data);
                diffBmp.Save(basePath + ".png", ImageFormat.Png);
            }

            // 2. Normal map (_n.png):
            // Red: Normal X (tangent space: 0 = -1.0, 128 = 0.0, 255 = +1.0)
            // Green: Normal Y (0 = -1.0, 128 = 0.0, 255 = +1.0)
            // Blue: Ambient Occlusion (0 = full occlusion, 255 = no occlusion)
            // Alpha: Parallax Height Map (0 = deep recess, 255 = full extrusion)
            using (Bitmap normBmp = new Bitmap(Width, Height, PixelFormat.Format32bppArgb)) {
                BitmapData data = normBmp.LockBits(new Rectangle(0, 0, Width, Height), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
                unsafe {
                    byte* ptr = (byte*)data.Scan0;
                    for (int y = 0; y < Height; y++) {
                        byte* row = ptr + y * data.Stride;
                        int rowIdx = y * Width;
                        int y0 = Math.Max(0, y - 1);
                        int y1 = Math.Min(Height - 1, y + 1);

                        for (int x = 0; x < Width; x++) {
                            int x0 = Math.Max(0, x - 1);
                            int x1 = Math.Min(Width - 1, x + 1);

                            int idx = rowIdx + x;
                            int px = x * 4;
                            if (Albedo[idx].A == 0) {
                                row[px + 0] = 255; // B (AO)
                                row[px + 1] = 128; // G (Y)
                                row[px + 2] = 128; // R (X)
                                row[px + 3] = 0;   // A (Height)
                                continue;
                            }

                            // Sobel 3x3 filter for high precision normal derivatives
                            float tl = HeightMap[y0 * Width + x0];
                            float tc = HeightMap[y0 * Width + x];
                            float tr = HeightMap[y0 * Width + x1];
                            float ml = HeightMap[rowIdx + x0];
                            float mr = HeightMap[rowIdx + x1];
                            float bl = HeightMap[y1 * Width + x0];
                            float bc = HeightMap[y1 * Width + x];
                            float br = HeightMap[y1 * Width + x1];

                            float dX = (tr + 2f * mr + br) - (tl + 2f * ml + bl);
                            float dY = (bl + 2f * bc + br) - (tl + 2f * tc + tr);
                            float dZ = 64f; // surface normal scale

                            // Normalize vector
                            float len = (float)Math.Sqrt(dX * dX + dY * dY + dZ * dZ);
                            float nx = dX / len;
                            float ny = dY / len;
                            float nz = dZ / len;

                            byte rX = (byte)Math.Clamp((int)((nx * 0.5f + 0.5f) * 255f), 0, 255);
                            byte rY = (byte)Math.Clamp((int)((ny * 0.5f + 0.5f) * 255f), 0, 255);

                            // Ambient occlusion baked into Blue channel
                            // Cavity AO from local height compared to surrounding neighborhood
                            float avgSurround = (tl + tc + tr + ml + mr + bl + bc + br) / 8f;
                            float curH = HeightMap[idx];
                            float aoDiff = curH - avgSurround;
                            byte ao = (byte)Math.Clamp((int)(240 + aoDiff * 1.5f), 70, 255);

                            row[px + 0] = ao;           // Blue (AO)
                            row[px + 1] = rY;           // Green (Normal Y)
                            row[px + 2] = rX;           // Red (Normal X)
                            row[px + 3] = (byte)curH;   // Alpha (Height)
                        }
                    }
                }
                normBmp.UnlockBits(data);
                normBmp.Save(basePath + "_n.png", ImageFormat.Png);
            }

            // 3. Specular map (_s.png):
            // Red: Smoothness / Perceptual Roughness (0 = diffuse, 255 = mirror)
            // Green: Metalness (0 = dielectric, 230 = Iron, 234 = Brass/Copper, 233 = Chrome)
            // Blue: Porosity (0..64) / SSS (65..255)
            // Alpha: Emissive (0..254 = emissive glow brightness, 255 = not emissive)
            using (Bitmap specBmp = new Bitmap(Width, Height, PixelFormat.Format32bppArgb)) {
                BitmapData data = specBmp.LockBits(new Rectangle(0, 0, Width, Height), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
                unsafe {
                    byte* ptr = (byte*)data.Scan0;
                    for (int y = 0; y < Height; y++) {
                        byte* row = ptr + y * data.Stride;
                        int rowIdx = y * Width;
                        for (int x = 0; x < Width; x++) {
                            int idx = rowIdx + x;
                            int px = x * 4;
                            if (Albedo[idx].A == 0) {
                                row[px + 0] = 0;
                                row[px + 1] = 0;
                                row[px + 2] = 0;
                                row[px + 3] = 255;
                            } else {
                                row[px + 0] = Porosity[idx];   // Blue
                                row[px + 1] = MetalId[idx];    // Green
                                row[px + 2] = Smoothness[idx]; // Red
                                row[px + 3] = Emissive[idx];   // Alpha
                            }
                        }
                    }
                }
                specBmp.UnlockBits(data);
                specBmp.Save(basePath + "_s.png", ImageFormat.Png);
            }
        }

        public void Dispose() {
            Albedo = null!;
            HeightMap = null!;
            Smoothness = null!;
            MetalId = null!;
            Porosity = null!;
            Emissive = null!;
        }
    }
}
