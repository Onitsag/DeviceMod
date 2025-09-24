package fr.onitsag.faritech.proxy;

import fr.onitsag.faritech.api.app.Application;
import fr.onitsag.faritech.api.print.IPrint;
import fr.onitsag.faritech.init.DeviceBlocks;
import fr.onitsag.faritech.network.PacketHandler;
import fr.onitsag.faritech.network.task.MessageSyncApplications;
import fr.onitsag.faritech.network.task.MessageSyncConfig;
import fr.onitsag.faritech.object.AppInfo;
import fr.onitsag.faritech.programs.system.SystemApplication;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommonProxy
{
	List<AppInfo> allowedApps;
	int hashCode = -1;

	public void preInit()
	{
		MinecraftForge.EVENT_BUS.register(this);
	}

	public void init() {}

	public void postInit() {}

	@Nullable
	public Application registerApplication(ResourceLocation identifier, Class<? extends Application> clazz)
	{
		if(allowedApps == null)
		{
			allowedApps = new ArrayList<>();
		}
		if(SystemApplication.class.isAssignableFrom(clazz))
		{
			allowedApps.add(new AppInfo(identifier, true));
		}
		else
		{
			allowedApps.add(new AppInfo(identifier, false));
		}
		return null;
	}

	public boolean registerPrint(ResourceLocation identifier, Class<? extends IPrint> classPrint)
	{
		return true;
	}

	public boolean hasAllowedApplications()
	{
		return allowedApps != null;
	}

	public List<AppInfo> getAllowedApplications()
	{
		if(allowedApps == null)
		{
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(allowedApps);
	}

	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if(allowedApps != null)
		{
			PacketHandler.INSTANCE.sendTo(new MessageSyncApplications(allowedApps), (EntityPlayerMP) event.player);
		}
		PacketHandler.INSTANCE.sendTo(new MessageSyncConfig(), (EntityPlayerMP) event.player);
		
		// Synchroniser les données business pour le joueur qui vient de se connecter
		NBTTagCompound businessData = fr.onitsag.faritech.programs.business.data.BusinessData.INSTANCE.toNetworkTag();
		PacketHandler.INSTANCE.sendTo(new fr.onitsag.faritech.programs.business.network.MessageSyncBusiness(businessData), (EntityPlayerMP) event.player);
	}

	@SubscribeEvent
	public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
	{
		World world = event.getWorld();
		if(!event.getItemStack().isEmpty() && event.getItemStack().getItem() == Items.PAPER)
		{
			if(world.getBlockState(event.getPos()).getBlock() == DeviceBlocks.PRINTER)
			{
				event.setUseBlock(Event.Result.ALLOW);
			}
		}
	}

	public void showNotification(NBTTagCompound tag) {}
}
