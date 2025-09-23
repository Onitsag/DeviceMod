package fr.onitsag.faritech.programs.police.task;

import fr.onitsag.faritech.api.task.Task;
import fr.onitsag.faritech.programs.police.PoliceReport;
import fr.onitsag.faritech.programs.police.PoliceReportManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.client.Minecraft;
import fr.onitsag.faritech.programs.police.ApplicationPolice;
import fr.onitsag.faritech.api.ApplicationManager;
import fr.onitsag.faritech.programs.police.network.MessageSyncPoliceReports;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayerMP;
import fr.onitsag.faritech.network.PacketHandler;

public class TaskAddPoliceReport extends Task {
    private PoliceReport report;

    public TaskAddPoliceReport() {
        super("add_police_report");
    }

    public TaskAddPoliceReport(PoliceReport report) {
        this();
        this.report = report;
    }

    @Override
    public void prepareRequest(NBTTagCompound nbt) {
        nbt.setString("suspectName", report.getSuspectName());
        nbt.setString("location", report.getLocation());
        nbt.setString("date", report.getDate());
        nbt.setString("details", report.getDetails());
    }

    @Override
    public void processRequest(NBTTagCompound nbt, World world, EntityPlayer player) {
        PoliceReport report = new PoliceReport(
            nbt.getString("suspectName"),
            nbt.getString("location"),
            nbt.getString("date"),
            nbt.getString("details")
        );
        PoliceReportManager.INSTANCE.addReport(report);
        // Synchronise la liste à tous les clients
        NBTTagCompound tag = new NBTTagCompound();
        PoliceReportManager.INSTANCE.writeToNBT(tag);
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if(server != null && server.getPlayerList() != null) {
            for(EntityPlayerMP p : server.getPlayerList().getPlayers()) {
                PacketHandler.INSTANCE.sendTo(new MessageSyncPoliceReports(tag), p);
            }
        }
        this.setSuccessful();
    }

    @Override
    public void prepareResponse(NBTTagCompound nbt) {}

    @Override
    public void processResponse(NBTTagCompound nbt) {
        // Côté client : recharge la liste après ajout
        Minecraft.getMinecraft().addScheduledTask(() -> {
            // Récupère l'instance de l'application police via la fenêtre du laptop
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if(mc.currentScreen instanceof fr.onitsag.faritech.core.Laptop) {
                fr.onitsag.faritech.core.Laptop laptop = (fr.onitsag.faritech.core.Laptop) mc.currentScreen;
                for (fr.onitsag.faritech.core.Window window : laptop.getOpenWindows()) {
                    if(window != null && window.getContent() instanceof fr.onitsag.faritech.programs.police.ApplicationPolice) {
                        ((fr.onitsag.faritech.programs.police.ApplicationPolice) window.getContent()).loadReports();
                    }
                }
            }
        });
    }
} 
