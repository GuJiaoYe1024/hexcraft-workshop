package com.hexcraft.link;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import com.hexcraft.config.HexConfig;
import com.hexcraft.event.HextechEvents;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.6.20 Iron's Spells 'n Spellbooks 法术联动监听层。
 * 只在安装 irons_spellbooks 时由 Hexcraft 主类初始化（ModList.isLoaded 守卫），
 * 未安装时本类不会被加载，零副作用。
 *
 * 消费通道：HextechEvents.effect(data, "spell_xxx")（海克斯 + 装备 event 汇总）：
 *  spell_power          总法术强度（增伤，含学校系）
 *  spell_fire/ice/lightning/holy/blood/nature/ender/evocation/eldritch  各学校增伤
 *  spell_mana_cost      施法耗魔减免（SpellOnCastEvent.setManaCost）
 *  spell_mana_on_cast   施法后返还已消耗魔力
 *  spell_echo           施法后概率全额返还魔力（卢登/回响施法）
 *  spell_cd             冷却缩减（SpellCooldownAddedEvent$Pre.setEffectiveCooldown）
 *  spell_lifesteal      法术伤害吸血（SpellDamageEvent 命中时回血施法者）
 *  spell_crit           法术暴击率（命中概率暴击，倍率 IRONS_CRIT_MULT）
 *  spell_double         双重施法（命中概率伤害翻倍）
 *  spell_execute        斩杀咒印（目标低血增伤，阈值 IRONS_EXECUTE_THRESHOLD）
 *  spell_pen            法术穿透（按比例增伤，等效忽略目标法抗）
 *  spell_magic_taken    受到法术伤害减免（受害者是玩家时）
 *  spell_mana_shield    奥术壁垒：受法术伤害时按比例用魔力抵扣
 *  spell_heal_power     治疗强度（SpellHealEvent 命中后补治疗）
 *  spell_mana_on_kill   击杀回蓝（法术击杀判定：最近 10 秒法术伤害缓存）
 *  spell_mana_regen / spell_mana_max / spell_cast_haste
 *                       ——属性注入型（tick 同步 Iron's MANA_REGEN / MAX_MANA / CAST_TIME_REDUCTION）
 */
public class IronsSpellLink {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** 法术伤害命中缓存（victim UUID → Hit{caster,time}，10 秒过期）：用于法术击杀判定（击杀回蓝） */
    private static final Map<UUID, Hit> SPELL_LAST_HIT = new ConcurrentHashMap<>();

    private static final class Hit {
        final UUID caster;
        final long time;
        Hit(UUID caster, long time) { this.caster = caster; this.time = time; }
    }

    /** 属性注入 modifier 固定 UUID（cast_haste / mana_max / mana_regen 三通道） */
    private static final UUID HASTE_MOD = UUID.fromString("0f6e20a1-3c41-4d7e-9a2b-a4b0c1e0a201");
    private static final UUID MANA_MAX_MOD = UUID.fromString("0f6e20a1-3c41-4d7e-9a2b-a4b0c2e0a202");
    private static final UUID MANA_REG_MOD = UUID.fromString("0f6e20a1-3c41-4d7e-9a2b-a4b0c3e0a203");

    private static boolean registered = false;

    /** 主类守卫调用：仅 isLoaded("irons_spellbooks") 为真时执行 */
    public static void init() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(IronsSpellLink.class);
        LOGGER.info("Hexcraft: Iron's Spells 'n Spellbooks 法术联动已挂载（29 海克斯 / 法术锻造词条 / LOL 法强装联动）");
    }

    // ================= 工具 =================

    /** 玩家某法术效果总值（海克斯+装备 event，已乘全局倍率 IRONS_EFFECT_MULT） */
    private static double eff(Player p, String key) {
        PlayerData data = ModCapabilities.get(p);
        return HextechEvents.effect(data, key) * HexConfig.IRONS_EFFECT_MULT.get();
    }

    /** 从法术伤害来源取施法者（1.20.1 DamageSource：getEntity()=因果实体/施法者，getDirectEntity()=直接实体/投射物） */
    private static LivingEntity casterOf(SpellDamageSource src) {
        if (src == null) return null;
        if (src.getEntity() instanceof LivingEntity le) return le;
        if (src.getDirectEntity() instanceof LivingEntity le) return le;
        return null;
    }

    /** 学校系增伤：Iron's 学校 id path → 效果键 */
    private static String schoolKey(String schoolPath) {
        return switch (schoolPath == null ? "" : schoolPath) {
            case "fire" -> "spell_fire";
            case "ice" -> "spell_ice";
            case "lightning" -> "spell_lightning";
            case "holy" -> "spell_holy";
            case "blood" -> "spell_blood";
            case "nature" -> "spell_nature";
            case "ender" -> "spell_ender";
            case "evocation" -> "spell_evocation";
            case "eldritch" -> "spell_eldritch";
            default -> "";
        };
    }

    // ================= 施法事件 =================

    /** 施法完成：耗魔减免 / 施法返还 / 回响全额返还 */
    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        double costRed = eff(sp, "spell_mana_cost");
        if (costRed > 0) {
            int orig = event.getOriginalManaCost();
            int newCost = (int) Math.max(0, Math.ceil(orig * (1 - Math.min(0.9, costRed))));
            event.setManaCost(newCost);
        }
        double refund = eff(sp, "spell_mana_on_cast");
        double echo = eff(sp, "spell_echo");
        int cost = event.getManaCost();
        if ((refund > 0 || echo > 0) && cost > 0) {
            MagicData md = MagicData.getPlayerMagicData(sp);
            if (md != null) {
                if (echo > 0 && sp.getRandom().nextFloat() < Math.min(1.0, echo)) {
                    md.addMana(cost); // 回响施法：全额返还
                    sp.displayClientMessage(Component.translatable("message.hexcraft.spell_echo"), true);
                } else if (refund > 0) {
                    md.addMana((float) (cost * Math.min(1.0, refund)));
                }
            }
        }
    }

    /** 冷却事件：spell_cd 缩减（事件 Pre 阶段改生效冷却） */
    @SubscribeEvent
    public static void onSpellCooldown(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        double cd = eff(sp, "spell_cd");
        if (cd > 0) {
            int effCd = (int) Math.max(0, Math.floor(event.getEffectiveCooldown() * (1 - Math.min(0.9, cd))));
            event.setEffectiveCooldown(effCd);
        }
    }

    /** 治疗强度：spell_heal_power（SpellHealEvent 只读，命中后补治疗） */
    @SubscribeEvent
    public static void onSpellHeal(SpellHealEvent event) {
        if (event.getTargetEntity() == null) return;
        LivingEntity healer = event.getEntity();
        if (!(healer instanceof ServerPlayer sp)) return;
        double healPow = eff(sp, "spell_heal_power");
        if (healPow > 0) {
            LivingEntity target = event.getTargetEntity();
            if (target.isAlive() && !target.isRemoved()) {
                target.heal(event.getHealAmount() * (float) Math.min(3.0, healPow));
            }
        }
    }

    // ================= 法术伤害事件 =================

    /** 法术伤害命中：增伤（总强度/学校系/斩杀/穿透）→ 暴击/双重 → 吸血 → 受害者减免/魔力护盾 */
    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        SpellDamageSource src = event.getSpellDamageSource();
        LivingEntity victim = event.getEntity();
        if (src == null || victim == null || victim.isRemoved()) return;
        LivingEntity caster = casterOf(src);
        // 记录最近法术伤害（法术击杀判定用；仅玩家施法者）
        if (caster instanceof ServerPlayer) {
            long now = System.currentTimeMillis();
            SPELL_LAST_HIT.put(victim.getUUID(), new Hit(caster.getUUID(), now));
            if (SPELL_LAST_HIT.size() > 512) SPELL_LAST_HIT.entrySet().removeIf(e -> now - e.getValue().time > 10_000L);
        }

        float amount = event.getAmount();
        if (amount <= 0) return;

        // ---- 施法者加成（仅玩家海克斯/装备生效）----
        if (caster instanceof ServerPlayer sp) {
            double power = eff(sp, "spell_power");
            double school = eff(sp, schoolKey(schoolPath(src)));
            double execute = eff(sp, "spell_execute");
            double pen = eff(sp, "spell_pen");
            double lifesteal = eff(sp, "spell_lifesteal");
            double crit = eff(sp, "spell_crit");
            double dbl = eff(sp, "spell_double");

            double mul = 1.0 + power + school;
            if (execute > 0 && victim.getHealth() <= victim.getMaxHealth() * HexConfig.IRONS_EXECUTE_THRESHOLD.get()) {
                mul *= (1.0 + Math.min(1.0, execute));
            }
            if (pen > 0) mul *= (1.0 + Math.min(1.0, pen));

            boolean critted = false;
            if (crit > 0 && sp.getRandom().nextFloat() < Math.min(0.9, crit)) {
                mul *= HexConfig.IRONS_CRIT_MULT.get();
                critted = true;
            }
            if (dbl > 0 && sp.getRandom().nextFloat() < Math.min(0.9, dbl)) {
                mul *= 2.0;
                sp.displayClientMessage(Component.translatable("message.hexcraft.spell_double"), true);
            }
            float newAmount = (float) (amount * mul);
            event.setAmount(newAmount);

            if (lifesteal > 0 && newAmount > 0) {
                float heal = (float) (newAmount * Math.min(1.0, lifesteal));
                if (sp.isAlive()) sp.heal(heal);
            }
            if (critted) {
                sp.displayClientMessage(Component.translatable("message.hexcraft.spell_crit",
                        (int) Math.round(newAmount)), true);
            }
        }

        // ---- 受害者减伤/魔力护盾（受害者是玩家时）----
        if (victim instanceof ServerPlayer vp) {
            double taken = eff(vp, "spell_magic_taken");
            double shield = eff(vp, "spell_mana_shield");
            float cur = event.getAmount();
            if (taken > 0) {
                event.setAmount(Math.max(0f, cur * (float) (1 - Math.min(0.9, taken))));
                cur = event.getAmount();
            }
            if (shield > 0 && cur > 0) {
                MagicData md = MagicData.getPlayerMagicData(vp);
                if (md != null && md.getMana() > 0) {
                    float absorb = (float) Math.min(md.getMana(), cur * (float) Math.min(1.0, shield));
                    if (absorb > 0) {
                        md.addMana(-absorb);
                        event.setAmount(Math.max(0f, cur - absorb));
                    }
                }
            }
        }
    }

    /** 学校 id path 提取（事件里 spell() 判空） */
    private static String schoolPath(SpellDamageSource src) {
        try {
            io.redspace.ironsspellbooks.api.spells.AbstractSpell s = src.spell();
            if (s == null) return "";
            io.redspace.ironsspellbooks.api.spells.SchoolType st = s.getSchoolType();
            if (st == null || st.getId() == null) return "";
            return st.getId().getPath();
        } catch (Throwable ignored) {
            return "";
        }
    }

    // ================= 击杀回蓝 =================

    /** 法术击杀：spell_mana_on_kill 击杀回蓝 */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        Hit hit = SPELL_LAST_HIT.get(event.getEntity().getUUID());
        if (hit == null) return;
        if (System.currentTimeMillis() - hit.time > 10_000L) {
            SPELL_LAST_HIT.remove(event.getEntity().getUUID());
            return;
        }
        net.minecraft.world.entity.Entity killer = event.getSource().getEntity();
        if (killer == null) {
            killer = event.getSource().getDirectEntity();
        }
        if (!(killer instanceof ServerPlayer sp)) return;
        // 校验与最近法术伤害施法者一致（防误判普通击杀）
        if (!sp.getUUID().equals(hit.caster)) return;
        double mk = eff(sp, "spell_mana_on_kill");
        if (mk > 0) {
            MagicData md = MagicData.getPlayerMagicData(sp);
            if (md != null) md.addMana((float) Math.min(9999, mk));
        }
        SPELL_LAST_HIT.remove(event.getEntity().getUUID());
    }

    // ================= 属性注入（tick 同步） =================

    /** 每 100 tick 同步一次：施法速度 / 魔力上限 / 魔力回复（读海克斯+装备效果，动态跟随等级变化） */
    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 100 != 0) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double haste = HextechEvents.effect(data, "spell_cast_haste") * HexConfig.IRONS_CAST_HASTE.get();
        double manaMax = HextechEvents.effect(data, "spell_mana_max") * HexConfig.IRONS_MANA_MAX.get();
        double manaReg = HextechEvents.effect(data, "spell_mana_regen") * HexConfig.IRONS_MANA_REGEN.get();
        syncMod(sp, AttributeRegistry.CAST_TIME_REDUCTION.get(), HASTE_MOD, haste);
        syncMod(sp, AttributeRegistry.MAX_MANA.get(), MANA_MAX_MOD, manaMax);
        syncMod(sp, AttributeRegistry.MANA_REGEN.get(), MANA_REG_MOD, manaReg);
    }

    /** 幂等同步：值变化才重建 modifier（同一 UUID 覆盖，防重复叠加） */
    private static void syncMod(ServerPlayer sp, Attribute attr, UUID id, double value) {
        if (attr == null) return;
        AttributeInstance inst = sp.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier mod = inst.getModifier(id);
        if (mod == null || Math.abs(mod.getAmount() - value) > 0.0001) {
            if (mod != null) inst.removeModifier(id);
            if (value != 0) {
                inst.addTransientModifier(new AttributeModifier(id, "hexcraft_spell_link", value,
                        AttributeModifier.Operation.ADDITION));
            }
        }
    }
}
