package fr.onitsag.faritech.programs.police.task;

import fr.onitsag.faritech.api.task.Task;
import fr.onitsag.faritech.programs.police.PoliceReportManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import fr.onitsag.faritech.programs.police.network.MessageSyncPoliceReports;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayerMP;
import fr.onitsag.faritech.network.PacketHandler;

public class TaskDeletePoliceReport extends Task {
    private int index;

    public TaskDeletePoliceReport() {
        super("delete_police_report");
    }

    public TaskDeletePoliceReport(int index) {
        this();
        this.index = index;
    }

    @Override
    public void prepareRequest(NBTTagCompound nbt) {
        nbt.setInteger("index", index);
    }

    @Override
    public void processRequest(NBTTagCompound nbt, World world, EntityPlayer player) {
        int idx = nbt.getInteger("index");
        PoliceReportManager.INSTANCE.deleteReport(idx);
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
    public void processResponse(NBTTagCompound nbt) {}
} 
