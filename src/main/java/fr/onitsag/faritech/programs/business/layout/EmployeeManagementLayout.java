package fr.onitsag.faritech.programs.business.layout;

import fr.onitsag.faritech.api.app.Icons;
import fr.onitsag.faritech.api.app.ScrollableLayout;
import fr.onitsag.faritech.api.app.component.*;
import fr.onitsag.faritech.api.app.Dialog;
import fr.onitsag.faritech.programs.business.ApplicationBusinessManager;
import fr.onitsag.faritech.programs.business.data.Employee;
import fr.onitsag.faritech.programs.system.layout.StandardLayout;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Layout pour la gestion des employés
 */
public class EmployeeManagementLayout extends StandardLayout
{
    private ApplicationBusinessManager app;
    private ScrollableLayout content;
    private ItemList<Employee> employeeList;
    private TextField firstNameField;
    private TextField lastNameField;
    private TextField positionField;
    private TextField departmentField;
    private TextField salaryField;
    private TextField hireDateField;
    private Button addButton;
    private Button editButton;
    private Button deleteButton;
    private Label statusLabel;

    private List<Employee> employees;

    public EmployeeManagementLayout(ApplicationBusinessManager app)
    {
        super("Gestion des Employés", 362, 240, app, null);
        this.app = app;
        this.employees = new ArrayList<>();
        // Zone scrollable: x=0, y=21 (sous la barre de titre), viewport=362x143 (zone visible), contenu initial 420px
        this.content = new ScrollableLayout(0, 21, 362, 420, 143);
        this.addComponent(content);
        initializeComponents();
        loadSampleData();
    }

    private void initializeComponents()
    {
        // Liste des employés
        employeeList = new ItemList<>(6, 6, 170, 120);
        employeeList.setItemClickListener((employee, index, mouseButton) -> {
            if(mouseButton == 0 && employee != null) {
                selectEmployee(employee);
            }
        });
        content.addComponent(employeeList);

        // Formulaire d'ajout/modification
        int formX = 180;
        content.addComponent(new Label("Prénom:", formX, 6));
        firstNameField = new TextField(formX, 14, 160);
        content.addComponent(firstNameField);

        content.addComponent(new Label("Nom:", formX, 34));
        lastNameField = new TextField(formX, 42, 160);
        content.addComponent(lastNameField);

        content.addComponent(new Label("Poste:", formX, 62));
        positionField = new TextField(formX, 70, 160);
        content.addComponent(positionField);

        content.addComponent(new Label("Département:", formX, 90));
        departmentField = new TextField(formX, 98, 160);
        content.addComponent(departmentField);

        content.addComponent(new Label("Salaire (€):", formX, 118));
        salaryField = new TextField(formX, 126, 160);
        content.addComponent(salaryField);

        content.addComponent(new Label("Date d'embauche:", formX, 146));
        hireDateField = new TextField(formX, 154, 160);
        content.addComponent(hireDateField);

        // Boutons d'action
        addButton = new Button(formX, 184, 70, 18, "Ajouter", Icons.PLUS);
        addButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                addEmployee();
            }
        });
        content.addComponent(addButton);

        editButton = new Button(formX + 75, 184, 70, 18, "Modifier", Icons.EDIT);
        editButton.setEnabled(false);
        editButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                editEmployee();
            }
        });
        content.addComponent(editButton);

        deleteButton = new Button(formX + 150, 184, 70, 18, "Supprimer", Icons.TRASH);
        deleteButton.setEnabled(false);
        deleteButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                deleteEmployee();
            }
        });
        content.addComponent(deleteButton);

        // Label de statut
        statusLabel = new Label("", 6, 208);
        content.addComponent(statusLabel);

        // Bouton retour
        Button backButton = new Button(300, 212, 56, 18, "Retour", Icons.ARROW_LEFT);
        backButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                app.returnToMainMenu();
            }
        });
        content.addComponent(backButton);
    }

    private void loadSampleData()
    {
        // Données d'exemple
        employees.add(new Employee(UUID.randomUUID().toString(), "Jean", "Dupont", "Développeur", 45000, "IT", "2023-01-15"));
        employees.add(new Employee(UUID.randomUUID().toString(), "Marie", "Martin", "Designer", 38000, "Design", "2023-03-20"));
        employees.add(new Employee(UUID.randomUUID().toString(), "Pierre", "Bernard", "Manager", 55000, "Management", "2022-11-10"));
        refreshEmployeeList();
    }

    private void selectEmployee(Employee employee)
    {
        firstNameField.setText(employee.getFirstName());
        lastNameField.setText(employee.getLastName());
        positionField.setText(employee.getPosition());
        departmentField.setText(employee.getDepartment());
        salaryField.setText(String.valueOf(employee.getSalary()));
        hireDateField.setText(employee.getHireDate());

        editButton.setEnabled(true);
        deleteButton.setEnabled(true);
        addButton.setText("Ajouter");
    }

    private void addEmployee()
    {
        try {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String position = positionField.getText().trim();
            String department = departmentField.getText().trim();
            String salaryText = salaryField.getText().trim();
            String hireDate = hireDateField.getText().trim();

            if(firstName.isEmpty() || lastName.isEmpty() || position.isEmpty()) {
                setStatus("&cVeuillez remplir tous les champs obligatoires");
                return;
            }

            double salary = Double.parseDouble(salaryText);
            
            Employee newEmployee = new Employee(
                UUID.randomUUID().toString(),
                firstName, lastName, position, salary, department, hireDate
            );

            employees.add(newEmployee);
            refreshEmployeeList();
            clearForm();
            setStatus("&aEmployé ajouté avec succès");

        } catch(NumberFormatException e) {
            setStatus("&cSalaire invalide");
        }
    }

    private void editEmployee()
    {
        Employee selected = employeeList.getSelectedItem();
        if(selected == null) return;

        try {
            selected.setFirstName(firstNameField.getText().trim());
            selected.setLastName(lastNameField.getText().trim());
            selected.setPosition(positionField.getText().trim());
            selected.setDepartment(departmentField.getText().trim());
            selected.setSalary(Double.parseDouble(salaryField.getText().trim()));
            selected.setHireDate(hireDateField.getText().trim());

            refreshEmployeeList();
            setStatus("&aEmployé modifié avec succès");

        } catch(NumberFormatException e) {
            setStatus("&cSalaire invalide");
        }
    }

    private void deleteEmployee()
    {
        Employee selected = employeeList.getSelectedItem();
        if(selected == null) return;

        Dialog.Confirmation dialog = new Dialog.Confirmation("Êtes-vous sûr de vouloir supprimer cet employé ?");
        dialog.setPositiveListener((mouseX, mouseY, mouseButton) -> {
            employees.remove(selected);
            refreshEmployeeList();
            clearForm();
            setStatus("&aEmployé supprimé");
        });
        app.openDialog(dialog);
    }

    private void clearForm()
    {
        firstNameField.setText("");
        lastNameField.setText("");
        positionField.setText("");
        departmentField.setText("");
        salaryField.setText("");
        hireDateField.setText("");
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private void refreshEmployeeList()
    {
        employeeList.removeAll();
        for(Employee employee : employees) {
            employeeList.addItem(employee);
        }
    }

    private void setStatus(String message)
    {
        statusLabel.setText(message);
    }
}
