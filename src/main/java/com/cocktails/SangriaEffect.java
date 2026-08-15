package com.cocktails;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;

public class SangriaEffect extends MobEffect {
    public static final DustParticleOptions RED_PARTICLE = new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.2F);

    public SangriaEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x990000);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return Collections.emptyList();
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;

        Level level = entity.level();
        BlockPos playerPos = entity.blockPosition();
        int radius = 32;

        JukeboxBlockEntity nearestJukebox = null;
        double nearestDistanceSq = Double.MAX_VALUE;

        BlockPos minPos = playerPos.offset(-radius, -16, -radius);
        BlockPos maxPos = playerPos.offset(radius, 16, radius);

        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            if (level.getBlockState(pos).hasProperty(JukeboxBlock.HAS_RECORD) && level.getBlockState(pos).getValue(JukeboxBlock.HAS_RECORD)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof JukeboxBlockEntity jukebox && jukebox.isRecordPlaying()) {
                    double distSq = pos.distSqr(playerPos);
                    if (distSq < nearestDistanceSq) {
                        nearestDistanceSq = distSq;
                        nearestJukebox = jukebox;
                    }
                }
            }
        }

        if (nearestJukebox != null) {
            ItemStack discStack = nearestJukebox.getFirstItem();
            MobEffect jukeboxEffect = getEffectForDisc(discStack);
            if (jukeboxEffect != null) {
                entity.addEffect(new MobEffectInstance(jukeboxEffect, 40, 0, true, true, true));
            }

            if (level instanceof ServerLevel serverLevel) {
                BlockPos jpos = nearestJukebox.getBlockPos();
                serverLevel.sendParticles(RED_PARTICLE, jpos.getX() + 0.5, jpos.getY() + 1.0, jpos.getZ() + 0.5, 6, 0.05, 0.05, 0.05, 0.15);
            }
        } else {
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, true, true, true));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, true, true));
        }
    }

    public static void spawnInitialBurst(ServerLevel level, LivingEntity entity) {
        BlockPos playerPos = entity.blockPosition();
        int radius = 32;
        JukeboxBlockEntity nearestJukebox = null;
        double nearestDistanceSq = Double.MAX_VALUE;

        BlockPos minPos = playerPos.offset(-radius, -16, -radius);
        BlockPos maxPos = playerPos.offset(radius, 16, radius);

        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            if (level.getBlockState(pos).hasProperty(JukeboxBlock.HAS_RECORD)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof JukeboxBlockEntity jukebox) {
                    double distSq = pos.distSqr(playerPos);
                    if (distSq < nearestDistanceSq) {
                        nearestDistanceSq = distSq;
                        nearestJukebox = jukebox;
                    }
                }
            }
        }

        if (nearestJukebox != null) {
            BlockPos jpos = nearestJukebox.getBlockPos();
            level.sendParticles(RED_PARTICLE, jpos.getX() + 0.5, jpos.getY() + 0.5, jpos.getZ() + 0.5, 15, 0.25, 0.25, 0.25, 0.02);
        }
    }

    private MobEffect getEffectForDisc(ItemStack discStack) {
        if (discStack.isEmpty() || !(discStack.getItem() instanceof RecordItem recordItem)) {
            return null;
        }

        String path = ForgeRegistries.ITEMS.getKey(recordItem).getPath();

        return switch (path) {
            case "music_disc_13" -> MobEffects.DIG_SPEED;
            case "music_disc_cat" -> MobEffects.REGENERATION;
            case "music_disc_blocks" -> MobEffects.DAMAGE_RESISTANCE;
            case "music_disc_chirp" -> MobEffects.MOVEMENT_SPEED;
            case "music_disc_far" -> MobEffects.LUCK;
            case "music_disc_mall" -> MobEffects.WATER_BREATHING;
            case "music_disc_mellohi" -> MobEffects.NIGHT_VISION;
            case "music_disc_stal" -> MobEffects.DAMAGE_BOOST;
            case "music_disc_strad" -> MobEffects.JUMP;
            case "music_disc_ward" -> MobEffects.FIRE_RESISTANCE;
            case "music_disc_11" -> MobEffects.INVISIBILITY;
            case "music_disc_wait" -> MobEffects.SLOW_FALLING;
            default -> MobEffects.HEALTH_BOOST;
        };
    }
}
