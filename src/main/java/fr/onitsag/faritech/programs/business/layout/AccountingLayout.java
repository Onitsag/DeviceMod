package fr.onitsag.faritech.programs.business.layout;

import fr.onitsag.faritech.api.app.Dialog;
import fr.onitsag.faritech.api.app.ScrollableLayout;
import fr.onitsag.faritech.api.app.Icons;
import fr.onitsag.faritech.api.app.component.*;
import fr.onitsag.faritech.programs.business.ApplicationBusinessManager;
import fr.onitsag.faritech.programs.business.data.Transaction;
import fr.onitsag.faritech.programs.system.layout.StandardLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Layout pour la comptabilité
 */
public class AccountingLayout extends StandardLayout
{
    private ApplicationBusinessManager app;
    private ScrollableLayout content;
    private ItemList<Transaction> transactionList;
    private ComboBox<Transaction.TransactionType> typeComboBox;
    private TextField amountField;
    private TextField descriptionField;
    private TextField categoryField;
    private TextField referenceField;
    private TextField dateField;
    private Button addTransactionButton;
    private Button generateReportButton;
    private Label balanceLabel;
    private Label incomeLabel;
    private Label expenseLabel;
    private Label statusLabel;
    private ComboBox<String> periodFilter;

    private List<Transaction> transactions;

    public AccountingLayout(ApplicationBusinessManager app)
    {
        super("Comptabilité", 362, 240, app, null);
        this.app = app;
        this.transactions = new ArrayList<>();
        this.content = new ScrollableLayout(0, 21, 362, 360, 143);
        this.addComponent(content);
        initializeComponents();
        loadSampleData();
    }

    private void initializeComponents()
    {
        // Filtre par période
        content.addComponent(new Label("Période:", 6, 6));
        String[] periods = {"Ce mois", "3 derniers mois", "Cette année", "Toutes"};
        periodFilter = new ComboBox.List<>(6, 14, 120, periods);
        periodFilter.setChangeListener((oldValue, newValue) -> {
            refreshTransactionList();
            updateFinancialSummary();
        });
        content.addComponent(periodFilter);

        // Liste des transactions
        content.addComponent(new Label("Transactions:", 6, 38));
        transactionList = new ItemList<>(6, 46, 210, 140);
        content.addComponent(transactionList);

        // Formulaire d'ajout de transaction
        int formX = 222;
        content.addComponent(new Label("Nouvelle Transaction:", formX, 6));

        content.addComponent(new Label("Type:", formX, 24));
        Transaction.TransactionType[] types = Transaction.TransactionType.values();
        typeComboBox = new ComboBox.List<>(formX, 32, 130, types);
        content.addComponent(typeComboBox);

        content.addComponent(new Label("Montant (€):", formX, 52));
        amountField = new TextField(formX, 60, 100);
        content.addComponent(amountField);

        content.addComponent(new Label("Description:", formX, 78));
        descriptionField = new TextField(formX, 86, 130);
        content.addComponent(descriptionField);

        content.addComponent(new Label("Catégorie:", formX, 104));
        categoryField = new TextField(formX, 112, 130);
        content.addComponent(categoryField);

        content.addComponent(new Label("Référence:", formX, 130));
        referenceField = new TextField(formX, 138, 130);
        content.addComponent(referenceField);

        content.addComponent(new Label("Date (JJ/MM/AAAA):", formX, 156));
        dateField = new TextField(formX, 164, 100);
        dateField.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        content.addComponent(dateField);

        addTransactionButton = new Button(formX, 186, 90, 18, "Ajouter", Icons.PLUS);
        addTransactionButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                addTransaction();
            }
        });
        content.addComponent(addTransactionButton);

        generateReportButton = new Button(formX + 96, 186, 110, 18, "Rapport", Icons.FILE);
        generateReportButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                generateFinancialReport();
            }
        });
        content.addComponent(generateReportButton);

        // Résumé financier
        content.addComponent(new Label("Résumé Financier:", 6, 208));
        balanceLabel = new Label("Solde: 0.00€", 6, 220);
        content.addComponent(balanceLabel);

        incomeLabel = new Label("Recettes: 0.00€", 6, 232);
        content.addComponent(incomeLabel);

        expenseLabel = new Label("Dépenses: 0.00€", 6, 244);
        content.addComponent(expenseLabel);

        // Label de statut
        statusLabel = new Label("", 6, 226);
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
        transactions.add(new Transaction(UUID.randomUUID().toString(), "15/01/2024", Transaction.TransactionType.SALE, 2500.00, "Vente ordinateurs", "Ventes", "VTE-001"));
        transactions.add(new Transaction(UUID.randomUUID().toString(), "16/01/2024", Transaction.TransactionType.PURCHASE, 1200.00, "Achat stock", "Achats", "ACH-001"));
        transactions.add(new Transaction(UUID.randomUUID().toString(), "20/01/2024", Transaction.TransactionType.SALARY, 3500.00, "Salaires janvier", "Personnel", "SAL-001"));
        transactions.add(new Transaction(UUID.randomUUID().toString(), "25/01/2024", Transaction.TransactionType.EXPENSE, 350.00, "Facture électricité", "Charges", "FAC-001"));
        transactions.add(new Transaction(UUID.randomUUID().toString(), "30/01/2024", Transaction.TransactionType.INCOME, 1800.00, "Prestation service", "Services", "SRV-001"));

        refreshTransactionList();
        updateFinancialSummary();
    }

    private void addTransaction()
    {
        try {
            Transaction.TransactionType type = typeComboBox.getValue();
            String amountText = amountField.getText().trim();
            String description = descriptionField.getText().trim();
            String category = categoryField.getText().trim();
            String reference = referenceField.getText().trim();
            String date = dateField.getText().trim();

            if(description.isEmpty() || amountText.isEmpty()) {
                setStatus("&cVeuillez remplir les champs obligatoires");
                return;
            }

            double amount = Double.parseDouble(amountText);
            
            Transaction newTransaction = new Transaction(
                UUID.randomUUID().toString(),
                date, type, amount, description, category, reference
            );

            transactions.add(newTransaction);
            refreshTransactionList();
            updateFinancialSummary();
            clearForm();
            setStatus("&aTransaction ajoutée avec succès");

        } catch(NumberFormatException e) {
            setStatus("&cMontant invalide");
        }
    }

    private void generateFinancialReport()
    {
        String period = periodFilter.getValue();
        
        double totalIncome = 0;
        double totalExpense = 0;
        int transactionCount = 0;

        StringBuilder report = new StringBuilder();
        report.append("=== RAPPORT FINANCIER - ").append(period.toUpperCase()).append(" ===\n\n");

        // Calcul des totaux et détails par type
        for(Transaction.TransactionType type : Transaction.TransactionType.values()) {
            double typeTotal = 0;
            int typeCount = 0;
            
            for(Transaction transaction : getFilteredTransactions()) {
                if(transaction.getType() == type) {
                    typeTotal += transaction.getAmount();
                    typeCount++;
                    transactionCount++;
                    
                    if(transaction.isIncome()) {
                        totalIncome += transaction.getAmount();
                    } else {
                        totalExpense += transaction.getAmount();
                    }
                }
            }
            
            if(typeCount > 0) {
                report.append(type.getDisplayName()).append(": ")
                      .append(typeCount).append(" transaction(s) - ")
                      .append(String.format("%.2f", typeTotal)).append("€\n");
            }
        }

        report.append("\n--- RÉSUMÉ ---\n");
        report.append("Nombre total de transactions: ").append(transactionCount).append("\n");
        report.append("Total recettes: ").append(String.format("%.2f", totalIncome)).append("€\n");
        report.append("Total dépenses: ").append(String.format("%.2f", totalExpense)).append("€\n");
        report.append("Solde: ").append(String.format("%.2f", totalIncome - totalExpense)).append("€\n");

        // Afficher le rapport dans une boîte de dialogue
        Dialog.Message reportDialog = new Dialog.Message(report.toString());
        reportDialog.setTitle("Rapport Financier");
        app.openDialog(reportDialog);
    }

    private void clearForm()
    {
        amountField.setText("");
        descriptionField.setText("");
        categoryField.setText("");
        referenceField.setText("");
        dateField.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
    }

    private void refreshTransactionList()
    {
        transactionList.removeAll();
        for(Transaction transaction : getFilteredTransactions()) {
            transactionList.addItem(transaction);
        }
    }

    private List<Transaction> getFilteredTransactions()
    {
        // Pour simplifier, on retourne toutes les transactions
        // Dans une vraie application, on filtrerait par période
        return transactions;
    }

    private void updateFinancialSummary()
    {
        double totalIncome = 0;
        double totalExpense = 0;

        for(Transaction transaction : getFilteredTransactions()) {
            if(transaction.isIncome()) {
                totalIncome += transaction.getAmount();
            } else {
                totalExpense += transaction.getAmount();
            }
        }

        double balance = totalIncome - totalExpense;

        balanceLabel.setText("Solde: " + String.format("%.2f", balance) + "€");
        incomeLabel.setText("Recettes: " + String.format("%.2f", totalIncome) + "€");
        expenseLabel.setText("Dépenses: " + String.format("%.2f", totalExpense) + "€");

        // Colorer le solde selon sa valeur
        if(balance > 0) {
            balanceLabel.setText("&asolde: " + String.format("%.2f", balance) + "€");
        } else if(balance < 0) {
            balanceLabel.setText("&csolde: " + String.format("%.2f", balance) + "€");
        }
    }

    private void setStatus(String message)
    {
        statusLabel.setText(message);
    }
}
