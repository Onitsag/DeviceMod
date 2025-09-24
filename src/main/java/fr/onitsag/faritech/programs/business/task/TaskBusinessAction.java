package fr.onitsag.faritech.programs.business.task;

import fr.onitsag.faritech.api.task.Task;
import fr.onitsag.faritech.programs.business.ApplicationBusinessManager;
import fr.onitsag.faritech.programs.business.data.BusinessData;
import fr.onitsag.faritech.programs.business.model.Company;
import fr.onitsag.faritech.programs.business.model.Grade;
import fr.onitsag.faritech.programs.business.model.Permissions;
import fr.onitsag.faritech.programs.business.network.MessageSyncBusiness;
import fr.onitsag.faritech.programs.business.service.BusinessRepository;
import fr.onitsag.faritech.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
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
                if(c != null && c.getOwnerUuid().equals(player.getUniqueID().toString()))
                {
                    Permissions perm = new Permissions(d.getBoolean("recruit"), d.getBoolean("manage"), d.getBoolean("change"), d.getBoolean("fire"), d.getDouble("limit"));
                    c.getGrades().add(new Grade(UUID.randomUUID().toString(), d.getString("name"), d.getInteger("level"), perm));
                    ok = true;
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
                    try
                    {
                        target = player.getServer().getPlayerList().getPlayerByUUID(UUID.fromString(playerUuid));
                    }
                    catch(IllegalArgumentException ignored) {}

                    if(target == null && !playerName.isEmpty())
                    {
                        target = player.getServer().getPlayerList().getPlayerByUsername(playerName);
                    }

                    if(target != null)
                    {
                        playerUuid = target.getUniqueID().toString();
                        playerName = target.getName();
                    }

                    final String finalUuid = playerUuid;
                    if(c.getEmployees().stream().noneMatch(emp -> emp.getPlayerUuid().equals(finalUuid)))
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
                    c.getEmployees().removeIf(e -> e.getPlayerUuid().equals(playerUuid));
                    ok = true;
                }
                break;
            }
            case "change_grade":
            {
                Company c = BusinessData.INSTANCE.getCompany(d.getString("companyId"));
                if(c != null) {
                    String playerUuid = d.getString("playerUuid");
                    String newGradeId = d.getString("gradeId");
                    c.getEmployees().stream()
                        .filter(e -> e.getPlayerUuid().equals(playerUuid))
                        .findFirst()
                        .ifPresent(e -> e.setGradeId(newGradeId));
                    ok = true;
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


