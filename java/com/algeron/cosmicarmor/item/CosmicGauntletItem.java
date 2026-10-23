package com.algeron.cosmicarmor.item;

import com.algeron.cosmicarmor.vfx.CosmicAuraVFX;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class CosmicGauntletItem extends Item implements ICurioItem {
    private final CosmicAuraVFX.AuraType auraType;

    public CosmicGauntletItem(CosmicAuraVFX.AuraType auraType) {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
        this.auraType = auraType;
    }

    public CosmicGauntletItem() {
        this(CosmicAuraVFX.AuraType.STANDARD);
    }

    public CosmicAuraVFX.AuraType getAuraType() {
        return this.auraType;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        // Allows equipping into hands, ring, or general curio slots
        String id = slotContext.identifier();
        return "hands".equals(id) || "curio".equals(id) || "ring".equals(id);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return canEquip(slotContext, stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        // Right click directly equips into Curios if available
        if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            var curioInv = CuriosApi.getCuriosInventory(player);
            if (curioInv.isPresent()) {
                var handler = curioInv.resolve().get();
                for (var stacks : handler.getCurios().values()) {
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        if (stacks.getStacks().getStackInSlot(i).isEmpty()) {
                            stacks.getStacks().setStackInSlot(i, stack.copy());
                            stack.shrink(1);
                            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                        }
                    }
                }
            }
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity().level().isClientSide) {
            CosmicAuraVFX.spawnAuraParticles(slotContext.entity(), this.auraType);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide && entity instanceof LivingEntity living) {
            // Emits the aura when held in either hand
            if (living.getMainHandItem() == stack || living.getOffhandItem() == stack) {
                CosmicAuraVFX.spawnAuraParticles(living, this.auraType);
            }
        }
    }
}
