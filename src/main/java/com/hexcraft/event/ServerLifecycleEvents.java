package com.hexcraft.event;

import com.hexcraft.Hexcraft;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 维护当前服务端实例引用，供不携带 level 的全局事件（如酿造 Post）定位玩家。
 * 仅在服务端启动后、停止前有效。
 */
public class ServerLifecycleEvents {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        Hexcraft.SERVER = event.getServer();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer current = Hexcraft.SERVER;
        if (current == event.getServer()) Hexcraft.SERVER = null;
    }
}
