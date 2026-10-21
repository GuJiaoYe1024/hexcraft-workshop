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

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hexcraft.client.ClientSetup.init());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }
}
