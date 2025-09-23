package fr.onitsag.faritech;

import fr.onitsag.faritech.api.ApplicationManager;
import fr.onitsag.faritech.api.print.PrintingManager;
import fr.onitsag.faritech.api.task.TaskManager;
import fr.onitsag.faritech.core.io.task.*;
import fr.onitsag.faritech.core.network.task.TaskConnect;
import fr.onitsag.faritech.core.network.task.TaskGetDevices;
import fr.onitsag.faritech.core.network.task.TaskPing;
import fr.onitsag.faritech.core.print.task.TaskPrint;
import fr.onitsag.faritech.core.task.TaskInstallApp;
import fr.onitsag.faritech.entity.EntitySeat;
import fr.onitsag.faritech.event.BankEvents;
import fr.onitsag.faritech.event.EmailEvents;
import fr.onitsag.faritech.event.PoliceEvents;
import fr.onitsag.faritech.gui.GuiHandler;
import fr.onitsag.faritech.init.DeviceTileEntites;
import fr.onitsag.faritech.init.RegistrationHandler;
import fr.onitsag.faritech.network.PacketHandler;
import fr.onitsag.faritech.programs.*;
import fr.onitsag.faritech.programs.debug.ApplicationTextArea;
import fr.onitsag.faritech.programs.email.ApplicationEmail;
import fr.onitsag.faritech.programs.email.task.*;
import fr.onitsag.faritech.programs.example.ApplicationExample;
import fr.onitsag.faritech.programs.example.task.TaskNotificationTest;
import fr.onitsag.faritech.programs.gitweb.ApplicationGitWeb;
import fr.onitsag.faritech.programs.police.ApplicationPolice;
import fr.onitsag.faritech.programs.ApplicationNoteStash;
import fr.onitsag.faritech.programs.ApplicationPixelPainter;
import fr.onitsag.faritech.programs.system.ApplicationAppStore;
import fr.onitsag.faritech.programs.system.ApplicationBank;
import fr.onitsag.faritech.programs.system.ApplicationFileBrowser;
import fr.onitsag.faritech.programs.system.ApplicationSettings;
import fr.onitsag.faritech.programs.system.task.*;
import fr.onitsag.faritech.proxy.CommonProxy;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.apache.logging.log4j.Logger;
import fr.onitsag.faritech.programs.police.task.TaskAddPoliceReport;
import fr.onitsag.faritech.programs.police.task.TaskDeletePoliceReport;
import fr.onitsag.faritech.programs.police.task.TaskRequestPoliceReports;

@Mod(modid = Reference.MOD_ID, name = Reference.NAME, version = Reference.VERSION, acceptedMinecraftVersions = Reference.WORKING_MC_VERSION)
public class FariTechMod 
{
	@Instance(Reference.MOD_ID)
	public static FariTechMod instance;
	
	@SidedProxy(clientSide = Reference.CLIENT_PROXY_CLASS, serverSide = Reference.COMMON_PROXY_CLASS)
	public static CommonProxy proxy;
	
	public static final CreativeTabs TAB_DEVICE = new FariTechTab("faritechTabDevice");

	private static Logger logger;

	public static final boolean DEVELOPER_MODE = false;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) throws LaunchException
	{
		if(FariTechMod.DEVELOPER_MODE && !(Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment"))
		{
			throw new LaunchException();
		}
		logger = event.getModLog();

		DeviceConfig.load(event.getSuggestedConfigurationFile());
		MinecraftForge.EVENT_BUS.register(new DeviceConfig());

		RegistrationHandler.init();
		
		proxy.preInit();
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event) 
	{
		/* Tile Entity Registering */
		DeviceTileEntites.register();

		EntityRegistry.registerModEntity(new ResourceLocation("faritech:seat"), EntitySeat.class, "Seat", 0, this, 80, 1, false);

		/* Packet Registering */
		PacketHandler.init();

		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

		MinecraftForge.EVENT_BUS.register(new EmailEvents());
		MinecraftForge.EVENT_BUS.register(new BankEvents());
		MinecraftForge.EVENT_BUS.register(new PoliceEvents());

		registerApplications();

		proxy.init();
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) 
	{
		proxy.postInit();
	}

	private void registerApplications()
	{
		// Applications (Both)
		ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "settings"), ApplicationSettings.class);
		ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "bank"), ApplicationBank.class);
		ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "file_browser"), ApplicationFileBrowser.class);
		ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "gitweb"), ApplicationGitWeb.class);
		ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "note_stash"), ApplicationNoteStash.class);
		ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "pixel_painter"), ApplicationPixelPainter.class);
		ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "ender_mail"), ApplicationEmail.class);
		ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "app_store"), ApplicationAppStore.class);
		ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "police"), ApplicationPolice.class);

		// Core
		TaskManager.registerTask(TaskInstallApp.class);
		TaskManager.registerTask(TaskUpdateApplicationData.class);
		TaskManager.registerTask(TaskPrint.class);
		TaskManager.registerTask(TaskUpdateSystemData.class);
		TaskManager.registerTask(TaskConnect.class);
		TaskManager.registerTask(TaskPing.class);
		TaskManager.registerTask(TaskGetDevices.class);

		//Bank
		TaskManager.registerTask(TaskDeposit.class);
		TaskManager.registerTask(TaskWithdraw.class);
		TaskManager.registerTask(TaskGetBalance.class);
		TaskManager.registerTask(TaskPay.class);
		TaskManager.registerTask(TaskAdd.class);
		TaskManager.registerTask(TaskRemove.class);

		//File browser
		TaskManager.registerTask(TaskSendAction.class);
		TaskManager.registerTask(TaskSetupFileBrowser.class);
		TaskManager.registerTask(TaskGetFiles.class);
		TaskManager.registerTask(TaskGetStructure.class);
		TaskManager.registerTask(TaskGetMainDrive.class);

		//Ender Mail
		TaskManager.registerTask(TaskUpdateInbox.class);
		TaskManager.registerTask(TaskSendEmail.class);
		TaskManager.registerTask(TaskCheckEmailAccount.class);
		TaskManager.registerTask(TaskRegisterEmailAccount.class);
		TaskManager.registerTask(TaskDeleteEmail.class);
		TaskManager.registerTask(TaskViewEmail.class);

		// Police
		TaskManager.registerTask(TaskAddPoliceReport.class);
		TaskManager.registerTask(TaskDeletePoliceReport.class);
		TaskManager.registerTask(TaskRequestPoliceReports.class);

		if(!DEVELOPER_MODE)
		{
			// Applications (Normal)
			//ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "boat_racers"), ApplicationBoatRacers.class);
			//ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "mine_bay"), ApplicationMineBay.class);

			// Tasks (Normal)
			//TaskManager.registerTask(TaskAddAuction.class);
			//TaskManager.registerTask(TaskGetAuctions.class);
			//TaskManager.registerTask(TaskBuyItem.class);
		}
		else
		{
			// Applications (Developers)
			ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "example"), ApplicationExample.class);
			ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "icons"), ApplicationIcons.class);
			ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "text_area"), ApplicationTextArea.class);
			ApplicationManager.registerApplication(new ResourceLocation(Reference.MOD_ID, "test"), ApplicationTest.class);

			TaskManager.registerTask(TaskNotificationTest.class);
		}

		PrintingManager.registerPrint(new ResourceLocation(Reference.MOD_ID, "picture"), ApplicationPixelPainter.PicturePrint.class);
	}

	public static Logger getLogger()
	{
		return logger;
	}
}
