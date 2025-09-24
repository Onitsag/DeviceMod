package fr.onitsag.faritech.programs.business.data;

import fr.onitsag.faritech.programs.business.model.*;
import fr.onitsag.faritech.programs.business.network.MessageSyncBusiness;
import fr.onitsag.faritech.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stockage serveur des entreprises, scindé par monde (persisté via BusinessEvents).
 */
public class BusinessData
{
    public static final BusinessData INSTANCE = new BusinessData();

    private final Map<String, Company> idToCompany = new HashMap<>();

    private BusinessData() {}

    public synchronized void clear()
    {
        idToCompany.clear();
    }

    public synchronized List<Company> listCompaniesForPlayer(String playerUuid)
    {
        return idToCompany.values().stream()
            .filter(c -> c.getOwnerUuid().equals(playerUuid) || c.getEmployees().stream().anyMatch(e -> e.getPlayerUuid().equals(playerUuid)))
            .sorted(Comparator.comparing(Company::getName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    public synchronized Company getCompany(String id)
    {
        return idToCompany.get(id);
    }

    public synchronized Company createCompany(String name, String ownerUuid, String ownerName)
    {
        String id = UUID.randomUUID().toString();
        Company c = new Company(id, name, ownerUuid);
        
        // Créer la hiérarchie de grades par défaut
        // 1. Fondateur (niveau 100) - Toutes les permissions
        Permissions foundatorPerm = new Permissions(true, true, true, true, Double.MAX_VALUE);
        Grade foundator = new Grade(UUID.randomUUID().toString(), "Fondateur", 100, foundatorPerm);
        c.getGrades().add(foundator);
        
        // 2. Co-Fondateur (niveau 80) - Presque toutes les permissions
        Permissions coFoundatorPerm = new Permissions(true, true, true, true, 100000.0);
        Grade coFoundator = new Grade(UUID.randomUUID().toString(), "Co-Fondateur", 80, coFoundatorPerm);
        c.getGrades().add(coFoundator);
        
        // 3. Chef (niveau 50) - Peut recruter et gérer les employés
        Permissions chefPerm = new Permissions(true, false, true, true, 50000.0);
        Grade chef = new Grade(UUID.randomUUID().toString(), "Chef", 50, chefPerm);
        c.getGrades().add(chef);
        
        // 4. Employé (niveau 10) - Permissions basiques
        Permissions employeePerm = new Permissions(false, false, false, false, 10000.0);
        Grade employee = new Grade(UUID.randomUUID().toString(), "Employé", 10, employeePerm);
        c.getGrades().add(employee);
        
        // Le créateur devient Fondateur
        c.getEmployees().add(new EmployeeRecord(ownerUuid, ownerName, foundator.getId()));
        idToCompany.put(id, c);
        broadcastState();
        return c;
    }

    public synchronized NBTTagCompound toNetworkTag()
    {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList companiesTag = new NBTTagList();
        Map<String, Set<String>> memberships = new LinkedHashMap<>();

        for(Company company : idToCompany.values())
        {
            companiesTag.appendTag(company.toTag());
            registerMembership(memberships, company.getOwnerUuid(), company.getId());
            for(EmployeeRecord employee : company.getEmployees())
            {
                registerMembership(memberships, employee.getPlayerUuid(), company.getId());
            }
        }

        tag.setTag("companies", companiesTag);

        NBTTagList membershipTag = new NBTTagList();
        for(Map.Entry<String, Set<String>> entry : memberships.entrySet())
        {
            NBTTagCompound membership = new NBTTagCompound();
            membership.setString("player", entry.getKey());

            NBTTagList ids = new NBTTagList();
            for(String companyId : entry.getValue())
            {
                NBTTagCompound idTag = new NBTTagCompound();
                idTag.setString("id", companyId);
                ids.appendTag(idTag);
            }

            membership.setTag("ids", ids);
            membershipTag.appendTag(membership);
        }

        tag.setTag("memberships", membershipTag);
        return tag;
    }

    private void registerMembership(Map<String, Set<String>> memberships, String playerUuid, String companyId)
    {
        if(playerUuid == null || playerUuid.isEmpty() || companyId == null)
        {
            return;
        }
        memberships.computeIfAbsent(playerUuid, k -> new LinkedHashSet<>()).add(companyId);
    }

    public synchronized void save(NBTTagCompound tag)
    {
        NBTTagCompound data = toNetworkTag();
        NBTTagList companiesTag = data.getTagList("companies", Constants.NBT.TAG_COMPOUND);
        NBTTagList membershipsTag = data.getTagList("memberships", Constants.NBT.TAG_COMPOUND);
        
        tag.setTag("companies", companiesTag);
        tag.setTag("memberships", membershipsTag);
    }

    public synchronized void load(NBTTagCompound tag)
    {
        idToCompany.clear();
        
        NBTTagList list = tag.getTagList("companies", Constants.NBT.TAG_COMPOUND);
        
        for(int i=0;i<list.tagCount();i++)
        {
            try {
                Company c = Company.fromTag(list.getCompoundTagAt(i));
                idToCompany.put(c.getId(), c);
            } catch(Exception e) {
                System.err.println("[ERROR] BusinessData.load() - Erreur lors du chargement de l'entreprise #" + i + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // memberships conservées pour compatibilité future
        tag.removeTag("memberships");
    }

    public synchronized void broadcastState()
    {
        NBTTagCompound tag = toNetworkTag();
        MessageSyncBusiness packet = new MessageSyncBusiness(tag);
        List<EntityPlayerMP> players = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
        for(EntityPlayerMP player : players)
        {
            PacketHandler.INSTANCE.sendTo(packet, player);
        }
    }
}


