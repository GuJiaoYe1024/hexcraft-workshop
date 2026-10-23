package com.hexcraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hexcraft.Hexcraft;
import com.hexcraft.hextech.HextechDefinition;
import com.hexcraft.hextech.HextechRarity;
import com.hexcraft.hextech.HextechRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 数据驱动装备加载器：data/hexworkshop/equipment/*.json（覆盖或新增内置装备定义） */
public class EquipmentDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, Attribute> ATTRIBUTES = Map.of(
            "max_health", Attributes.MAX_HEALTH,
            "armor", Attributes.ARMOR,
            "attack_damage", Attributes.ATTACK_DAMAGE,
            "attack_speed", Attributes.ATTACK_SPEED,
            "movement_speed", Attributes.MOVEMENT_SPEED,
            "luck", Attributes.LUCK,
            "knockback_resistance", Attributes.KNOCKBACK_RESISTANCE,
            "attack_knockback", Attributes.ATTACK_KNOCKBACK,
            "tough", Attributes.ARMOR_TOUGHNESS);

    public EquipmentDataLoader() {
        super(GSON, "equipment");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        int loaded = 0, failed = 0;
        for (Map.Entry<ResourceLocation, JsonElement> e : map.entrySet()) {
            try {
                String id = e.getKey().getPath();
                HextechDefinition def = parse(id, e.getValue().getAsJsonObject());
                if (def == null) { failed++; continue; }
                HextechRegistry.putEquip(id, def);
                loaded++;
            } catch (Exception ex) {
                failed++;
                Hexcraft.LOGGER.error("Hexcraft: failed to load equipment {}", e.getKey(), ex);
            }
        }
        // v0.6.2 DLC 外接包：加载启用的纯数据包内容（equipment 类）
        loaded += com.hexcraft.dlc.DlcManager.applyEquipPacks();
        Hexcraft.LOGGER.info("Hexcraft: loaded {} equipment definition(s) from datapack ({} failed)", loaded, failed);
        Hexcraft.LOGGER.info("Hexcraft: equip pools -> gold={} prismatic={}",
                HextechRegistry.equipPool(HextechRarity.GOLD).size(),
                HextechRegistry.equipPool(HextechRarity.PRISMATIC).size());
    }

    /** v0.6.2 抽公共解析：数据包 JSON / DLC 外接包共用 */
    public static HextechDefinition parse(String id, JsonObject obj) {
        HextechRarity rarity = HextechRarity.byKey(GsonHelper.getAsString(obj, "rarity", "GOLD"));
        int weight = GsonHelper.getAsInt(obj, "weight", 10);
        String event = GsonHelper.getAsString(obj, "event", "");
        String icon = GsonHelper.getAsString(obj, "icon", "◆");
        int sell = GsonHelper.getAsInt(obj, "sell", 0);
        String source = GsonHelper.getAsString(obj, "source", "");
        List<HextechDefinition.ModSpec> mods = new ArrayList<>();
        if (obj.has("mods")) {
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "mods");
            for (JsonElement m : arr) {
                JsonObject mo = m.getAsJsonObject();
                String attrName = GsonHelper.getAsString(mo, "attr");
                double perLevel = GsonHelper.getAsDouble(mo, "perLevel");
                Attribute attr = ATTRIBUTES.get(attrName);
                if (attr == null) {
                    Hexcraft.LOGGER.warn("Hexcraft: unknown attr '{}' in equipment {}", attrName, id);
                    continue;
                }
                mods.add(new HextechDefinition.ModSpec(attr, perLevel));
            }
        }
        return new HextechDefinition(id, rarity, weight, mods, event, icon, sell, source);
    }
}
