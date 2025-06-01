package com.mrcrayfish.device.programs.police;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class PoliceReportManager
{
    public static final PoliceReportManager INSTANCE = new PoliceReportManager();
    private List<PoliceReport> reports = new ArrayList<>();

    public void addReport(PoliceReport report)
    {
        reports.add(0, report); // Ajoute au début de la liste
    }

    public List<PoliceReport> getReports()
    {
        return reports;
    }

    public void deleteReport(int index)
    {
        if(index >= 0 && index < reports.size())
        {
            reports.remove(index);
        }
    }

    public void readFromNBT(NBTTagCompound nbt)
    {
        reports.clear();
        NBTTagList list = nbt.getTagList("reports", Constants.NBT.TAG_COMPOUND);
        for(int i = 0; i < list.tagCount(); i++)
        {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            reports.add(new PoliceReport(
                tag.getString("suspectName"),
                tag.getString("location"),
                tag.getString("date"),
                tag.getString("details")
            ));
        }
    }

    public void writeToNBT(NBTTagCompound nbt)
    {
        NBTTagList list = new NBTTagList();
        for(PoliceReport report : reports)
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("suspectName", report.getSuspectName());
            tag.setString("location", report.getLocation());
            tag.setString("date", report.getDate());
            tag.setString("details", report.getDetails());
            list.appendTag(tag);
        }
        nbt.setTag("reports", list);
    }
} 