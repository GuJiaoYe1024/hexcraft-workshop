package com.hexcraft;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.command.HexCommand;
import com.hexcraft.config.HexConfig;
import com.hexcraft.event.HextechEvents;
import com.hexcraft.network.PacketHandler;
import com.hexcraft.registry.ModCreativeTabs;
import com.hexcraft.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Hexcraft.MODID)
public class Hexcraft {
    public static final String MODID = "hexworkshop";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 注意：1.20.1 的 FMLModContainer 仅支持无参构造器实例化 @Mod 类（构造器参数注入是后续版本特性），
    // 因此必须通过 FMLJavaModLoadingContext 取 mod event bus（该方法在 1.20.1 标 deprecated，但无官方替代）。
    public Hexcraft() {
        // 触发死亡保留 gamerule 注册（类加载即 register，必须在服务器创建前）
        com.hexcraft.config.HexGameRules.touch();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);

        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        // 1.20.1 的 ModLoadingContext#registerConfig 无官方替代，保留（仅编译期 deprecation 提示）
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HexConfig.COMMON_SPEC, "hexworkshop-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HexConfig.CLIENT_SPEC, "hexworkshop-client.toml");

        MinecraftForge.EVENT_BUS.register(ModCapabilities.class);
        MinecraftForge.EVENT_BUS.register(HextechEvents.class);
        MinecraftForge.EVENT_BUS.register(com.hexcraft.event.WorldHexEvents.class);
        MinecraftForge.EVENT_BUS.register(HexCommand.class);
        // v0.6.20 Iron's Spells 'n Spellbooks 法术联动：仅安装时挂载（未安装零副作用，联动内容自动隐藏）
        if (net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) {
            com.hexcraft.link.IronsSpellLink.init();
        }
        // v0.6.21 冰与火之歌 / Alex's Caves / 暮色森林 三联动：各自仅安装时挂载（未安装零副作用）
        if (net.minecraftforge.fml.ModList.get().isLoaded("iceandfire")) {
            com.hexcraft.link.IceAndFireLink.init();
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("alexscaves")) {
            com.hexcraft.link.AlexCavesLink.init();
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("twilightforest")) {
            com.hexcraft.link.TwilightLink.init();
        }
        // v0.6.22 Alex's Mobs 联动：仅安装时挂载（未安装零副作用）
        if (net.minecraftforge.fml.ModList.get().isLoaded("alexsmobs")) {
            com.hexcraft.link.AlexMobsLink.init();
        }
        // v0.8.3 第二批联动（末地灾变/巫术/Mowzie's Mobs/神器/遗物/拔刀剑/农夫乐事/FTB Quests）：
        // 8 组任一安装即挂载（未装的组事件判定自动不命中、抽取池自动隐藏，零副作用）
        if (net.minecraftforge.fml.ModList.get().isLoaded("cataclysm")
                || net.minecraftforge.fml.ModList.get().isLoaded("goety")
                || net.minecraftforge.fml.ModList.get().isLoaded("mowziesmobs")
                || net.minecraftforge.fml.ModList.get().isLoaded("artifacts")
                || net.minecraftforge.fml.ModList.get().isLoaded("relics")
                || net.minecraftforge.fml.ModList.get().isLoaded("slashblade")
                || net.minecraftforge.fml.ModList.get().isLoaded("farmersdelight")
                || net.minecraftforge.fml.ModList.get().isLoaded("ftbquests")) {
            com.hexcraft.link.MoreLink.init();
        }
        // v0.8.3 ProjectE 等价交换联动（EMC A：充值页 EMC⇄晶币；EMC C：等价交换海克斯）：仅安装时挂载
        if (net.minecraftforge.fml.ModList.get().isLoaded("projecte")) {
            com.hexcraft.link.EmcLink.init();
        }

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hexcraft.client.ClientSetup.init());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
        // v0.6.0 属性上限突破（护甲/生命/攻击距离等 maxValue 放大，无上限锻造不撞天花板）
        event.enqueueWork(AttributeUnlock::unlock);
    }
}
