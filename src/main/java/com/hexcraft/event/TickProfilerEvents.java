package com.hexcraft.event;

import com.hexcraft.util.TickProfiler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** v0.9.27 P3：tick 计时的挂载点（见 {@link TickProfiler}）。
 *
 *  <p>只挂两个事件、各两个优先级，不改任何现有事件类的代码：
 *  <ul>
 *    <li>`ServerTickEvent`：HIGHEST 取起点、LOWEST 结算，得到整 tick 时长；</li>
 *    <li>`PlayerTickEvent`：同样两端夹住，得到「所有 PlayerTick 处理器」的单玩家耗时。</li>
 *  </ul>
 *  服务端与客户端共用同一个类，用 `level().isClientSide` 只统计服务端。
 */
public final class TickProfilerEvents {

    private TickProfilerEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void serverStart(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.START) TickProfiler.serverStart();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void serverEnd(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) TickProfiler.serverEnd();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void playerStart(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        if (e.player.level().isClientSide) return;
        TickProfiler.playerStart();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerEnd(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (e.player.level().isClientSide) return;
        TickProfiler.playerEnd();
    }
}
