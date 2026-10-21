package com.hexcraft.registry;

import com.hexcraft.Hexcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Hexcraft.MODID);

    public static final RegistryObject<CreativeModeTab> HEXCRAFT_TAB = TABS.register("hexcraft", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.HEX_COIN.get()))
            .title(Component.translatable("itemGroup.hexcraft"))
            .displayItems((params, output) -> {
                output.accept(new ItemStack(ModItems.HEX_COIN.get()));
            })
            .build());
}
