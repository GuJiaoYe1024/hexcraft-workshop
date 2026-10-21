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

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_HEXTECH);
    }
}
