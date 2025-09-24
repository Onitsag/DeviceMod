package fr.onitsag.faritech.programs.business.model;

import net.minecraft.nbt.NBTTagCompound;

public class EmployeeRecord
{
    private String playerUuid;
    private String playerName;
    private String gradeId;

    public EmployeeRecord() {}

    public EmployeeRecord(String playerUuid, String playerName, String gradeId)
    {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.gradeId = gradeId;
    }

    public String getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getGradeId() { return gradeId; }
    public void setGradeId(String gradeId) { this.gradeId = gradeId; }

    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("uuid", playerUuid);
        tag.setString("name", playerName);
        tag.setString("gradeId", gradeId);
        return tag;
    }

    public static EmployeeRecord fromTag(NBTTagCompound tag)
    {
        return new EmployeeRecord(tag.getString("uuid"), tag.getString("name"), tag.getString("gradeId"));
    }
}


