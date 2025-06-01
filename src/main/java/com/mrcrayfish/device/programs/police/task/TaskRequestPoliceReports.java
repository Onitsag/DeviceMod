package com.mrcrayfish.device.programs.police.task;

import com.mrcrayfish.device.api.task.Task;
import com.mrcrayfish.device.programs.police.PoliceReportManager;
import com.mrcrayfish.device.programs.police.network.MessageSyncPoliceReports;
import com.mrcrayfish.device.network.PacketHandler;
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