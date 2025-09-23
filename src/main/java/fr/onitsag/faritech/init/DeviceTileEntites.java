package fr.onitsag.faritech.init;

import fr.onitsag.faritech.tileentity.*;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class DeviceTileEntites 
{
	public static void register()
    {
		GameRegistry.registerTileEntity(TileEntityLaptop.class, "faritech:laptop");
        GameRegistry.registerTileEntity(TileEntityRouter.class, "faritech:router");
		GameRegistry.registerTileEntity(TileEntityPrinter.class, "faritech:printer");
		GameRegistry.registerTileEntity(TileEntityPaper.class, "faritech:printed_paper");
		GameRegistry.registerTileEntity(TileEntityOfficeChair.class, "faritech:office_chair");
	}
}
