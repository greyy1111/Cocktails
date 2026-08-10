package com.cocktails;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class YorshEffect extends MobEffect {
    public static final UUID YORSH_DAMAGE_MODIFIER_UUID = UUID.fromString("b896f4e1-7d1a-4c28-98e6-123456789abc");

    public YorshEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFD700);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return Collections.emptyList();
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;

        MobEffectInstance instance = entity.getEffect(this);
        if (instance == null) return;

        double targetModifier = isActivePhase(entity, instance) ? 6.0 : -4.0;

        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            AttributeModifier currentMod = attr.getModifier(YORSH_DAMAGE_MODIFIER_UUID);
            if (currentMod == null || currentMod.getAmount() != targetModifier) {
                if (currentMod != null) {
                    attr.removeModifier(YORSH_DAMAGE_MODIFIER_UUID);
                }
                attr.addTransientModifier(new AttributeModifier(
                        YORSH_DAMAGE_MODIFIER_UUID,
                        "Yorsh Phase Damage Modifier",
                        targetModifier,
                        AttributeModifier.Operation.ADDITION
                ));
            }
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap map, int amplifier) {
        super.removeAttributeModifiers(entity, map, amplifier);
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null && attr.getModifier(YORSH_DAMAGE_MODIFIER_UUID) != null) {
            attr.removeModifier(YORSH_DAMAGE_MODIFIER_UUID);
        }
    }

    public static boolean isInActivePhase(LivingEntity entity) {
        MobEffectInstance instance = entity.getEffect(CocktailsMod.YORSH_EFFECT.get());
        return instance != null && isActivePhase(entity, instance);
    }

    private static boolean isActivePhase(LivingEntity entity, MobEffectInstance instance) {
        int duration = instance.getDuration();
        int cycle = duration < 0
                ? Math.floorMod(entity.tickCount, 300)
                : Math.floorMod(-duration, 300);
        return cycle < 200;
    }
}
