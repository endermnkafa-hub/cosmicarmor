package com.algeron.cosmicarmor.item;

import com.algeron.cosmicarmor.client.CosmicArmorRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public final class CosmicArmorItem extends ArmorItem implements GeoItem {
    private final ArmorItem.Type armorType;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CosmicArmorItem(ArmorItem.Type type) {
        super(CosmicArmorMaterial.COSMIC, type, new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        this.armorType = type;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§6§lSon Kademe Zırh (12. Boss Seviyesi)").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("§7Evrenin derinliklerinden dövülmüş kırılmaz zırh.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§c◆ Kırılmaz Dayanıklılık").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§e◆ %95 Saf Hasar Azaltımı (Tam Set)").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§b◆ Sarsılmaz Geri İtilme Direnci").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CosmicArmorRenderer renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity,
                                                          ItemStack itemStack,
                                                          EquipmentSlot equipmentSlot,
                                                          HumanoidModel<?> original) {
                if (this.renderer == null) {
                    this.renderer = new CosmicArmorRenderer(CosmicArmorItem.this.armorType == ArmorItem.Type.LEGGINGS);
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }
}
