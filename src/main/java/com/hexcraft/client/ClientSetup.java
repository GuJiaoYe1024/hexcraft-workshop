package com.hexcraft.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientSetup {
    public static void init() {
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
        // 按键绑定：显式注册到 mod 总线（不依赖注解扫描，生产环境更可靠）
        FMLJavaModLoadingContext.get().getModEventBus().register(ClientKeybinds.class);
        // 配置界面双通道（可选依赖模式）：
        // - 装了 Cloth Config API → Mods 列表 Config 按钮打开 Cloth 标准界面
        // - 未装 → 不注册 ConfigScreenHandler，Forge 自动回退到原生配置界面（
        //   registerConfig 已注册的 common/client toml 可直接编辑），不崩溃
        if (ModList.get().isLoaded("cloth_config")) {
            registerClothConfigScreen();
        }
    }

    // 独立方法隔离 Cloth 引用：仅在 Cloth 已加载的分支中调用，
    // 未装 Cloth 时 JVM 懒加载不会解析此方法内的 Cloth 类型，避免 NoClassDefFoundError
    private static void registerClothConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) ->
                        com.hexcraft.client.gui.HexConfigScreen.create(screen)));
    }
}
