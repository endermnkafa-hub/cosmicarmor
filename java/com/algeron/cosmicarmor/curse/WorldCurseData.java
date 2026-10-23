package com.algeron.cosmicarmor.curse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class WorldCurseData extends SavedData {
    private static final String DATA_NAME = "cosmicarmor_world_curse";
    private int curseLevel = 0;

    public WorldCurseData() {
    }

    public static WorldCurseData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                WorldCurseData::load,
                WorldCurseData::new,
                DATA_NAME
        );
    }

    public static WorldCurseData load(CompoundTag tag) {
        WorldCurseData data = new WorldCurseData();
        data.curseLevel = tag.getInt("CurseLevel");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("CurseLevel", this.curseLevel);
        return tag;
    }

    public int getCurseLevel() {
        return this.curseLevel;
    }

    public void setCurseLevel(int level) {
        this.curseLevel = level;
        this.setDirty();
    }
}
