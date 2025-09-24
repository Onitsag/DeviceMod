package fr.onitsag.faritech.programs.business;

import fr.onitsag.faritech.api.app.Application;
import fr.onitsag.faritech.api.app.Icons;
import fr.onitsag.faritech.api.app.component.Button;
import fr.onitsag.faritech.programs.business.model.Company;
import fr.onitsag.faritech.programs.business.service.BusinessRepository;
import fr.onitsag.faritech.programs.business.task.TaskBusinessAction;
import fr.onitsag.faritech.api.task.TaskManager;
import net.minecraft.nbt.NBTTagCompound;
import fr.onitsag.faritech.programs.business.ui.CompanyLayout;
import fr.onitsag.faritech.programs.business.ui.MainCompanyListLayout;

import javax.annotation.Nullable;

/**
 * Application de gestion d'entreprise pour FariTech
 * Permet de gérer employés, salaires, stock et comptabilité
 * 
 * @author FariTech
 */
public class ApplicationBusinessManager extends Application
{
    private final BusinessRepository repo = BusinessRepository.get();
    private MainCompanyListLayout mainLayout;

    @Override
    public void init(@Nullable NBTTagCompound intent)
    {
        // Auth auto par pseudo: rien à saisir, on lit directement le joueur courant via repo
        mainLayout = new MainCompanyListLayout(this, repo);
        setCurrentLayout(mainLayout);
    }

    public void returnToMainMenu() { setCurrentLayout(mainLayout); }

    public void openCompany(String companyId)
    {
        setCurrentLayout(new CompanyLayout(this, repo, companyId));
    }

    public void requestCreateCompany(String name)
    {
        NBTTagCompound d = new NBTTagCompound();
        d.setString("name", name);
        TaskManager.sendTask(new TaskBusinessAction().op("create_company", d).setCallback((nbt, success) -> {
            if (success) {
                // Recharger l'écran principal pour afficher la nouvelle entreprise
                returnToMainMenu();
                if(mainLayout != null) mainLayout.refresh();
                markForLayoutUpdate();
            }
        }));
    }

    public void onBusinessDataSynced()
    {
        if(mainLayout != null) mainLayout.refresh();

        // Si on est dans une page d'entreprise, vérifier si le joueur fait toujours partie de cette entreprise
        if(getCurrentLayout() instanceof CompanyLayout)
        {
            CompanyLayout companyLayout = (CompanyLayout) getCurrentLayout();
            String currentCompanyId = companyLayout.getCompanyId();
            String currentPlayerUuid = repo.getCurrentPlayerUuid();
            
            // Vérifier si le joueur fait encore partie de cette entreprise
            Company company = repo.getCompany(currentCompanyId).orElse(null);
            boolean stillMember = company != null && 
                (company.getOwnerUuid().equals(currentPlayerUuid) || 
                 company.getEmployees().stream().anyMatch(e -> e.getPlayerUuid().equals(currentPlayerUuid)));
            
            if(!stillMember)
            {
                // Le joueur n'est plus dans cette entreprise, le rediriger vers la page principale
                returnToMainMenu();
            }
            else
            {
                // Le joueur est encore dans l'entreprise, rafraîchir la page
                companyLayout.refresh();
            }
        }

        markForLayoutUpdate();
    }

    @Override
    public void load(NBTTagCompound tag)
    {
        // Charger les données sauvegardées si nécessaire
    }

    @Override
    public void save(NBTTagCompound tag)
    {
        // Sauvegarder les données si nécessaire
    }
}
