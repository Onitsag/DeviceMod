package fr.onitsag.faritech.programs.business.layout;

import fr.onitsag.faritech.api.app.Dialog;
import fr.onitsag.faritech.api.app.ScrollableLayout;
import fr.onitsag.faritech.api.app.Icons;
import fr.onitsag.faritech.api.app.component.*;
import fr.onitsag.faritech.programs.business.ApplicationBusinessManager;
import fr.onitsag.faritech.programs.business.data.Employee;
import fr.onitsag.faritech.programs.business.data.Transaction;
import fr.onitsag.faritech.programs.system.layout.StandardLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Layout pour la gestion des salaires
 */
public class PayrollLayout extends StandardLayout
{
    private ApplicationBusinessManager app;
    private ScrollableLayout content;
    private ItemList<Employee> employeeList;
    private ItemList<PayrollEntry> payrollHistory;
    private TextField bonusField;
    private TextField deductionField;
    private TextField monthField;
    private Button processPayrollButton;
    private Button generateReportButton;
    private Label totalLabel;
    private Label statusLabel;

    private List<Employee> employees;
    private List<PayrollEntry> payrollEntries;

    public PayrollLayout(ApplicationBusinessManager app)
    {
        super("Gestion des Salaires", 362, 240, app, null);
        this.app = app;
        this.employees = new ArrayList<>();
        this.payrollEntries = new ArrayList<>();
        // viewport=362x143 comme les apps système (barre titre + barre d'état)
        this.content = new ScrollableLayout(0, 21, 362, 340, 143);
        this.addComponent(content);
        initializeComponents();
        loadSampleData();
    }

    private void initializeComponents()
    {
        // Liste des employés
        content.addComponent(new Label("Employés:", 6, 6));
        employeeList = new ItemList<>(6, 14, 162, 90);
        employeeList.setItemClickListener((employee, index, mouseButton) -> {
            if(mouseButton == 0 && employee != null) {
                selectEmployee(employee);
            }
        });
        content.addComponent(employeeList);

        // Formulaire de paie
        int formX = 176;
        content.addComponent(new Label("Mois (MM/YYYY):", formX, 6));
        monthField = new TextField(formX, 14, 80);
        monthField.setText(new SimpleDateFormat("MM/yyyy").format(new Date()));
        content.addComponent(monthField);

        content.addComponent(new Label("Prime (€):", formX, 34));
        bonusField = new TextField(formX, 42, 80);
        bonusField.setText("0");
        content.addComponent(bonusField);

        content.addComponent(new Label("Déduction (€):", formX, 62));
        deductionField = new TextField(formX, 70, 80);
        deductionField.setText("0");
        content.addComponent(deductionField);

        processPayrollButton = new Button(formX, 98, 110, 18, "Traiter la Paie", Icons.CASH);
        processPayrollButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                processPayroll();
            }
        });
        content.addComponent(processPayrollButton);

        generateReportButton = new Button(formX + 116, 98, 80, 18, "Rapport", Icons.FILE);
        generateReportButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                generateReport();
            }
        });
        content.addComponent(generateReportButton);

        // Historique des paies
        content.addComponent(new Label("Historique des Paies:", 6, 126));
        payrollHistory = new ItemList<>(6, 134, 350, 60);
        content.addComponent(payrollHistory);

        // Total mensuel
        totalLabel = new Label("Total mensuel: 0.00€", 6, 198);
        content.addComponent(totalLabel);

        // Label de statut
        statusLabel = new Label("", 6, 210);
        content.addComponent(statusLabel);

        // Bouton retour
        Button backButton = new Button(302, 212, 56, 18, "Retour", Icons.ARROW_LEFT);
        backButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                app.returnToMainMenu();
            }
        });
        content.addComponent(backButton);
    }

    private void loadSampleData()
    {
        // Données d'exemple d'employés
        employees.add(new Employee(UUID.randomUUID().toString(), "Jean", "Dupont", "Développeur", 45000, "IT", "2023-01-15"));
        employees.add(new Employee(UUID.randomUUID().toString(), "Marie", "Martin", "Designer", 38000, "Design", "2023-03-20"));
        employees.add(new Employee(UUID.randomUUID().toString(), "Pierre", "Bernard", "Manager", 55000, "Management", "2022-11-10"));
        
        refreshEmployeeList();
        refreshPayrollHistory();
        updateTotal();
    }

    private void selectEmployee(Employee employee)
    {
        // Mise à jour de l'interface pour l'employé sélectionné
        if(employee != null) {
            setStatus("Employé sélectionné: " + employee.getFullName() + " - Salaire: " + String.format("%.2f", employee.getSalary()) + "€");
        }
    }

    private void processPayroll()
    {
        Employee selectedEmployee = employeeList.getSelectedItem();
        if(selectedEmployee == null) {
            setStatus("&cVeuillez sélectionner un employé");
            return;
        }

        try {
            String month = monthField.getText().trim();
            double bonus = Double.parseDouble(bonusField.getText().trim());
            double deduction = Double.parseDouble(deductionField.getText().trim());
            
            double monthlySalary = selectedEmployee.getSalary() / 12.0;
            double totalPay = monthlySalary + bonus - deduction;

            PayrollEntry entry = new PayrollEntry(
                UUID.randomUUID().toString(),
                selectedEmployee.getId(),
                selectedEmployee.getFullName(),
                month,
                monthlySalary,
                bonus,
                deduction,
                totalPay,
                new SimpleDateFormat("dd/MM/yyyy").format(new Date())
            );

            payrollEntries.add(entry);
            refreshPayrollHistory();
            updateTotal();
            
            bonusField.setText("0");
            deductionField.setText("0");
            
            setStatus("&aPaie traitée pour " + selectedEmployee.getFullName() + ": " + String.format("%.2f", totalPay) + "€");

        } catch(NumberFormatException e) {
            setStatus("&cVeuillez entrer des montants valides");
        }
    }

    private void generateReport()
    {
        String month = monthField.getText().trim();
        double totalPayroll = 0;
        int employeeCount = 0;

        StringBuilder report = new StringBuilder();
        report.append("=== RAPPORT DE PAIE - ").append(month).append(" ===\n\n");

        for(PayrollEntry entry : payrollEntries) {
            if(entry.getMonth().equals(month)) {
                report.append(entry.getEmployeeName())
                      .append(" - Salaire: ").append(String.format("%.2f", entry.getBaseSalary()))
                      .append("€ + Prime: ").append(String.format("%.2f", entry.getBonus()))
                      .append("€ - Déduction: ").append(String.format("%.2f", entry.getDeduction()))
                      .append("€ = Total: ").append(String.format("%.2f", entry.getTotalPay()))
                      .append("€\n");
                totalPayroll += entry.getTotalPay();
                employeeCount++;
            }
        }

        report.append("\nNombre d'employés: ").append(employeeCount);
        report.append("\nTotal des salaires: ").append(String.format("%.2f", totalPayroll)).append("€");

        // Créer une fenêtre de dialogue pour afficher le rapport
        Dialog.Message reportDialog = new Dialog.Message(report.toString());
        reportDialog.setTitle("Rapport de Paie");
        app.openDialog(reportDialog);
    }

    private void refreshEmployeeList()
    {
        employeeList.removeAll();
        for(Employee employee : employees) {
            employeeList.addItem(employee);
        }
    }

    private void refreshPayrollHistory()
    {
        payrollHistory.removeAll();
        for(PayrollEntry entry : payrollEntries) {
            payrollHistory.addItem(entry);
        }
    }

    private void updateTotal()
    {
        String currentMonth = monthField.getText().trim();
        double total = 0;
        
        for(PayrollEntry entry : payrollEntries) {
            if(entry.getMonth().equals(currentMonth)) {
                total += entry.getTotalPay();
            }
        }
        
        totalLabel.setText("Total mensuel (" + currentMonth + "): " + String.format("%.2f", total) + "€");
    }

    private void setStatus(String message)
    {
        statusLabel.setText(message);
    }

    // Classe interne pour les entrées de paie
    private static class PayrollEntry
    {
        private String id;
        private String employeeId;
        private String employeeName;
        private String month;
        private double baseSalary;
        private double bonus;
        private double deduction;
        private double totalPay;
        private String processDate;

        public PayrollEntry(String id, String employeeId, String employeeName, String month, double baseSalary, double bonus, double deduction, double totalPay, String processDate)
        {
            this.id = id;
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.month = month;
            this.baseSalary = baseSalary;
            this.bonus = bonus;
            this.deduction = deduction;
            this.totalPay = totalPay;
            this.processDate = processDate;
        }

        @Override
        public String toString()
        {
            return month + " - " + employeeName + " - " + String.format("%.2f", totalPay) + "€ (le " + processDate + ")";
        }

        // Getters
        public String getId() { return id; }
        public String getEmployeeId() { return employeeId; }
        public String getEmployeeName() { return employeeName; }
        public String getMonth() { return month; }
        public double getBaseSalary() { return baseSalary; }
        public double getBonus() { return bonus; }
        public double getDeduction() { return deduction; }
        public double getTotalPay() { return totalPay; }
        public String getProcessDate() { return processDate; }
    }
}
