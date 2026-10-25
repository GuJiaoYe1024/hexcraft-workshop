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
        // v0.6.3 打开界面时请求同步：回包到达后用最新数据重建主界面一次（保留当前页签），
        // 避免联机玩家抽取/变更后打开界面读到旧缓存（显示为空）的问题
        if (ClientDataStore.requestPendingSync) {
            ClientDataStore.requestPendingSync = false;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof com.hexcraft.client.gui.HextechPageScreen) {
                com.hexcraft.client.gui.HextechPageScreen old = (com.hexcraft.client.gui.HextechPageScreen) mc.screen;
                int t = old.currentTab();
                com.hexcraft.client.gui.HextechPageScreen fresh = new com.hexcraft.client.gui.HextechPageScreen();
                fresh.setInitialTab(t);
                mc.setScreen(fresh);
            }
        }
    }

    /** v0.6.4 服务端 Registry 定义同步：填充客户端定义缓存（联机客户端海克斯页/属性页依赖） */
    public static void syncRegistry(Packets.S2CSyncRegistry msg) {
        ClientDataStore.applyRegistry(msg);
        // 已打开的海克斯主界面依赖 def（稀有度/分类/effect），收到定义后重建一次（保留当前页签）
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof com.hexcraft.client.gui.HextechPageScreen) {
            com.hexcraft.client.gui.HextechPageScreen old = (com.hexcraft.client.gui.HextechPageScreen) mc.screen;
            int t = old.currentTab();
            com.hexcraft.client.gui.HextechPageScreen fresh = new com.hexcraft.client.gui.HextechPageScreen();
            fresh.setInitialTab(t);
            mc.setScreen(fresh);
        }
    }

    public static void syncWorldHexes(Packets.S2CSyncWorldHexes msg) {
        ClientDataStore.applyWorld(msg);
        // v0.6.3 世界海克斯列表/费用变更：已打开的世界海克斯界面立即重建（不再依赖重开）
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof com.hexcraft.client.gui.WorldHexScreen) {
            mc.setScreen(new com.hexcraft.client.gui.WorldHexScreen());
        } else if (mc.screen instanceof com.hexcraft.client.gui.HextechPageScreen) {
            com.hexcraft.client.gui.HextechPageScreen old = (com.hexcraft.client.gui.HextechPageScreen) mc.screen;
            int t = old.currentTab();
            com.hexcraft.client.gui.HextechPageScreen fresh = new com.hexcraft.client.gui.HextechPageScreen();
            fresh.setInitialTab(t);
            mc.setScreen(fresh);
        }
    }

    public static void worldDrawOptions(Packets.S2CWorldDrawOptions msg) {
        Minecraft.getInstance().setScreen(new com.hexcraft.client.gui.WorldRollScreen(msg));
    }

    /** v0.6.16 滚动提示：解析 Component JSON 入客户端提示队列（HexTipOverlay 渲染） */
    public static void tip(Packets.S2CTipMessage msg) {
        try {
            net.minecraft.network.chat.Component c =
                    net.minecraft.network.chat.Component.Serializer.fromJson(msg.json);
            if (c != null) com.hexcraft.client.gui.HexTipOverlay.push(c.getString(), msg.type);
        } catch (Exception ignored) {
        }
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
