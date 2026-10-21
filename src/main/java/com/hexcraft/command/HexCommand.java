package com.hexcraft.command;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import com.hexcraft.event.HextechEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
                .then(Commands.literal("reload").executes(ctx -> {
                    ctx.getSource().getServer().reloadResources(Collections.emptyList());
                    ctx.getSource().sendSuccess(() -> Component.translatable("message.hexcraft.reloaded"), true);
                    return 1;
                })));
    }
}
