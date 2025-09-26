package fr.onitsag.faritech.programs.business.task;

import fr.onitsag.faritech.api.task.Task;
import fr.onitsag.faritech.programs.business.ApplicationBusinessManager;
import fr.onitsag.faritech.programs.business.data.BusinessData;
import fr.onitsag.faritech.programs.business.model.Company;
import fr.onitsag.faritech.programs.business.model.EmployeeRecord;
import fr.onitsag.faritech.programs.business.model.Grade;
import fr.onitsag.faritech.programs.business.model.Permissions;
import fr.onitsag.faritech.programs.business.service.BusinessRepository;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.client.Minecraft;
import java.util.UUID;

/**
 * Task générique pour opérations Business (multi-serveur sécurisé via serveur).
 */
public class TaskBusinessAction extends Task
{
    public TaskBusinessAction() { super("business_action"); }

    // champs envoyés
    private String op;
    private NBTTagCompound payload;
    private double responseBalance = -1;

    public TaskBusinessAction op(String operation, NBTTagCompound payload)
    {
        this.op = operation;
        this.payload = payload;
        return this;
    }

    @Override
    public void prepareRequest(NBTTagCompound nbt)
    {
        nbt.setString("op", op);
        if(payload != null) nbt.setTag("d", payload);
    }

    @Override
    public void processRequest(NBTTagCompound nbt, World world, EntityPlayer player)
    {
        String operation = nbt.getString("op");
        NBTTagCompound d = nbt.getCompoundTag("d");
        boolean ok = false;

        switch(operation)
        {
            case "create_company":
            {
                String name = d.getString("name");
                Company company = BusinessData.INSTANCE.createCompany(name, player.getUniqueID().toString(), player.getName());
                if(company != null) {
                    ok = true;
                } else {
                    // Nom déjà pris ou invalide
                    ok = false;
                }
                break;
            }
            case "add_grade":
            {
                Company c = BusinessData.INSTANCE.getCompany(d.getString("companyId"));
                if(c != null) {
                    // Vérifier les permissions du joueur
                    boolean isOwner = c.getOwnerUuid().equals(player.getUniqueID().toString());
                    Grade playerGrade = null;
                    
                    if(!isOwner) {
                        EmployeeRecord playerRecord = c.getEmployees().stream()
                            .filter(e -> e.getPlayerUuid().equals(player.getUniqueID().toString()))
                            .findFirst().orElse(null);
                        
                        if(playerRecord != null) {
                            playerGrade = c.getGrades().stream()
                                .filter(g -> g.getId().equals(playerRecord.getGradeId()))
                                .findFirst().orElse(null);
                        }
                    }
                    
                    // Récupérer le niveau demandé
                    int newLevel = d.getInteger("level");
                    
                    // Vérifications de sécurité
                    boolean canCreate = false;
                    if(isOwner) {
                        // Le propriétaire peut créer des grades jusqu'au niveau 100
                        canCreate = newLevel <= 100;
                    } else if(playerGrade != null && playerGrade.getPermissions().canManageGrades) {
                        // L'employé peut créer seulement des grades strictement inférieurs au sien
                        canCreate = newLevel < playerGrade.getLevel();
                    }
                    
                    if(canCreate) {
                        Permissions perm = new Permissions(d.getBoolean("recruit"), d.getBoolean("manage"), d.getBoolean("change"), d.getBoolean("fire"), d.getDouble("limit"));
                        c.getGrades().add(new Grade(UUID.randomUUID().toString(), d.getString("name"), newLevel, perm));
                        ok = true;
                    }
                }
                break;
            }
            case "add_employee":
            {
                Company c = BusinessData.INSTANCE.getCompany(d.getString("companyId"));
                if(c != null) {
                    String playerUuid = d.getString("playerUuid");
                    String playerName = d.getString("playerName");
                    String gradeId = d.getString("gradeId");

                    EntityPlayerMP target = null;
                    
                    // Si l'UUID est vide ou invalide, chercher directement par nom
                    if(playerUuid == null || playerUuid.isEmpty()) {
                        target = player.getServer().getPlayerList().getPlayerByUsername(playerName);
                    } else {
                        try
                        {
                            target = player.getServer().getPlayerList().getPlayerByUUID(UUID.fromString(playerUuid));
                        }
                        catch(IllegalArgumentException e) {
                            // UUID invalide, continuer avec la recherche par nom
                        }

                        if(target == null && !playerName.isEmpty())
                        {
                            target = player.getServer().getPlayerList().getPlayerByUsername(playerName);
                        }
                    }

                    if(target != null)
                    {
                        playerUuid = target.getUniqueID().toString();
                        playerName = target.getName();
                    }

                    // Vérifier que le grade existe
                    boolean gradeExists = c.getGrades().stream().anyMatch(g -> g.getId().equals(gradeId));

                    final String finalUuid = playerUuid;
                    boolean alreadyEmployee = c.getEmployees().stream().anyMatch(emp -> emp.getPlayerUuid().equals(finalUuid));

                    if(target != null && gradeExists && !alreadyEmployee)
                    {
                        c.getEmployees().add(new fr.onitsag.faritech.programs.business.model.EmployeeRecord(playerUuid, playerName, gradeId));
                        ok = true;
                    }
                }
                break;
            }
            case "fire_employee":
            {
                Company c = BusinessData.INSTANCE.getCompany(d.getString("companyId"));
                if(c != null) {
                    String playerUuid = d.getString("playerUuid");
                    String playerName = d.getString("playerName");
                    
                    // Si l'UUID est vide, chercher le joueur par nom pour obtenir son vrai UUID
                    if(playerUuid == null || playerUuid.isEmpty()) {
                        if(playerName != null && !playerName.isEmpty()) {
                            EntityPlayerMP target = player.getServer().getPlayerList().getPlayerByUsername(playerName);
                            if(target != null) {
                                playerUuid = target.getUniqueID().toString();
                            } else {
                                // Chercher dans la liste des employés par nom
                                for(EmployeeRecord emp : c.getEmployees()) {
                                    if(emp.getPlayerName().equals(playerName)) {
                                        playerUuid = emp.getPlayerUuid();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    if(playerUuid != null && !playerUuid.isEmpty()) {
                        final String finalPlayerUuid = playerUuid;
                        boolean removed = c.getEmployees().removeIf(e -> e.getPlayerUuid().equals(finalPlayerUuid));
                        System.out.println("[DEBUG] fire_employee: " + playerName + " -> " + (removed ? "licencié" : "non trouvé"));
                        ok = removed;
                    }
                }
                break;
            }
            case "change_grade":
            {
                Company c = BusinessData.INSTANCE.getCompany(d.getString("companyId"));
                if(c != null) {
                    String playerUuid = d.getString("playerUuid");
                    String playerName = d.getString("playerName");
                    String newGradeId = d.getString("gradeId");
                    
                    // Si l'UUID est vide, chercher le joueur par nom pour obtenir son vrai UUID
                    if(playerUuid == null || playerUuid.isEmpty()) {
                        if(playerName != null && !playerName.isEmpty()) {
                            EntityPlayerMP target = player.getServer().getPlayerList().getPlayerByUsername(playerName);
                            if(target != null) {
                                playerUuid = target.getUniqueID().toString();
                            } else {
                                // Chercher dans la liste des employés par nom
                                for(EmployeeRecord emp : c.getEmployees()) {
                                    if(emp.getPlayerName().equals(playerName)) {
                                        playerUuid = emp.getPlayerUuid();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    if(playerUuid != null && !playerUuid.isEmpty()) {
                        final String finalPlayerUuid = playerUuid;
                        boolean changed = c.getEmployees().stream()
                            .filter(e -> e.getPlayerUuid().equals(finalPlayerUuid))
                            .findFirst()
                            .map(e -> { e.setGradeId(newGradeId); return true; })
                            .orElse(false);
                        System.out.println("[DEBUG] change_grade: " + playerName + " -> " + (changed ? "changé" : "non trouvé"));
                        ok = changed;
                    }
                }
                break;
            }
            case "transfer":
            {
                Company from = BusinessData.INSTANCE.getCompany(d.getString("fromCompanyId"));
                Company to = BusinessData.INSTANCE.getCompany(d.getString("toCompanyId"));
                double amount = d.getDouble("amount");
                if(from != null && to != null && amount > 0 && from.getBalance() >= amount) {
                    from.setBalance(from.getBalance() - amount);
                    to.setBalance(to.getBalance() + amount);
                    long now = System.currentTimeMillis();
                    from.getTransactions().add(new fr.onitsag.faritech.programs.business.model.BizTransaction(
                        java.util.UUID.randomUUID().toString(), now,
                        from.getId(), to.getId(), amount,
                        fr.onitsag.faritech.programs.business.model.BizTransaction.Type.TRANSFER_OUT,
                        "Virement vers " + to.getName()
                    ));
                    to.getTransactions().add(new fr.onitsag.faritech.programs.business.model.BizTransaction(
                        java.util.UUID.randomUUID().toString(), now,
                        from.getId(), to.getId(), amount,
                        fr.onitsag.faritech.programs.business.model.BizTransaction.Type.TRANSFER_IN,
                        "Virement de " + from.getName()
                    ));
                    ok = true;
                }
                break;
            }
            case "player_deposit_to_company":
            {
                Company company = BusinessData.INSTANCE.getCompany(d.getString("companyId"));
                double amount = d.getDouble("amount");
                if(company != null && amount > 0)
                {
                    // Débiter le joueur via Vault (côté serveur)
                    String playerUuid = player.getUniqueID().toString();
                    // Vérifier solde joueur
                    double bal = fr.onitsag.faritech.economy.EconomyManager.getPlayerBalance(playerUuid);
                    if(bal < amount) {
                        ok = false;
                        break;
                    }
                    boolean debited = fr.onitsag.faritech.economy.EconomyManager.withdrawFromPlayer(playerUuid, amount);
                    if(debited)
                    {
                        company.setBalance(company.getBalance() + amount);
                        company.getTransactions().add(new fr.onitsag.faritech.programs.business.model.BizTransaction(
                            java.util.UUID.randomUUID().toString(),
                            System.currentTimeMillis(),
                            "PERSONAL_" + playerUuid,
                            company.getId(),
                            amount,
                            fr.onitsag.faritech.programs.business.model.BizTransaction.Type.TRANSFER_IN,
                            "Dépôt de " + player.getName()
                        ));
                        ok = true;
                    }
                }
                break;
            }
            case "company_withdraw_to_player":
            {
                Company company = BusinessData.INSTANCE.getCompany(d.getString("companyId"));
                double amount = d.getDouble("amount");
                if(company != null && amount > 0 && company.getBalance() >= amount)
                {
                    String playerUuid = player.getUniqueID().toString();
                    boolean credited = fr.onitsag.faritech.economy.EconomyManager.depositToPlayer(playerUuid, amount);
                    if(credited)
                    {
                        company.setBalance(company.getBalance() - amount);
                        company.getTransactions().add(new fr.onitsag.faritech.programs.business.model.BizTransaction(
                            java.util.UUID.randomUUID().toString(),
                            System.currentTimeMillis(),
                            company.getId(),
                            "PERSONAL_" + playerUuid,
                            amount,
                            fr.onitsag.faritech.programs.business.model.BizTransaction.Type.TRANSFER_OUT,
                            "Retrait de " + player.getName()
                        ));
                        ok = true;
                    }
                }
                break;
            }
            case "company_pay_player":
            {
                Company company = BusinessData.INSTANCE.getCompany(d.getString("companyId"));
                String targetName = d.getString("targetName");
                double amount = d.getDouble("amount");
                if(company != null && amount > 0 && company.getBalance() >= amount && targetName != null && !targetName.isEmpty())
                {
                    // Trouver le joueur cible par nom (en ligne)
                    net.minecraft.entity.player.EntityPlayerMP target = player.getServer().getPlayerList().getPlayerByUsername(targetName);
                    if(target != null)
                    {
                        String targetUuid = target.getUniqueID().toString();
                        boolean credited = fr.onitsag.faritech.economy.EconomyManager.depositToPlayer(targetUuid, amount);
                        if(credited)
                        {
                            company.setBalance(company.getBalance() - amount);
                            company.getTransactions().add(new fr.onitsag.faritech.programs.business.model.BizTransaction(
                                java.util.UUID.randomUUID().toString(),
                                System.currentTimeMillis(),
                                company.getId(),
                                "PLAYER_" + targetName,
                                amount,
                                fr.onitsag.faritech.programs.business.model.BizTransaction.Type.PAYMENT,
                                "Paiement à " + targetName
                            ));
                            ok = true;
                        }
                    }
                }
                break;
            }
            case "remove_grade":
            {
                Company c = BusinessData.INSTANCE.getCompany(d.getString("companyId"));
                if(c != null) {
                    String gradeId = d.getString("gradeId");
                    c.getGrades().removeIf(g -> g.getId().equals(gradeId));
                    ok = true;
                }
                break;
            }
            case "modify_grade":
            {
                Company c = BusinessData.INSTANCE.getCompany(d.getString("companyId"));
                if(c != null) {
                    String gradeId = d.getString("gradeId");
                    Grade grade = c.getGrades().stream()
                        .filter(g -> g.getId().equals(gradeId))
                        .findFirst().orElse(null);
                    
                    if(grade != null) {
                        // Vérifier les permissions du joueur
                        boolean isOwner = c.getOwnerUuid().equals(player.getUniqueID().toString());
                        Grade playerGrade = null;
                        
                        if(!isOwner) {
                            EmployeeRecord playerRecord = c.getEmployees().stream()
                                .filter(e -> e.getPlayerUuid().equals(player.getUniqueID().toString()))
                                .findFirst().orElse(null);
                            
                            if(playerRecord != null) {
                                playerGrade = c.getGrades().stream()
                                    .filter(g -> g.getId().equals(playerRecord.getGradeId()))
                                    .findFirst().orElse(null);
                            }
                        }
                        
                        // Récupérer le nouveau niveau demandé
                        int newLevel = d.getInteger("level");
                        
                        // Vérifications de sécurité
                        boolean canModify = false;
                        if(isOwner) {
                            // Le propriétaire peut tout modifier sauf créer des grades de niveau supérieur à 100
                            canModify = newLevel <= 100;
                        } else if(playerGrade != null && playerGrade.getPermissions().canManageGrades) {
                            // L'employé peut modifier seulement les grades STRICTEMENT inférieurs au sien
                            // ET ne peut pas créer un grade de niveau >= au sien
                            canModify = grade.getLevel() < playerGrade.getLevel() && newLevel < playerGrade.getLevel();
                        }
                        
                        if(canModify) {
                            // Modifier le grade
                            grade.setName(d.getString("name"));
                            grade.setLevel(newLevel);
                            
                            // Modifier les permissions
                            Permissions newPerms = new Permissions(
                                d.getBoolean("canRecruit"),
                                d.getBoolean("canManageGrades"), 
                                d.getBoolean("canChangeEmployeeGrade"),
                                d.getBoolean("canFire"),
                                d.getDouble("transferLimit")
                            );
                            grade.setPermissions(newPerms);
                            
                            ok = true;
                        }
                    }
                }
                break;
            }
            case "get_vault_balance":
            {
                // Retourner le solde Vault réel du joueur appelant
                String playerUuid = player.getUniqueID().toString();
                String playerName = player.getName();
                try {
                    double bal = fr.onitsag.faritech.economy.EconomyManager.getPlayerBalance(playerUuid);
                    this.responseBalance = bal;
                    ok = true;
                    System.out.println("[FariTech] get_vault_balance pour " + playerName + ": " + bal + "€");
                } catch (Exception e) {
                    System.err.println("[FariTech] Erreur get_vault_balance pour " + playerName + ": " + e.getMessage());
                    e.printStackTrace();
                    // Même en cas d'erreur, on retourne 0 et succès pour éviter le blocage de l'interface
                    this.responseBalance = 0.0;
                    ok = true;
                }
                break;
            }
        }

        if(ok) setSuccessful();
        if(ok)
        {
            // Ne pas broadcaster pour get_vault_balance qui est juste une lecture
            if(!"get_vault_balance".equals(operation))
            {
                BusinessData.INSTANCE.broadcastState();
            }
        }
    }

    @Override
    public void prepareResponse(NBTTagCompound nbt)
    {
        if(isSucessful())
        {
            // Ajouter un champ optionnel balance pour certaines opérations (get_vault_balance)
            if(this.responseBalance >= 0)
            {
                nbt.setDouble("balance", this.responseBalance);
            }
            
            // Inclure l'état business seulement pour les opérations qui modifient les données
            // Ne pas envoyer pour get_vault_balance qui est juste une lecture
            if(!"get_vault_balance".equals(this.op))
            {
                nbt.setTag("data", BusinessData.INSTANCE.toNetworkTag());
            }
        }
    }

    @Override
    public void processResponse(NBTTagCompound nbt)
    {
        if(nbt.hasKey("data"))
        {
            BusinessRepository.get().updateFromNetwork(nbt.getCompoundTag("data"));
            Minecraft.getMinecraft().addScheduledTask(() -> {
                fr.onitsag.faritech.core.Laptop laptop = null;
                if(Minecraft.getMinecraft().currentScreen instanceof fr.onitsag.faritech.core.Laptop)
                {
                    laptop = (fr.onitsag.faritech.core.Laptop) Minecraft.getMinecraft().currentScreen;
                }
                if(laptop != null)
                {
                    for(fr.onitsag.faritech.core.Window window : laptop.getOpenWindows())
                    {
                        if(window != null && window.getContent() instanceof ApplicationBusinessManager)
                        {
                            ((ApplicationBusinessManager) window.getContent()).onBusinessDataSynced();
                        }
                    }
                }
            });
        }
    }
}


