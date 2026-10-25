using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;

public class TextureUtils {
    public static int Clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    public static float Clamp(float v, float min, float max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    public static Color Blend(Color c1, Color c2, float t) {
        t = Clamp(t, 0f, 1f);
        int a = (int)(c1.A * (1f - t) + c2.A * t);
        int r = (int)(c1.R * (1f - t) + c2.R * t);
        int g = (int)(c1.G * (1f - t) + c2.G * t);
        int b = (int)(c1.B * (1f - t) + c2.B * t);
        return Color.FromArgb(Clamp(a, 0, 255), Clamp(r, 0, 255), Clamp(g, 0, 255), Clamp(b, 0, 255));
    }

    public static float Hash(int x, int y, int seed) {
        int n = x + y * 57 + seed * 131;
        n = (n << 13) ^ n;
        return (1.0f - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0f) * 0.5f + 0.5f;
    }

    public static float SmoothNoise(float x, float y, int seed) {
        int ix = (int)Math.Floor(x);
        int iy = (int)Math.Floor(y);
        float fx = x - ix;
        float fy = y - iy;

        float sX = fx * fx * (3.0f - 2.0f * fx);
        float sY = fy * fy * (3.0f - 2.0f * fy);

        float n00 = Hash(ix, iy, seed);
        float n10 = Hash(ix + 1, iy, seed);
        float n01 = Hash(ix, iy + 1, seed);
        float n11 = Hash(ix + 1, iy + 1, seed);

        float nx0 = n00 * (1.0f - sX) + n10 * sX;
        float nx1 = n01 * (1.0f - sX) + n11 * sX;

        return nx0 * (1.0f - sY) + nx1 * sY;
    }
}
