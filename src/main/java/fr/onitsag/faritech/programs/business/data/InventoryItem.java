package fr.onitsag.faritech.programs.business.data;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Représente un article en stock
 */
public class InventoryItem
{
    private String id;
    private String name;
    private String category;
    private int quantity;
    private double unitPrice;
    private double cost;
    private String supplier;
    private int minStock;

    public InventoryItem(String id, String name, String category, int quantity, double unitPrice, double cost, String supplier, int minStock)
    {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.cost = cost;
        this.supplier = supplier;
        this.minStock = minStock;
    }

    public InventoryItem(NBTTagCompound tag)
    {
        this.id = tag.getString("id");
        this.name = tag.getString("name");
        this.category = tag.getString("category");
        this.quantity = tag.getInteger("quantity");
        this.unitPrice = tag.getDouble("unitPrice");
        this.cost = tag.getDouble("cost");
        this.supplier = tag.getString("supplier");
        this.minStock = tag.getInteger("minStock");
    }

    public NBTTagCompound writeToNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", id);
        tag.setString("name", name);
        tag.setString("category", category);
        tag.setInteger("quantity", quantity);
        tag.setDouble("unitPrice", unitPrice);
        tag.setDouble("cost", cost);
        tag.setString("supplier", supplier);
        tag.setInteger("minStock", minStock);
        return tag;
    }

    public boolean isLowStock()
    {
        return quantity <= minStock;
    }

    public double getTotalValue()
    {
        return quantity * unitPrice;
    }

    public String getDisplayInfo()
    {
        return name + " - Qté: " + quantity + " - Prix: " + String.format("%.2f", unitPrice) + "€";
    }

    @Override
    public String toString()
    {
        return getDisplayInfo();
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
}
