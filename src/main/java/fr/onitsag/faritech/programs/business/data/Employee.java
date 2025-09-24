package fr.onitsag.faritech.programs.business.data;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Représente un employé dans le système de gestion d'entreprise
 */
public class Employee
{
    private String id;
    private String firstName;
    private String lastName;
    private String position;
    private double salary;
    private String department;
    private String hireDate;
    private boolean isActive;

    public Employee(String id, String firstName, String lastName, String position, double salary, String department, String hireDate)
    {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.position = position;
        this.salary = salary;
        this.department = department;
        this.hireDate = hireDate;
        this.isActive = true;
    }

    public Employee(NBTTagCompound tag)
    {
        this.id = tag.getString("id");
        this.firstName = tag.getString("firstName");
        this.lastName = tag.getString("lastName");
        this.position = tag.getString("position");
        this.salary = tag.getDouble("salary");
        this.department = tag.getString("department");
        this.hireDate = tag.getString("hireDate");
        this.isActive = tag.getBoolean("isActive");
    }

    public NBTTagCompound writeToNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", id);
        tag.setString("firstName", firstName);
        tag.setString("lastName", lastName);
        tag.setString("position", position);
        tag.setDouble("salary", salary);
        tag.setString("department", department);
        tag.setString("hireDate", hireDate);
        tag.setBoolean("isActive", isActive);
        return tag;
    }

    public String getFullName()
    {
        return firstName + " " + lastName;
    }

    public String getDisplayInfo()
    {
        return getFullName() + " - " + position + " (" + department + ")";
    }

    @Override
    public String toString()
    {
        return getDisplayInfo();
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getHireDate() { return hireDate; }
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
