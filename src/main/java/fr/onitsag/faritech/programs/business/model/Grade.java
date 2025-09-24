package fr.onitsag.faritech.programs.business.model;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Grade d'entreprise avec hiérarchie (niveau plus grand = plus haut).
 */
public class Grade
{
    private String id;
    private String name;
    private int level;
    private Permissions permissions;

    public Grade() {}

    public Grade(String id, String name, int level, Permissions permissions)
    {
        this.id = id;
        this.name = name;
        this.level = level;
        this.permissions = permissions;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public Permissions getPermissions() { return permissions; }

    public void setName(String name) { this.name = name; }
    public void setLevel(int level) { this.level = level; }
    public void setPermissions(Permissions permissions) { this.permissions = permissions; }

    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", id);
        tag.setString("name", name);
        tag.setInteger("level", level);
        tag.setTag("permissions", permissions != null ? permissions.toTag() : new NBTTagCompound());
        return tag;
    }

    public static Grade fromTag(NBTTagCompound tag)
    {
        Grade g = new Grade();
        g.id = tag.getString("id");
        g.name = tag.getString("name");
        g.level = tag.getInteger("level");
        g.permissions = Permissions.fromTag(tag.getCompoundTag("permissions"));
        return g;
    }

    @Override
    public String toString()
    {
        return name != null ? name : "";
    }
}


