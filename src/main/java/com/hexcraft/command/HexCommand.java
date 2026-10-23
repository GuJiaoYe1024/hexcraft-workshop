package com.hexcraft.command;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import com.hexcraft.event.HextechEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;

/** /hexcraft 管理指令（OP） */
public class HexCommand {

    /** v0.6.0 数据包热重载：向所有在线玩家广播 S2CDataRefresh（对应模块界面自动刷新） */
    private static void broadcastRefresh(CommandSourceStack ctx, String module) {
        for (ServerPlayer p : ctx.getServer().getPlayerList().getPlayers()) {
            com.hexcraft.network.Packets.S2CDataRefresh.sendTo(p, module);
        }
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("hexworkshop")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("coins")
                        .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    PlayerData data = ModCapabilities.get(p);
                                    if (data == null) return 0;
                                    data.addCoins(IntegerArgumentType.getInteger(ctx, "amount"));
                                    HextechEvents.sync(p);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.hexcraft.coins_add", p.getName(), IntegerArgumentType.getInteger(ctx, "amount")), true);
                                    return 1;
                                }))))
                        .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    PlayerData data = ModCapabilities.get(p);
                                    if (data == null) return 0;
                                    data.setCoins(IntegerArgumentType.getInteger(ctx, "amount"));
                                    HextechEvents.sync(p);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.hexcraft.coins_add", p.getName(), IntegerArgumentType.getInteger(ctx, "amount")), true);
                                    return 1;
                                })))))
                .then(Commands.literal("view").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    PlayerData data = ModCapabilities.get(p);
                    if (data == null) return 0;
                    ctx.getSource().sendSuccess(() -> Component.literal("Hexcraft [" + p.getName().getString() + "] coins=" + data.getCoins()
                            + " forges=" + data.getForgesUsed() + "/" + data.getHexes().size() + "hexes"), true);
                    return 1;
                })))
                .then(Commands.literal("clear").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    PlayerData data = ModCapabilities.get(p);
                    if (data == null) return 0;
                    data.clearAll();
                    data.applyModifiers(p);
                    HextechEvents.sync(p);
                    ctx.getSource().sendSuccess(() -> Component.literal("Hexcraft: cleared " + p.getName().getString()), true);
                    return 1;
                })))
                .then(Commands.literal("reload")
                        // v0.6.0 分模块热重载：all=全量（默认）；hexes/equipment/world=对应数据包模块；
                        // 服务端统一 reloadResources（SimpleJsonResourceReloadListener 全量监听），客户端按模块刷新对应界面
                        .executes(ctx -> {
                            ctx.getSource().getServer().reloadResources(Collections.emptyList());
                            broadcastRefresh(ctx.getSource(), "all");
                            ctx.getSource().sendSuccess(() -> Component.translatable("message.hexcraft.reloaded"), true);
                            return 1;
                        })
                        .then(Commands.argument("module", StringArgumentType.word())
                                .executes(ctx -> {
                                    String module = StringArgumentType.getString(ctx, "module");
                                    String mod = switch (module) {
                                        case "hexes", "forging", "equipment", "world", "dlc" -> module;
                                        default -> "all";
                                    };
                                    // v0.6.2 DLC 外接包：/hexcraft reload dlc 独立热重载（扫描 packs + 应用数据包，无需整包资源重载）
                                    if ("dlc".equals(mod)) {
                                        com.hexcraft.dlc.DlcManager.reloadDlc();
                                        broadcastRefresh(ctx.getSource(), "all");
                                        ctx.getSource().sendSuccess(() -> Component.literal("Hexcraft: reloaded module [dlc]"), true);
                                        return 1;
                                    }
                                    ctx.getSource().getServer().reloadResources(Collections.emptyList());
                                    broadcastRefresh(ctx.getSource(), mod);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Hexcraft: reloaded module [" + mod + "]"), true);
                                    return 1;
                                }))));
    }
}
