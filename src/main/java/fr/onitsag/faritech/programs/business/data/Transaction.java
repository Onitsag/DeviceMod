package fr.onitsag.faritech.programs.business.data;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Représente une transaction comptable
 */
public class Transaction
{
    public enum TransactionType
    {
        INCOME("Recette"),
        EXPENSE("Dépense"),
        SALARY("Salaire"),
        PURCHASE("Achat"),
        SALE("Vente");

        private final String displayName;

        TransactionType(String displayName)
        {
            this.displayName = displayName;
        }

        public String getDisplayName()
        {
            return displayName;
        }
    }

    private String id;
    private String date;
    private TransactionType type;
    private double amount;
    private String description;
    private String category;
    private String reference;

    public Transaction(String id, String date, TransactionType type, double amount, String description, String category, String reference)
    {
        this.id = id;
        this.date = date;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.reference = reference;
    }

    public Transaction(NBTTagCompound tag)
    {
        this.id = tag.getString("id");
        this.date = tag.getString("date");
        this.type = TransactionType.valueOf(tag.getString("type"));
        this.amount = tag.getDouble("amount");
        this.description = tag.getString("description");
        this.category = tag.getString("category");
        this.reference = tag.getString("reference");
    }

    public NBTTagCompound writeToNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", id);
        tag.setString("date", date);
        tag.setString("type", type.name());
        tag.setDouble("amount", amount);
        tag.setString("description", description);
        tag.setString("category", category);
        tag.setString("reference", reference);
        return tag;
    }

    public String getDisplayInfo()
    {
        String sign = (type == TransactionType.INCOME || type == TransactionType.SALE) ? "+" : "-";
        return date + " - " + type.getDisplayName() + " - " + sign + String.format("%.2f", amount) + "€ - " + description;
    }

    public boolean isIncome()
    {
        return type == TransactionType.INCOME || type == TransactionType.SALE;
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
