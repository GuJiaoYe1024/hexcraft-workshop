package com.hexcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 客户端按键绑定。
 * 注册方式：ClientSetup.init() 中显式 FMLJavaModLoadingContext#getModEventBus().register(ClientKeybinds.class)，
 * 不依赖注解自动扫描（生产环境更可靠），保证 RegisterKeyMappingsEvent 一定会触发、按键绑定界面必然出现。
 */
public class ClientKeybinds {

    public static final KeyMapping OPEN_HEXTECH = new KeyMapping(
            "key.hexcraft.open_hextech",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            "key.categories.hexcraft");

    /** v0.4.1：世界海克斯直达按键（默认 K，可在按键绑定中修改） */
    public static final KeyMapping OPEN_WORLD_HEX = new KeyMapping(
            "key.hexcraft.open_world",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.hexcraft");

    /** v0.7.2：界面布局编辑器按键（默认 L：游戏内直接弹半透明画布拖动调整提示/状态栏/页签位置） */
    public static final KeyMapping OPEN_LAYOUT = new KeyMapping(
            "key.hexcraft.open_layout",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_L,
            "key.categories.hexcraft");

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_HEXTECH);
        event.register(OPEN_WORLD_HEX);
        event.register(OPEN_LAYOUT);
    }
}
