package fr.onitsag.faritech.programs.business.ui;

import fr.onitsag.faritech.api.app.Dialog;
import fr.onitsag.faritech.api.app.Icons;
import fr.onitsag.faritech.api.app.Layout;
import fr.onitsag.faritech.api.app.ScrollableLayout;
import fr.onitsag.faritech.api.app.component.*;
import fr.onitsag.faritech.api.app.renderer.ListItemRenderer;
import fr.onitsag.faritech.programs.business.ApplicationBusinessManager;
import fr.onitsag.faritech.programs.business.model.*;
import fr.onitsag.faritech.programs.business.service.BusinessRepository;
import fr.onitsag.faritech.programs.business.task.TaskBusinessAction;
import fr.onitsag.faritech.programs.business.data.BusinessData;
import fr.onitsag.faritech.api.task.TaskManager;
import fr.onitsag.faritech.economy.EconomyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Comparator;

public class CompanyLayout extends Layout
{
    public enum Tab { TRANSACTIONS, EMPLOYEES, GRADES }

    private final ApplicationBusinessManager app;
    private final BusinessRepository repo;
    private final String companyId;

    private Tab current = Tab.TRANSACTIONS;
    private boolean requestedVaultBalance = false;

    // Référence persistante pour mettre à jour le label de solde
    private Label personalBalanceLabelRef;
    private boolean balanceRequestInFlight = false;
    private long lastBalanceRequestMs = 0L;
    private static int paginationPage = 0;

    public CompanyLayout(ApplicationBusinessManager app, BusinessRepository repo, String companyId)
    {
        super(362, 164);
        this.app = app;
        this.repo = repo;
        this.companyId = companyId;
    }
    
    public String getCompanyId()
    {
        return companyId;
    }

    @Override
    public void init()
    {
        build();
    }

    public void refresh()
    {
        build();
    }

    private void build()
    {
        clear();
        // Fermer tout contexte ouvert (ex: ComboBox) pour éviter un overlay qui capte les clics
        try { fr.onitsag.faritech.core.Laptop.getSystem().closeContext(); } catch (Throwable ignore) {}
        
        // Header compact avec onglets
        Button bTx = new Button(6, 6, 74, 14, "Transactions");
        Button bEm = new Button(82, 6, 60, 14, "Employés"); 
        Button bGr = new Button(144, 6, 60, 14, "Grades");
        
        bTx.setEnabled(current != Tab.TRANSACTIONS);
        bEm.setEnabled(current != Tab.EMPLOYEES);
        bGr.setEnabled(current != Tab.GRADES);
        
        bTx.setClickListener((mx,my,mb)->{ if(mb==0){ try { fr.onitsag.faritech.core.Laptop.getSystem().closeContext(); } catch (Throwable ignore) {} Minecraft.getMinecraft().addScheduledTask(() -> { current=Tab.TRANSACTIONS; build(); }); }});
        bEm.setClickListener((mx,my,mb)->{ if(mb==0){ try { fr.onitsag.faritech.core.Laptop.getSystem().closeContext(); } catch (Throwable ignore) {} Minecraft.getMinecraft().addScheduledTask(() -> { current=Tab.EMPLOYEES; build(); }); }});
        bGr.setClickListener((mx,my,mb)->{ if(mb==0){ try { fr.onitsag.faritech.core.Laptop.getSystem().closeContext(); } catch (Throwable ignore) {} Minecraft.getMinecraft().addScheduledTask(() -> { current=Tab.GRADES; build(); }); }});
        
        addComponent(bTx); 
        addComponent(bEm); 
        addComponent(bGr);

        // Bouton retour compact
        Button back = new Button(304, 6, 52, 14, "Retour", Icons.ARROW_LEFT);
        back.setClickListener((mx,my,mb)->{ if(mb==0) app.returnToMainMenu(); });
        addComponent(back);

        // Contenu selon l'onglet
        switch (current)
        {
            case TRANSACTIONS: buildTransactions(); break;
            case EMPLOYEES: 
            case GRADES: 
            {
                // Conteneur scrollable pour le contenu (sauf transactions)
                ScrollableLayout content = new ScrollableLayout(0, 24, 362, 1, 140);
                addComponent(content);
                if(current == Tab.EMPLOYEES) buildEmployees(content);
                else buildGrades(content);
                break;
            }
        }

        // IMPORTANT: après avoir reconstruit les composants, forcer la mise à jour
        // des positions absolues (xPosition/yPosition) pour que les TextField/ComboBox
        // reçoivent correctement les événements de clic/sélection.
        app.markForLayoutUpdate();
    }

    private void buildTransactions()
    {
        Company c = repo.getCompany(companyId).orElse(null);
        if(c == null) return;

        // Créer une zone scrollable pour tout le contenu
        // Largeur intérieure égale à la largeur de la fenêtre, hauteur initiale = 0 (s'ajuste à la fin)
        ScrollableLayout mainContent = new ScrollableLayout(0, 24, 362, 1, 140);
        addComponent(mainContent);
        
        int y = 0;
        
        // ============ SECTION SOLDES ============
        mainContent.addComponent(new Label("💰 SOLDES", 10, y));
        y += 16;
        
        // Solde de l'entreprise
        mainContent.addComponent(new Label("Entreprise: " + String.format("%.2f", c.getBalance()) + "€", 20, y));
        y += 12;
        
        // Solde personnel du joueur
        personalBalanceLabelRef = new Label("Personnel: chargement...", 20, y);
        mainContent.addComponent(personalBalanceLabelRef);
        // Requête serveur pour récupérer le solde Vault réel (anti-spam: max 1/5s)
        long now = System.currentTimeMillis();
        if (!balanceRequestInFlight && now - lastBalanceRequestMs > 5000) {
            balanceRequestInFlight = true;
            lastBalanceRequestMs = now;
            TaskManager.sendTask(new TaskBusinessAction().op("get_vault_balance", new NBTTagCompound()).setCallback((nbt, success) -> {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    try {
                        if(success && nbt != null && nbt.hasKey("balance")) {
                            double bal = nbt.getDouble("balance");
                            if (personalBalanceLabelRef != null) {
                                personalBalanceLabelRef.setText("Personnel: " + String.format("%.2f€", bal));
                            }
                            System.out.println("[CompanyLayout] Solde personnel mis à jour: " + bal + "€");
                        } else {
                            if (personalBalanceLabelRef != null) {
                                personalBalanceLabelRef.setText("Personnel: erreur (Vault indisponible)");
                            }
                            System.err.println("[CompanyLayout] Échec de récupération du solde - success:" + success + " nbt:" + nbt);
                        }
                    } catch (Exception e) {
                        if (personalBalanceLabelRef != null) {
                            personalBalanceLabelRef.setText("Personnel: erreur de traitement");
                        }
                        System.err.println("[CompanyLayout] Exception dans callback: " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        balanceRequestInFlight = false;
                        app.markForLayoutUpdate();
                    }
                });
            }));
        }
        y += 20;
        
        // ============ SECTION TRANSFERTS PERSONNELS ============
        mainContent.addComponent(new Label("👤 TRANSFERTS PERSONNELS", 10, y));
        y += 16;
        
        // Description
        mainContent.addComponent(new Label("Entre votre compte personnel et l'entreprise", 20, y));
        y += 16;
        
        // Champ montant
        TextField personalAmount = new TextField(20, y, 100);
        personalAmount.setText("100.00");
        personalAmount.setPlaceholder("Montant en €");
        mainContent.addComponent(personalAmount);
        
        // Boutons côte à côte (envoi via tâches serveur)
        Button depositBtn = new Button(125, y, 80, 18, "Déposer", Icons.ARROW_DOWN);
        depositBtn.setClickListener((mx,my,mb) -> {
            if(mb == 0) {
                String amountStr = personalAmount.getText().trim();
                try {
                    double amount = Double.parseDouble(amountStr);
                    if(amount <= 0) { app.openDialog(new Dialog.Message("Montant invalide")); return; }
                    NBTTagCompound d = new NBTTagCompound();
                    d.setString("companyId", companyId);
                    d.setDouble("amount", amount);
                    TaskManager.sendTask(new TaskBusinessAction().op("player_deposit_to_company", d).setCallback((nbt, success) -> {
                        if(success) {
                            app.openDialog(new Dialog.Message("Dépôt effectué"));
                            Minecraft.getMinecraft().addScheduledTask(this::build);
                        } else {
                            app.openDialog(new Dialog.Message(TextFormatting.RED + "Échec du dépôt (solde insuffisant ?)"));
                        }
                    }));
                } catch (NumberFormatException e) {
                    app.openDialog(new Dialog.Message("Montant invalide"));
                }
            }
        });
        mainContent.addComponent(depositBtn);
        
        Button withdrawBtn = new Button(210, y, 80, 18, "Retirer", Icons.ARROW_UP);
        withdrawBtn.setClickListener((mx,my,mb) -> {
            if(mb == 0) {
                String amountStr = personalAmount.getText().trim();
                try {
                    double amount = Double.parseDouble(amountStr);
                    if(amount <= 0) { app.openDialog(new Dialog.Message("Montant invalide")); return; }
                    NBTTagCompound d = new NBTTagCompound();
                    d.setString("companyId", companyId);
                    d.setDouble("amount", amount);
                    TaskManager.sendTask(new TaskBusinessAction().op("company_withdraw_to_player", d).setCallback((nbt, success) -> {
                        if(success) {
                            app.openDialog(new Dialog.Message("Retrait effectué"));
                            Minecraft.getMinecraft().addScheduledTask(this::build);
                        } else {
                            app.openDialog(new Dialog.Message(TextFormatting.RED + "Échec du retrait (fonds insuffisants ?)"));
                        }
                    }));
                } catch (NumberFormatException e) {
                    app.openDialog(new Dialog.Message("Montant invalide"));
                }
            }
        });
        mainContent.addComponent(withdrawBtn);
        y += 30;
        
        // ============ SECTION VIREMENTS INTER-ENTREPRISES ============
        mainContent.addComponent(new Label("🏢 VIREMENTS INTER-ENTREPRISES", 10, y));
        y += 16;
        
        mainContent.addComponent(new Label("Transférer vers une autre entreprise", 20, y));
        y += 16;
        
        // Nom de l'entreprise cible
        mainContent.addComponent(new Label("Entreprise destinataire:", 20, y));
        y += 12;
        TextField targetCompany = new TextField(20, y, 180);
        targetCompany.setPlaceholder("Nom de l'entreprise");
        mainContent.addComponent(targetCompany);
        y += 20;
        
        // Montant et bouton sur même ligne
        mainContent.addComponent(new Label("Montant:", 20, y));
        TextField transferAmount = new TextField(70, y, 80);
        transferAmount.setText("0.00");
        transferAmount.setPlaceholder("€");
        mainContent.addComponent(transferAmount);
        
        Button transferBtn = new Button(155, y, 80, 18, "Virer", Icons.CASH);
        transferBtn.setClickListener((mx,my,mb) -> {
            if(mb == 0) {
                handleTransfer(c, targetCompany.getText().trim(), transferAmount.getText().trim());
            }
        });
        mainContent.addComponent(transferBtn);
        y += 30;
        
        // ============ SECTION TRANSFERTS VERS JOUEURS ============
        mainContent.addComponent(new Label("👥 TRANSFERTS VERS JOUEURS", 10, y));
        y += 16;
        
        mainContent.addComponent(new Label("Verser de l'argent directement à un joueur", 20, y));
        y += 16;
        
        // Nom du joueur
        mainContent.addComponent(new Label("Nom du joueur:", 20, y));
        y += 12;
        TextField targetPlayer = new TextField(20, y, 180);
        targetPlayer.setPlaceholder("Nom du joueur");
        mainContent.addComponent(targetPlayer);
        y += 20;
        
        // Montant et bouton pour joueur
        mainContent.addComponent(new Label("Montant:", 20, y));
        TextField playerAmount = new TextField(70, y, 80);
        playerAmount.setText("0.00");
        playerAmount.setPlaceholder("€");
        mainContent.addComponent(playerAmount);
        
        Button payPlayerBtn = new Button(155, y, 80, 18, "Payer", Icons.COIN);
        payPlayerBtn.setClickListener((mx,my,mb) -> {
            if(mb == 0) {
                String name = targetPlayer.getText().trim();
                String amountStr = playerAmount.getText().trim();
                try {
                    if(name.isEmpty()) { app.openDialog(new Dialog.Message("Nom du joueur requis")); return; }
                    double amount = Double.parseDouble(amountStr);
                    if(amount <= 0) { app.openDialog(new Dialog.Message("Montant invalide")); return; }
                    Dialog.Confirmation confirm = new Dialog.Confirmation("Payer " + name + " " + EconomyManager.formatMoney(amount) + " depuis l'entreprise ?");
                    confirm.setPositiveListener((mx2,my2,mb2) -> {
                        NBTTagCompound d = new NBTTagCompound();
                        d.setString("companyId", companyId);
                        d.setString("targetName", name);
                        d.setDouble("amount", amount);
                        TaskManager.sendTask(new TaskBusinessAction().op("company_pay_player", d).setCallback((nbt, success) -> {
                            if(success) {
                                app.openDialog(new Dialog.Message("Paiement effectué"));
                                Minecraft.getMinecraft().addScheduledTask(this::build);
                            } else {
                                app.openDialog(new Dialog.Message(TextFormatting.RED + "Échec du paiement (joueur hors ligne ? fonds insuffisants ?)"));
                            }
                        }));
                    });
                    app.openDialog(confirm);
                } catch (NumberFormatException e) {
                    app.openDialog(new Dialog.Message("Montant invalide"));
                }
            }
        });
        mainContent.addComponent(payPlayerBtn);
        y += 30;
        
        // ============ SECTION HISTORIQUE AVEC PAGINATION ============
        mainContent.addComponent(new Label("📋 HISTORIQUE DES TRANSACTIONS", 10, y));
        y += 16;

        final int pageSize = 12;
        // Conserver la page dans un champ statique simple par session (option rapide)
        if(paginationPage < 0) paginationPage = 0;
        int total = c.getTransactions().size();
        int totalPages = Math.max(1, (int)Math.ceil(total / (double)pageSize));
        if(paginationPage >= totalPages) paginationPage = totalPages - 1;

        // Boutons pagination
        Button prev = new Button(10, y, 16, 12, "<");
        Button next = new Button(30, y, 16, 12, ">");
        Label pageInfo = new Label((paginationPage+1) + "/" + totalPages, 52, y+2);
        prev.setEnabled(paginationPage > 0);
        next.setEnabled(paginationPage < totalPages - 1);
        prev.setClickListener((mx,my,mb)->{ if(mb==0){ paginationPage--; Minecraft.getMinecraft().addScheduledTask(this::build); }});
        next.setClickListener((mx,my,mb)->{ if(mb==0){ paginationPage++; Minecraft.getMinecraft().addScheduledTask(this::build); }});
        mainContent.addComponent(prev);
        mainContent.addComponent(next);
        mainContent.addComponent(pageInfo);
        y += 16;

        if(total == 0) {
            mainContent.addComponent(new Label("Aucune transaction pour le moment", 20, y));
            y += 16;
        } else {
            int start = total - (paginationPage * pageSize) - 1; // index de départ (du plus récent)
            int end = Math.max(-1, start - pageSize); // exclusif
            for(int i = start; i > end; i--) {
                if(i < 0) break;
                BizTransaction t = c.getTransactions().get(i);
                String typeIcon = getTransactionIcon(t.getType());
                String amount = String.format("%.2f€", t.getAmount());
                String description = t.getDescription();
                if(description.length() > 35) description = description.substring(0, 32) + "...";
                String transactionText = typeIcon + " " + amount + " - " + description;
                Label transactionLabel = new Label(transactionText, 20, y);
                mainContent.addComponent(transactionLabel);
                y += 14;
            }
        }
        
        // Ajuster la hauteur du contenu scrollable (contenu interne)
        mainContent.height = Math.max(y + 24, 200);
    }

    private void buildEmployees(Layout content)
    {
        Company c = repo.getCompany(companyId).orElse(null);
        if(c == null) {
            content.addComponent(new Label("Entreprise introuvable", 10, 10));
            content.height = 30;
            return;
        }
        
        Grade userGrade = currentUserGrade(c);
        boolean canChangeGrade = userGrade != null && userGrade.getPermissions().canChangeEmployeeGrade;
        boolean canFire = userGrade != null && userGrade.getPermissions().canFire;
        boolean canRecruit = userGrade != null && userGrade.getPermissions().canRecruit;

        // GAUCHE : Liste des employés
        content.addComponent(new Label("Employés :", 6, 6));
        
        ItemList<EmployeeRecord> employeeList = new ItemList<>(6, 22, 180, 120);
        employeeList.setItems(c.getEmployees());
        employeeList.setListItemRenderer(new ListItemRenderer<EmployeeRecord>(18) {
            @Override
            public void render(EmployeeRecord emp, Gui gui, Minecraft mc, int x, int y, int width, int height, boolean selected) {
                // Fond de sélection
                if(selected) {
                    Gui.drawRect(x, y, x + width, y + height, 0xFF3F51B5);
                }
                
                // Trouver le grade
                Grade empGrade = c.getGrades().stream()
                    .filter(g -> g.getId().equals(emp.getGradeId()))
                    .findFirst().orElse(null);
                String gradeText = empGrade != null ? empGrade.getName() : "Sans grade";
                
                // Afficher nom + grade
                String text = emp.getPlayerName() + " (" + gradeText + ")";
                mc.fontRenderer.drawString(text, x + 2, y + 4, selected ? 0xFFFFFF : 0x000000);
            }
        });
        
        content.addComponent(employeeList);
        
        // DROITE : Informations et actions
        int rightX = 200;
        content.addComponent(new Label("Actions :", rightX, 6));
        
        // Info sur l'employé sélectionné
        Label selectedEmployeeLabel = new Label("Sélectionnez un employé", rightX, 22);
        content.addComponent(selectedEmployeeLabel);
        
        Label selectedGradeLabel = new Label("", rightX, 34);
        content.addComponent(selectedGradeLabel);
        
        // Boutons d'action
        Button changeGradeBtn = new Button(rightX, 56, 70, 18, "Gérer", Icons.EDIT);
        changeGradeBtn.setEnabled(false);
        changeGradeBtn.setVisible(false);
        content.addComponent(changeGradeBtn);
        
        Button fireBtn = new Button(rightX + 75, 56, 70, 18, "Licencier", Icons.TRASH);
        fireBtn.setEnabled(false);
        fireBtn.setVisible(false);
        content.addComponent(fireBtn);
        
        // Section recrutement (visible uniquement si permission)
        if(canRecruit) {
            content.addComponent(new Label("Recruter :", rightX, 90));
            
            TextField playerField = new TextField(rightX, 106, 120);
            playerField.setPlaceholder("Nom du joueur");
            content.addComponent(playerField);
            
            Grade[] availableGrades = c.getGrades().stream()
                .filter(g -> userGrade != null && g.getLevel() < userGrade.getLevel())
                .toArray(Grade[]::new);
                
            ComboBox.List<Grade> gradeCombo = new ComboBox.List<>(rightX, 124, 120, availableGrades);
            // Pré-sélectionner "Employé"
            for(Grade grade : availableGrades) {
                if(grade.getName().equals("Employé")) {
                    gradeCombo.setSelectedItem(grade);
                    break;
                }
            }
            content.addComponent(gradeCombo);
            
            Button recruitBtn = new Button(rightX, 142, 80, 18, "Recruter", Icons.PLUS);
            recruitBtn.setEnabled(true);
            content.addComponent(recruitBtn);
            
            recruitBtn.setClickListener((mx, my, mb) -> {
                if(mb == 0) {
                    handleAddEmployee(c, playerField, gradeCombo);
                }
            });
        }
        
        // LOGIQUE DE SÉLECTION
        employeeList.setItemClickListener((employee, index, mouseButton) -> {
            if(mouseButton == 0) {
                if(employee != null) {
                    // Mettre à jour les infos affichées
                    Grade empGrade = c.getGrades().stream()
                        .filter(g -> g.getId().equals(employee.getGradeId()))
                        .findFirst().orElse(null);
                        
                    selectedEmployeeLabel.setText("Employé: " + employee.getPlayerName());
                    selectedGradeLabel.setText("Grade: " + (empGrade != null ? empGrade.getName() : "Sans grade"));
                    
                    // Activer/afficher les boutons selon les permissions et la hiérarchie
                    boolean canManageThisEmployee = userGrade != null && 
                        ((empGrade == null && userGrade.getLevel() > 0) || 
                         (empGrade != null && empGrade.getLevel() < userGrade.getLevel()));
                         
                    boolean showChange = canChangeGrade && canManageThisEmployee;
                    boolean showFire = canFire && canManageThisEmployee;
                    
                    changeGradeBtn.setEnabled(showChange);
                    changeGradeBtn.setVisible(showChange);
                    fireBtn.setEnabled(showFire);
                    fireBtn.setVisible(showFire);
                } else {
                    // Aucune sélection : masquer les actions
                    changeGradeBtn.setEnabled(false);
                    changeGradeBtn.setVisible(false);
                    fireBtn.setEnabled(false);
                    fireBtn.setVisible(false);
                }
            }
        });
        
        // ACTIONS DES BOUTONS
        changeGradeBtn.setClickListener((mx, my, mb) -> {
            if(mb == 0) {
                EmployeeRecord selectedEmployee = employeeList.getSelectedItem();
                if(selectedEmployee != null) {
                    Grade empGrade = c.getGrades().stream()
                        .filter(g -> g.getId().equals(selectedEmployee.getGradeId()))
                        .findFirst().orElse(null);
                    openEmployeeManagementDialog(selectedEmployee, empGrade, c);
                }
            }
        });
        
        fireBtn.setClickListener((mx, my, mb) -> {
            if(mb == 0) {
                EmployeeRecord selectedEmployee = employeeList.getSelectedItem();
                if(selectedEmployee != null) {
                    confirmFireEmployee(selectedEmployee);
                }
            }
        });
        
        // (Gestion du click du bouton recruter déplacée dans le bloc conditionnel)
        
        content.height = 170;
    }

    private void buildGrades(Layout content)
    {
        Company c = repo.getCompany(companyId).orElse(null);
        if(c == null) return;
        
        Grade userGrade = currentUserGrade(c);
        boolean canManageGrades = userGrade != null && userGrade.getPermissions().canManageGrades;
        int userLevel = userGrade != null ? userGrade.getLevel() : -1;
        
        // Liste des grades triés par niveau décroissant (hiérarchie)
        content.addComponent(new Label("Hiérarchie des grades :", 10, 0));
        content.addComponent(new Label("(Du plus élevé au plus bas)", 10, 10));
        
        int y = 24;
        Grade[] grades = c.getGrades().stream()
            .sorted(Comparator.comparingInt(Grade::getLevel).reversed())
            .toArray(Grade[]::new);
            
        for(int i = 0; i < grades.length; i++) {
            Grade grade = grades[i];
            boolean canModifyThisGrade = canManageGrades && grade.getLevel() < userLevel;
            
            // Bouton modifier en premier
            if(canModifyThisGrade) {
                Button modifyBtn = new Button(12, y, 60, 16, "Modifier", Icons.EDIT);
                modifyBtn.setClickListener((mx,my,mb)->{
                    if(mb==0) openGradeEditDialog(c, grade);
                });
                content.addComponent(modifyBtn);
                
                
                // Icône poubelle pour supprimer
                Button deleteBtn = new Button(78, y, 16, 16, "", Icons.TRASH);
                deleteBtn.setClickListener((mx,my,mb)->{
                    if(mb==0) handleDeleteGradeWithConfirmation(c, grade);
                });
                content.addComponent(deleteBtn);
            }
            
            // Nom et niveau du grade
            int labelX = canModifyThisGrade ? 100 : 12;
            content.addComponent(new Label(grade.getName() + " (Niveau " + grade.getLevel() + ")", labelX, y + 2));
            
            // Afficher les permissions sous le nom
            Permissions p = grade.getPermissions();
            String perms = "";
            if(p.canRecruit) perms += "Recruter ";
            if(p.canManageGrades) perms += "Gérer ";
            if(p.canChangeEmployeeGrade) perms += "Promouvoir ";
            if(p.canFire) perms += "Virer ";
            if(!perms.isEmpty()) {
                content.addComponent(new Label("  → " + perms + "(Max: " + (int)p.transferLimit + "€)", labelX, y + 14));
            }
            
            y += 30;
        }

        // Séparateur visuel
        content.addComponent(new Label("─────────────────────────────────────────────────", 10, y + 5));
        y += 20;

        // Section création de grade (seulement si on a les permissions)
        if(canManageGrades) {
            content.addComponent(new Label("Créer un nouveau grade :", 10, y));
            y += 14;
            
            // Nom du grade
            content.addComponent(new Label("Nom du grade :", 12, y));
            y += 12;
            TextField nameField = new TextField(12, y, 200);
            nameField.setPlaceholder("Ex: Manager, Assistant...");
            content.addComponent(nameField);
            y += 22;
        
            // Niveau hiérarchique
            content.addComponent(new Label("Niveau hiérarchique :", 12, y));
            y += 12;
            TextField levelField = new TextField(12, y, 100);
            levelField.setPlaceholder("Ex: " + Math.max(1, userLevel - 10));
            content.addComponent(levelField);
            content.addComponent(new Label("(Doit être < " + userLevel + ")", 120, y + 2));
            y += 22;
            
            // Limite de transfert
            content.addComponent(new Label("Limite de virement (€) :", 12, y));
            y += 12;
            TextField limitField = new TextField(12, y, 120);
            limitField.setPlaceholder("Ex: 10000");
            content.addComponent(limitField);
            y += 22;
            
            // Permissions avec descriptions
            content.addComponent(new Label("Permissions :", 12, y));
            y += 12;
            
            CheckBox pRecruit = new CheckBox("Peut recruter des employés", 12, y);
            content.addComponent(pRecruit); 
            y += 16;
            
            CheckBox pManage = new CheckBox("Peut gérer les grades", 12, y);
            content.addComponent(pManage); 
            y += 16;
            
            CheckBox pChange = new CheckBox("Peut promouvoir/rétrograder", 12, y);
            content.addComponent(pChange); 
            y += 16;
            
            CheckBox pFire = new CheckBox("Peut licencier des employés", 12, y);
            content.addComponent(pFire);
            y += 20;

            // Bouton de création
            Button createBtn = new Button(12, y, 100, 18, "Créer le grade", Icons.PLUS);
        createBtn.setClickListener((mx,my,mb)->{
            if(mb==0) {
                    handleCreateGradeWithValidation(c, nameField, levelField, limitField, pRecruit, pManage, pChange, pFire, userLevel);
            }
        });
        content.addComponent(createBtn);

            y += 25;
        } else {
            content.addComponent(new Label("Vous n'avez pas les permissions pour gérer les grades.", 10, y));
            y += 20;
        }

        content.height = y;
    }

    // Méthodes d'action simplifiées
    private void handlePersonalDeposit(Company c, String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                app.openDialog(new Dialog.Message("Montant invalide"));
                return;
            }
            
            String playerUuid = repo.getCurrentPlayerUuid();
            
            if (EconomyManager.transferToCompany(playerUuid, companyId, amount)) {
                app.openDialog(new Dialog.Message("Dépôt de " + EconomyManager.formatMoney(amount) + " effectué"));
                Minecraft.getMinecraft().addScheduledTask(this::build);
            } else {
                app.openDialog(new Dialog.Message(TextFormatting.RED + "Solde personnel insuffisant"));
            }
        } catch (NumberFormatException e) {
            app.openDialog(new Dialog.Message("Montant invalide"));
        }
    }
    
    private void handlePersonalWithdraw(Company c, String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                app.openDialog(new Dialog.Message("Montant invalide"));
                return;
            }
            
            // Vérifier les permissions et limites
            Grade userGrade = currentUserGrade(c);
            if (userGrade == null || amount > userGrade.getPermissions().transferLimit) {
                app.openDialog(new Dialog.Message(TextFormatting.RED + "Limite de retrait dépassée (" + EconomyManager.formatMoney(userGrade != null ? userGrade.getPermissions().transferLimit : 0) + ")"));
                return;
            }
            
            if (c.getBalance() < amount) {
                app.openDialog(new Dialog.Message(TextFormatting.RED + "Fonds insuffisants dans l'entreprise"));
                return;
            }
            
            String playerUuid = repo.getCurrentPlayerUuid();
            
            if (EconomyManager.withdrawFromCompany(companyId, playerUuid, amount)) {
                app.openDialog(new Dialog.Message("Retrait de " + EconomyManager.formatMoney(amount) + " effectué"));
                Minecraft.getMinecraft().addScheduledTask(this::build);
            } else {
                app.openDialog(new Dialog.Message(TextFormatting.RED + "Erreur lors du retrait"));
            }
        } catch (NumberFormatException e) {
            app.openDialog(new Dialog.Message("Montant invalide"));
        }
    }

    private void handleTransfer(Company c, String toName, String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            String toId = findCompanyIdByName(toName);
            if(toId == null) {
                app.openDialog(new Dialog.Message("Entreprise introuvable"));
                return;
            }

            Grade playerGrade = currentUserGrade(c);
            if (playerGrade == null || !playerGrade.getPermissions().canChangeEmployeeGrade || amount > playerGrade.getPermissions().transferLimit) {
                app.openDialog(new Dialog.Message(TextFormatting.RED + "Permission insuffisante"));
                return;
            }

            // Envoyer l'opération au serveur
            NBTTagCompound d = new NBTTagCompound();
            d.setString("fromCompanyId", companyId);
            d.setString("toCompanyId", toId);
            d.setDouble("amount", amount);

            TaskManager.sendTask(new TaskBusinessAction().op("transfer", d).setCallback((nbt, success) -> {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if(success) {
                        app.openDialog(new Dialog.Message("Virement effectué vers " + toName));
                        build();
                    } else {
                        app.openDialog(new Dialog.Message(TextFormatting.RED + "Échec du virement (fonds insuffisants ?)"));
                    }
                });
            }));
        } catch (NumberFormatException e) {
            app.openDialog(new Dialog.Message("Montant invalide"));
        }
    }

    private void handlePayPlayer(Company c, String playerName, String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                app.openDialog(new Dialog.Message("Montant invalide"));
                return;
            }
            
            if (playerName.isEmpty()) {
                app.openDialog(new Dialog.Message("Nom du joueur requis"));
                return;
            }
            
            // Vérifier les permissions et limites
            Grade userGrade = currentUserGrade(c);
            if (userGrade == null || amount > userGrade.getPermissions().transferLimit) {
                app.openDialog(new Dialog.Message(TextFormatting.RED + "Limite de virement dépassée (" + EconomyManager.formatMoney(userGrade != null ? userGrade.getPermissions().transferLimit : 0) + ")"));
                return;
            }
            
            if (c.getBalance() < amount) {
                app.openDialog(new Dialog.Message(TextFormatting.RED + "Solde de l'entreprise insuffisant"));
                return;
            }
            
            // Confirmation du paiement
            Dialog.Confirmation confirm = new Dialog.Confirmation(
                "Confirmer le paiement\n\n" +
                "Payer " + EconomyManager.formatMoney(amount) + " à " + playerName + " ?\n" +
                "Cette somme sera déduite des fonds de l'entreprise."
            );
            
            confirm.setPositiveListener((mx, my, mb) -> {
                // Effectuer le paiement via EconomyManager
                if (EconomyManager.depositToPlayer(playerName, amount)) {
                    // Déduire de l'entreprise et enregistrer la transaction
                    c.setBalance(c.getBalance() - amount);
                    c.getTransactions().add(new BizTransaction(
                        java.util.UUID.randomUUID().toString(),
                        System.currentTimeMillis(),
                        companyId,
                        "PLAYER_" + playerName,
                        amount,
                        BizTransaction.Type.PAYMENT,
                        "Paiement à " + playerName
                    ));
                    
                    app.openDialog(new Dialog.Message("Paiement de " + EconomyManager.formatMoney(amount) + " effectué à " + playerName));
                    Minecraft.getMinecraft().addScheduledTask(this::build);
                } else {
                    app.openDialog(new Dialog.Message(TextFormatting.RED + "Erreur lors du paiement (joueur introuvable ou hors ligne ?)"));
                }
            });
            
            app.openDialog(confirm);
            
        } catch (NumberFormatException e) {
            app.openDialog(new Dialog.Message("Montant invalide"));
        }
    }

    private String getTransactionIcon(BizTransaction.Type type) {
        switch (type) {
            case TRANSFER_IN: return "⬅";
            case TRANSFER_OUT: return "➡";
            case PAYMENT: return "💳";
            case DEPOSIT: return "⬇";
            case WITHDRAWAL: return "⬆";
            default: return "•";
        }
    }

    private int getTransactionColor(BizTransaction.Type type) {
        switch (type) {
            case TRANSFER_IN:
            case DEPOSIT:
                return 0x00AA00; // Vert pour les entrées
            case TRANSFER_OUT:
            case PAYMENT:
            case WITHDRAWAL:
                return 0xAA0000; // Rouge pour les sorties
            default:
                return 0x000000; // Noir par défaut
        }
    }

    private void handleAddEmployee(Company c, TextField playerField, ComboBox<Grade> gradeCombo) {
        Grade playerGrade = currentUserGrade(c);
        if (playerGrade == null || !playerGrade.getPermissions().canRecruit) {
            app.openDialog(new Dialog.Message(TextFormatting.RED + "Pas de permission"));
            return;
        }

        String name = playerField.getText().trim();
        if(name.isEmpty()) return;
        
        Grade selectedGrade = (Grade)gradeCombo.getValue();
        if(selectedGrade == null) {
            app.openDialog(new Dialog.Message(TextFormatting.RED + "Sélectionnez un grade"));
            return;
        }
        
        // Vérifier que le grade sélectionné est inférieur ou égal au nôtre
        if(selectedGrade.getLevel() > playerGrade.getLevel()) {
            app.openDialog(new Dialog.Message(TextFormatting.RED + "Vous ne pouvez recruter qu'à un grade inférieur ou égal au vôtre"));
            return;
        }
        
        // Ne pas générer d'UUID côté client, laisser le serveur résoudre
        // Envoyer la tâche au serveur au lieu de modifier directement
        NBTTagCompound d = new NBTTagCompound();
        d.setString("companyId", companyId);
        d.setString("playerUuid", ""); // UUID vide, le serveur résoudra
        d.setString("playerName", name);
        d.setString("gradeId", selectedGrade.getId());
        
        TaskManager.sendTask(new TaskBusinessAction().op("add_employee", d).setCallback((nbt, success) -> {
            if (success) {
                playerField.setText("");
        Minecraft.getMinecraft().addScheduledTask(this::build);
            } else {
                app.openDialog(new Dialog.Message(TextFormatting.RED + "Erreur lors de l'ajout"));
            }
        }));
    }

    private void openEmployeeManagementDialog(EmployeeRecord employee, Grade currentGrade, Company company) {
        Grade userGrade = currentUserGrade(company);
        if(userGrade == null) return;
        
        // Vérifier les permissions
        boolean canChangeGrade = userGrade.getPermissions().canChangeEmployeeGrade;
        boolean canFire = userGrade.getPermissions().canFire;
        
        if(!canChangeGrade && !canFire) {
            app.openDialog(new Dialog.Message("Aucune action disponible pour cet employé"));
            return;
        }
        
        // Créer un dialogue compact avec menu déroulant
        
        if(canChangeGrade) {
            // Menu déroulant pour les grades
            Grade[] availableGrades = company.getGrades().stream()
                .filter(g -> g.getLevel() < userGrade.getLevel())
                .sorted(java.util.Comparator.comparingInt(Grade::getLevel).reversed())
                .toArray(Grade[]::new);
                
            if(availableGrades.length > 0) {
                boolean isFounder = userGrade.getLevel() == 100;
                openGradeSelectionDialogWithFounderOption(employee, currentGrade, availableGrades, canFire, isFounder, company, userGrade);
            } else if(canFire) {
                // Seulement licenciement possible
                confirmFireEmployee(employee);
            } else {
                app.openDialog(new Dialog.Message("Aucun grade modifiable disponible"));
            }
        } else if(canFire) {
            // Seulement licenciement
            confirmFireEmployee(employee);
        }
    }
    
    private void openGradeSelectionDialogWithFounderOption(EmployeeRecord employee, Grade currentGrade, Grade[] availableGrades, boolean canFire, boolean isFounder, Company company, Grade userGrade) {
        // Créer un dialogue personnalisé pour la sélection de grade
        Dialog gradeDialog = new Dialog() {
            @Override
            public void init(@Nullable NBTTagCompound intent) {
                super.init(intent);
                
                // Configuration du dialogue
                setTitle("Gestion d'employé");
                
                // Créer le layout personnalisé
                Layout content = new Layout(300, 140);
                
                String currentGradeText = currentGrade != null ? currentGrade.getName() : "Sans grade";
                
                // Labels
                content.addComponent(new Label("Gestion de " + employee.getPlayerName(), 10, 10));
                content.addComponent(new Label("Grade actuel: " + currentGradeText, 10, 28));
                content.addComponent(new Label("Grade :", 10, 50));
                
                // ComboBox
                ComboBox.List<Grade> gradeCombo = new ComboBox.List<>(70, 48, 140, availableGrades);
                if(currentGrade != null) {
                    gradeCombo.setSelectedItem(currentGrade);
                }
                content.addComponent(gradeCombo);
                
                // Boutons
                Button confirmBtn = new Button(10, 80, 80, 18, "Confirmer", Icons.CHECK);
                Button cancelBtn = new Button(95, 80, 70, 18, "Annuler", Icons.CROSS);
                
                confirmBtn.setClickListener((mx, my, mb) -> {
                    if(mb == 0) {
                        Grade selectedGrade = gradeCombo.getSelectedItem();
                        if(selectedGrade != null && !selectedGrade.equals(currentGrade)) {
                            close();
                            handleChangeEmployeeGrade(employee, selectedGrade);
                        } else if(selectedGrade != null && selectedGrade.equals(currentGrade)) {
                            app.openDialog(new Dialog.Message("Veuillez sélectionner un grade différent"));
                        } else {
                            app.openDialog(new Dialog.Message("Veuillez sélectionner un grade"));
                        }
                    }
                });
                
                cancelBtn.setClickListener((mx, my, mb) -> {
                    if(mb == 0) {
                        close();
                    }
                });
                
                content.addComponent(confirmBtn);
                content.addComponent(cancelBtn);
                
                if(canFire) {
                    Button fireBtn = new Button(170, 80, 70, 18, "Licencier", Icons.TRASH);
                    fireBtn.setClickListener((mx, my, mb) -> {
                        if(mb == 0) {
                            close();
                            confirmFireEmployee(employee);
                        }
                    });
                    content.addComponent(fireBtn);
                    content.width = 250;
                }
                
                // Option spéciale Fondateur (niveau 100): proposer transfert du rôle
                if(isFounder) {
                    boolean targetIsFounder = currentGrade != null && currentGrade.getLevel() == 100;
                    if(!targetIsFounder) {
                        Button makeFounderBtn = new Button(10, 102, 230, 18, "Définir en tant que Fondateur", Icons.STAR_ON);
                        makeFounderBtn.setClickListener((mx, my, mb) -> {
                            if(mb == 0) {
                                close();
                                handleTransferFounder(employee, company);
                            }
                        });
                        content.addComponent(makeFounderBtn);
                        content.height = 140;
                    }
                }
                
                setLayout(content);
            }
        };
        
        app.openDialog(gradeDialog);
    }

    private void handleTransferFounder(EmployeeRecord targetEmployee, Company company) {
        // Trouver le grade Fondateur (niveau 100) et le grade juste en dessous
        Grade founderGrade = company.getGrades().stream()
            .filter(g -> g.getLevel() == 100)
            .findFirst().orElse(null);
        if(founderGrade == null) {
            app.openDialog(new Dialog.Message(TextFormatting.RED + "Grade 'Fondateur' introuvable (niveau 100)"));
            return;
        }

        Grade nextBelow = company.getGrades().stream()
            .filter(g -> g.getLevel() < 100)
            .max(Comparator.comparingInt(Grade::getLevel))
            .orElse(null);
        if(nextBelow == null) {
            app.openDialog(new Dialog.Message(TextFormatting.RED + "Aucun grade disponible sous Fondateur"));
            return;
        }

        // Empêcher le transfert à soi-même
        String currentUserUuid = repo.getCurrentPlayerUuid();
        if(targetEmployee.getPlayerUuid().equals(currentUserUuid)) {
            app.openDialog(new Dialog.Message(TextFormatting.RED + "Vous êtes déjà Fondateur"));
            return;
        }

        // Message de confirmation explicite
        String msg = "Transfert de Fondateur\n\n" +
                "Vous allez attribuer le rôle de Fondateur à:\n" +
                "👤 " + targetEmployee.getPlayerName() + "\n\n" +
                "Il ne peut y avoir qu'un seul Fondateur.\n" +
                "Conséquences:\n" +
                "• Votre grade deviendra: '" + nextBelow.getName() + "'.\n" +
                "Confirmer ?";

        Dialog.Confirmation confirm = new Dialog.Confirmation(msg);
        confirm.setPositiveListener((mx, my, mb) -> {
            // 1) Promouvoir la cible en Fondateur
            NBTTagCompound promote = new NBTTagCompound();
            promote.setString("companyId", companyId);
            promote.setString("playerUuid", targetEmployee.getPlayerUuid());
            promote.setString("gradeId", founderGrade.getId());

            TaskManager.sendTask(new TaskBusinessAction().op("change_grade", promote).setCallback((nbt1, success1) -> {
                if(!success1) {
                    app.openDialog(new Dialog.Message(TextFormatting.RED + "Échec du transfert de Fondateur (promotion)"));
                    return;
                }

                // 2) Se rétrograder au grade juste en dessous
                EmployeeRecord me = company.getEmployees().stream()
                    .filter(e -> e.getPlayerUuid().equals(currentUserUuid))
                    .findFirst().orElse(null);
                if(me == null) {
                    app.openDialog(new Dialog.Message(TextFormatting.RED + "Votre fiche employé est introuvable"));
                    return;
                }

                NBTTagCompound demote = new NBTTagCompound();
                demote.setString("companyId", companyId);
                demote.setString("playerUuid", me.getPlayerUuid());
                demote.setString("gradeId", nextBelow.getId());

                TaskManager.sendTask(new TaskBusinessAction().op("change_grade", demote).setCallback((nbt2, success2) -> {
                    if(success2) {
                        app.openDialog(new Dialog.Message("Le rôle de Fondateur a été transféré à " + targetEmployee.getPlayerName() + "."));
                        Minecraft.getMinecraft().addScheduledTask(this::build);
                    } else {
                        app.openDialog(new Dialog.Message(TextFormatting.RED + "Promotion réussie mais rétrogradation impossible"));
                    }
                }));
            }));
        });
        app.openDialog(confirm);
    }
    
    private void handleChangeEmployeeGrade(EmployeeRecord employee, Grade newGrade) {
        // Envoyer la tâche au serveur
        NBTTagCompound d = new NBTTagCompound();
        d.setString("companyId", companyId);
        d.setString("playerUuid", ""); // UUID vide, le serveur résoudra par nom
        d.setString("playerName", employee.getPlayerName());
        d.setString("gradeId", newGrade.getId());
        
        TaskManager.sendTask(new TaskBusinessAction().op("change_grade", d).setCallback((nbt, success) -> {
            if (success) {
                app.openDialog(new Dialog.Message("Grade de " + employee.getPlayerName() + " changé vers " + newGrade.getName()));
                Minecraft.getMinecraft().addScheduledTask(this::build);
            } else {
                app.openDialog(new Dialog.Message(TextFormatting.RED + "Erreur lors du changement de grade"));
            }
        }));
    }
    
    private void confirmFireEmployee(EmployeeRecord employee) {
        Company c = repo.getCompany(companyId).orElse(null);
        Grade empGrade = null;
        if(c != null) {
            empGrade = c.getGrades().stream().filter(g -> g.getId().equals(employee.getGradeId())).findFirst().orElse(null);
        }
        String gradeText = empGrade != null ? empGrade.getName() : "Sans grade";

        String message = "CONFIRMATION DE LICENCIEMENT\n\n" +
                "Employé: " + employee.getPlayerName() + "\n" +
                "Grade actuel: " + gradeText + "\n\n" +
                "Voulez-vous vraiment licencier cet employé ?\n";
        
        Dialog.Confirmation dialog = new Dialog.Confirmation(message);
        dialog.setPositiveListener((mouseX, mouseY, mouseButton) -> {
            // Envoyer la tâche de licenciement au serveur
            NBTTagCompound d = new NBTTagCompound();
            d.setString("companyId", companyId);
            d.setString("playerUuid", ""); // UUID vide, le serveur résoudra par nom
            d.setString("playerName", employee.getPlayerName());
            
            TaskManager.sendTask(new TaskBusinessAction().op("fire_employee", d).setCallback((nbt, success) -> {
                if (success) {
                    app.openDialog(new Dialog.Message("✅ " + employee.getPlayerName() + " a été licencié avec succès"));
                    Minecraft.getMinecraft().addScheduledTask(this::build);
                } else {
                    app.openDialog(new Dialog.Message(TextFormatting.RED + "❌ Erreur lors du licenciement"));
                }
            }));
        });
        app.openDialog(dialog);
    }



    private void handleCreateGradeWithValidation(Company c, TextField nameField, TextField levelField, TextField limitField, 
                                             CheckBox pRecruit, CheckBox pManage, CheckBox pChange, CheckBox pFire, int userLevel) {
        try {
            String name = nameField.getText().trim();
            if(name.isEmpty()) {
                app.openDialog(new Dialog.Message("Le nom du grade est requis"));
            return;
        }
            
            int level = Integer.parseInt(levelField.getText().trim());
            double limit = Double.parseDouble(limitField.getText().trim());
            
            // Vérifier que le niveau est inférieur à celui de l'utilisateur
            if(level >= userLevel) {
                app.openDialog(new Dialog.Message(TextFormatting.RED + "Le niveau doit être inférieur à " + userLevel));
                return;
            }
            
            // Vérifier que le nom n'existe pas déjà
            boolean nameExists = c.getGrades().stream().anyMatch(g -> g.getName().equalsIgnoreCase(name));
            if(nameExists) {
                app.openDialog(new Dialog.Message(TextFormatting.RED + "Un grade avec ce nom existe déjà"));
                return;
            }
            
            // Envoyer la tâche au serveur au lieu de modifier directement
            NBTTagCompound d = new NBTTagCompound();
            d.setString("companyId", companyId);
            d.setString("name", name);
            d.setInteger("level", level);
            d.setDouble("limit", limit);
            d.setBoolean("recruit", pRecruit.isSelected());
            d.setBoolean("manage", pManage.isSelected());
            d.setBoolean("change", pChange.isSelected());
            d.setBoolean("fire", pFire.isSelected());
            
            TaskManager.sendTask(new TaskBusinessAction().op("add_grade", d).setCallback((nbt, success) -> {
                if (success) {
                    // Vider les champs après création
                    nameField.setText("");
                    levelField.setText("");
                    limitField.setText("");
                    pRecruit.setSelected(false);
                    pManage.setSelected(false);
                    pChange.setSelected(false);
                    pFire.setSelected(false);
                    
            Minecraft.getMinecraft().addScheduledTask(this::build);
                } else {
                    app.openDialog(new Dialog.Message(TextFormatting.RED + "Erreur lors de la création du grade"));
                }
            }));
        } catch (NumberFormatException e) {
            app.openDialog(new Dialog.Message("Niveau et limite doivent être des nombres valides"));
        }
    }


    private void handleDeleteGradeWithConfirmation(Company c, Grade gradeToDelete) {
        // Compter les employés ayant ce grade
        long employeeCount = c.getEmployees().stream()
            .filter(emp -> emp.getGradeId().equals(gradeToDelete.getId()))
            .count();
            
        // Trouver le grade le plus bas (niveau le plus petit)
        Grade lowestGrade = c.getGrades().stream()
            .filter(g -> !g.getId().equals(gradeToDelete.getId())) // Exclure le grade à supprimer
            .min(Comparator.comparingInt(Grade::getLevel))
            .orElse(null);
            
        if(lowestGrade == null) {
            app.openDialog(new Dialog.Message(TextFormatting.RED + "Impossible de supprimer le dernier grade"));
            return;
        }
        
        String message = "Supprimer le grade '" + gradeToDelete.getName() + "' ?";
        if(employeeCount > 0) {
            message += "\n" + employeeCount + " employé(s) seront transférés vers '" + lowestGrade.getName() + "'";
        }
        
        Dialog.Confirmation dialog = new Dialog.Confirmation(message);
        dialog.setPositiveListener((mouseX, mouseY, mouseButton) -> {
            // D'abord transférer tous les employés vers le grade le plus bas
            for(EmployeeRecord emp : c.getEmployees()) {
                if(emp.getGradeId().equals(gradeToDelete.getId())) {
                    NBTTagCompound changeData = new NBTTagCompound();
                    changeData.setString("companyId", companyId);
                    changeData.setString("playerUuid", emp.getPlayerUuid());
                    changeData.setString("gradeId", lowestGrade.getId());
                    
                    TaskManager.sendTask(new TaskBusinessAction().op("change_grade", changeData));
                }
            }
            
            // Puis supprimer le grade
            NBTTagCompound d = new NBTTagCompound();
            d.setString("companyId", companyId);
            d.setString("gradeId", gradeToDelete.getId());
            
            TaskManager.sendTask(new TaskBusinessAction().op("remove_grade", d).setCallback((nbt, success) -> {
                if (success) {
                    Minecraft.getMinecraft().addScheduledTask(this::build);
                } else {
                    app.openDialog(new Dialog.Message(TextFormatting.RED + "Erreur lors de la suppression"));
                }
            }));
        });
        app.openDialog(dialog);
    }


    private String findCompanyIdByName(String name) {
        for(Company cc : repo.listCompaniesForPlayer(repo.getCurrentPlayerUuid())) {
            if(cc.getName().equalsIgnoreCase(name)) {
                return cc.getId();
            }
        }
        return null;
    }

    private Grade currentUserGrade(Company c) {
        String me = repo.getCurrentPlayerUuid();
        EmployeeRecord rec = c.getEmployees().stream()
            .filter(e -> e.getPlayerUuid().equals(me))
            .findFirst().orElse(null);
        if(rec == null) return null;
        
        return c.getGrades().stream()
            .filter(g -> g.getId().equals(rec.getGradeId()))
            .findFirst().orElse(null);
    }
    
    private void openGradeEditDialog(Company company, Grade grade) {
        Grade userGrade = currentUserGrade(company);
        if(userGrade == null || !userGrade.getPermissions().canManageGrades) {
            app.openDialog(new Dialog.Message("Pas de permission pour modifier les grades"));
            return;
        }
        
        if(grade.getLevel() >= userGrade.getLevel()) {
            app.openDialog(new Dialog.Message("Vous ne pouvez pas modifier un grade supérieur ou égal au vôtre"));
            return;
        }
        
        // Créer un dialogue avec interface graphique moderne
        Dialog editDialog = new Dialog() {
            @Override
            public void init(@Nullable NBTTagCompound intent) {
                super.init(intent);
                
                setTitle("Modification du grade");
                
                // Créer le layout avec scroll pour éviter le débordement
                ScrollableLayout content = new ScrollableLayout(300, 280, 180);
                
                int y = 10;
                
                // Titre
                content.addComponent(new Label("Modification du grade: " + grade.getName(), 12, y));
                y += 20;
                
                // Nom du grade
                content.addComponent(new Label("Nom du grade :", 12, y));
                y += 12;
                TextField nameField = new TextField(12, y, 200);
                nameField.setText(grade.getName());
                content.addComponent(nameField);
                y += 25;
                
                // Niveau hiérarchique
                content.addComponent(new Label("Niveau hiérarchique :", 12, y));
                content.addComponent(new Label("(Doit être < " + userGrade.getLevel() + ")", 140, y));
                y += 12;
                TextField levelField = new TextField(12, y, 100);
                levelField.setText(String.valueOf(grade.getLevel()));
                content.addComponent(levelField);
                y += 25;
                
                // Limite de virement
                content.addComponent(new Label("Limite de virement (€) :", 12, y));
                y += 12;
                TextField limitField = new TextField(12, y, 120);
                limitField.setText(String.valueOf((int)grade.getPermissions().transferLimit));
                content.addComponent(limitField);
                y += 25;
                
                // Permissions
                content.addComponent(new Label("Permissions :", 12, y));
                y += 15;
                
                CheckBox recruitBox = new CheckBox("Peut recruter des employés", 12, y);
                recruitBox.setSelected(grade.getPermissions().canRecruit);
                content.addComponent(recruitBox);
                y += 18;
                
                CheckBox manageBox = new CheckBox("Peut gérer les grades", 12, y);
                manageBox.setSelected(grade.getPermissions().canManageGrades);
                content.addComponent(manageBox);
                y += 18;
                
                CheckBox promoteBox = new CheckBox("Peut promouvoir/rétrograder", 12, y);
                promoteBox.setSelected(grade.getPermissions().canChangeEmployeeGrade);
                content.addComponent(promoteBox);
                y += 18;
                
                CheckBox fireBox = new CheckBox("Peut licencier des employés", 12, y);
                fireBox.setSelected(grade.getPermissions().canFire);
                content.addComponent(fireBox);
                y += 25;
                
                // Boutons
                Button saveBtn = new Button(12, y, 100, 18, "Sauvegarder", Icons.CHECK);
                Button cancelBtn = new Button(120, y, 80, 18, "Annuler", Icons.CROSS);
                
                content.addComponent(saveBtn);
                content.addComponent(cancelBtn);
                
                // Ajuster la hauteur du contenu pour permettre le scroll
                content.height = Math.max(y + 25, 280);
                
                // Actions des boutons
                saveBtn.setClickListener((mx, my, mb) -> {
                    if(mb == 0) {
                        String newName = nameField.getText().trim();
                        String levelText = levelField.getText().trim();
                        String limitText = limitField.getText().trim();
                        
                        if(newName.isEmpty()) {
                            app.openDialog(new Dialog.Message("Le nom du grade ne peut pas être vide"));
                            return;
                        }
                        
                        try {
                            int newLevel = Integer.parseInt(levelText);
                            double newLimit = Double.parseDouble(limitText);
                            
                            // Vérifier que le niveau est valide selon le statut du joueur
                            Company company = BusinessData.INSTANCE.getCompany(companyId);
                            boolean isOwner = company != null && company.getOwnerUuid().equals(Minecraft.getMinecraft().player.getUniqueID().toString());
                            
                            if(isOwner) {
                                // Le propriétaire peut créer des grades jusqu'au niveau 100
                                if(newLevel <= 0 || newLevel > 100) {
                                    app.openDialog(new Dialog.Message("Niveau invalide (doit être entre 1 et 100)"));
                                    return;
                                }
                            } else {
                                // Un employé ne peut créer que des grades strictement inférieurs au sien
                                if(newLevel <= 0 || newLevel >= userGrade.getLevel()) {
                                    app.openDialog(new Dialog.Message("Niveau invalide (doit être entre 1 et " + (userGrade.getLevel() - 1) + ")"));
                                    return;
                                }
                            }
                            
                            if(newLimit < 0) {
                                app.openDialog(new Dialog.Message("La limite de virement ne peut pas être négative"));
                                return;
                            }
                            
                            // Mettre à jour le grade
                            NBTTagCompound d = new NBTTagCompound();
                            d.setString("companyId", companyId);
                            d.setString("gradeId", grade.getId());
                            d.setString("name", newName);
                            d.setInteger("level", newLevel);
                            d.setDouble("transferLimit", newLimit);
                            d.setBoolean("canRecruit", recruitBox.isSelected());
                            d.setBoolean("canManageGrades", manageBox.isSelected());
                            d.setBoolean("canChangeEmployeeGrade", promoteBox.isSelected());
                            d.setBoolean("canFire", fireBox.isSelected());
                            
                            close();
                            
                            TaskManager.sendTask(new TaskBusinessAction().op("modify_grade", d).setCallback((nbt, success2) -> {
                                if (success2) {
                                    app.openDialog(new Dialog.Message("Grade modifié avec succès"));
                                    Minecraft.getMinecraft().addScheduledTask(CompanyLayout.this::build);
                                } else {
                                    app.openDialog(new Dialog.Message(TextFormatting.RED + "Erreur lors de la modification du grade"));
                                }
                            }));
                            
                        } catch(NumberFormatException e) {
                            app.openDialog(new Dialog.Message("Erreur de format dans les nombres"));
                        }
                    }
                });
                
                cancelBtn.setClickListener((mx, my, mb) -> {
                    if(mb == 0) {
                        close();
                    }
                });
                
                setLayout(content);
            }
        };
        
        app.openDialog(editDialog);
    }
}