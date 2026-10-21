package com.hexcraft.hextech;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** 世界海克斯持久化（LevelSavedData，存主世界 data 目录，全世界共享一份）：
 *  active = 当前已激活的世界海克斯 id 列表（跨玩家、跨维度全局生效）。
 *  注意 1.20.1 的 SavedData 抽象方法签名：CompoundTag save(CompoundTag)（返回 NBT，1.20.2 起才改为 void 双参）。
 */
public class WorldHexData extends SavedData {
    public static final String DATA_NAME = "hexcraft_world_hexes";

    public final List<String> active = new ArrayList<>();

    public static WorldHexData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(WorldHexData::load, WorldHexData::new, DATA_NAME);
    }

    public static WorldHexData load(CompoundTag tag) {
        WorldHexData d = new WorldHexData();
        ListTag list = tag.getList("active", 8);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            if (WorldHexRegistry.exists(id) && !d.active.contains(id)) d.active.add(id);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String id : active) list.add(StringTag.valueOf(id));
        tag.put("active", list);
        return tag;
    }
}
