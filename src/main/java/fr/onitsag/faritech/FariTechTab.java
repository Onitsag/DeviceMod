package fr.onitsag.faritech;

import fr.onitsag.faritech.init.DeviceBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;

public class FariTechTab extends CreativeTabs 
{
	public FariTechTab(String label) 
	{
		super(label);
	}

	@Override
	public ItemStack getTabIconItem() 
	{
		return new ItemStack(DeviceBlocks.LAPTOP, 1, EnumDyeColor.RED.getMetadata());
	}
}
