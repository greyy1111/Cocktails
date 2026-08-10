package com.cocktails;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

public class ScrewdriverEffect extends MobEffect {
    public ScrewdriverEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF8C00);
        this.addAttributeModifier(
                Attributes.KNOCKBACK_RESISTANCE,
                "c5a709d2-4321-4f10-9111-abcdef123456",
                0.9D,
                AttributeModifier.Operation.ADDITION
        );
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return Collections.emptyList();
    }
}
