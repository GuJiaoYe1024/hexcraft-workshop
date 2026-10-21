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
    private static int discounted(PlayerData data, int price) {
        double disc = HextechEvents.effect(data, "trade_discount");
        return (int) Math.floor(price * Math.max(0.1, 1 - disc));
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
                int cost = HexConfig.XP_COST_PER_DRAW.get();
                if (sp.totalExperience < cost) {
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
                        ? HextechRoller.rollForgeOptions()
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
                if (offer.forge) {
                    data.getForges().add(new PlayerData.ForgeEntry(pick, 1, offer.tier));
                } else if (offer.equip) {
                    int refund = data.equipItem(pick);
                    if (refund > 0) data.addCoins(refund);
                } else {
                    int refund = data.applyHex(pick);
                    if (refund > 0) data.addCoins(refund);
                }
                data.setPendingRoll(null);
                data.applyModifiers(sp);
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
                offer.options.addAll(HextechRoller.rollForgeOptions());
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
                offer.options.addAll(HextechRoller.rollForgeOptions());
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
        public static void encode(C2SSetGameRule m, FriendlyByteBuf b) { b.writeInt(m.ruleIndex); }
        public static C2SSetGameRule decode(FriendlyByteBuf b) { C2SSetGameRule m = new C2SSetGameRule(); m.ruleIndex = b.readInt(); return m; }
        public static void handle(C2SSetGameRule m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null || m.ruleIndex < 0 || m.ruleIndex >= com.hexcraft.config.HexGameRules.KEYS.length) return;
                var key = com.hexcraft.config.HexGameRules.KEYS[m.ruleIndex];
                GameRules rules = sp.server.getGameRules();
                boolean next = !rules.getBoolean(key);
                rules.getRule(key).set(next, sp.server);
                sp.displayClientMessage(Component.translatable("message.hexcraft.gamerule_set",
                        Component.translatable("gamerule." + key.getId()),
                        Component.translatable(next ? "message.hexcraft.on" : "message.hexcraft.off")), true);
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
        msg.rerollCost = HexConfig.REROLL_COST.get();
        msg.canReroll = !offer.rerolled;
        PlayerData d = ModCapabilities.get(sp);
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
}
