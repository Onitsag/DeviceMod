package com.mrcrayfish.device.programs.police.task;

import com.mrcrayfish.device.api.task.Task;
import com.mrcrayfish.device.programs.police.PoliceReport;
import com.mrcrayfish.device.programs.police.PoliceReportManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.client.Minecraft;
import com.mrcrayfish.device.programs.police.ApplicationPolice;
import com.mrcrayfish.device.api.ApplicationManager;
import com.mrcrayfish.device.programs.police.network.MessageSyncPoliceReports;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayerMP;
import com.mrcrayfish.device.network.PacketHandler;

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
            if(mc.currentScreen instanceof com.mrcrayfish.device.core.Laptop) {
                com.mrcrayfish.device.core.Laptop laptop = (com.mrcrayfish.device.core.Laptop) mc.currentScreen;
                for (com.mrcrayfish.device.core.Window window : laptop.getOpenWindows()) {
                    if(window != null && window.getContent() instanceof com.mrcrayfish.device.programs.police.ApplicationPolice) {
                        ((com.mrcrayfish.device.programs.police.ApplicationPolice) window.getContent()).loadReports();
                    }
                }
            }
        });
    }
} 