package com.algeron.cosmicarmor.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public enum CosmicArmorMaterial implements ArmorMaterial {
    COSMIC("cosmic", 0, new int[]{14, 22, 30, 16}, 30, SoundEvents.ARMOR_EQUIP_NETHERITE, 12.0F, 0.25F);

    private final String name;
    private final int durabilityMultiplier;
    private final int[] protectionFunctionForType;
    private final int enchantmentValue;
    private final SoundEvent sound;
    private final float toughness;
    private final float knockbackResistance;

    CosmicArmorMaterial(String name, int durabilityMultiplier, int[] protectionFunctionForType, int enchantmentValue,
                        SoundEvent sound, float toughness, float knockbackResistance) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protectionFunctionForType = protectionFunctionForType;
        this.enchantmentValue = enchantmentValue;
        this.sound = sound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        // Kırılmaz zırh - dayanıklılık tükenmez
        return 0;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        // 12. Boss seviyesi devasa koruma değerleri (Toplam 82 Zırh Değeri)
        return switch (type) {
            case BOOTS -> 14;
            case LEGGINGS -> 22;
            case CHESTPLATE -> 30;
            case HELMET -> 16;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.sound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.NETHERITE_INGOT);
    }

    @Override
    public String getName() {
        return "cosmicarmor:cosmic";
    }

    @Override
    public float getToughness() {
        return this.toughness; // Parça başı 12.0F (Toplam 48.0F Zırh Sertliği)
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance; // Parça başı 0.25F (Toplam 1.0F = %100 Geri İtilme Bağışıklığı)
    }
}
