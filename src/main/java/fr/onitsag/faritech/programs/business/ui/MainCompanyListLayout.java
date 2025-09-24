package fr.onitsag.faritech.programs.business.ui;

import fr.onitsag.faritech.api.app.Icons;
import fr.onitsag.faritech.api.app.Layout;
import fr.onitsag.faritech.api.app.component.Button;
import fr.onitsag.faritech.api.app.component.Label;
import fr.onitsag.faritech.api.app.component.TextField;
import fr.onitsag.faritech.programs.business.ApplicationBusinessManager;
import fr.onitsag.faritech.programs.business.model.Company;
import fr.onitsag.faritech.programs.business.service.BusinessRepository;

public class MainCompanyListLayout extends Layout
{
    private final ApplicationBusinessManager app;
    private final BusinessRepository repo;

    private TextField newCompanyName;
    private Button createBtn;

    public MainCompanyListLayout(ApplicationBusinessManager app, BusinessRepository repo)
    {
        super(362, 164);
        this.app = app;
        this.repo = repo;
    }

    @Override
    public void init()
    {
        rebuild();
    }

    private void rebuild()
    {
        clear();

        // Header ultra-compact
        String playerName = repo.getCurrentPlayerName();
        addComponent(new Label("Connecté: " + playerName, 10, 8));
        

        // Liste des entreprises SANS ScrollableLayout - affichage direct
        addComponent(new Label("Vos entreprises:", 10, 24));

        int y = 36;
        int count = 0;
        for(Company company : repo.listCompaniesForPlayer(repo.getCurrentPlayerUuid())) {
            if(count >= 12) break; // Maximum 12 entreprises affichées
            
            // Bouton cliquable pour chaque entreprise
            Button companyBtn = new Button(12, y, 200, 14, "► " + company.getName());
            final String companyId = company.getId(); // Variable finale pour lambda
            companyBtn.setClickListener((mx,my,mb) -> {
                if(mb == 0) {
                    app.openCompany(companyId);
                }
            });
            addComponent(companyBtn);
            
            // Affichage du solde à côté
            addComponent(new Label(String.format("%.2f€", company.getBalance()), 220, y + 3));
            
            y += 16;
            count++;
        }

        // Zone de création compacte en bas
        addComponent(new Label("Créer:", 10, 140));
        newCompanyName = new TextField(48, 138, 160);
        newCompanyName.setPlaceholder("Nom entreprise");
        addComponent(newCompanyName);

        createBtn = new Button(212, 138, 50, 16, "Créer", Icons.PLUS);
        createBtn.setClickListener((mx,my,mb) ->
        {
            if(mb == 0)
            {
                String name = newCompanyName.getText().trim();
                if(!name.isEmpty())
                {
                    app.requestCreateCompany(name);
                    newCompanyName.setText("");
                }
            }
        });
        addComponent(createBtn);
    }

    public void refresh()
    {
        rebuild();
    }
}