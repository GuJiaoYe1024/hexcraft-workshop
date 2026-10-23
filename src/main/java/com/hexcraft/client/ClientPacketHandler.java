package com.hexcraft.client;

import com.hexcraft.client.gui.EquipRollScreen;
import com.hexcraft.client.gui.RollScreen;
import com.hexcraft.network.Packets;
import net.minecraft.client.Minecraft;

/** S2C 包客户端处理入口 */
public class ClientPacketHandler {
    public static void openRoll(Packets.S2COpenRoll msg) {
        if (msg.equip) {
            Minecraft.getInstance().setScreen(new EquipRollScreen(msg));
        } else {
            Minecraft.getInstance().setScreen(new RollScreen(msg));
        }
    }

    public static void syncData(Packets.S2CSyncData msg) {
        ClientDataStore.apply(msg);
    }

    public static void syncWorldHexes(Packets.S2CSyncWorldHexes msg) {
        ClientDataStore.applyWorld(msg);
    }

    public static void worldDrawOptions(Packets.S2CWorldDrawOptions msg) {
        Minecraft.getInstance().setScreen(new com.hexcraft.client.gui.WorldRollScreen(msg));
    }

    /** v0.6.0 数据刷新广播：数据包热重载后打开的界面自动重建（重新 init，列表/池/属性即时反映新内容） */
    public static void refreshData(String module) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return;
        if ("world".equals(module)) {
            if (mc.screen instanceof com.hexcraft.client.gui.WorldHexScreen) {
                mc.setScreen(new com.hexcraft.client.gui.WorldHexScreen());
            }
            return;
        }
        // hexes / forging / equipment / all → 海克斯工坊主界面重建（各页签从 Registry 与 ClientDataStore 实时读取）
        if (mc.screen instanceof com.hexcraft.client.gui.HextechPageScreen) {
            com.hexcraft.client.gui.HextechPageScreen old = (com.hexcraft.client.gui.HextechPageScreen) mc.screen;
            int t = old.currentTab();
            com.hexcraft.client.gui.HextechPageScreen fresh = new com.hexcraft.client.gui.HextechPageScreen();
            fresh.setInitialTab(t);
            mc.setScreen(fresh);
        } else if (mc.screen instanceof com.hexcraft.client.gui.WorldHexScreen) {
            mc.setScreen(new com.hexcraft.client.gui.WorldHexScreen());
        }
    }
}
