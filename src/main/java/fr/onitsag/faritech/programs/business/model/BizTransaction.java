package fr.onitsag.faritech.programs.business.model;

import net.minecraft.nbt.NBTTagCompound;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BizTransaction
{
    public enum Type { TRANSFER_IN, TRANSFER_OUT }

    private String id;
    private long timestamp;
    private String fromCompanyId;
    private String toCompanyId;
    private double amount;
    private Type type;
    private String description;

    public BizTransaction() {}

    public BizTransaction(String id, long timestamp, String fromCompanyId, String toCompanyId, double amount, Type type, String description)
    {
        this.id = id;
        this.timestamp = timestamp;
        this.fromCompanyId = fromCompanyId;
        this.toCompanyId = toCompanyId;
        this.amount = amount;
        this.type = type;
        this.description = description;
    }

    public String getDisplay()
    {
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(timestamp));
        String sign = type == Type.TRANSFER_IN ? "+" : "-";
        return date + " " + sign + String.format("%.2f", amount) + "€ - " + description;
    }

    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", id);
        tag.setLong("ts", timestamp);
        tag.setString("from", fromCompanyId);
        tag.setString("to", toCompanyId);
        tag.setDouble("amount", amount);
        tag.setString("type", type.name());
        tag.setString("desc", description == null ? "" : description);
        return tag;
    }

    public static BizTransaction fromTag(NBTTagCompound tag)
    {
        BizTransaction t = new BizTransaction();
        t.id = tag.getString("id");
        t.timestamp = tag.getLong("ts");
        t.fromCompanyId = tag.getString("from");
        t.toCompanyId = tag.getString("to");
        t.amount = tag.getDouble("amount");
        t.type = Type.valueOf(tag.getString("type"));
        t.description = tag.getString("desc");
        return t;
    }
}


