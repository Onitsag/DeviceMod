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
                    ok = true;
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
        }

        if(ok) setSuccessful();
        if(ok)
        {
            BusinessData.INSTANCE.broadcastState();
        }
    }

    @Override
    public void prepareResponse(NBTTagCompound nbt)
    {
        if(isSucessful())
        {
            nbt.setTag("data", BusinessData.INSTANCE.toNetworkTag());
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


