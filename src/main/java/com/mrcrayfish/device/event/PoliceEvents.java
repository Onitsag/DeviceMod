package com.mrcrayfish.device.event;

import com.mrcrayfish.device.programs.police.PoliceReportManager;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.IOException;

public class PoliceEvents
{
    @SubscribeEvent
    public void load(WorldEvent.Load event)
    {
        World world = event.getWorld();
        if(world.provider.getDimension() == 0)
        {
            File file = new File(world.getSaveHandler().getWorldDirectory(), "police_reports.dat");
            if(file.exists())
            {
                try
                {
                    NBTTagCompound tag = CompressedStreamTools.read(file);
                    if(tag != null)
                    {
                        PoliceReportManager.INSTANCE.readFromNBT(tag);
                    }
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    @SubscribeEvent
    public void save(WorldEvent.Save event)
    {
        World world = event.getWorld();
        if(world.provider.getDimension() == 0)
        {
            File file = new File(world.getSaveHandler().getWorldDirectory(), "police_reports.dat");
            try
            {
                NBTTagCompound tag = new NBTTagCompound();
                PoliceReportManager.INSTANCE.writeToNBT(tag);
                CompressedStreamTools.write(tag, file);
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
} 