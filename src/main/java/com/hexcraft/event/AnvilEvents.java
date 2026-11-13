package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 铁砧类专属海克斯事件（v0.9.12 新增）。
 * anvil_save：铁砧操作的经验消耗减半。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class AnvilEvents {

    /** v0.9.17：铁砧界面每次改动（含逐字改名、来回换材料）都会重算输出并触发本事件，
     *  若每次都叠层，玩家只改个名字就能刷层。这里按「左槽+右槽+产物类型/耐久」去重，
     *  同一组合 2 秒内只叠 1 层；重复触发时只按当前层数补写 NBT，不叠加。 */
    private static final java.util.Map<java.util.UUID, String> ANVIL_LAST_KEY = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<java.util.UUID, Long> ANVIL_LAST_TIME = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long ANVIL_DEDUP_TICKS = 40L;

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double save = HextechEvents.effect(data, "anvil_save");
        if (save > 0) {
            int cost = event.getCost();
            if (cost > 0) {
                event.setCost(Math.max(0, cost - (int) Math.round(cost * save)));
            }
        }
        // v0.9.17 职业包·铁匠宗师：修复额外恢复耐久 + 把层数写进被处理的那件装备
        net.minecraft.world.item.ItemStack out = event.getOutput();
        if (out == null || out.isEmpty()) return;
        double repair = HextechEvents.effect(data, "anvil_repair");
        if (repair > 0 && out.isDamageableItem() && out.getDamageValue() > 0) {
            int cut = (int) Math.round(out.getMaxDamage() * repair);
            out.setDamageValue(Math.max(0, out.getDamageValue() - cut));
        }
        boolean armorKind = out.getItem() instanceof net.minecraft.world.item.ArmorItem;
        if (!armorKind && !HextechEvents.isWeaponOrTool(out.getItem())) return;
        String comboKey = out.getItem() + "|" + out.getDamageValue()
                + "|" + event.getLeft().getItem() + "|" + event.getRight().getItem();
        long now = sp.level().getGameTime();
        Long last = ANVIL_LAST_TIME.get(sp.getUUID());
        boolean repeat = comboKey.equals(ANVIL_LAST_KEY.get(sp.getUUID()))
                && last != null && now - last < ANVIL_DEDUP_TICKS;
        HextechEvents.ScionHit hit = repeat ? HextechEvents.scionCurrent(data, "forge_scion_anvil")
                : HextechEvents.scionStackUp(data, "forge_scion_anvil");
        if (hit == null) return;
        HextechEvents.writeScionTag(out, hit, armorKind);
        event.setOutput(out);
        ANVIL_LAST_KEY.put(sp.getUUID(), comboKey);
        ANVIL_LAST_TIME.put(sp.getUUID(), now);
        if (!repeat) data.applyModifiers(sp);
    }
}
