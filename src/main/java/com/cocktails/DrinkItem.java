package com.cocktails;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class DrinkItem extends Item {
    private final Supplier<MobEffectInstance> effectSupplier;

    public DrinkItem(Properties properties) {
        this(properties, null);
    }

    public DrinkItem(Properties properties, Supplier<MobEffectInstance> effectSupplier) {
        super(properties);
        this.effectSupplier = effectSupplier;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && effectSupplier != null) {
            MobEffectInstance effect = effectSupplier.get();

            if (effect != null) {
                boolean applied = entity.addEffect(new MobEffectInstance(effect));
                if (applied && effect.getEffect() == CocktailsMod.SANGRIA_EFFECT.get() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    SangriaEffect.spawnInitialBurst(serverLevel, entity);
                }
            }
        }

        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);

            if (result.isEmpty()) {
                return bottle;
            }

            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }

        return result;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }
}
