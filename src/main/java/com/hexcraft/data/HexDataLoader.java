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

/** 数据驱动海克斯加载器：data/hexcraft/hextech/*.json（覆盖或新增内置定义） */
public class HexDataLoader extends SimpleJsonResourceReloadListener {
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

    public HexDataLoader() {
        super(GSON, "hextech");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        int loaded = 0, failed = 0;
        for (Map.Entry<ResourceLocation, JsonElement> e : map.entrySet()) {
            try {
                String id = e.getKey().getPath();
                HextechDefinition def = parse(id, e.getValue().getAsJsonObject());
                if (def == null) { failed++; continue; }
                HextechRegistry.put(id, def);
                loaded++;
            } catch (Exception ex) {
                failed++;
                Hexcraft.LOGGER.error("Hexcraft: failed to load hextech {}", e.getKey(), ex);
            }
        }
        // v0.6.2 DLC 外接包：加载 hexworkshop_dlc/packs 下启用的纯数据包内容（hex 类）
        loaded += com.hexcraft.dlc.DlcManager.applyHexPacks();
        Hexcraft.LOGGER.info("Hexcraft: loaded {} hextech definition(s) from datapack ({} failed)", loaded, failed);
        Hexcraft.LOGGER.info("Hexcraft: hex pools -> silver={} gold={} prismatic={}",
                HextechRegistry.pool(HextechRarity.SILVER).size(),
                HextechRegistry.pool(HextechRarity.GOLD).size(),
                HextechRegistry.pool(HextechRarity.PRISMATIC).size());
    }

    /** v0.6.2 抽公共解析：数据包 JSON / DLC 外接包共用（stackTag[] 解析进定义，tooltip 运行时拼接标签后缀） */
    public static HextechDefinition parse(String id, JsonObject obj) {
        HextechRarity rarity = HextechRarity.byKey(GsonHelper.getAsString(obj, "rarity", "SILVER"));
        int weight = GsonHelper.getAsInt(obj, "weight", 10);
        String event = GsonHelper.getAsString(obj, "event", "");
        String source = GsonHelper.getAsString(obj, "source", "");
        HextechDefinition.StackSpec stackSpec = null;
        if (obj.has("stack")) {
            JsonObject so = obj.getAsJsonObject("stack");
            String skey = GsonHelper.getAsString(so, "key", "stack_" + id);
            double scap = GsonHelper.getAsDouble(so, "cap", 50);
            boolean scombat = GsonHelper.getAsBoolean(so, "combat", true);
            int sdelay = GsonHelper.getAsInt(so, "decayDelay", -1);
            double srate = GsonHelper.getAsDouble(so, "decayRate", 1);
            boolean sonKill = GsonHelper.getAsBoolean(so, "onKill", false);
            stackSpec = new HextechDefinition.StackSpec(skey, scap, scombat, sdelay, srate, sonKill);
        }
        List<HextechDefinition.ModSpec> mods = new ArrayList<>();
        if (obj.has("mods")) {
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "mods");
            for (JsonElement m : arr) {
                JsonObject mo = m.getAsJsonObject();
                String attrName = GsonHelper.getAsString(mo, "attr");
                double perLevel = GsonHelper.getAsDouble(mo, "perLevel");
                Attribute attr = ATTRIBUTES.get(attrName);
                if (attr == null) {
                    Hexcraft.LOGGER.warn("Hexcraft: unknown attr '{}' in hextech {}", attrName, id);
                    continue;
                }
                mods.add(new HextechDefinition.ModSpec(attr, perLevel));
            }
        }
        // v0.6.2 叠层标签：stackTag[]（可选，tooltip 显示用；未知标签名跳过不崩溃）
        List<String> stackTags = null;
        if (obj.has("stackTag")) {
            stackTags = new ArrayList<>();
            for (JsonElement t : GsonHelper.getAsJsonArray(obj, "stackTag")) {
                String tag = t.getAsString();
                if (com.hexcraft.hextech.StackTagKeys.has(tag)) stackTags.add(tag);
            }
            if (stackTags.isEmpty()) stackTags = null;
        }
        return new HextechDefinition(id, rarity, weight, mods, event, null, 0, source, stackSpec, stackTags);
    }
}
