package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 盾牌/格挡专属海克斯事件（v0.9.12 新增）。
 * 全部基于 ShieldBlockEvent，数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class ShieldEvents {

    @SubscribeEvent
    public void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;

        // 一次批量查询三个格挡效果键，替代逐键全量扫描
        java.util.Map<String, Double> v = HextechEvents.effectBatch(data,
                java.util.List.of("shield_guard", "shield_counter", "shield_daze"));

        // 坚毅格挡：本次格挡盾牌不掉耐久
        if (v.get("shield_guard") > 0) {
            event.setShieldTakesDamage(false);
        }

        // 格挡反击：成功格挡后获得 5 秒力量
        if (v.get("shield_counter") > 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0));
        }

        // 盾击致弱：给攻击者施加 5 秒虚弱
        if (v.get("shield_daze") > 0
                && event.getDamageSource().getEntity() instanceof LivingEntity attacker) {
            attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
    }
}
