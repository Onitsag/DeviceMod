package fr.onitsag.faritech.item;

import fr.onitsag.faritech.FariTechMod;
import net.minecraft.item.Item;

/**
 * Author: MrCrayfish
 */
public class ItemComponent extends Item
{
    public ItemComponent(String id)
    {
        this.setUnlocalizedName(id);
        this.setRegistryName(id);
        this.setCreativeTab(FariTechMod.TAB_DEVICE);
    }
}
