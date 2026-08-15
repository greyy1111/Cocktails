package com.cocktails;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = CocktailsMod.MODID, value = Dist.CLIENT)
public final class SangriaMusicClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean enabled;
    private static SangriaLoopSound activeSound;

    private SangriaMusicClient() {
    }

    public static void setEnabled(boolean enabled) {
        SangriaMusicClient.enabled = enabled;
        LOGGER.info("Sangria music setting synced: {}", enabled ? "ON" : "OFF");
        if (!enabled) {
            stopActiveSound();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (shouldPlay(minecraft)) {
            if (activeSound == null) {
                activeSound = new SangriaLoopSound();
                minecraft.getSoundManager().play(activeSound);
                LOGGER.info("Starting Sangria music");
            }
        } else {
            stopActiveSound();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        enabled = false;
        stopActiveSound();
    }

    @SubscribeEvent
    public static void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        stopActiveSound();
    }

    private static boolean shouldPlay(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        return enabled
                && player != null
                && player.hasEffect(CocktailsMod.SANGRIA_EFFECT.get())
                && minecraft.options.getSoundSourceVolume(SoundSource.MASTER) > 0.0F
                && minecraft.options.getSoundSourceVolume(SoundSource.RECORDS) > 0.0F;
    }

    private static void stopActiveSound() {
        if (activeSound != null) {
            activeSound.stopPlayback();
            SoundManager soundManager = Minecraft.getInstance().getSoundManager();
            soundManager.stop(activeSound);
            activeSound = null;
            LOGGER.info("Stopping Sangria music");
        }
    }

    private static final class SangriaLoopSound extends AbstractTickableSoundInstance {
        private SangriaLoopSound() {
            super(CocktailsMod.SANGRIA_MUSIC.get(), SoundSource.RECORDS, RandomSource.create());
            looping = true;
            delay = 0;
            volume = 1.0F;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            if (!shouldPlay(Minecraft.getInstance())) {
                stop();
            }
        }

        private void stopPlayback() {
            stop();
        }
    }

    @Mod.EventBusSubscriber(modid = CocktailsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void onSoundEngineLoad(SoundEngineLoadEvent event) {
            if (activeSound != null) {
                activeSound.stopPlayback();
                activeSound = null;
            }
        }
    }
}
