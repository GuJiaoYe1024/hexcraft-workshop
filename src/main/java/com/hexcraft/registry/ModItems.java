package com.hexcraft.registry;

import com.hexcraft.Hexcraft;
import com.hexcraft.item.HexCoinItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Hexcraft.MODID);

    /** 海克斯晶币：合成产物，手持右键存入余额 */
    public static final RegistryObject<Item> HEX_COIN = ITEMS.register("hex_coin", HexCoinItem::new);
}
