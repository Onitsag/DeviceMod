package com.mrcrayfish.device.programs.police;

import com.mrcrayfish.device.api.app.Application;
import com.mrcrayfish.device.api.app.Icons;
import com.mrcrayfish.device.api.app.Layout;
import com.mrcrayfish.device.api.app.Dialog;
import com.mrcrayfish.device.api.app.ScrollableLayout;
import com.mrcrayfish.device.api.app.component.*;
import com.mrcrayfish.device.api.app.renderer.ListItemRenderer;
import com.mrcrayfish.device.core.Laptop;

import com.mrcrayfish.device.api.task.TaskManager;
import com.mrcrayfish.device.programs.police.task.TaskAddPoliceReport;
import com.mrcrayfish.device.programs.police.task.TaskDeletePoliceReport;
import com.mrcrayfish.device.programs.police.task.TaskRequestPoliceReports;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Calendar;
import java.util.List;

/**
 * Application « Police »
 * © MrCrayfish – exemple ré-agencé 2025-06
 */
public class ApplicationPolice extends Application
{
    /* --------------------------------------------------------------------- */
    /*  Constantes                                                           */
    /* --------------------------------------------------------------------- */
    private static final int WIDTH   = 260;
    private static final int HEIGHT  = 150;
    private static final int PAD     = 5;
    private static final int TAB_H   = 20;

    /* --------------------------------------------------------------------- */
    /*  États / données                                                      */
    /* --------------------------------------------------------------------- */
    private enum Tab { LIST, NEW, VIEW }
    private Tab currentTab = Tab.LIST;

    private ItemList<PoliceReport> list;
    private Label                  emptyLabel;
    private Label                  lblTitre;
    private Text                   txtContenu;

    /*  — champs onglet « Nouveau » — */
    private TextField tfSuspect;
    private TextField tfLieu;
    private TextField tfDateCache;
    private TextArea  taDetails;

    private int currentJour;
    private int currentAnnee;
    private TextField tfJour;
    private TextField tfAnnee;
    private ComboBox.List<String> selMois;

    /*  pour double-clic dans la liste  */
    private long lastClick   = 0;
    private int  lastIndex   = -1;

    public ApplicationPolice()
    {
        setDefaultWidth(WIDTH);
        setDefaultHeight(HEIGHT);
    }

    /* --------------------------------------------------------------------- */
    /*  Init                                                                  */
    /* --------------------------------------------------------------------- */
    @Override
    public void init(@Nullable NBTTagCompound intent)
    {
        TaskManager.sendTask(new TaskRequestPoliceReports());
        openTab(Tab.LIST);
    }

    /* --------------------------------------------------------------------- */
    /*  Changement d'onglet                                                  */
    /* --------------------------------------------------------------------- */
    private void openTab(Tab t)
    {
        currentTab = t;
        Layout lay = new Layout(WIDTH, HEIGHT);

        /* barre d'onglets */
        Button bList = new Button(PAD, PAD, 60, TAB_H, "Rapports");
        bList.setEnabled(t != Tab.LIST);
        bList.setClickListener((mx, my, mb) -> openTab(Tab.LIST));
        lay.addComponent(bList);

        Button bNew  = new Button(65 + PAD, PAD, 60, TAB_H, "Nouveau");
        bNew.setEnabled(t != Tab.NEW);
        bNew.setClickListener((mx, my, mb) -> openTab(Tab.NEW));
        lay.addComponent(bNew);

        switch (t)
        {
            case LIST: buildTabList(lay); break;
            case NEW : buildTabNew (lay); break;
            case VIEW: buildTabView(lay); break;
        }

        setCurrentLayout(lay);
    }

    /* --------------------------------------------------------------------- */
    /*  ONGLET 1 : Liste                                                     */
    /* --------------------------------------------------------------------- */
    private void buildTabList(Layout lay)
    {
        /* boutons action à droite */
        int yBtns = PAD;
        int btnSize = TAB_H;
        int xBtn  = WIDTH - PAD - btnSize;

        Button bRefresh = new Button(xBtn, yBtns, btnSize, btnSize, Icons.RELOAD);
        bRefresh.setToolTip("Actualiser", "Actualiser la liste des rapports");
        bRefresh.setClickListener((mx,my,mb)->reload());
        lay.addComponent(bRefresh);

        Button bDel = new Button(xBtn - (btnSize+1), yBtns, btnSize, btnSize, Icons.TRASH);
        bDel.setToolTip("Supprimer", "Supprimer le rapport sélectionné");
        bDel.setEnabled(false);
        lay.addComponent(bDel);

        Button bView = new Button(xBtn - 2*(btnSize+1), yBtns, btnSize, btnSize, Icons.EYE_DROPPER);
        bView.setToolTip("Voir", "Voir les détails du rapport");
        bView.setEnabled(false);
        lay.addComponent(bView);

        /* liste (3 éléments visibles) */
        list = new ItemList<>(PAD, PAD + TAB_H + 2, WIDTH - PAD*2, 3);
        list.setListItemRenderer(new PoliceReportRenderer());
        list.setItemClickListener((rep, idx, mb) -> {
            boolean ok = idx != -1;
            bView.setEnabled(ok);
            bDel .setEnabled(ok);

            /* double-clic -> vue directe */
            long now = java.lang.System.currentTimeMillis();
            if(ok && idx == lastIndex && now - lastClick < 350)
                showReport();
            lastClick = now;
            lastIndex = idx;
        });
        lay.addComponent(list);

        /* suppression confirmée */
        bDel.setClickListener((mx,my,mb)->{
            if(list.getSelectedIndex()==-1) return;
            Dialog.Confirmation d = new Dialog.Confirmation(
                    "Supprimer ce rapport ?");
            d.setPositiveText("Oui");
            d.setNegativeText("Non");
            d.setPositiveListener((x,y,b)->{
                if(b==0) {
                    TaskManager.sendTask(
                            new TaskDeletePoliceReport(list.getSelectedIndex()));
                    reload();
                }
            });
            openDialog(d);
        });

        /* vue */
        bView.setClickListener((mx,my,mb)->showReport());

        /* label "aucun" */
        emptyLabel = new Label("Aucun rapport", WIDTH/2-40,
                               PAD+TAB_H+35);
        emptyLabel.setTextColor(Color.LIGHT_GRAY);
        lay.addComponent(emptyLabel);

        reload();
    }

    private void reload()
    {
        list.removeAll();
        List<PoliceReport> data = PoliceReportManager.INSTANCE.getReports();
        if(data.isEmpty())
        {
            emptyLabel.setVisible(true);
        }
        else
        {
            emptyLabel.setVisible(false);
            data.forEach(list::addItem);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  ONGLET 2 : Nouveau                                                   */
    /* --------------------------------------------------------------------- */
    private void buildTabNew(Layout lay)
    {
        /* Création d'un conteneur scrollable */
        int scrollHeight = HEIGHT - (PAD + TAB_H + 2) - PAD;  // Plus besoin de soustraire la hauteur des boutons du bas
        ScrollableLayout scrollLayout = new ScrollableLayout(PAD, PAD + TAB_H + 2, WIDTH - PAD*2, 300, scrollHeight);
        scrollLayout.setScrollSpeed(5);
        lay.addComponent(scrollLayout);

        /* Boutons en haut à droite */
        int btnSize = TAB_H;
        int xBtn = WIDTH - PAD - btnSize;
        Button bSave = new Button(xBtn - (btnSize+1), PAD, btnSize, btnSize, Icons.SAVE);
        bSave.setToolTip("Enregistrer", "Enregistrer le rapport");
        bSave.setClickListener((mx,my,mb)->saveReport(false));
        lay.addComponent(bSave);

        Button bCancel = new Button(xBtn, PAD, btnSize, btnSize, Icons.CROSS);
        bCancel.setToolTip("Annuler", "Annuler la création du rapport");
        bCancel.setClickListener((mx,my,mb)->openTab(Tab.LIST));
        lay.addComponent(bCancel);

        int y = 0;

        /* champ suspect */
        tfSuspect = new TextField(0, y, WIDTH-PAD*2);
        tfSuspect.setIcon(Icons.USER);
        tfSuspect.setPlaceholder("Nom du suspect");
        scrollLayout.addComponent(tfSuspect);
        y += 22;

        /* champ lieu */
        tfLieu = new TextField(0, y, WIDTH-PAD*2);
        tfLieu.setIcon(Icons.LOCATION);
        tfLieu.setPlaceholder("Lieu");
        scrollLayout.addComponent(tfLieu);
        y += 22;

        /* champ date "caché" pour stockage final */
        tfDateCache = new TextField(0,0,0);
        tfDateCache.setVisible(false);
        scrollLayout.addComponent(tfDateCache);

        /* ligne date (jour / mois / an) */
        final int wJour  = 24;
        final int wAnnee = 38;
        final int hChamp = 14; // Hauteur standard de la ComboBox
        Calendar cal = Calendar.getInstance();

        int xDate = 0;
        int yDate = y;

        // Icône calendrier
        Image imgIconDate = new Image(xDate, yDate+1, 12, 12, Icons.CLOCK);
        scrollLayout.addComponent(imgIconDate);
        xDate += 18;

        // Jour
        currentJour = cal.get(Calendar.DAY_OF_MONTH);
        tfJour = new TextField(xDate+16, yDate, wJour);
        tfJour.setText(String.format("%02d", currentJour));
        tfJour.setEnabled(false);
        scrollLayout.addComponent(tfJour);

        Button btnJourLeft = new Button(xDate, yDate, Icons.CHEVRON_LEFT);
        btnJourLeft.setSize(16, hChamp);
        btnJourLeft.setClickListener((mx,my,mb) -> {
            if(mb == 0 && currentJour > 1) {
                currentJour--;
                tfJour.setText(String.format("%02d", currentJour));
            }
        });
        scrollLayout.addComponent(btnJourLeft);

        Button btnJourRight = new Button(xDate+16+wJour, yDate, Icons.CHEVRON_RIGHT);
        btnJourRight.setSize(16, hChamp);
        btnJourRight.setClickListener((mx,my,mb) -> {
            if(mb == 0 && currentJour < 31) {
                currentJour++;
                tfJour.setText(String.format("%02d", currentJour));
            }
        });
        scrollLayout.addComponent(btnJourRight);

        xDate += 16 + wJour + 16 + 6; // flèche + champ + flèche + espace

        // Mois
        String[] moisItems = {"Janv.","Févr.","Mars","Avr.","Mai","Juin",
                           "Juil.","Août","Sept.","Oct.","Nov.","Déc."};
        // Calcule la largeur maximale du texte
        int maxWidth = 0;
        for(String mois : moisItems) {
            int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(mois);
            if(width > maxWidth) maxWidth = width;
        }
        int dropdownWidth = maxWidth + 20; // Ajoute un peu d'espace pour les marges

        selMois = new ComboBox.List<>(xDate, yDate, 70, dropdownWidth, moisItems);
        selMois.setListItemRenderer(new ListItemRenderer<String>(hChamp) {
            @Override
            public void render(String month, Gui gui, Minecraft mc, int x, int y, int width, int height, boolean selected) {
                Color bgColor = new Color(Laptop.getSystem().getSettings().getColorScheme().getBackgroundColor());
                gui.drawRect(x, y, x + width, y + height, selected ? bgColor.brighter().brighter().getRGB() : bgColor.brighter().getRGB());
                mc.fontRenderer.drawString(month, x + 3, y + (height - 8) / 2, Color.WHITE.getRGB(), true);
            }
        });
        selMois.setSelectedItem(cal.get(Calendar.MONTH));
        scrollLayout.addComponent(selMois);
        xDate += 70 + 8;

        // Année
        currentAnnee = cal.get(Calendar.YEAR);
        tfAnnee = new TextField(xDate+16, yDate, wAnnee);
        tfAnnee.setText(String.valueOf(currentAnnee));
        tfAnnee.setEnabled(false);
        scrollLayout.addComponent(tfAnnee);

        Button btnAnneeLeft = new Button(xDate, yDate, Icons.CHEVRON_LEFT);
        btnAnneeLeft.setSize(16, hChamp);
        btnAnneeLeft.setClickListener((mx,my,mb) -> {
            if(mb == 0 && currentAnnee > 2000) {
                currentAnnee--;
                tfAnnee.setText(String.valueOf(currentAnnee));
            }
        });
        scrollLayout.addComponent(btnAnneeLeft);

        Button btnAnneeRight = new Button(xDate+16+wAnnee, yDate, Icons.CHEVRON_RIGHT);
        btnAnneeRight.setSize(16, hChamp);
        btnAnneeRight.setClickListener((mx,my,mb) -> {
            if(mb == 0 && currentAnnee < 2100) {
                currentAnnee++;
                tfAnnee.setText(String.valueOf(currentAnnee));
            }
        });
        scrollLayout.addComponent(btnAnneeRight);

        y += 22;

        /* zone détails */
        taDetails = new TextArea(0, y, WIDTH-PAD*2, 100);
        taDetails.setPlaceholder("Détails du rapport");
        taDetails.setWrapText(true);
        taDetails.setScrollBarSize(5);
        scrollLayout.addComponent(taDetails);

        tfSuspect.setFocused(true);
    }

    private void saveReport(boolean avecHeure)
    {
        /* construction de la date */
        String[] moisArray = {"Janv.","Févr.","Mars","Avr.","Mai","Juin","Juil.","Août","Sept.","Oct.","Nov.","Déc."};
        int moisIndex = 0;
        String moisSelected = selMois.getSelectedItem();
        for(int i=0; i<moisArray.length; i++) {
            if(moisArray[i].equals(moisSelected)) {
                moisIndex = i;
                break;
            }
        }
        String date = String.format("%02d/%02d/%d",
                currentJour,
                moisIndex+1,
                currentAnnee);

        tfDateCache.setText(date);

        /* vérif champs */
        if(tfSuspect.getText().isEmpty() ||
           tfLieu.getText().isEmpty() ||
           tfDateCache.getText().isEmpty() ||
           taDetails.getText().isEmpty())
        {
            openDialog(new Dialog.Message("Veuillez remplir tous les champs"));
            return;
        }

        PoliceReport pr = new PoliceReport(
                tfSuspect.getText(),
                tfLieu.getText(),
                tfDateCache.getText(),
                taDetails.getText());

        TaskManager.sendTask(new TaskAddPoliceReport(pr));
        openTab(Tab.LIST);
    }

    /* --------------------------------------------------------------------- */
    /*  ONGLET 3 : Vue                                                       */
    /* --------------------------------------------------------------------- */
    private void buildTabView(Layout lay)
    {
        final int yTop = PAD + TAB_H + 2;

        /* fond sombre derrière le texte */
        lay.setBackground((g,mc,x,y0,w,h,mx,my,a) ->
                g.drawRect(x, y0 + yTop - 4, x + w, y0 + h, 0xFF333333));

        lblTitre = new Label("", PAD, yTop);
        lblTitre.setTextColor(new Color(255,170,0));
        lay.addComponent(lblTitre);

        txtContenu = new Text("", PAD, yTop+18, WIDTH-PAD*2);
        txtContenu.setShadow(false);
        lay.addComponent(txtContenu);

        /* bouton retour en haut à gauche */
        int btnSize = TAB_H;
        Button back = new Button(PAD, PAD, btnSize, btnSize, Icons.ARROW_LEFT);
        back.setToolTip("Retour", "Retourner à la liste des rapports");
        back.setClickListener((mx,my,mb)->openTab(Tab.LIST));
        lay.addComponent(back);

        // On définit le layout actuel
        setCurrentLayout(lay);
        currentTab = Tab.VIEW;
    }

    private void showReport()
    {
        if(list == null) return;
        
        PoliceReport pr = list.getSelectedItem();
        if(pr == null) return;
        
        // Vérifie que tous les champs du rapport sont non-null
        if(pr.getSuspectName() == null || pr.getDate() == null || 
           pr.getLocation() == null || pr.getDetails() == null) {
            openDialog(new Dialog.Message("Erreur : rapport corrompu"));
            return;
        }

        // On crée d'abord le layout
        Layout lay = new Layout(WIDTH, HEIGHT);
        
        // On met à jour le contenu avant de créer les composants
        String titre = pr.getSuspectName();
        String contenu = "Date : "+pr.getDate()+"\n"+
                        "Lieu : "+pr.getLocation()+"\n\n"+
                        "Détails :\n"+pr.getDetails();
        
        // On crée les composants avec le contenu
        final int yTop = PAD + TAB_H + 2;
        
        /* fond sombre derrière le texte */
        lay.setBackground((g,mc,x,y0,w,h,mx,my,a) ->
                g.drawRect(x, y0 + yTop - 4, x + w, y0 + h, 0xFF333333));

        lblTitre = new Label(titre, PAD, yTop);
        lblTitre.setTextColor(new Color(255,170,0));
        lay.addComponent(lblTitre);

        txtContenu = new Text(contenu, PAD, yTop+18, WIDTH-PAD*2);
        txtContenu.setShadow(false);
        lay.addComponent(txtContenu);

        /* bouton retour en haut à gauche */
        int btnSize = TAB_H;
        Button back = new Button(PAD, PAD, btnSize, btnSize, Icons.ARROW_LEFT);
        back.setToolTip("Retour", "Retourner à la liste des rapports");
        back.setClickListener((mx,my,mb)->openTab(Tab.LIST));
        lay.addComponent(back);

        // On définit le layout actuel
        setCurrentLayout(lay);
        currentTab = Tab.VIEW;
    }

    /* --------------------------------------------------------------------- */
    /*  Exigences d'API                                                      */
    /* --------------------------------------------------------------------- */
    @Override public void load(NBTTagCompound tag) { }
    @Override public void save(NBTTagCompound tag) { }

    /** Méthode appelée par MessageSyncPoliceReports */
    public void loadReports()
    {
        reload();
    }
}
