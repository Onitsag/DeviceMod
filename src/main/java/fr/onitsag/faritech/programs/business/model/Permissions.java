package fr.onitsag.faritech.programs.business.model;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Permissions granulaires par grade.
 */
public class Permissions
{
    public boolean canRecruit;
    public boolean canManageGrades;
    public boolean canChangeEmployeeGrade;
    public boolean canFire;
    public double transferLimit;

    public Permissions() {}

    public Permissions(boolean canRecruit,
                       boolean canManageGrades,
                       boolean canChangeEmployeeGrade,
                       boolean canFire,
                       double transferLimit)
    {
        this.canRecruit = canRecruit;
        this.canManageGrades = canManageGrades;
        this.canChangeEmployeeGrade = canChangeEmployeeGrade;
        this.canFire = canFire;
        this.transferLimit = transferLimit;
    }

    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("canRecruit", canRecruit);
        tag.setBoolean("canManageGrades", canManageGrades);
        tag.setBoolean("canChangeEmployeeGrade", canChangeEmployeeGrade);
        tag.setBoolean("canFire", canFire);
        tag.setDouble("transferLimit", transferLimit);
        return tag;
    }

    public static Permissions fromTag(NBTTagCompound tag)
    {
        Permissions p = new Permissions();
        p.canRecruit = tag.getBoolean("canRecruit");
        p.canManageGrades = tag.getBoolean("canManageGrades");
        p.canChangeEmployeeGrade = tag.getBoolean("canChangeEmployeeGrade");
        p.canFire = tag.getBoolean("canFire");
        p.transferLimit = tag.getDouble("transferLimit");
        return p;
    }
}


