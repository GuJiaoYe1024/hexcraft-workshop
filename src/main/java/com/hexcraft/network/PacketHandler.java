package com.hexcraft.network;

import com.hexcraft.Hexcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class PacketHandler {
    public static final String VERSION = "20";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Hexcraft.MODID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, Packets.C2SRequestDraw.class,
                Packets.C2SRequestDraw::encode, Packets.C2SRequestDraw::decode, Packets.C2SRequestDraw::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.C2SRequestSync.class,
                Packets.C2SRequestSync::encode, Packets.C2SRequestSync::decode, Packets.C2SRequestSync::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.S2COpenRoll.class,
                Packets.S2COpenRoll::encode, Packets.S2COpenRoll::decode, Packets.S2COpenRoll::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Packets.C2SChooseRoll.class,
                Packets.C2SChooseRoll::encode, Packets.C2SChooseRoll::decode, Packets.C2SChooseRoll::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.C2SReroll.class,
                Packets.C2SReroll::encode, Packets.C2SReroll::decode, Packets.C2SReroll::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // ---- v0.6.3 追加：取消本轮三选一（清 pendingRoll/worldDrawOptions，防取消后死锁）；协议 VERSION bump 18→19 ----
        CHANNEL.registerMessage(id++, Packets.C2SCancelRoll.class,
                Packets.C2SCancelRoll::encode, Packets.C2SCancelRoll::decode, Packets.C2SCancelRoll::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.S2CSyncData.class,
                Packets.S2CSyncData::encode, Packets.S2CSyncData::decode, Packets.S2CSyncData::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Packets.C2SBuyForge.class,
                Packets.C2SBuyForge::encode, Packets.C2SBuyForge::decode, Packets.C2SBuyForge::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.C2SRandomForge.class,
                Packets.C2SRandomForge::encode, Packets.C2SRandomForge::decode, Packets.C2SRandomForge::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.C2SBuyEquip.class,
                Packets.C2SBuyEquip::encode, Packets.C2SBuyEquip::decode, Packets.C2SBuyEquip::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.C2SSellEquip.class,
                Packets.C2SSellEquip::encode, Packets.C2SSellEquip::decode, Packets.C2SSellEquip::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.C2SExchangeCoin.class,
                Packets.C2SExchangeCoin::encode, Packets.C2SExchangeCoin::decode, Packets.C2SExchangeCoin::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.C2SSetGameRule.class,
                Packets.C2SSetGameRule::encode, Packets.C2SSetGameRule::decode, Packets.C2SSetGameRule::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // ---- v0.3.21 世界海克斯（5 个新包，VERSION bump 10→11）；v0.4.0 新增 8 个高难海克斯（VERSION bump 11→12）；v0.4.1 炼狱数值再拉高+UI/按键（bump 12→13，强制全员同版本）----
        CHANNEL.registerMessage(id++, Packets.S2CSyncWorldHexes.class,
                Packets.S2CSyncWorldHexes::encode, Packets.S2CSyncWorldHexes::decode, Packets.S2CSyncWorldHexes::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Packets.C2SRequestWorldDraw.class,
                Packets.C2SRequestWorldDraw::encode, Packets.C2SRequestWorldDraw::decode, Packets.C2SRequestWorldDraw::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.S2CWorldDrawOptions.class,
                Packets.S2CWorldDrawOptions::encode, Packets.S2CWorldDrawOptions::decode, Packets.S2CWorldDrawOptions::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Packets.C2SConfirmWorldHex.class,
                Packets.C2SConfirmWorldHex::encode, Packets.C2SConfirmWorldHex::decode, Packets.C2SConfirmWorldHex::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.C2SCancelWorldHex.class,
                Packets.C2SCancelWorldHex::encode, Packets.C2SCancelWorldHex::decode, Packets.C2SCancelWorldHex::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // ---- v0.6.0 数据刷新广播（热重载/内容变更 → 客户端界面自动刷新）；协议 VERSION bump 15→16（新增包类型，双端必须同版本）----
        CHANNEL.registerMessage(id++, Packets.S2CDataRefresh.class,
                Packets.S2CDataRefresh::encode, Packets.S2CDataRefresh::decode, Packets.S2CDataRefresh::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // ---- v0.6.4 服务端 Registry 定义同步（联机客户端 HextechRegistry 无数据包加载，界面靠此缓存）；VERSION bump 19→20 ----
        CHANNEL.registerMessage(id++, Packets.S2CSyncRegistry.class,
                Packets.S2CSyncRegistry::encode, Packets.S2CSyncRegistry::decode, Packets.S2CSyncRegistry::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
