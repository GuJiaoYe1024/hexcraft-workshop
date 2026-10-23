package com.hexcraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hexcraft.Hexcraft;
import com.hexcraft.hextech.WorldHexDefinition;
import com.hexcraft.hextech.WorldHexRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** v0.6.0 世界海克斯数据包加载器：data/hexworkshop/world_hexes/*.json（覆盖或新增内置定义）。
 *  JSON 字段：category = general/friendly/neutral/hostile；rarity = silver/gold/prismatic；effect = key:value 逗号分隔。
 *  世界海克斯四大系统数据包开放之一（另：hexes=HexDataLoader / equipment=EquipmentDataLoader / forging=内置静态池）。 */
public class WorldHexDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public WorldHexDataLoader() {
        super(GSON, "world_hexes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        int loaded = 0, failed = 0;
        for (Map.Entry<ResourceLocation, JsonElement> e : map.entrySet()) {
            try {
                String id = e.getKey().getPath();
                JsonObject obj = e.getValue().getAsJsonObject();
                String category = GsonHelper.getAsString(obj, "category", WorldHexRegistry.CAT_GENERAL);
                String rarity = GsonHelper.getAsString(obj, "rarity", "gold");
                String effect = GsonHelper.getAsString(obj, "effect", "");
                WorldHexRegistry.put(id, new WorldHexDefinition(id, category, rarity, effect));
                loaded++;
            } catch (Exception ex) {
                failed++;
                Hexcraft.LOGGER.error("Hexcraft: failed to load world hex {}", e.getKey(), ex);
            }
        }
        Hexcraft.LOGGER.info("Hexcraft: loaded {} world hex definition(s) from datapack ({} failed); total={}",
                loaded, failed, WorldHexRegistry.all().size());
    }
}
