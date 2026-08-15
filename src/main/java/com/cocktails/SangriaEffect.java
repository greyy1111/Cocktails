package com.cocktails;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;

public class SangriaEffect extends MobEffect {
    private static final int HORIZONTAL_RADIUS = 32;
    private static final int VERTICAL_RADIUS = 16;
    public static final DustParticleOptions RED_PARTICLE = new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.2F);

    public SangriaEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x990000);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return Collections.emptyList();
    }

    public static void tick(LivingEntity entity) {
        MobEffectInstance sangria = entity.getEffect(CocktailsMod.SANGRIA_EFFECT.get());
        if (sangria == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }

        int intervalSource = sangria.isInfiniteDuration() ? entity.tickCount : sangria.getDuration();
        if (Math.floorMod(intervalSource, 10) != 0) {
            return;
        }

        JukeboxBlockEntity nearestJukebox = findNearestPlayingJukebox(level, entity.blockPosition());
        if (nearestJukebox != null) {
            ItemStack discStack = nearestJukebox.getFirstItem();
            MobEffect jukeboxEffect = getEffectForDisc(discStack);
            if (jukeboxEffect != null) {
                refreshEffect(level, entity, jukeboxEffect);
            }

            BlockPos jpos = nearestJukebox.getBlockPos();
            sendParticles(level, entity, jpos.getX() + 0.5, jpos.getY() + 1.0, jpos.getZ() + 0.5, 6, 0.05, 0.05, 0.05, 0.15);
        } else {
            refreshEffect(level, entity, MobEffects.DIG_SLOWDOWN);
            refreshEffect(level, entity, MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    private static void refreshEffect(ServerLevel level, LivingEntity entity, MobEffect effect) {
        MobEffectInstance refreshed = new MobEffectInstance(effect, 40, 0, true, true, true);
        if (effect != MobEffects.HEALTH_BOOST) {
            entity.addEffect(refreshed);
            return;
        }

        MobEffectInstance current = entity.getEffect(effect);
        if (current == null) {
            entity.addEffect(refreshed);
        } else if (entity.canBeAffected(refreshed) && current.update(refreshed)) {
            level.getChunkSource().broadcastAndSend(entity, new ClientboundUpdateMobEffectPacket(entity.getId(), current));
        }
    }

    public static void spawnInitialBurst(ServerLevel level, LivingEntity entity) {
        JukeboxBlockEntity nearestJukebox = findNearestPlayingJukebox(level, entity.blockPosition());
        if (nearestJukebox != null) {
            BlockPos jpos = nearestJukebox.getBlockPos();
            sendParticles(level, entity, jpos.getX() + 0.5, jpos.getY() + 0.5, jpos.getZ() + 0.5, 15, 0.25, 0.25, 0.25, 0.02);
        }
    }

    private static void sendParticles(ServerLevel level, LivingEntity entity, double x, double y, double z,
                                      int count, double spreadX, double spreadY, double spreadZ, double speed) {
        level.sendParticles(RED_PARTICLE, x, y, z, count, spreadX, spreadY, spreadZ, speed);
        if (entity instanceof ServerPlayer player && player.distanceToSqr(x, y, z) > HORIZONTAL_RADIUS * HORIZONTAL_RADIUS) {
            level.sendParticles(player, RED_PARTICLE, true, x, y, z, count, spreadX, spreadY, spreadZ, speed);
        }
    }

    private static JukeboxBlockEntity findNearestPlayingJukebox(ServerLevel level, BlockPos origin) {
        JukeboxBlockEntity nearestJukebox = null;
        double nearestDistanceSq = Double.MAX_VALUE;
        int minChunkX = SectionPos.blockToSectionCoord(origin.getX() - HORIZONTAL_RADIUS);
        int maxChunkX = SectionPos.blockToSectionCoord(origin.getX() + HORIZONTAL_RADIUS);
        int minChunkZ = SectionPos.blockToSectionCoord(origin.getZ() - HORIZONTAL_RADIUS);
        int maxChunkZ = SectionPos.blockToSectionCoord(origin.getZ() + HORIZONTAL_RADIUS);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof JukeboxBlockEntity jukebox) || !jukebox.isRecordPlaying()) {
                        continue;
                    }

                    BlockPos jukeboxPos = jukebox.getBlockPos();
                    if (Math.abs(jukeboxPos.getY() - origin.getY()) > VERTICAL_RADIUS) {
                        continue;
                    }

                    double distanceSq = jukeboxPos.distSqr(origin);
                    if (distanceSq > HORIZONTAL_RADIUS * HORIZONTAL_RADIUS) {
                        continue;
                    }

                    if (distanceSq < nearestDistanceSq) {
                        nearestDistanceSq = distanceSq;
                        nearestJukebox = jukebox;
                    }
                }
            }
        }

        return nearestJukebox;
    }

    private static MobEffect getEffectForDisc(ItemStack discStack) {
        if (discStack.isEmpty() || !(discStack.getItem() instanceof RecordItem recordItem)) {
            return null;
        }

        ResourceLocation discId = ForgeRegistries.ITEMS.getKey(recordItem);
        if (discId == null || !"minecraft".equals(discId.getNamespace())) {
            return MobEffects.HEALTH_BOOST;
        }

        return switch (discId.getPath()) {
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
