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
}
