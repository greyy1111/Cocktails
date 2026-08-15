package com.cocktails;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class SangriaMusicSavedData extends SavedData {
    private static final String DATA_NAME = CocktailsMod.MODID + "_sangria_music";
    private static final String ENABLED_TAG = "Enabled";

    private boolean enabled;

    public static SangriaMusicSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                SangriaMusicSavedData::load,
                SangriaMusicSavedData::new,
                DATA_NAME
        );
    }

    private static SangriaMusicSavedData load(CompoundTag tag) {
        SangriaMusicSavedData data = new SangriaMusicSavedData();
        data.enabled = tag.getBoolean(ENABLED_TAG);
        return data;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean(ENABLED_TAG, enabled);
        return tag;
    }
}
