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

    public static final int PLACEMENT_SPACING = 64;
    public static final int PLACEMENT_SEPARATION = 16;
    public static final int PLACEMENT_SALT = 1642893;

    /**
     * Her dünyada tam olarak 1 adet Kızıltaş Derinlikleri merkezi deterministik ve nadir olarak belirlenir.
     * Orijinden 2400-4000 blok uzakta rastgele bir açıda tek bir bölüm seçilir.
     * Bu merkez, StructurePlacement'in o bölümdeki aday chunk'ı ile birebir aynıdır.
     */
    public static net.minecraft.world.level.ChunkPos getUniqueCenterChunk(long seed) {
        net.minecraft.world.level.levelgen.WorldgenRandom r = new net.minecraft.world.level.levelgen.WorldgenRandom(
                new net.minecraft.world.level.levelgen.LegacyRandomSource(0L)
        );
        r.setLargeFeatureWithSalt(seed, 0, 0, PLACEMENT_SALT);

        // 360 derecelik rastgele bir açıda 150-250 chunk (2400 - 4000 blok) mesafe
        double angle = (r.nextInt(360)) * (Math.PI / 180.0D);
        int chunkDistance = 150 + r.nextInt(100);

        int targetChunkX = (int) Math.round(Math.cos(angle) * chunkDistance);
        int targetChunkZ = (int) Math.round(Math.sin(angle) * chunkDistance);

        int sectionX = Math.floorDiv(targetChunkX, PLACEMENT_SPACING);
        int sectionZ = Math.floorDiv(targetChunkZ, PLACEMENT_SPACING);

        r.setLargeFeatureWithSalt(seed, sectionX, sectionZ, PLACEMENT_SALT);
        int k = PLACEMENT_SPACING - PLACEMENT_SEPARATION; // 48
        int chunkX = sectionX * PLACEMENT_SPACING + r.nextInt(k);
        int chunkZ = sectionZ * PLACEMENT_SPACING + r.nextInt(k);

        return new net.minecraft.world.level.ChunkPos(chunkX, chunkZ);
    }

    public static BlockPos getUniqueCenterPos(long seed) {
        net.minecraft.world.level.ChunkPos chunk = getUniqueCenterChunk(seed);
        return new BlockPos(chunk.getMiddleBlockX(), -42, chunk.getMiddleBlockZ());
    }

    /**
     * Biyom bölgesi kontrolü:
     * Dünya başına tek bir merkez etrafında, en az 160 blok yarıçaplı (320x320 alan)
     * organik dalgalı kenarlara sahip geniş bölgeyi tanımlar.
     */
    public boolean isRedstoneDepthsZone(int quartX, int quartZ) {
        BlockPos center = getUniqueCenterPos(this.worldSeed);
        int centerQuartX = QuartPos.fromBlock(center.getX());
        int centerQuartZ = QuartPos.fromBlock(center.getZ());

        double dx = (quartX - centerQuartX) * 4.0D;
        double dz = (quartZ - centerQuartZ) * 4.0D;
        double distSq = dx * dx + dz * dz;

        // Asgari 160 blok yarıçap garantisi (167 - 183 blok arası organik dalgalanma)
        double angle = Math.atan2(dz, dx);
        double seedShift = ((this.worldSeed & 0xFFFF) * 0.001D);
        double wobble = Math.sin(3.0D * angle + seedShift) * 5.0D + Math.cos(5.0D * angle - seedShift) * 3.0D;
        double radius = 175.0D + wobble; // 167.0D ile 183.0D arasında (kesinlikle >= 160.0D)

        return distSq <= (radius * radius);
    }

    /**
     * /locate biome komutu için doğrudan gerçek biyom merkezini döner:
     * Biyom dikeyde yalnızca Y in [-64, -16] aralığında yer aldığından,
     * merkez koordinat Y=-42 seviyesinde hedeflenir.
     */
    @Override
    public Pair<BlockPos, Holder<Biome>> findClosestBiome3d(
            BlockPos origin, int radius, int horizontalStep, int verticalStep,
            Predicate<Holder<Biome>> predicate, Climate.Sampler sampler, LevelReader levelReader) {

        if (predicate.test(this.redstoneDepths)) {
            BlockPos center = getUniqueCenterPos(this.worldSeed);
            return Pair.of(center, this.redstoneDepths);
        }

        return this.delegate.findClosestBiome3d(origin, radius, horizontalStep, verticalStep, predicate, sampler, levelReader);
    }
}
