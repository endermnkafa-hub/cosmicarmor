package com.algeron.cosmicarmor.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Kızıltaş Derinlikleri için 3D Yeraltı Biyom Kaynağı (BiomeSource Decorator).
 *
 * Temel Özellikler:
 * 1. possibleBiomes() kümesine cosmicarmor:redstone_depths biyomunu garanti olarak ekler.
 * 2. Yükseklik sınırını kesin olarak korur: Biyom yalnızca Y in [-64, -16] aralığında
 *    (QuartPos: -16 ile -4 arası) döndürülür. Yüzeyde (Y >= 0) ASLA üretilmez, yüzey %100 vanilla kalır.
 * 3. /locate biome komutunun dikey 64 blok adımıyla 48 blokluk derinlik tabakasını atlamasını önler;
 *    aramayı doğrudan biyomun gerçek derinliğinde (Y = -42) hedefler.
 */
public class RedstoneDepthsBiomeSource extends BiomeSource {

    private final BiomeSource delegate;
    private final Holder<Biome> redstoneDepths;
    private final Set<Holder<Biome>> allPossibleBiomes;
    private final long worldSeed;

    // Bedrock tabakası hemen üstü (Y: -64 ile -16 arası, QuartPos: -16 ile -4)
    public static final int MIN_QUART_Y = -16; // Y = -64 blok
    public static final int MAX_QUART_Y = -4;  // Y = -16 blok
    public static final int TARGET_QUART_Y = -11; // Y = -44..-41 blok (ortalama Y = -42)

    public RedstoneDepthsBiomeSource(BiomeSource delegate, Holder<Biome> redstoneDepths, long seed) {
        this.delegate = delegate;
        this.redstoneDepths = redstoneDepths;
        this.worldSeed = seed;

        Set<Holder<Biome>> set = new LinkedHashSet<>(delegate.possibleBiomes());
        set.add(redstoneDepths);
        this.allPossibleBiomes = Collections.unmodifiableSet(set);
    }

    public static final Codec<RedstoneDepthsBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("delegate").forGetter(RedstoneDepthsBiomeSource::getDelegate),
                    Biome.CODEC.fieldOf("redstone_depths").forGetter(RedstoneDepthsBiomeSource::getRedstoneDepthsHolder),
                    Codec.LONG.fieldOf("seed").forGetter(RedstoneDepthsBiomeSource::getWorldSeed)
            ).apply(instance, RedstoneDepthsBiomeSource::new)
    );

    public BiomeSource getDelegate() {
        return this.delegate;
    }

    public Holder<Biome> getRedstoneDepthsHolder() {
        return this.redstoneDepths;
    }

    public long getWorldSeed() {
        return this.worldSeed;
    }

    @Override
    public Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(delegate.possibleBiomes().stream(), Stream.of(redstoneDepths));
    }

    @Override
    public Set<Holder<Biome>> possibleBiomes() {
        return this.allPossibleBiomes;
    }

    /**
     * Biyom belirleme fonksiyonu:
     * - Y > -16 (ve yüzey Y >= 0): Kesinlikle redstone_depths döndürmez!
     * - Y in [-64, -16] (QuartPos: -16..-4): Yalnızca hedef nadir bölgelerde redstone_depths döner.
     */
    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        if (quartY >= MIN_QUART_Y && quartY <= MAX_QUART_Y) {
            if (isRedstoneDepthsZone(quartX, quartZ)) {
                return this.redstoneDepths;
            }
        }
        return this.delegate.getNoiseBiome(quartX, quartY, quartZ, sampler);
    }

    /**
     * Deterministik, dünya tohumuna bağlı, ~150-250 blok genişliğinde nadir yeraltı cepleri.
     * Dünyanın derin katmanında yaklaşık %2.5-3.0 oranında Kızıltaş Derinlikleri oluşturur.
     */
    public boolean isRedstoneDepthsZone(int quartX, int quartZ) {
        double scale = 0.0075D; // ~150-200 blokluk biyom yamaları
        double nx = quartX * scale;
        double nz = quartZ * scale;

        double seedShiftX = ((this.worldSeed >> 16) & 0xFFFF) * 0.1D;
        double seedShiftZ = ((this.worldSeed) & 0xFFFF) * 0.1D;

        double v1 = Math.sin(nx + seedShiftX) * Math.cos(nz + seedShiftZ);
        double v2 = Math.sin((nx * 1.7D) - (nz * 1.3D) + 2.1D) * 0.5D;
        double v3 = Math.cos((nx * 0.7D) + (nz * 2.1D) - 1.4D) * 0.3D;

        double composite = (v1 + v2 + v3) / 1.8D;

        return composite > 0.68D;
    }

    /**
     * /locate biome komutu için 3D arama:
     * LocateCommand dikeyde 64 blok aralıklarla örnekleme yaptığından,
     * oyuncu dağda veya yüksekte olduğunda 48 blokluk derinlik tabakası atlanabilir.
     * Bu metod, hedef biyomumuz arandığında dikey aramayı tam olarak biyomun var olduğu
     * Y = -42 (QuartPos: -11) seviyesinde gerçekleştirir.
     * Biyom asla yüzeye taşınmaz, doğrudan mağaranın gerçek koordinatını döner.
     */
    @Override
    public Pair<BlockPos, Holder<Biome>> findClosestBiome3d(
            BlockPos origin, int radius, int horizontalStep, int verticalStep,
            Predicate<Holder<Biome>> predicate, Climate.Sampler sampler, LevelReader levelReader) {

        if (predicate.test(this.redstoneDepths)) {
            int step = Math.max(16, horizontalStep);
            int maxR = radius > 0 ? radius : 6400;
            int originX = origin.getX();
            int originZ = origin.getZ();

            int maxSteps = maxR / step;
            int x = 0;
            int z = 0;
            int dx = 0;
            int dz = -1;

            for (int i = 0; i < (maxSteps * 2 + 1) * (maxSteps * 2 + 1); i++) {
                if ((-maxSteps <= x) && (x <= maxSteps) && (-maxSteps <= z) && (z <= maxSteps)) {
                    int worldX = originX + x * step;
                    int worldZ = originZ + z * step;
                    int qX = QuartPos.fromBlock(worldX);
                    int qZ = QuartPos.fromBlock(worldZ);

                    if (isRedstoneDepthsZone(qX, qZ)) {
                        BlockPos foundPos = new BlockPos(worldX, -42, worldZ);
                        return Pair.of(foundPos, this.redstoneDepths);
                    }
                }

                if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                    int temp = dx;
                    dx = -dz;
                    dz = temp;
                }
                x += dx;
                z += dz;
            }

            return null;
        }

        return this.delegate.findClosestBiome3d(origin, radius, horizontalStep, verticalStep, predicate, sampler, levelReader);
    }
}
