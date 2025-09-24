package fr.onitsag.faritech.programs.business.model;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class Company
{
    private String id;
    private String name;
    private String ownerUuid;
    private double balance;
    private List<EmployeeRecord> employees = new ArrayList<>();
    private List<Grade> grades = new ArrayList<>();
    private List<BizTransaction> transactions = new ArrayList<>();

    public Company() {}

    public Company(String id, String name, String ownerUuid)
    {
        this.id = id;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.balance = 0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getOwnerUuid() { return ownerUuid; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public List<EmployeeRecord> getEmployees() { return employees; }
    public List<Grade> getGrades() { return grades; }
    public List<BizTransaction> getTransactions() { return transactions; }

    @Override
    public String toString()
    {
        return name;
    }

    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", id);
        tag.setString("name", name);
        tag.setString("owner", ownerUuid);
        tag.setDouble("balance", balance);

        NBTTagList empList = new NBTTagList();
        for(EmployeeRecord e : employees) empList.appendTag(e.toTag());
        tag.setTag("employees", empList);

        NBTTagList gradeList = new NBTTagList();
        for(Grade g : grades) gradeList.appendTag(g.toTag());
        tag.setTag("grades", gradeList);

        NBTTagList txList = new NBTTagList();
        for(BizTransaction t : transactions) txList.appendTag(t.toTag());
        tag.setTag("tx", txList);

        return tag;
    }

    public static Company fromTag(NBTTagCompound tag)
    {
        Company c = new Company();
        c.id = tag.getString("id");
        c.name = tag.getString("name");
        c.ownerUuid = tag.getString("owner");
        c.balance = tag.getDouble("balance");

        NBTTagList empList = tag.getTagList("employees", Constants.NBT.TAG_COMPOUND);
        for(int i=0;i<empList.tagCount();i++) c.employees.add(EmployeeRecord.fromTag(empList.getCompoundTagAt(i)));

        NBTTagList gradeList = tag.getTagList("grades", Constants.NBT.TAG_COMPOUND);
        for(int i=0;i<gradeList.tagCount();i++) c.grades.add(Grade.fromTag(gradeList.getCompoundTagAt(i)));

        NBTTagList txList = tag.getTagList("tx", Constants.NBT.TAG_COMPOUND);
        for(int i=0;i<txList.tagCount();i++) c.transactions.add(BizTransaction.fromTag(txList.getCompoundTagAt(i)));

        return c;
    }
}


