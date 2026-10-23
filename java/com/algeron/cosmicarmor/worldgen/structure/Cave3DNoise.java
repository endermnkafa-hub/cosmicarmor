package com.algeron.cosmicarmor.worldgen.structure;

/**
 * Kızıltaş Derinlikleri için yüksek performanslı, deterministik 3D Coherent Gradient Gürültü Üreticisi.
 *
 * Sıfır nesne tahsisi (zero-allocation) ile çalışır.
 * 3D Simplex/Perlin gradyan gürültüsü, çok oktavlı fBm ve 3D Domain Warping desteği sunar.
 */
public final class Cave3DNoise {

    private Cave3DNoise() {}

    private static final int[] GRAD3 = {
            1, 1, 0, -1, 1, 0, 1, -1, 0, -1, -1, 0,
            1, 0, 1, -1, 0, 1, 1, 0, -1, -1, 0, -1,
            0, 1, 1, 0, -1, 1, 0, 1, -1, 0, -1, -1
    };

    /**
     * 32-bit deterministik tam sayı karma fonksiyonu (Murmur / Wang hash türevi)
     */
    private static int hash(int x, int y, int z, long seed) {
        long h = seed ^ (x * 73856093L) ^ (y * 19349663L) ^ (z * 83492791L);
        h = (h ^ (h >>> 16)) * 0x85ebca6bL;
        h = (h ^ (h >>> 13)) * 0xc2b2ae35L;
        return (int) (h ^ (h >>> 16));
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D); // Quintic interpolant
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z) {
        int h = (hash & 15);
        int idx = (h < 12 ? h : h & 3) * 3;
        int gx = GRAD3[idx];
        int gy = GRAD3[idx + 1];
        int gz = GRAD3[idx + 2];
        return gx * x + gy * y + gz * z;
    }

    /**
     * Temel 3D Coherent Perlin/Gradient Gürültüsü.
     * @return [-1.0, 1.0] aralığında sürekli, pürüzsüz değer
     */
    public static double noise3D(double x, double y, double z, long seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int z0 = (int) Math.floor(z);

        double dx = x - x0;
        double dy = y - y0;
        double dz = z - z0;

        double u = fade(dx);
        double v = fade(dy);
        double w = fade(dz);

        int h000 = hash(x0, y0, z0, seed);
        int h100 = hash(x0 + 1, y0, z0, seed);
        int h010 = hash(x0, y0 + 1, z0, seed);
        int h110 = hash(x0 + 1, y0 + 1, z0, seed);
        int h001 = hash(x0, y0, z0 + 1, seed);
        int h101 = hash(x0 + 1, y0, z0 + 1, seed);
        int h011 = hash(x0, y0 + 1, z0 + 1, seed);
        int h111 = hash(x0 + 1, y0 + 1, z0 + 1, seed);

        double g000 = grad(h000, dx, dy, dz);
        double g100 = grad(h100, dx - 1, dy, dz);
        double g010 = grad(h010, dx, dy - 1, dz);
        double g110 = grad(h110, dx - 1, dy - 1, dz);
        double g001 = grad(h001, dx, dy, dz - 1);
        double g101 = grad(h101, dx - 1, dy, dz - 1);
        double g011 = grad(h011, dx, dy - 1, dz - 1);
        double g111 = grad(h111, dx - 1, dy - 1, dz - 1);

        double x1 = lerp(g000, g100, u);
        double x2 = lerp(g010, g110, u);
        double y1 = lerp(x1, x2, v);

        double x3 = lerp(g001, g101, u);
        double x4 = lerp(g011, g111, u);
        double y2 = lerp(x3, x4, v);

        return lerp(y1, y2, w);
    }

    /**
     * Çok oktavlı Fractal Brownian Motion (fBm) 3D gürültü.
     */
    public static double fbm3D(double x, double y, double z, int octaves, double lacunarity, double gain, long seed) {
        double sum = 0.0D;
        double amp = 1.0D;
        double maxAmp = 0.0D;

        for (int i = 0; i < octaves; i++) {
            sum += noise3D(x, y, z, seed + (i * 31L)) * amp;
            maxAmp += amp;
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
            amp *= gain;
        }

        return sum / maxAmp;
    }

    /**
     * 3 Boyutlu Domain Warped Gürültü.
     * Koordinatları kendi gürültü alanıyla bükerek doğal mağara kıvrımları, kemerler ve organik formlar üretir.
     */
    public static double domainWarp3D(double x, double y, double z, double warpStrength, long seed) {
        double wx = noise3D(x, y, z, seed);
        double wy = noise3D(x + 5.2D, y + 1.3D, z + 2.8D, seed + 11L);
        double wz = noise3D(x - 3.4D, y - 2.7D, z + 7.1D, seed + 23L);

        return noise3D(x + wx * warpStrength, y + wy * warpStrength, z + wz * warpStrength, seed + 37L);
    }

    /**
     * 2D Gradyan Gürültü (Yüzey yamaları ve sütun dağılımı için)
     */
    public static double noise2D(double x, double z, long seed) {
        return noise3D(x, 0.0D, z, seed);
    }
}
