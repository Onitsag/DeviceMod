package fr.onitsag.faritech.programs.business.layout;

import fr.onitsag.faritech.api.app.Dialog;
import fr.onitsag.faritech.api.app.Icons;
import fr.onitsag.faritech.api.app.ScrollableLayout;
import fr.onitsag.faritech.api.app.component.*;
import fr.onitsag.faritech.programs.business.ApplicationBusinessManager;
import fr.onitsag.faritech.programs.business.data.InventoryItem;
import fr.onitsag.faritech.programs.system.layout.StandardLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Layout pour la gestion du stock
 */
public class InventoryLayout extends StandardLayout
{
    private ApplicationBusinessManager app;
    private ScrollableLayout content;
    private ItemList<InventoryItem> inventoryList;
    private TextField nameField;
    private TextField categoryField;
    private TextField quantityField;
    private TextField priceField;
    private TextField costField;
    private TextField supplierField;
    private TextField minStockField;
    private Button addButton;
    private Button editButton;
    private Button deleteButton;
    private Button adjustStockButton;
    private Label totalValueLabel;
    private Label lowStockLabel;
    private Label statusLabel;
    private ComboBox<String> filterComboBox;

    private List<InventoryItem> inventory;

    public InventoryLayout(ApplicationBusinessManager app)
    {
        super("Gestion du Stock", 362, 240, app, null);
        this.app = app;
        this.inventory = new ArrayList<>();
        this.content = new ScrollableLayout(0, 21, 362, 380, 143);
        this.addComponent(content);
        initializeComponents();
        loadSampleData();
    }

    private void initializeComponents()
    {
        // Filtre par catégorie
        content.addComponent(new Label("Filtrer par catégorie:", 6, 6));
        String[] categories = {"Toutes les catégories", "Électronique", "Mobilier", "Fournitures", "Outillage"};
        filterComboBox = new ComboBox.List<>(6, 14, 150, categories);
        filterComboBox.setChangeListener((oldValue, newValue) -> refreshInventoryList());
        content.addComponent(filterComboBox);

        // Liste du stock
        inventoryList = new ItemList<>(6, 38, 160, 150);
        inventoryList.setItemClickListener((item, index, mouseButton) -> {
            if(mouseButton == 0 && item != null) {
                selectItem(item);
            }
        });
        content.addComponent(inventoryList);

        // Formulaire d'ajout/modification
        int formX = 176;
        content.addComponent(new Label("Nom de l'article:", formX, 6));
        nameField = new TextField(formX, 14, 176);
        content.addComponent(nameField);

        content.addComponent(new Label("Catégorie:", formX, 34));
        categoryField = new TextField(formX, 42, 176);
        content.addComponent(categoryField);

        content.addComponent(new Label("Quantité:", formX, 62));
        quantityField = new TextField(formX, 70, 80);
        content.addComponent(quantityField);

        content.addComponent(new Label("Prix unitaire (€):", formX, 90));
        priceField = new TextField(formX, 98, 80);
        content.addComponent(priceField);

        content.addComponent(new Label("Coût d'achat (€):", formX, 118));
        costField = new TextField(formX, 126, 80);
        content.addComponent(costField);

        content.addComponent(new Label("Fournisseur:", formX, 146));
        supplierField = new TextField(formX, 154, 176);
        content.addComponent(supplierField);

        content.addComponent(new Label("Stock minimum:", formX, 174));
        minStockField = new TextField(formX, 182, 80);
        content.addComponent(minStockField);

        // Boutons d'action
        addButton = new Button(formX, 210, 70, 18, "Ajouter", Icons.PLUS);
        addButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                addItem();
            }
        });
        content.addComponent(addButton);

        editButton = new Button(formX + 76, 210, 70, 18, "Modifier", Icons.EDIT);
        editButton.setEnabled(false);
        editButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                editItem();
            }
        });
        content.addComponent(editButton);

        deleteButton = new Button(formX + 152, 210, 80, 18, "Supprimer", Icons.TRASH);
        deleteButton.setEnabled(false);
        deleteButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                deleteItem();
            }
        });
        content.addComponent(deleteButton);

        adjustStockButton = new Button(formX, 230, 120, 18, "Ajuster Stock", Icons.EDIT);
        adjustStockButton.setEnabled(false);
        adjustStockButton.setClickListener((mouseX, mouseY, mouseButton) -> {
            if(mouseButton == 0) {
                adjustStock();
            }
        });
        content.addComponent(adjustStockButton);

        // Informations sur le stock
        totalValueLabel = new Label("Valeur totale du stock: 0.00€", 6, 210);
        content.addComponent(totalValueLabel);

        lowStockLabel = new Label("Articles en rupture: 0", 6, 222);
        content.addComponent(lowStockLabel);

        // Label de statut
        statusLabel = new Label("", 6, 234);
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
        inventory.add(new InventoryItem(UUID.randomUUID().toString(), "Ordinateur portable", "Électronique", 15, 899.99, 650.00, "TechSupplier", 5));
        inventory.add(new InventoryItem(UUID.randomUUID().toString(), "Chaise de bureau", "Mobilier", 8, 149.99, 89.50, "OfficeStuff", 3));
        inventory.add(new InventoryItem(UUID.randomUUID().toString(), "Ramette papier A4", "Fournitures", 25, 4.99, 2.50, "PaperCorp", 10));
        inventory.add(new InventoryItem(UUID.randomUUID().toString(), "Perceuse sans fil", "Outillage", 3, 89.99, 45.00, "ToolMaster", 2));
        inventory.add(new InventoryItem(UUID.randomUUID().toString(), "Écran 24 pouces", "Électronique", 12, 299.99, 180.00, "TechSupplier", 4));

        refreshInventoryList();
        updateStockInfo();
    }

    private void selectItem(InventoryItem item)
    {
        nameField.setText(item.getName());
        categoryField.setText(item.getCategory());
        quantityField.setText(String.valueOf(item.getQuantity()));
        priceField.setText(String.valueOf(item.getUnitPrice()));
        costField.setText(String.valueOf(item.getCost()));
        supplierField.setText(item.getSupplier());
        minStockField.setText(String.valueOf(item.getMinStock()));

        editButton.setEnabled(true);
        deleteButton.setEnabled(true);
        adjustStockButton.setEnabled(true);
        addButton.setText("Ajouter");
    }

    private void addItem()
    {
        try {
            String name = nameField.getText().trim();
            String category = categoryField.getText().trim();
            String quantityText = quantityField.getText().trim();
            String priceText = priceField.getText().trim();
            String costText = costField.getText().trim();
            String supplier = supplierField.getText().trim();
            String minStockText = minStockField.getText().trim();

            if(name.isEmpty() || category.isEmpty()) {
                setStatus("&cVeuillez remplir tous les champs obligatoires");
                return;
            }

            int quantity = Integer.parseInt(quantityText);
            double price = Double.parseDouble(priceText);
            double cost = Double.parseDouble(costText);
            int minStock = Integer.parseInt(minStockText);

            InventoryItem newItem = new InventoryItem(
                UUID.randomUUID().toString(),
                name, category, quantity, price, cost, supplier, minStock
            );

            inventory.add(newItem);
            refreshInventoryList();
            updateStockInfo();
            clearForm();
            setStatus("&aArticle ajouté avec succès");

        } catch(NumberFormatException e) {
            setStatus("&cVeuillez entrer des valeurs numériques valides");
        }
    }

    private void editItem()
    {
        InventoryItem selected = inventoryList.getSelectedItem();
        if(selected == null) return;

        try {
            selected.setName(nameField.getText().trim());
            selected.setCategory(categoryField.getText().trim());
            selected.setQuantity(Integer.parseInt(quantityField.getText().trim()));
            selected.setUnitPrice(Double.parseDouble(priceField.getText().trim()));
            selected.setCost(Double.parseDouble(costField.getText().trim()));
            selected.setSupplier(supplierField.getText().trim());
            selected.setMinStock(Integer.parseInt(minStockField.getText().trim()));

            refreshInventoryList();
            updateStockInfo();
            setStatus("&aArticle modifié avec succès");

        } catch(NumberFormatException e) {
            setStatus("&cVeuillez entrer des valeurs numériques valides");
        }
    }

    private void deleteItem()
    {
        InventoryItem selected = inventoryList.getSelectedItem();
        if(selected == null) return;

        Dialog.Confirmation dialog = new Dialog.Confirmation("Êtes-vous sûr de vouloir supprimer cet article ?");
        dialog.setPositiveListener((mouseX, mouseY, mouseButton) -> {
            inventory.remove(selected);
            refreshInventoryList();
            updateStockInfo();
            clearForm();
            setStatus("&aArticle supprimé");
        });
        app.openDialog(dialog);
    }

    private void adjustStock()
    {
        InventoryItem selected = inventoryList.getSelectedItem();
        if(selected == null) return;

        Dialog.Input dialog = new Dialog.Input("Entrez la nouvelle quantité:");
        dialog.setTitle("Ajustement de stock");
        dialog.setInputText(String.valueOf(selected.getQuantity()));
        dialog.setResponseHandler((success, response) -> {
            if(success) {
                try {
                    int newQuantity = Integer.parseInt(response);
                    selected.setQuantity(newQuantity);
                    refreshInventoryList();
                    updateStockInfo();
                    quantityField.setText(String.valueOf(newQuantity));
                    setStatus("&aStock ajusté pour " + selected.getName());
                } catch(NumberFormatException e) {
                    setStatus("&cQuantité invalide");
                }
            }
            return true; // Fermer le dialogue
        });
        app.openDialog(dialog);
    }

    private void clearForm()
    {
        nameField.setText("");
        categoryField.setText("");
        quantityField.setText("");
        priceField.setText("");
        costField.setText("");
        supplierField.setText("");
        minStockField.setText("");
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        adjustStockButton.setEnabled(false);
    }

    private void refreshInventoryList()
    {
        inventoryList.removeAll();
        String selectedFilter = filterComboBox.getValue();
        
        for(InventoryItem item : inventory) {
            if("Toutes les catégories".equals(selectedFilter) || item.getCategory().equals(selectedFilter)) {
                inventoryList.addItem(item);
            }
        }
    }

    private void updateStockInfo()
    {
        double totalValue = 0;
        int lowStockCount = 0;

        for(InventoryItem item : inventory) {
            totalValue += item.getTotalValue();
            if(item.isLowStock()) {
                lowStockCount++;
            }
        }

        totalValueLabel.setText("Valeur totale du stock: " + String.format("%.2f", totalValue) + "€");
        
        String lowStockText = "Articles en rupture: " + lowStockCount;
        if(lowStockCount > 0) {
            lowStockText = "&c" + lowStockText;
        }
        lowStockLabel.setText(lowStockText);
    }

    private void setStatus(String message)
    {
        statusLabel.setText(message);
    }
}
