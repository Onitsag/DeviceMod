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
        Button bTx = new Button(6, 6, 60, 14, "Transactions");
        Button bEm = new Button(68, 6, 60, 14, "Employés"); 
        Button bGr = new Button(130, 6, 60, 14, "Grades");
        
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

        // Solde fixe en haut
        addComponent(new Label("Solde: " + String.format("%.2f", c.getBalance()) + "€", 10, 24));

        // Liste des transactions scrollable au milieu
        addComponent(new Label("Dernières transactions:", 10, 40));
        ScrollableLayout transactionsList = new ScrollableLayout(0, 52, 362, 1, 80);
        addComponent(transactionsList);
        
        int y = 0;
        for(BizTransaction t : c.getTransactions()) {
            String text = t.toString();
            if(Minecraft.getMinecraft().fontRenderer.getStringWidth(text) > 340) {
                text = Minecraft.getMinecraft().fontRenderer.trimStringToWidth(text, 340);
            }
            transactionsList.addComponent(new Label(text, 12, y));
            y += 12;
        }
        transactionsList.height = Math.max(y, 80);

        // Section virement fixe en bas
        int formY = 140;
        addComponent(new Label("Virement:", 10, formY));
        formY += 12;
        
        TextField target = new TextField(10, formY, 120);
        target.setPlaceholder("Entreprise");
        addComponent(target);
        
        TextField amount = new TextField(134, formY, 60);
        amount.setText("0");
        addComponent(amount);
        
        Button send = new Button(198, formY, 50, 16, "Virer", Icons.CASH);
        send.setClickListener((mx,my,mb)->{
            if(mb == 0) {
                handleTransfer(c, target.getText().trim(), amount.getText().trim());
            }
        });
        addComponent(send);
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
        Button changeGradeBtn = new Button(rightX, 56, 80, 18, "Gérer", Icons.EDIT);
        changeGradeBtn.setEnabled(false);
        content.addComponent(changeGradeBtn);
        
        Button fireBtn = new Button(rightX + 85, 56, 60, 18, "Licencier", Icons.TRASH);
        fireBtn.setEnabled(false);
        content.addComponent(fireBtn);
        
        // Section recrutement
        content.addComponent(new Label("Recruter :", rightX, 90));
        
        TextField playerField = new TextField(rightX, 106, 120);
        playerField.setPlaceholder("Nom du joueur");
        content.addComponent(playerField);
        
        Grade[] availableGrades = c.getGrades().stream()
            .filter(g -> g.getLevel() <= (userGrade != null ? userGrade.getLevel() : -1))
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
        recruitBtn.setEnabled(canRecruit);
        content.addComponent(recruitBtn);
        
        // LOGIQUE DE SÉLECTION
        employeeList.setItemClickListener((employee, index, mouseButton) -> {
            if(mouseButton == 0 && employee != null) {
                // Mettre à jour les infos affichées
                Grade empGrade = c.getGrades().stream()
                    .filter(g -> g.getId().equals(employee.getGradeId()))
                    .findFirst().orElse(null);
                    
                selectedEmployeeLabel.setText("Employé: " + employee.getPlayerName());
                selectedGradeLabel.setText("Grade: " + (empGrade != null ? empGrade.getName() : "Sans grade"));
                
                // Activer/désactiver les boutons selon les permissions
                boolean canManageThisEmployee = userGrade != null && 
                    ((empGrade == null && userGrade.getLevel() > 0) || 
                     (empGrade != null && empGrade.getLevel() < userGrade.getLevel()));
                     
                changeGradeBtn.setEnabled(canChangeGrade && canManageThisEmployee);
                fireBtn.setEnabled(canFire && canManageThisEmployee);
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
        
        recruitBtn.setClickListener((mx, my, mb) -> {
            if(mb == 0) {
                handleAddEmployee(c, playerField, gradeCombo);
            }
        });
        
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

            repo.transfer(companyId, toId, amount, "Virement vers " + toName);
            Minecraft.getMinecraft().addScheduledTask(this::build);
        } catch (NumberFormatException e) {
            app.openDialog(new Dialog.Message("Montant invalide"));
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
                openGradeSelectionDialog(employee, currentGrade, availableGrades, canFire);
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
    
    private void openGradeSelectionDialog(EmployeeRecord employee, Grade currentGrade, Grade[] availableGrades, boolean canFire) {
        // Créer un dialogue personnalisé pour la sélection de grade
        Dialog gradeDialog = new Dialog() {
            @Override
            public void init(@Nullable NBTTagCompound intent) {
                super.init(intent);
                
                // Configuration du dialogue
                setTitle("Gestion d'employé");
                
                // Créer le layout personnalisé
                Layout content = new Layout(280, 120);
                
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
                
                setLayout(content);
            }
        };
        
        app.openDialog(gradeDialog);
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
        String message = "⚠️ CONFIRMATION DE LICENCIEMENT ⚠️\n\n";
        message += "Êtes-vous sûr de vouloir licencier :\n";
        message += "👤 " + employee.getPlayerName() + " ?\n\n";
        message += "Cette action est irréversible !";
        
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