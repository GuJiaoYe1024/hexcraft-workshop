package com.hexcraft.network;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import com.hexcraft.client.ClientPacketHandler;
import com.hexcraft.config.HexConfig;
import com.hexcraft.event.HextechEvents;
import com.hexcraft.hextech.HextechDefinition;
import com.hexcraft.hextech.HextechRegistry;
import com.hexcraft.hextech.HextechRoller;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** 全部网络包（C2S 服务端结算；S2C 客户端渲染） */
public class Packets {

    /** 精明商人（v0.3.18）：工坊晶币消费折扣（每级 8%，最高 9 折；经验消费不折扣） */
    public static int discounted(PlayerData data, int price) {
        double disc = HextechEvents.effect(data, "trade_discount");
        return (int) Math.floor(price * Math.max(0.1, 1 - disc));
    }

    /** v0.6.3 世界海克斯抽取消耗（服务端权威，含 draw_cost 独立乘区；下限 1，禁止返还经验/白嫖） */
    public static int worldDrawCost(ServerPlayer sp) {
        return Math.max(1, (int) Math.round(HexConfig.WORLD_DRAW_XP.get()
                * (1 + com.hexcraft.event.WorldHexEvents.drawCostMult(sp.serverLevel()))));
    }

    /** v0.4.8 赌博海克斯：解析 event 中的 transform:目标（gold / prismatic / double），非赌博返回 null */
    private static String transformOf(String id) {
        HextechDefinition def = HextechRegistry.get(id);
        if (def == null || def.event == null) return null;
        for (String tok : def.event.split(",")) {
            int ci = tok.indexOf(':');
            if (ci > 0 && tok.substring(0, ci).equals("transform")) return tok.substring(ci + 1);
        }
        return null;
    }

    // ================= C2S：打开海克斯工坊主界面时请求完整同步（客户端缓存兜底） =================
    // 解决联机玩家抽取/变更后打开界面读到旧缓存（空）的问题：界面打开即向服务端拉取最新数据，
    // 服务端回 S2CSyncData（含费用快照）并广播世界海克斯列表
    public static class C2SRequestSync {
        public static void encode(C2SRequestSync m, FriendlyByteBuf b) { }
        public static C2SRequestSync decode(FriendlyByteBuf b) { return new C2SRequestSync(); }
        public static void handle(C2SRequestSync m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                HextechEvents.sync(sp);
                // v0.6.3 顺带刷新世界海克斯列表缓存（打开任意工坊界面都拉到最新，界面侧收到后重建）
                broadcastWorldHexes(sp.server);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ================= C2S：请求抽取海克斯 =================
    public static class C2SRequestDraw {
        public static void encode(C2SRequestDraw m, FriendlyByteBuf b) { }
        public static C2SRequestDraw decode(FriendlyByteBuf b) { return new C2SRequestDraw(); }
        public static void handle(C2SRequestDraw m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                PlayerData data = ModCapabilities.get(sp);
                if (data == null) return;
                // v0.6.3 防重入：已有待选三选一未完成时禁止再开，避免重复扣经验
                if (data.getPendingRoll() != null) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.roll_pending"), true);
                    return;
                }
                int cost = HexConfig.XP_COST_PER_DRAW.get();
                if (realTotalXp(sp) < cost) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.xp_not_enough", cost), true);
                    return;
                }
                sp.giveExperiencePoints(-cost);
                PlayerData.RollOffer offer = new PlayerData.RollOffer(false, 0);
                offer.options.addAll(HextechRoller.rollHexOptions(data));
                data.setPendingRoll(offer);
                sendOpenRoll(sp, offer);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * 玩家真实总经验（点数）。
     * totalExperience 是缓存字段：FTB Quest 等 mod 直接赋值 experienceLevel / experienceProgress
     * （或经由 NBT/等级 API 注入）时缓存不同步，会误判"经验不足"无法抽取。
     * 这里按 Minecraft 标准等级公式逐级实时重算，任何来源给予的经验都能被识别。
     */
    private static int realTotalXp(ServerPlayer sp) {
        int lvl = sp.experienceLevel;
        long total = 0;
        for (int i = 0; i < lvl; i++) {
            total += xpNeededForLevel(i);
        }
        total += (long) (sp.experienceProgress * xpNeededForLevel(lvl));
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    /** Minecraft 1.20 标准经验公式：某等级升到下一级所需经验点数 */
    private static int xpNeededForLevel(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 15) return 37 + (level - 15) * 5;
        return 7 + level * 2;
    }

    // ================= C2S：取消本轮三选一（放弃抽取/锻造/装备/世界海克斯，清理服务端待选状态）=================
    public static class C2SCancelRoll {
        public static void encode(C2SCancelRoll m, FriendlyByteBuf b) { }
        public static C2SCancelRoll decode(FriendlyByteBuf b) { return new C2SCancelRoll(); }
        public static void handle(C2SCancelRoll m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                PlayerData data = ModCapabilities.get(sp);
                if (data == null) return;
                // 放弃本轮：清理待选状态（玩家海克斯/锻造/装备 pendingRoll + 世界海克斯 worldDrawOptions）
                // 费用不退（取消=放弃本轮，防止零成本无限预览白嫖刷新）
                boolean hadRoll = data.getPendingRoll() != null || data.getWorldDrawOptions() != null;
                data.setPendingRoll(null);
                data.setWorldDrawOptions(null);
                if (hadRoll) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.roll_cancelled"), true);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ================= C2S：三选一刷新 =================
    public static class C2SReroll {
        public static void encode(C2SReroll m, FriendlyByteBuf b) { }
        public static C2SReroll decode(FriendlyByteBuf b) { return new C2SReroll(); }
        public static void handle(C2SReroll m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                PlayerData data = ModCapabilities.get(sp);
                if (data == null) return;
                PlayerData.RollOffer offer = data.getPendingRoll();
                if (offer == null || offer.rerolled) return;
                int cost = discounted(data, HexConfig.REROLL_COST.get());
                if (!data.deductCoins(cost)) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.coins_not_enough", cost), true);
                    return;
                }
                offer.options.clear();
                offer.options.addAll(offer.forge
                        ? HextechRoller.rollForgeOptions(offer.tier) // v0.5.0 按档位分池
                        : (offer.equip ? HextechRegistry.rollEquipOptions(
                                offer.tier == 1 ? com.hexcraft.hextech.HextechRarity.PRISMATIC : com.hexcraft.hextech.HextechRarity.GOLD)
                                : HextechRoller.rollHexOptions(data)));
                offer.rerolled = true;
                sendOpenRoll(sp, offer);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ================= C2S：三选一确认 =================
    public static class C2SChooseRoll {
        public int index;
        public static void encode(C2SChooseRoll m, FriendlyByteBuf b) { b.writeInt(m.index); }
        public static C2SChooseRoll decode(FriendlyByteBuf b) { C2SChooseRoll m = new C2SChooseRoll(); m.index = b.readInt(); return m; }
        public static void handle(C2SChooseRoll m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                PlayerData data = ModCapabilities.get(sp);
                if (data == null) return;
                PlayerData.RollOffer offer = data.getPendingRoll();
                if (offer == null || m.index < 0 || m.index >= offer.options.size()) return;
                String pick = offer.options.get(m.index);
                // v0.6.0 校准：锻造 hp/hppct 前记录最大生命（回血差额用）
                double oldMaxHp = -1;
                if (offer.forge) {
                    oldMaxHp = sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getValue();
                    // v0.6.0 同词条叠加规则（STACK 默认 / BEST_ONLY 最高档保留+回收）
                    int refund = data.addForge(pick, 1, offer.tier);
                    if (refund > 0) {
                        data.addCoins(refund);
                        sp.displayClientMessage(Component.translatable("message.hexcraft.forge_refund",
                                Component.translatable("hexcraft.forge." + pick + ".name"), refund), true);
                    }
                } else if (offer.equip) {
                    int refund = data.equipItem(pick);
                    if (refund > 0) data.addCoins(refund);
                } else {
                    int refund = data.applyHex(pick);
                    if (refund > 0) data.addCoins(refund);
                    // v0.4.8 赌博海克斯（transform:*）：选中即一次性兑现——白银赌随机黄金 / 黄金赌随机棱彩 / 棱彩赌随机两个海克斯
                    String tx = transformOf(pick);
                    if (tx != null) {
                        data.removeHex(pick);
                        switch (tx) {
                            case "gold" -> {
                                String g = com.hexcraft.hextech.HextechRegistry.randomOf(com.hexcraft.hextech.HextechRarity.GOLD, pick);
                                if (g != null) data.applyHex(g);
                                sp.displayClientMessage(Component.translatable("message.hexcraft.gamble_gold",
                                        Component.translatable("hexcraft.hextech." + (g != null ? g : pick) + ".name")), true);
                            }
                            case "prismatic" -> {
                                String p = com.hexcraft.hextech.HextechRegistry.randomOf(com.hexcraft.hextech.HextechRarity.PRISMATIC, pick);
                                if (p != null) data.applyHex(p);
                                sp.displayClientMessage(Component.translatable("message.hexcraft.gamble_prism",
                                        Component.translatable("hexcraft.hextech." + (p != null ? p : pick) + ".name")), true);
                            }
                            case "double" -> {
                                String a = com.hexcraft.hextech.HextechRegistry.randomAny(pick);
                                String b = com.hexcraft.hextech.HextechRegistry.randomAny(a);
                                if (a != null) data.applyHex(a);
                                if (b != null) data.applyHex(b);
                                sp.displayClientMessage(Component.translatable("message.hexcraft.gamble_double",
                                        Component.translatable("hexcraft.hextech." + (a != null ? a : pick) + ".name"),
                                        Component.translatable("hexcraft.hextech." + (b != null ? b : a != null ? a : pick) + ".name")), true);
                            }
                            default -> { }
                        }
                    }
                }
                data.setPendingRoll(null);
                data.applyModifiers(sp);
                // v0.6.0 校准（问题3）：锻造 hp/hppct 提升最大生命后回补当前血量——
                // 钳制为 min(增量, 新上限-当前血)，防止溢出产生额外吸收护盾；满血锻造仅提升上限
                if (oldMaxHp > 0) {
                    double newMax = sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getValue();
                    double delta = newMax - oldMaxHp;
                    if (delta > 0) {
                        double healAmt = Math.min(delta, newMax - sp.getHealth());
                        if (healAmt > 0.0001) sp.heal((float) healAmt);
                    }
                }
                HextechEvents.sync(sp);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ================= C2S：购买锻造器 =================
    public static class C2SBuyForge {
        public int tier;
        public static void encode(C2SBuyForge m, FriendlyByteBuf b) { b.writeInt(m.tier); }
        public static C2SBuyForge decode(FriendlyByteBuf b) { C2SBuyForge m = new C2SBuyForge(); m.tier = b.readInt(); return m; }
        public static void handle(C2SBuyForge m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null || m.tier < 0 || m.tier > 2) return;
                PlayerData data = ModCapabilities.get(sp);
                if (data == null) return;
                if (data.getPendingRoll() != null) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.roll_pending"), true);
                    return;
                }
                int price = discounted(data, switch (m.tier) {
                    case 0 -> HexConfig.FORGE_BASIC_PRICE.get();
                    case 1 -> HexConfig.FORGE_GOLD_PRICE.get();
                    default -> HexConfig.FORGE_PRISMATIC_PRICE.get();
                });
                int limit = HexConfig.MAX_FORGES.get();
                if (limit > 0 && data.getForgesUsed() >= limit) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.forge_limit"), true);
                    return;
                }
                if (!data.deductCoins(price)) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.coins_not_enough", price), true);
                    return;
                }
                data.setForgesUsed(data.getForgesUsed() + 1);
                PlayerData.RollOffer offer = new PlayerData.RollOffer(true, m.tier);
                offer.options.addAll(HextechRoller.rollForgeOptions(m.tier)); // v0.5.0 按档位分池
                data.setPendingRoll(offer);
                sendOpenRoll(sp, offer);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ================= C2S：随机锻造器（随机稀有度，三选一属性）=================
    public static class C2SRandomForge {
        public static void encode(C2SRandomForge m, FriendlyByteBuf b) { }
        public static C2SRandomForge decode(FriendlyByteBuf b) { return new C2SRandomForge(); }
        public static void handle(C2SRandomForge m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                PlayerData data = ModCapabilities.get(sp);
                if (data == null) return;
                if (data.getPendingRoll() != null) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.roll_pending"), true);
                    return;
                }
                int tier = HextechRegistry.randomTier();
                int price = discounted(data, switch (tier) {
                    case 0 -> HexConfig.FORGE_BASIC_PRICE.get();
                    case 1 -> HexConfig.FORGE_GOLD_PRICE.get();
                    default -> HexConfig.FORGE_PRISMATIC_PRICE.get();
                });
                int limit = HexConfig.MAX_FORGES.get();
                if (limit > 0 && data.getForgesUsed() >= limit) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.forge_limit"), true);
                    return;
                }
                if (!data.deductCoins(price)) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.coins_not_enough", price), true);
                    return;
                }
                data.setForgesUsed(data.getForgesUsed() + 1);
                PlayerData.RollOffer offer = new PlayerData.RollOffer(true, tier);
                offer.options.addAll(HextechRoller.rollForgeOptions(tier)); // v0.5.0 按档位分池
                data.setPendingRoll(offer);
                sendOpenRoll(sp, offer);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ================= C2S：购买装备三选一（price=2500 随机装备池 / 4000 随机棱彩装备池）=================
    public static class C2SBuyEquip {
        public int price;
        public static void encode(C2SBuyEquip m, FriendlyByteBuf b) { b.writeInt(m.price); }
        public static C2SBuyEquip decode(FriendlyByteBuf b) { C2SBuyEquip m = new C2SBuyEquip(); m.price = b.readInt(); return m; }
        public static void handle(C2SBuyEquip m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                PlayerData data = ModCapabilities.get(sp);
                if (data == null) return;
                if (data.getPendingRoll() != null) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.roll_pending"), true);
                    return;
                }
                if (m.price != 2500 && m.price != 4000) return;
                int finalPrice = discounted(data, m.price);
                if (!data.deductCoins(finalPrice)) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.coins_not_enough", finalPrice), true);
                    return;
                }
                boolean prismatic = m.price == 4000;
                PlayerData.RollOffer offer = new PlayerData.RollOffer(false, true, prismatic ? 1 : 0);
                offer.options.addAll(HextechRegistry.rollEquipOptions(
                        prismatic ? com.hexcraft.hextech.HextechRarity.PRISMATIC : com.hexcraft.hextech.HextechRarity.GOLD));
                data.setPendingRoll(offer);
                sendOpenRoll(sp, offer);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ================= C2S：卖出装备（index）=================
    public static class C2SSellEquip {
        public int index;
        public static void encode(C2SSellEquip m, FriendlyByteBuf b) { b.writeInt(m.index); }
        public static C2SSellEquip decode(FriendlyByteBuf b) { C2SSellEquip m = new C2SSellEquip(); m.index = b.readInt(); return m; }
        public static void handle(C2SSellEquip m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                PlayerData data = ModCapabilities.get(sp);
                if (data == null) return;
                int sell = data.sellEquipAt(m.index);
                if (sell < 0) return;
                data.addCoins(sell);
                sp.displayClientMessage(Component.translatable("message.hexcraft.equip_sold", sell), true);
                data.applyModifiers(sp);
                HextechEvents.sync(sp);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ================= C2S：晶币兑换（充值页；多材质 → 晶币 + 晶币 → 绿宝石，支持批量 1~256）=================
    public static class C2SExchangeCoin {
        /** 0=材质→晶币；1=晶币→绿宝石（仅材质 0） */
        public int dir;
        /** 0=绿宝石 1=铜锭 2=铁锭 3=金锭 4=钻石 5=下界合金锭 */
        public int material;
        /** 兑换数量（1~256，服务端钳制） */
        public int count = 1;
        public static void encode(C2SExchangeCoin m, FriendlyByteBuf b) { b.writeInt(m.dir); b.writeInt(m.material); b.writeInt(m.count); }
        public static C2SExchangeCoin decode(FriendlyByteBuf b) { C2SExchangeCoin m = new C2SExchangeCoin(); m.dir = b.readInt(); m.material = b.readInt(); m.count = b.readInt(); return m; }
        public static void handle(C2SExchangeCoin m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null || (m.dir != 0 && m.dir != 1)) return;
                PlayerData data = ModCapabilities.get(sp);
                if (data == null) return;
                int[] rates = {30, 1, 2, 4, 8, 64}; // 绿宝石/铜/铁/金/钻石/下界合金
                if (m.material < 0 || m.material >= rates.length) return;
                int rate = rates[m.material];
                net.minecraft.world.item.Item item = switch (m.material) {
                    case 0 -> net.minecraft.world.item.Items.EMERALD;
                    case 1 -> net.minecraft.world.item.Items.COPPER_INGOT;
                    case 2 -> net.minecraft.world.item.Items.IRON_INGOT;
                    case 3 -> net.minecraft.world.item.Items.GOLD_INGOT;
                    case 4 -> net.minecraft.world.item.Items.DIAMOND;
                    default -> net.minecraft.world.item.Items.NETHERITE_INGOT;
                };
                int n = Math.max(1, Math.min(256, m.count));
                if (m.dir == 0) {
                    // 材质 → 晶币（批量；汇率高手加成，每级额外 +20%）
                    int total = 0;
                    for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                        var st = sp.getInventory().getItem(i);
                        if (st.is(item)) total += st.getCount();
                    }
                    if (total < n) {
                        sp.displayClientMessage(Component.translatable("message.hexcraft.no_material", Component.translatable(item.getDescriptionId())), true);
                        return;
                    }
                    int remain = n;
                    for (int i = 0; i < sp.getInventory().getContainerSize() && remain > 0; i++) {
                        var st = sp.getInventory().getItem(i);
                        if (st.is(item)) {
                            int take = Math.min(remain, st.getCount());
                            st.shrink(take);
                            remain -= take;
                        }
                    }
                    int gained = rate * n + (int) Math.floor(rate * n * HextechEvents.effect(data, "exchange_bonus"));
                    data.addCoins(gained);
                    sp.displayClientMessage(Component.translatable("message.hexcraft.exchange_gem", n, gained), true);
                } else {
                    // 晶币 → 绿宝石（批量，背包空位不足时部分回滚；仅材质 0）
                    if (m.material != 0) return;
                    if (!data.deductCoins(rate * n)) {
                        sp.displayClientMessage(Component.translatable("message.hexcraft.coins_not_enough", rate * n), true);
                        return;
                    }
                    int placed = 0;
                    for (int i = 0; i < n; i++) {
                        if (sp.addItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD))) {
                            placed++;
                        } else break;
                    }
                    if (placed < n) {
                        data.addCoins(rate * (n - placed)); // 回滚未放置部分
                        sp.displayClientMessage(Component.translatable("message.hexcraft.exchange_coin_partial", placed, rate * placed), true);
                    } else {
                        sp.displayClientMessage(Component.translatable("message.hexcraft.exchange_coin", n, rate * n), true);
                    }
                }
                HextechEvents.sync(sp);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ================= S2C：打开三选一界面 =================
    public static class S2COpenRoll {
        public boolean forge;
        public boolean equip;
        public int tier;
        public List<String> options = new ArrayList<>();
        public List<String> rarities = new ArrayList<>();
        public List<String> icons = new ArrayList<>();
        public int rerollCost;
        public boolean canReroll;
        public int pityGold;
        public int pityPrismatic;

        public static void encode(S2COpenRoll m, FriendlyByteBuf b) {
            b.writeBoolean(m.forge);
            b.writeBoolean(m.equip);
            b.writeInt(m.tier);
            b.writeInt(m.options.size());
            for (int i = 0; i < m.options.size(); i++) {
                b.writeUtf(m.options.get(i));
                b.writeUtf(m.rarities.get(i));
                b.writeUtf(i < m.icons.size() ? m.icons.get(i) : "◆");
            }
            b.writeInt(m.rerollCost);
            b.writeBoolean(m.canReroll);
            b.writeInt(m.pityGold);
            b.writeInt(m.pityPrismatic);
        }
        public static S2COpenRoll decode(FriendlyByteBuf b) {
            S2COpenRoll m = new S2COpenRoll();
            m.forge = b.readBoolean();
            m.equip = b.readBoolean();
            m.tier = b.readInt();
            int n = b.readInt();
            for (int i = 0; i < n; i++) {
                m.options.add(b.readUtf());
                m.rarities.add(b.readUtf());
                m.icons.add(b.readUtf());
            }
            m.rerollCost = b.readInt();
            m.canReroll = b.readBoolean();
            m.pityGold = b.readInt();
            m.pityPrismatic = b.readInt();
            return m;
        }
        public static void handle(S2COpenRoll m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openRoll(m)));
            ctx.get().setPacketHandled(true);
        }
    }

    // ================= C2S：配置界面切换游戏规则（服务端权威翻转并回广播） =================
    public static class C2SSetGameRule {
        /** 0=海克斯 1=锻造 2=装备 3=晶币 */
        public int ruleIndex;
        /** 目标值：true=保留，false=死亡清零（v0.4.10 由"翻转"改为"直接设置"，修复界面显示与服务端脱节时点反方向） */
        public boolean value;
        public static void encode(C2SSetGameRule m, FriendlyByteBuf b) { b.writeInt(m.ruleIndex); b.writeBoolean(m.value); }
        public static C2SSetGameRule decode(FriendlyByteBuf b) { C2SSetGameRule m = new C2SSetGameRule(); m.ruleIndex = b.readInt(); m.value = b.readBoolean(); return m; }
        public static void handle(C2SSetGameRule m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null || m.ruleIndex < 0 || m.ruleIndex >= com.hexcraft.config.HexGameRules.KEYS.length) return;
                // v0.4.14：权限守卫——仅房主（单机 integrated server）或 OP（hasPermissions(2)）可改服务器规则；
                // 联机普通玩家只改自己客户端配置（本地生效），无权改服务器 gamerule
                if (sp.server.isDedicatedServer() && !sp.hasPermissions(2)) {
                    com.hexcraft.Hexcraft.LOGGER.info("[Hexcraft] 拒绝玩家 {} 修改服务器游戏规则（非 OP）", sp.getName().getString());
                    return;
                }
                var key = com.hexcraft.config.HexGameRules.KEYS[m.ruleIndex];
                GameRules rules = sp.server.getGameRules();
                rules.getRule(key).set(m.value, sp.server);
                sp.displayClientMessage(Component.translatable("message.hexcraft.gamerule_set",
                        Component.translatable("gamerule." + key.getId()),
                        Component.translatable(m.value ? "message.hexcraft.on" : "message.hexcraft.off")), true);
                HextechEvents.sync(sp); // 回广播新值到客户端（含配置界面显示）
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ================= S2C：玩家数据同步 =================
    public static class S2CSyncData {
        public int coins;
        public int forgesUsed;
        public int maxForges;
        public int pityGold;
        public int pityPrismatic;
        public boolean keepHexes;
        public boolean keepForges;
        public boolean keepEquips;
        public boolean keepCoins;
        public List<PlayerData.HexEntry> hexes = new ArrayList<>();
        public List<PlayerData.ForgeEntry> forges = new ArrayList<>();
        public List<PlayerData.EquipEntry> equipment = new ArrayList<>();
        public Map<String, Double> attrs = new java.util.HashMap<>();
        // v0.6.0 通用叠层（战斗叠层/永久叠层）客户端展示数据
        public List<PlayerData.StackEntry> stacks = new ArrayList<>();
        // v0.6.3 服务端权威费用快照（界面显示与实扣一致；折扣/世界乘区实时计算）
        public int rerollCost;
        public int[] forgeCosts = new int[3];
        public int equipGoldCost;
        public int equipPrismaticCost;
        public int worldDrawCost;

        public static S2CSyncData of(PlayerData d, ServerPlayer sp) {
            S2CSyncData m = new S2CSyncData();
            m.coins = d.getCoins();
            m.forgesUsed = d.getForgesUsed();
            m.maxForges = HexConfig.MAX_FORGES.get();
            m.pityGold = d.getPityGold();
            m.pityPrismatic = d.getPityPrismatic();
            GameRules rules = sp.server.getGameRules();
            m.keepHexes = rules.getBoolean(com.hexcraft.config.HexGameRules.KEEP_HEXES);
            m.keepForges = rules.getBoolean(com.hexcraft.config.HexGameRules.KEEP_FORGES);
            m.keepEquips = rules.getBoolean(com.hexcraft.config.HexGameRules.KEEP_EQUIPS);
            m.keepCoins = rules.getBoolean(com.hexcraft.config.HexGameRules.KEEP_COINS);
            m.hexes.addAll(d.getHexes().stream().map(h -> {
                PlayerData.HexEntry ne = new PlayerData.HexEntry(h.id, h.level);
                ne.stacks = h.stacks;
                return ne;
            }).toList());
            m.forges.addAll(d.getForges().stream().map(f -> {
                PlayerData.ForgeEntry nf = new PlayerData.ForgeEntry(f.stat, f.level);
                nf.stacks = f.stacks;
                nf.tier = f.tier;
                return nf;
            }).toList());
            m.equipment.addAll(d.getEquipment().stream().map(e -> {
                PlayerData.EquipEntry ne = new PlayerData.EquipEntry(e.id, e.level);
                ne.stacks = e.stacks;
                return ne;
            }).toList());
            m.attrs.putAll(PlayerData.computeAttrs(sp));
            m.stacks.clear();
            for (PlayerData.StackEntry s : d.getStacksForBonus()) {
                PlayerData.StackEntry ns = new PlayerData.StackEntry(s.key, s.cap, s.combat, s.decayDelay, s.decayRate);
                ns.value = s.value;
                m.stacks.add(ns);
            }
            // v0.6.3 费用快照：与实扣完全同源（discounted / draw_cost 乘区）
            m.rerollCost = discounted(d, HexConfig.REROLL_COST.get());
            m.forgeCosts[0] = discounted(d, HexConfig.FORGE_BASIC_PRICE.get());
            m.forgeCosts[1] = discounted(d, HexConfig.FORGE_GOLD_PRICE.get());
            m.forgeCosts[2] = discounted(d, HexConfig.FORGE_PRISMATIC_PRICE.get());
            m.equipGoldCost = discounted(d, 2500);
            m.equipPrismaticCost = discounted(d, 4000);
            m.worldDrawCost = worldDrawCost(sp);
            return m;
        }

        public static void encode(S2CSyncData m, FriendlyByteBuf b) {
            b.writeInt(m.coins);
            b.writeInt(m.forgesUsed);
            b.writeInt(m.maxForges);
            b.writeInt(m.pityGold);
            b.writeInt(m.pityPrismatic);
            b.writeBoolean(m.keepHexes);
            b.writeBoolean(m.keepForges);
            b.writeBoolean(m.keepEquips);
            b.writeBoolean(m.keepCoins);
            b.writeInt(m.hexes.size());
            for (PlayerData.HexEntry h : m.hexes) { b.writeUtf(h.id); b.writeInt(h.level); b.writeInt(h.stacks); }
            b.writeInt(m.forges.size());
            for (PlayerData.ForgeEntry f : m.forges) { b.writeUtf(f.stat); b.writeInt(f.level); b.writeInt(f.stacks); b.writeInt(f.tier); }
            b.writeInt(m.equipment.size());
            for (PlayerData.EquipEntry e : m.equipment) { b.writeUtf(e.id); b.writeInt(e.level); b.writeInt(e.stacks); }
            b.writeInt(m.attrs.size());
            for (Map.Entry<String, Double> a : m.attrs.entrySet()) { b.writeUtf(a.getKey()); b.writeDouble(a.getValue()); }
            b.writeInt(m.stacks.size());
            for (PlayerData.StackEntry s : m.stacks) {
                b.writeUtf(s.key); b.writeDouble(s.value); b.writeDouble(s.cap);
                b.writeBoolean(s.combat); b.writeInt(s.decayDelay); b.writeDouble(s.decayRate);
            }
            b.writeInt(m.rerollCost);
            for (int i = 0; i < 3; i++) b.writeInt(m.forgeCosts[i]);
            b.writeInt(m.equipGoldCost);
            b.writeInt(m.equipPrismaticCost);
            b.writeInt(m.worldDrawCost);
        }
        public static S2CSyncData decode(FriendlyByteBuf b) {
            S2CSyncData m = new S2CSyncData();
            m.coins = b.readInt();
            m.forgesUsed = b.readInt();
            m.maxForges = b.readInt();
            m.pityGold = b.readInt();
            m.pityPrismatic = b.readInt();
            m.keepHexes = b.readBoolean();
            m.keepForges = b.readBoolean();
            m.keepEquips = b.readBoolean();
            m.keepCoins = b.readBoolean();
            int hn = b.readInt();
            for (int i = 0; i < hn; i++) {
                PlayerData.HexEntry h = new PlayerData.HexEntry(b.readUtf(), b.readInt());
                h.stacks = b.readInt();
                m.hexes.add(h);
            }
            int fn = b.readInt();
            for (int i = 0; i < fn; i++) {
                PlayerData.ForgeEntry f = new PlayerData.ForgeEntry(b.readUtf(), b.readInt());
                f.stacks = b.readInt();
                f.tier = b.readInt();
                m.forges.add(f);
            }
            int en = b.readInt();
            for (int i = 0; i < en; i++) {
                PlayerData.EquipEntry e = new PlayerData.EquipEntry(b.readUtf(), b.readInt());
                e.stacks = b.readInt();
                m.equipment.add(e);
            }
            int an = b.readInt();
            for (int i = 0; i < an; i++) m.attrs.put(b.readUtf(), b.readDouble());
            int sn = b.readInt();
            for (int i = 0; i < sn; i++) {
                String sk = b.readUtf();
                double sv = b.readDouble();
                double sc = b.readDouble();
                boolean cb = b.readBoolean();
                int dd = b.readInt();
                double dr = b.readDouble();
                PlayerData.StackEntry s = new PlayerData.StackEntry(sk, sc, cb, dd, dr);
                s.value = sv;
                m.stacks.add(s);
            }
            m.rerollCost = b.readInt();
            for (int i = 0; i < 3; i++) m.forgeCosts[i] = b.readInt();
            m.equipGoldCost = b.readInt();
            m.equipPrismaticCost = b.readInt();
            m.worldDrawCost = b.readInt();
            return m;
        }
        public static void handle(S2CSyncData m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.syncData(m)));
            ctx.get().setPacketHandled(true);
        }
    }

    private static void sendOpenRoll(ServerPlayer sp, PlayerData.RollOffer offer) {
        S2COpenRoll msg = new S2COpenRoll();
        msg.forge = offer.forge;
        msg.equip = offer.equip;
        msg.tier = offer.tier;
        msg.canReroll = !offer.rerolled;
        PlayerData d = ModCapabilities.get(sp);
        // v0.6.3 刷新价打折（与 C2SReroll 实扣同源，界面显示=实扣）
        msg.rerollCost = d != null ? discounted(d, HexConfig.REROLL_COST.get()) : HexConfig.REROLL_COST.get();
        if (d != null) {
            msg.pityGold = d.getPityGold();
            msg.pityPrismatic = d.getPityPrismatic();
        }
        msg.options = new ArrayList<>(offer.options);
        for (String id : offer.options) {
            if (offer.forge) {
                msg.rarities.add("forge");
            } else if (offer.equip) {
                HextechDefinition ed = HextechRegistry.getEquip(id);
                msg.rarities.add(ed != null ? ed.rarity.key : "gold");
                msg.icons.add(ed != null && ed.icon != null ? ed.icon : "◆");
            } else {
                msg.rarities.add(HextechRegistry.get(id) != null ? HextechRegistry.get(id).rarity.key : "silver");
            }
        }
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), msg);
    }

    // ================= 世界海克斯（v0.3.21）：全服共享、全局即时生效 =================

    // ===== S2C：世界海克斯列表同步（登录 + 变更广播，全员可见）=====
    public static class S2CSyncWorldHexes {
        public List<String> active = new ArrayList<>();
        public int maxPerCategory;
        public int drawXp;
        public int cancelCoin;

        public static S2CSyncWorldHexes of(net.minecraft.server.MinecraftServer server) {
            S2CSyncWorldHexes m = new S2CSyncWorldHexes();
            com.hexcraft.hextech.WorldHexData wh = com.hexcraft.hextech.WorldHexData.get(server.overworld());
            m.active.addAll(wh.active);
            m.maxPerCategory = HexConfig.WORLD_MAX_PER_CATEGORY.get();
            m.cancelCoin = HexConfig.WORLD_CANCEL_COIN.get();
            // v0.6.3 消耗含 draw_cost 独立乘区（与实扣同源；下限 1）
            m.drawXp = Math.max(1, (int) Math.round(HexConfig.WORLD_DRAW_XP.get()
                    * (1 + com.hexcraft.event.WorldHexEvents.drawCostMult(server.overworld()))));
            return m;
        }

        public static void encode(S2CSyncWorldHexes m, FriendlyByteBuf b) {
            b.writeInt(m.active.size());
            for (String id : m.active) b.writeUtf(id);
            b.writeInt(m.maxPerCategory);
            b.writeInt(m.drawXp);
            b.writeInt(m.cancelCoin);
        }
        public static S2CSyncWorldHexes decode(FriendlyByteBuf b) {
            S2CSyncWorldHexes m = new S2CSyncWorldHexes();
            int n = b.readInt();
            for (int i = 0; i < n; i++) m.active.add(b.readUtf());
            m.maxPerCategory = b.readInt();
            m.drawXp = b.readInt();
            m.cancelCoin = b.readInt();
            return m;
        }
        public static void handle(S2CSyncWorldHexes m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.syncWorldHexes(m)));
            ctx.get().setPacketHandled(true);
        }
    }

    // ===== C2S：请求世界海克斯抽取（三选一；消耗经验，独立乘区 draw_cost 可减耗）=====
    public static class C2SRequestWorldDraw {
        public static void encode(C2SRequestWorldDraw m, FriendlyByteBuf b) { }
        public static C2SRequestWorldDraw decode(FriendlyByteBuf b) { return new C2SRequestWorldDraw(); }
        public static void handle(C2SRequestWorldDraw m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                if (!(sp.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
                if (!HexConfig.WORLD_ENABLED.get()) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.world_disabled"), true);
                    return;
                }
                PlayerData data = ModCapabilities.get(sp);
                if (data == null) return;
                // v0.6.0 校准（备注5）：抽取消耗结果不得小于 0——负 draw_cost 只允许减耗，禁止返还经验/白嫖（下限 1 点）
                int cost = Math.max(1, (int) Math.round(HexConfig.WORLD_DRAW_XP.get()
                        * (1 + com.hexcraft.event.WorldHexEvents.drawCostMult(sl))));
                if (realTotalXp(sp) < cost) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.xp_not_enough", cost), true);
                    return;
                }
                sp.giveExperiencePoints(-cost);
                com.hexcraft.hextech.WorldHexData wh = com.hexcraft.hextech.WorldHexData.get(sl);
                List<String> opts = com.hexcraft.hextech.WorldHexRegistry.rollOptions(wh.active);
                if (opts.isEmpty()) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.world_all_owned"), true);
                    return;
                }
                data.setWorldDrawOptions(opts);
                sendWorldDrawOptions(sp, opts);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ===== S2C：世界海克斯三选一下发 =====
    public static class S2CWorldDrawOptions {
        public List<String> options = new ArrayList<>();
        public int drawXp;
        public static void encode(S2CWorldDrawOptions m, FriendlyByteBuf b) {
            b.writeInt(m.options.size());
            for (String id : m.options) b.writeUtf(id);
            b.writeInt(m.drawXp);
        }
        public static S2CWorldDrawOptions decode(FriendlyByteBuf b) {
            S2CWorldDrawOptions m = new S2CWorldDrawOptions();
            int n = b.readInt();
            for (int i = 0; i < n; i++) m.options.add(b.readUtf());
            m.drawXp = b.readInt();
            return m;
        }
        public static void handle(S2CWorldDrawOptions m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.worldDrawOptions(m)));
            ctx.get().setPacketHandled(true);
        }
    }

    // ===== C2S：确认世界海克斯（index；服务端校验上限/重复，全局生效广播）=====
    public static class C2SConfirmWorldHex {
        public int index;
        public static void encode(C2SConfirmWorldHex m, FriendlyByteBuf b) { b.writeInt(m.index); }
        public static C2SConfirmWorldHex decode(FriendlyByteBuf b) { C2SConfirmWorldHex m = new C2SConfirmWorldHex(); m.index = b.readInt(); return m; }
        public static void handle(C2SConfirmWorldHex m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                if (!(sp.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
                PlayerData data = ModCapabilities.get(sp);
                if (data == null) return;
                List<String> opts = data.getWorldDrawOptions();
                if (opts == null || m.index < 0 || m.index >= opts.size()) return;
                String pick = opts.get(m.index);
                com.hexcraft.hextech.WorldHexDefinition def = com.hexcraft.hextech.WorldHexRegistry.get(pick);
                com.hexcraft.hextech.WorldHexData wh = com.hexcraft.hextech.WorldHexData.get(sl);
                data.setWorldDrawOptions(null);
                if (def == null) return;
                if (wh.active.contains(pick)) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.world_duplicate"), true);
                    return;
                }
                int max = HexConfig.WORLD_MAX_PER_CATEGORY.get();
                if (max > 0) {
                    long inCat = wh.active.stream()
                            .map(com.hexcraft.hextech.WorldHexRegistry::get)
                            .filter(d -> d != null && d.category.equals(def.category)).count();
                    if (inCat >= max) {
                        sp.displayClientMessage(Component.translatable("message.hexcraft.world_category_full", max), true);
                        return;
                    }
                }
                wh.active.add(pick);
                wh.setDirty();
                com.hexcraft.event.WorldHexEvents.refreshLevel(sl);
                broadcastWorldHexes(sp.server);
                sp.displayClientMessage(Component.translatable("message.hexcraft.world_activated",
                        Component.translatable("hexcraft.worldhex." + pick + ".name")), true);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ===== C2S：取消世界海克斯（id；消耗晶币代价，全服即时移除）=====
    public static class C2SCancelWorldHex {
        public String id;
        public static void encode(C2SCancelWorldHex m, FriendlyByteBuf b) { b.writeUtf(m.id); }
        public static C2SCancelWorldHex decode(FriendlyByteBuf b) { C2SCancelWorldHex m = new C2SCancelWorldHex(); m.id = b.readUtf(); return m; }
        public static void handle(C2SCancelWorldHex m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                if (!(sp.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
                if (!com.hexcraft.hextech.WorldHexRegistry.exists(m.id)) return;
                com.hexcraft.hextech.WorldHexData wh = com.hexcraft.hextech.WorldHexData.get(sl);
                if (!wh.active.contains(m.id)) return;
                PlayerData data = ModCapabilities.get(sp);
                if (data == null) return;
                int cost = HexConfig.WORLD_CANCEL_COIN.get();
                if (!data.deductCoins(cost)) {
                    sp.displayClientMessage(Component.translatable("message.hexcraft.coins_not_enough", cost), true);
                    return;
                }
                wh.active.remove(m.id);
                wh.setDirty();
                com.hexcraft.event.WorldHexEvents.refreshLevel(sl);
                broadcastWorldHexes(sp.server);
                sp.displayClientMessage(Component.translatable("message.hexcraft.world_cancelled",
                        Component.translatable("hexcraft.worldhex." + m.id + ".name"), cost), true);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    private static void sendWorldDrawOptions(ServerPlayer sp, List<String> opts) {
        S2CWorldDrawOptions msg = new S2CWorldDrawOptions();
        msg.options = new ArrayList<>(opts);
        // v0.6.3 消耗含 draw_cost 乘区（与 C2SRequestWorldDraw 实扣同源）
        msg.drawXp = worldDrawCost(sp);
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), msg);
    }

    /** 世界海克斯变更/登录后全服广播（所有玩家都看到即时生效） */
    public static void broadcastWorldHexes(net.minecraft.server.MinecraftServer server) {
        S2CSyncWorldHexes msg = S2CSyncWorldHexes.of(server);
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), msg);
        }
    }

    // ================= S2C：数据刷新广播（v0.6.0 热重载——数据包内容变化后客户端界面自动刷新） =================
    public static class S2CDataRefresh {
        public String module; // all / hexes / forging / equipment / world

        public static S2CDataRefresh of(String module) {
            S2CDataRefresh m = new S2CDataRefresh();
            m.module = module;
            return m;
        }

        public static void encode(S2CDataRefresh m, FriendlyByteBuf b) { b.writeUtf(m.module); }
        public static S2CDataRefresh decode(FriendlyByteBuf b) { return of(b.readUtf()); }

        public static void handle(S2CDataRefresh m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.refreshData(m.module)));
            ctx.get().setPacketHandled(true);
        }

        public static void sendTo(ServerPlayer sp, String module) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), of(module));
        }
    }
}
