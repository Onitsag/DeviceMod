package fr.onitsag.faritech.programs.police.task;

import fr.onitsag.faritech.api.task.Task;
import fr.onitsag.faritech.programs.police.PoliceReportManager;
import fr.onitsag.faritech.programs.police.network.MessageSyncPoliceReports;
import fr.onitsag.faritech.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class TaskRequestPoliceReports extends Task {
    public TaskRequestPoliceReports() {
        super("request_police_reports");
    }

    @Override
    public void prepareRequest(NBTTagCompound nbt) {}

    @Override
    public void processRequest(NBTTagCompound nbt, World world, EntityPlayer player) {
        NBTTagCompound tag = new NBTTagCompound();
        PoliceReportManager.INSTANCE.writeToNBT(tag);
        PacketHandler.INSTANCE.sendTo(new MessageSyncPoliceReports(tag), (net.minecraft.entity.player.EntityPlayerMP) player);
        this.setSuccessful();
    }

    @Override
    public void prepareResponse(NBTTagCompound nbt) {}
    @Override
    public void processResponse(NBTTagCompound nbt) {}
} 
