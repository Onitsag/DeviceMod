package fr.onitsag.faritech.programs.business.ui;

import fr.onitsag.faritech.api.app.Dialog;
import fr.onitsag.faritech.api.app.Icons;
import fr.onitsag.faritech.api.app.Layout;
import fr.onitsag.faritech.api.app.ScrollableLayout;
import fr.onitsag.faritech.api.app.component.*;
import fr.onitsag.faritech.programs.business.ApplicationBusinessManager;
import fr.onitsag.faritech.programs.business.model.*;
import fr.onitsag.faritech.programs.business.service.BusinessRepository;
import fr.onitsag.faritech.programs.business.task.TaskBusinessAction;
import fr.onitsag.faritech.api.task.TaskManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;

import java.util.Comparator;
import java.util.UUID;

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
        Button back = new Button(310, 6, 46, 14, "Retour", Icons.ARROW_LEFT);
        back.setClickListener((mx,my,mb)->{ if(mb==0) app.returnToMainMenu(); });
        addComponent(back);

        // Conteneur scrollable pour le contenu
        ScrollableLayout content = new ScrollableLayout(0, 24, 362, 1, 140);
        addComponent(content);

        // Contenu selon l'onglet
        switch (current)
        {
            case TRANSACTIONS: buildTransactions(content); break;
            case EMPLOYEES:    buildEmployees(content); break;
            case GRADES:       buildGrades(content); break;
        }

        // IMPORTANT: après avoir reconstruit les composants, forcer la mise à jour
        // des positions absolues (xPosition/yPosition) pour que les TextField/ComboBox
        // reçoivent correctement les événements de clic/sélection.
        app.markForLayoutUpdate();
    }

    private void buildTransactions(Layout content)
    {
        Company c = repo.getCompany(companyId).orElse(null);
        if(c == null) return;

        // Solde
        content.addComponent(new Label("Solde: " + String.format("%.2f", c.getBalance()) + "€", 10, 0));

        // Liste des dernières transactions
        content.addComponent(new Label("Dernières transactions:", 10, 16));
        
        int y = 28;
        int count = 0;
        for(BizTransaction t : c.getTransactions()) {
            if(count >= 8) break; // Maximum 8 lignes pour rester dans l'espace
            
            String text = t.toString();
            if(Minecraft.getMinecraft().fontRenderer.getStringWidth(text) > 340) {
                text = Minecraft.getMinecraft().fontRenderer.trimStringToWidth(text, 340);
            }
            content.addComponent(new Label(text, 12, y));
            y += 12;
            count++;
        }

        // Section virement
        y = Math.max(y + 8, 120);
        content.addComponent(new Label("Virement:", 10, y));
        int formY = y + 12;
        
        TextField target = new TextField(10, formY, 120);
        target.setPlaceholder("Entreprise");
        content.addComponent(target);
        
        TextField amount = new TextField(134, formY, 60);
        amount.setText("0");
        content.addComponent(amount);
        
        Button send = new Button(198, formY, 50, 16, "Virer", Icons.CASH);
        send.setClickListener((mx,my,mb)->{
            if(mb == 0) {
                handleTransfer(c, target.getText().trim(), amount.getText().trim());
            }
        });
        content.addComponent(send);

        // Ajuster la hauteur du contenu exactement au dernier élément
        int bottom = formY + 24;
        content.height = bottom;
    }

    private void buildEmployees(Layout content)
    {
        Company c = repo.getCompany(companyId).orElse(null);
        if(c == null) return;
        
        // Colonne gauche - Liste employés
        content.addComponent(new Label("Employés:", 10, 0));
        
        int y = 14;
        int count = 0;
        for(EmployeeRecord emp : c.getEmployees()) {
            if(count >= 10) break; // Max 10 employés affichés
            
            Grade grade = c.getGrades().stream()
                .filter(g -> g.getId().equals(emp.getGradeId()))
                .findFirst().orElse(null);
            String gradeText = grade != null ? " (" + grade.getName() + ")" : "";
            content.addComponent(new Label("• " + emp.getPlayerName() + gradeText, 12, y));
            y += 12;
            count++;
        }

        // Colonne droite - Contrôles
        content.addComponent(new Label("Ajouter:", 180, 0));
        
        TextField playerField = new TextField(180, 14, 100);
        playerField.setPlaceholder("Joueur");
        content.addComponent(playerField);
        
        // Filtrer les grades selon la hiérarchie (seulement les grades inférieurs ou égaux si on peut recruter)
        Grade userGrade = currentUserGrade(c);
        Grade[] availableGrades;
        if(userGrade != null) {
            availableGrades = c.getGrades().stream()
                .filter(g -> g.getLevel() <= userGrade.getLevel()) // <= au lieu de < pour inclure notre niveau
                .toArray(Grade[]::new);
        } else {
            availableGrades = c.getGrades().toArray(new Grade[0]); // Si pas de grade, montrer tous
        }
        
        ComboBox.List<Grade> gradeCombo = new ComboBox.List<>(180, 32, 100, availableGrades);
        
        // Pré-sélectionner le grade "Employé" par défaut
        for(Grade grade : availableGrades) {
            if(grade.getName().equals("Employé")) {
                gradeCombo.setSelectedItem(grade);
                break;
            }
        }
        
        content.addComponent(gradeCombo);
        
        Button addBtn = new Button(286, 14, 50, 16, "Ajouter", Icons.PLUS);
        addBtn.setClickListener((mx,my,mb)->{
            if(mb==0) {
                handleAddEmployee(c, playerField, gradeCombo);
            }
        });
        content.addComponent(addBtn);

        Button fireBtn = new Button(286, 32, 50, 16, "Virer", Icons.TRASH);
        fireBtn.setClickListener((mx,my,mb)->{
            if(mb==0) {
                handleFireEmployee(c);
            }
        });
        content.addComponent(fireBtn);

        // Changement de grade
        content.addComponent(new Label("Changer grade:", 180, 56));
        
        TextField targetPlayer = new TextField(180, 70, 100);
        targetPlayer.setPlaceholder("Joueur");
        content.addComponent(targetPlayer);
        
        ComboBox.List<Grade> gradeCombo2 = new ComboBox.List<>(180, 88, 100, availableGrades);
        
        // Pré-sélectionner le grade "Employé" par défaut
        for(Grade grade : availableGrades) {
            if(grade.getName().equals("Employé")) {
                gradeCombo2.setSelectedItem(grade);
                break;
            }
        }
        
        content.addComponent(gradeCombo2);
        
        Button changeBtn = new Button(286, 70, 50, 16, "OK", Icons.EDIT);
        changeBtn.setClickListener((mx,my,mb)->{
            if(mb==0) {
                handleChangeGrade(c, targetPlayer.getText().trim(), gradeCombo2);
            }
        });
        content.addComponent(changeBtn);

        content.height = Math.max(110, y);
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
            
            // Flèches de réorganisation (seulement si on peut gérer et que ce n'est pas notre grade ou supérieur)
            if(canModifyThisGrade) {
                // Flèche vers le haut (augmenter niveau)
                if(i > 0) {
                    Button upBtn = new Button(12, y, 16, 16, "", Icons.ARROW_UP);
                    upBtn.setClickListener((mx,my,mb)->{
                        if(mb==0) handleMoveGrade(c, grade, true);
                    });
                    content.addComponent(upBtn);
                }
                
                // Flèche vers le bas (diminuer niveau)
                if(i < grades.length - 1) {
                    Button downBtn = new Button(30, y, 16, 16, "", Icons.ARROW_DOWN);
                    downBtn.setClickListener((mx,my,mb)->{
                        if(mb==0) handleMoveGrade(c, grade, false);
                    });
                    content.addComponent(downBtn);
                }
                
                // Icône poubelle pour supprimer
                Button deleteBtn = new Button(48, y, 16, 16, "", Icons.TRASH);
                deleteBtn.setClickListener((mx,my,mb)->{
                    if(mb==0) handleDeleteGradeWithConfirmation(c, grade);
                });
                content.addComponent(deleteBtn);
            }
            
            // Nom et niveau du grade
            int labelX = canModifyThisGrade ? 70 : 12;
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
            TextField nameField = new TextField(12, y + 10, 120);
            nameField.setPlaceholder("Ex: Manager, Assistant...");
            content.addComponent(nameField);
            
            // Niveau hiérarchique
            content.addComponent(new Label("Niveau hiérarchique :", 140, y));
            content.addComponent(new Label("(Doit être < " + userLevel + ")", 140, y + 8));
            TextField levelField = new TextField(140, y + 18, 60);
            levelField.setPlaceholder("Ex: " + Math.max(1, userLevel - 10));
            content.addComponent(levelField);
            
            y += 35;
            
            // Limite de transfert
            content.addComponent(new Label("Limite de virement (€) :", 12, y));
            TextField limitField = new TextField(12, y + 10, 100);
            limitField.setPlaceholder("Ex: 10000");
            content.addComponent(limitField);
            
            y += 25;
            
            // Permissions avec descriptions
            content.addComponent(new Label("Permissions :", 12, y));
            y += 12;
            
            CheckBox pRecruit = new CheckBox("Peut recruter de nouveaux employés", 12, y);
            content.addComponent(pRecruit);
            y += 14;
            
            CheckBox pManage = new CheckBox("Peut créer/supprimer des grades", 12, y);
            content.addComponent(pManage);
            y += 14;
            
            CheckBox pChange = new CheckBox("Peut promouvoir/rétrograder", 12, y);
            content.addComponent(pChange);
            y += 14;
            
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
        
        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()).toString();
        
        // Envoyer la tâche au serveur au lieu de modifier directement
        NBTTagCompound d = new NBTTagCompound();
        d.setString("companyId", companyId);
        d.setString("playerUuid", uuid);
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

    private void handleFireEmployee(Company c) {
        Grade playerGrade = currentUserGrade(c);
        if (playerGrade == null || !playerGrade.getPermissions().canFire) {
            app.openDialog(new Dialog.Message(TextFormatting.RED + "Pas de permission"));
            return;
        }

        // Pour simplicité, on demande le nom à licencier
        Dialog.Input dialog = new Dialog.Input("Nom du joueur à licencier:");
        dialog.setResponseHandler((success, response) -> {
            if (success && !response.trim().isEmpty()) {
                String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + response.trim()).getBytes()).toString();
                
                // Envoyer la tâche au serveur au lieu de modifier directement
                NBTTagCompound d = new NBTTagCompound();
                d.setString("companyId", companyId);
                d.setString("playerUuid", uuid);
                
                TaskManager.sendTask(new TaskBusinessAction().op("fire_employee", d).setCallback((nbt, successTask) -> {
                    if (successTask) {
                        Minecraft.getMinecraft().addScheduledTask(this::build);
                    } else {
                        app.openDialog(new Dialog.Message(TextFormatting.RED + "Erreur lors du licenciement"));
                    }
                }));
            }
            return true;
        });
        app.openDialog(dialog);
    }

    private void handleChangeGrade(Company c, String playerName, ComboBox<Grade> gradeCombo2) {
        Grade playerGrade = currentUserGrade(c);
        if (playerGrade == null || !playerGrade.getPermissions().canChangeEmployeeGrade) {
            app.openDialog(new Dialog.Message(TextFormatting.RED + "Pas de permission"));
            return;
        }

        if(playerName.isEmpty()) return;
        
        Grade newGrade = (Grade) gradeCombo2.getValue();
        if(newGrade.getLevel() > playerGrade.getLevel()) {
            app.openDialog(new Dialog.Message(TextFormatting.RED + "Grade trop élevé"));
            return;
        }
        
        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes()).toString();
        repo.changeEmployeeGrade(companyId, uuid, newGrade.getId());
        Minecraft.getMinecraft().addScheduledTask(this::build);
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
            
            Permissions perm = new Permissions(pRecruit.isSelected(), pManage.isSelected(), 
                                             pChange.isSelected(), pFire.isSelected(), limit);
            repo.addGrade(companyId, name, level, perm);
            
            // Vider les champs après création
            nameField.setText("");
            levelField.setText("");
            limitField.setText("");
            pRecruit.setSelected(false);
            pManage.setSelected(false);
            pChange.setSelected(false);
            pFire.setSelected(false);
            
            Minecraft.getMinecraft().addScheduledTask(this::build);
        } catch (NumberFormatException e) {
            app.openDialog(new Dialog.Message("Niveau et limite doivent être des nombres valides"));
        }
    }

    private void handleMoveGrade(Company c, Grade grade, boolean moveUp) {
        Grade[] grades = c.getGrades().stream()
            .sorted(Comparator.comparingInt(Grade::getLevel).reversed())
            .toArray(Grade[]::new);
            
        // Trouver l'index actuel du grade
        int currentIndex = -1;
        for(int i = 0; i < grades.length; i++) {
            if(grades[i].getId().equals(grade.getId())) {
                currentIndex = i;
                break;
            }
        }
        
        if(currentIndex == -1) return;
        
        // Déterminer le grade avec lequel échanger
        Grade targetGrade = null;
        if(moveUp && currentIndex > 0) {
            targetGrade = grades[currentIndex - 1]; // Grade au-dessus
        } else if(!moveUp && currentIndex < grades.length - 1) {
            targetGrade = grades[currentIndex + 1]; // Grade en-dessous
        }
        
        if(targetGrade != null) {
            // Échanger les niveaux
            int tempLevel = grade.getLevel();
            grade.setLevel(targetGrade.getLevel());
            targetGrade.setLevel(tempLevel);
            
            // Sauvegarder (à implémenter dans le repo si nécessaire)
            Minecraft.getMinecraft().addScheduledTask(this::build);
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
            // Transférer tous les employés vers le grade le plus bas
            for(EmployeeRecord emp : c.getEmployees()) {
                if(emp.getGradeId().equals(gradeToDelete.getId())) {
                    emp.setGradeId(lowestGrade.getId());
                }
            }
            
            // Supprimer le grade
            repo.removeGrade(companyId, gradeToDelete.getId());
            Minecraft.getMinecraft().addScheduledTask(this::build);
        });
        app.openDialog(dialog);
    }

    private void handleDeleteGrade(Company c, String gradeName) {
        Grade playerGrade = currentUserGrade(c);
        if (playerGrade == null || !playerGrade.getPermissions().canManageGrades) {
            app.openDialog(new Dialog.Message(TextFormatting.RED + "Pas de permission"));
            return;
        }
        
        if(gradeName.isEmpty()) return;
        
        Grade toDelete = c.getGrades().stream()
            .filter(g -> g.getName().equalsIgnoreCase(gradeName))
            .findFirst().orElse(null);
            
        if(toDelete != null) {
            repo.removeGrade(companyId, toDelete.getId());
            Minecraft.getMinecraft().addScheduledTask(this::build);
        }
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
}