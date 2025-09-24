package fr.onitsag.faritech.programs.business.service;

import fr.onitsag.faritech.programs.business.data.BusinessData;
import fr.onitsag.faritech.programs.business.model.BizTransaction;
import fr.onitsag.faritech.programs.business.model.Company;
import fr.onitsag.faritech.programs.business.model.EmployeeRecord;
import fr.onitsag.faritech.programs.business.model.Grade;
import fr.onitsag.faritech.programs.business.model.Permissions;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Dépôt client pour les données Business.
 */
public class BusinessRepository
{
    private static final BusinessRepository INSTANCE = new BusinessRepository();

    private final Map<String, Company> companiesById = new LinkedHashMap<>();
    private final Map<String, Set<String>> membershipsByPlayer = new LinkedHashMap<>();

    public static BusinessRepository get()
    {
        return INSTANCE;
    }

    private BusinessRepository() {}

    public String getCurrentPlayerUuid()
    {
        return Minecraft.getMinecraft().player.getUniqueID().toString();
    }

    public String getCurrentPlayerName()
    {
        return Minecraft.getMinecraft().player.getName();
    }

    public List<Company> listCompaniesForPlayer(String playerUuid)
    {
        Set<String> ids = membershipsByPlayer.getOrDefault(playerUuid, Collections.emptySet());
        List<Company> result = new ArrayList<>();
        for(String id : ids)
        {
            Company company = companiesById.get(id);
            if(company != null)
            {
                result.add(company);
            }
        }
        return result;
    }

    public Collection<Company> listAll()
    {
        return Collections.unmodifiableCollection(companiesById.values());
    }

    public Optional<Company> getCompany(String companyId)
    {
        return Optional.ofNullable(companiesById.get(companyId));
    }

    public Optional<Company> findCompanyByName(String name)
    {
        for(Company company : companiesById.values())
        {
            if(company.getName().equalsIgnoreCase(name))
            {
                return Optional.of(company);
            }
        }
        return Optional.empty();
    }

    public Company createCompany(String name)
    {
        String id = UUID.randomUUID().toString();
        String ownerUuid = getCurrentPlayerUuid();
        Company company = new Company(id, name, ownerUuid);

        // Créer la hiérarchie de grades par défaut (identique au serveur)
        // 1. Fondateur (niveau 100) - Toutes les permissions
        Permissions foundatorPerm = new Permissions(true, true, true, true, Double.MAX_VALUE);
        Grade foundator = new Grade(UUID.randomUUID().toString(), "Fondateur", 100, foundatorPerm);
        company.getGrades().add(foundator);
        
        // 2. Co-Fondateur (niveau 80) - Presque toutes les permissions
        Permissions coFoundatorPerm = new Permissions(true, true, true, true, 100000.0);
        Grade coFoundator = new Grade(UUID.randomUUID().toString(), "Co-Fondateur", 80, coFoundatorPerm);
        company.getGrades().add(coFoundator);
        
        // 3. Chef (niveau 50) - Peut recruter et gérer les employés
        Permissions chefPerm = new Permissions(true, false, true, true, 50000.0);
        Grade chef = new Grade(UUID.randomUUID().toString(), "Chef", 50, chefPerm);
        company.getGrades().add(chef);
        
        // 4. Employé (niveau 10) - Permissions basiques
        Permissions employeePerm = new Permissions(false, false, false, false, 10000.0);
        Grade employee = new Grade(UUID.randomUUID().toString(), "Employé", 10, employeePerm);
        company.getGrades().add(employee);
        
        // Le créateur devient Fondateur
        company.getEmployees().add(new EmployeeRecord(ownerUuid, getCurrentPlayerName(), foundator.getId()));

        companiesById.put(id, company);
        membershipsByPlayer.computeIfAbsent(ownerUuid, key -> new LinkedHashSet<>()).add(id);
        return company;
    }

    public void addEmployee(String companyId, String playerUuid, String playerName, String gradeId)
    {
        Company company = companiesById.get(companyId);
        if(company == null)
        {
            company = BusinessData.INSTANCE.getCompany(companyId);
            if(company == null)
            {
                return;
            }
            companiesById.put(companyId, company);
        }

        for(EmployeeRecord record : company.getEmployees())
        {
            if(record.getPlayerUuid().equals(playerUuid))
            {
                return;
            }
        }

        company.getEmployees().add(new EmployeeRecord(playerUuid, playerName, gradeId));
        membershipsByPlayer.computeIfAbsent(playerUuid, key -> new LinkedHashSet<>()).add(companyId);
    }

    public void fireEmployee(String companyId, String playerUuid)
    {
        Company company = companiesById.get(companyId);
        if(company == null)
        {
            return;
        }

        company.getEmployees().removeIf(record -> record.getPlayerUuid().equals(playerUuid));
        Set<String> set = membershipsByPlayer.get(playerUuid);
        if(set != null)
        {
            set.remove(companyId);
        }
    }

    public void changeEmployeeGrade(String companyId, String playerUuid, String newGradeId)
    {
        Company company = companiesById.get(companyId);
        if(company == null)
        {
            return;
        }

        company.getEmployees().stream()
            .filter(record -> record.getPlayerUuid().equals(playerUuid))
            .findFirst()
            .ifPresent(record -> record.setGradeId(newGradeId));
    }

    public String addGrade(String companyId, String name, int level, Permissions permissions)
    {
        Company company = companiesById.get(companyId);
        if(company == null)
        {
            return null;
        }

        String id = UUID.randomUUID().toString();
        company.getGrades().add(new Grade(id, name, level, permissions));
        return id;
    }

    public void removeGrade(String companyId, String gradeId)
    {
        Company company = companiesById.get(companyId);
        if(company == null)
        {
            return;
        }

        company.getGrades().removeIf(grade -> grade.getId().equals(gradeId));
    }

    public boolean transfer(String fromId, String toId, double amount, String description)
    {
        if(amount <= 0)
        {
            return false;
        }

        Company from = companiesById.get(fromId);
        Company to = companiesById.get(toId);
        if(from == null || to == null)
        {
            return false;
        }

        if(from.getBalance() < amount)
        {
            return false;
        }

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        long now = System.currentTimeMillis();
        from.getTransactions().add(new BizTransaction(UUID.randomUUID().toString(), now, fromId, toId, amount, BizTransaction.Type.TRANSFER_OUT, description));
        to.getTransactions().add(new BizTransaction(UUID.randomUUID().toString(), now, fromId, toId, amount, BizTransaction.Type.TRANSFER_IN, description));
        return true;
    }

    public NBTTagCompound saveToTag()
    {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList companies = new NBTTagList();
        for(Company company : companiesById.values())
        {
            companies.appendTag(company.toTag());
        }
        tag.setTag("companies", companies);

        NBTTagList membershipList = new NBTTagList();
        for(Map.Entry<String, Set<String>> entry : membershipsByPlayer.entrySet())
        {
            NBTTagCompound membershipTag = new NBTTagCompound();
            membershipTag.setString("player", entry.getKey());

            NBTTagList ids = new NBTTagList();
            for(String id : entry.getValue())
            {
                NBTTagCompound idTag = new NBTTagCompound();
                idTag.setString("id", id);
                ids.appendTag(idTag);
            }

            membershipTag.setTag("ids", ids);
            membershipList.appendTag(membershipTag);
        }
        tag.setTag("memberships", membershipList);
        return tag;
    }

    public void loadFromTag(NBTTagCompound tag)
    {
        companiesById.clear();
        membershipsByPlayer.clear();

        NBTTagList companies = tag.getTagList("companies", Constants.NBT.TAG_COMPOUND);
        for(int i = 0; i < companies.tagCount(); i++)
        {
            Company company = Company.fromTag(companies.getCompoundTagAt(i));
            companiesById.put(company.getId(), company);
        }

        NBTTagList memberships = tag.getTagList("memberships", Constants.NBT.TAG_COMPOUND);
        for(int i = 0; i < memberships.tagCount(); i++)
        {
            NBTTagCompound membershipTag = memberships.getCompoundTagAt(i);
            String player = membershipTag.getString("player");
            NBTTagList ids = membershipTag.getTagList("ids", Constants.NBT.TAG_COMPOUND);
            Set<String> companyIds = new LinkedHashSet<>();
            for(int j = 0; j < ids.tagCount(); j++)
            {
                companyIds.add(ids.getCompoundTagAt(j).getString("id"));
            }
            membershipsByPlayer.put(player, companyIds);
        }
    }

    public void updateFromNetwork(NBTTagCompound tag)
    {
        loadFromTag(tag);
    }
}


