package com.mrcrayfish.device.programs.police;

import com.mrcrayfish.device.api.app.Application;
import com.mrcrayfish.device.api.app.Icons;
import com.mrcrayfish.device.api.app.Layout;
import com.mrcrayfish.device.api.app.component.*;
import com.mrcrayfish.device.api.app.Dialog;
import com.mrcrayfish.device.core.io.FileSystem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import com.mrcrayfish.device.api.task.TaskManager;
import com.mrcrayfish.device.programs.police.task.TaskAddPoliceReport;
import com.mrcrayfish.device.programs.police.task.TaskDeletePoliceReport;
import com.mrcrayfish.device.programs.police.task.TaskRequestPoliceReports;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ApplicationPolice extends Application
{
    /* Dimensions constantes */
    private static final int WIDTH          = 260;
    private static final int HEIGHT         = 150;
    private static final int PADDING        = 5;
    private static final int HEADER_HEIGHT  = 25;

    /* États & données */
    private enum Tab { LIST, NEW, VIEW }
    private Tab currentTab = Tab.LIST;
    private final List<PoliceReport> reports = new ArrayList<>();

    /* Composants communs (ré-utilisés) */
    private ItemList<PoliceReport> reportList;
    private Label                  emptyLabel;
    private TextField              suspectField, locationField, dateField;
    private TextArea               detailsField;
    private Label                  reportTitle;
    private Text                   reportContent;

    private String tempTitle = "";
    private String tempContent = "";

    /* ----- Construction de l'appli ----- */

    public ApplicationPolice()
    {
        this.setDefaultWidth(WIDTH);
        this.setDefaultHeight(HEIGHT);
    }

    @Override
    public void init(@Nullable NBTTagCompound intent)
    {
        com.mrcrayfish.device.api.task.TaskManager.sendTask(new TaskRequestPoliceReports());
        setTab(Tab.LIST);
    }

    public void loadReports()
    {
        reportList.removeAll();
        if (PoliceReportManager.INSTANCE.getReports().isEmpty())
        {
            emptyLabel.setVisible(true);
        }
        else
        {
            emptyLabel.setVisible(false);
            for (PoliceReport r : PoliceReportManager.INSTANCE.getReports())
            {
                reportList.addItem(r);
            }
        }
    }

    /* ----- Changement d'onglet ----- */

    private void setTab(Tab tab)
    {
        this.currentTab = tab;
        Layout layout = new Layout(WIDTH, HEIGHT);

        /* Barre d'onglets (toujours en premier pour rester au-dessus) */
        Button tabList = new Button(PADDING, PADDING, 60, 16, "Rapports");
        tabList.setEnabled(tab != Tab.LIST);
        tabList.setClickListener((x, y, btn) -> setTab(Tab.LIST));
        layout.addComponent(tabList);

        Button tabNew = new Button(70, PADDING, 60, 16, "Nouveau");
        tabNew.setEnabled(tab != Tab.NEW);
        tabNew.setClickListener((x, y, btn) -> setTab(Tab.NEW));
        layout.addComponent(tabNew);

        /* Contenu spécifique */
        switch (tab)
        {
            case LIST:
                buildListLayout(layout);
                loadReports();
                break;

            case NEW:
                buildNewLayout(layout);
                suspectField.setFocused(true);               // focus initial
                break;

            case VIEW:
                buildViewLayout(layout);
                break;
        }

        setCurrentLayout(layout);
    }

    /* ------------------------------------------------------------------
     * Onglet 1 : Liste des rapports
     * ------------------------------------------------------------------ */
    private void buildListLayout(Layout layout)
    {
        int buttonY = HEADER_HEIGHT + PADDING;

        /* Boutons action */
        Button viewBtn = new Button(WIDTH - 60, buttonY, 18, 18, Icons.EYE_DROPPER);
        viewBtn.setToolTip("Voir", "Ouvrir le rapport sélectionné");
        viewBtn.setEnabled(false);
        viewBtn.setClickListener((x, y, b) ->
        {
            if (reportList.getSelectedIndex() != -1) showViewReport();
        });
        layout.addComponent(viewBtn);

        Button deleteBtn = new Button(WIDTH - 39, buttonY, 18, 18, Icons.TRASH);
        deleteBtn.setToolTip("Supprimer", "Effacer le rapport sélectionné");
        deleteBtn.setEnabled(false);
        deleteBtn.setClickListener((x, y, b) ->
        {
            if (reportList.getSelectedIndex() != -1) confirmDelete();
        });
        layout.addComponent(deleteBtn);

        Button refreshBtn = new Button(WIDTH - 18, buttonY, 18, 18, Icons.RELOAD);
        refreshBtn.setToolTip("Actualiser", "Recharger la liste");
        refreshBtn.setClickListener((x, y, b) -> loadReports());
        layout.addComponent(refreshBtn);

        /* Liste */
        reportList = new ItemList<>(PADDING, buttonY + 25, WIDTH - PADDING * 2, 2); // 2 × 35 px =  70 px → tient dans 150 px
        reportList.setListItemRenderer(new PoliceReportRenderer());
        reportList.setItemClickListener((rep, idx, btn) ->
        {
            boolean hasSel = idx != -1;
            viewBtn.setEnabled(hasSel);
            deleteBtn.setEnabled(hasSel);
        });
        layout.addComponent(reportList);

        emptyLabel = new Label("Aucun rapport", WIDTH / 2 - 40, buttonY + 55);
        emptyLabel.setTextColor(Color.LIGHT_GRAY);
        layout.addComponent(emptyLabel);
    }

    /* ------------------------------------------------------------------
     * Onglet 2 : Nouveau rapport
     * ------------------------------------------------------------------ */
    private void buildNewLayout(Layout layout)
    {
        int y = HEADER_HEIGHT + PADDING;

        suspectField  = new TextField(PADDING, y,              WIDTH - PADDING * 2);               y += 25;
        suspectField.setPlaceholder("Nom du suspect");
        suspectField.setIcon(Icons.USER);
        layout.addComponent(suspectField);

        locationField = new TextField(PADDING, y,              WIDTH - PADDING * 2);               y += 25;
        locationField.setPlaceholder("Lieu");
        locationField.setIcon(Icons.LOCATION);
        layout.addComponent(locationField);

        dateField     = new TextField(PADDING, y,              WIDTH - PADDING * 2);               y += 25;
        dateField.setPlaceholder("Date");
        dateField.setIcon(Icons.CLOCK);
        layout.addComponent(dateField);

        detailsField  = new TextArea (PADDING, y,              WIDTH - PADDING * 2, 30);           y += 35;
        detailsField.setPlaceholder("Détails du rapport");
        detailsField.setScrollBarSize(5);
        detailsField.setWrapText(true);
        layout.addComponent(detailsField);

        /* Boutons bas */
        int buttonY = HEIGHT - PADDING - 18;
        Button saveBtn = new Button(WIDTH - 130, buttonY, 60, 18, "Enregistrer");
        saveBtn.setClickListener((x, y1, b) -> saveReport());
        layout.addComponent(saveBtn);

        Button cancelBtn = new Button(WIDTH - 65, buttonY, 60, 18, "Annuler");
        cancelBtn.setClickListener((x, y1, b) -> setTab(Tab.LIST));
        layout.addComponent(cancelBtn);
    }

    private void clearForm()
    {
        suspectField.setText("");
        locationField.setText("");
        dateField.setText("");
        detailsField.setText("");
    }

    private void saveReport()
    {
        String s = suspectField.getText();
        String l = locationField.getText();
        String d = dateField.getText();
        String t = detailsField.getText();

        if (s.isEmpty() || l.isEmpty() || d.isEmpty() || t.isEmpty())
        {
            openDialog(new Dialog.Message("Veuillez remplir tous les champs"));
            return;
        }

        PoliceReport report = new PoliceReport(s, l, d, t);
        TaskManager.sendTask(new TaskAddPoliceReport(report));
        setTab(Tab.LIST);
    }

    /* ------------------------------------------------------------------
     * Onglet 3 : Affichage d'un rapport
     * ------------------------------------------------------------------ */
    private void buildViewLayout(Layout layout)
    {
        int y = HEADER_HEIGHT + PADDING;

        // Ajout d'un fond pour le contenu
        layout.setBackground((gui, mc, x, yPos, width, height, mouseX, mouseY, windowActive) -> {
            gui.drawRect(x, yPos + y - 5, x + width, yPos + height, 0xFF333333);
        });

        reportTitle = new Label("", PADDING, y);
        reportTitle.setTextColor(new Color(255, 170, 0));
        layout.addComponent(reportTitle);

        reportContent = new Text("", PADDING, y + 25, WIDTH - PADDING * 2);
        reportContent.setShadow(false);
        layout.addComponent(reportContent);

        Button backBtn = new Button(PADDING, HEIGHT - PADDING - 18, 60, 18, "Retour");
        backBtn.setClickListener((x, yPos, b) -> setTab(Tab.LIST));
        layout.addComponent(backBtn);
    }

    private void showViewReport()
    {
        PoliceReport r = reportList.getSelectedItem();
        if (r != null)
        {
            try {
                tempTitle = r.getSuspectName();
                tempContent = String.format("Date : %s\nLieu : %s\n\nDétails :\n%s",
                        r.getDate(), r.getLocation(), r.getDetails());
                setTab(Tab.VIEW);
                reportTitle.setText(tempTitle);
                reportContent.setText(tempContent);
            } catch (Exception e) {
                openDialog(new Dialog.Message("Erreur lors de l'affichage du rapport"));
            }
        }
    }

    private void confirmDelete()
    {
        Dialog.Confirmation d = new Dialog.Confirmation("Voulez-vous vraiment supprimer ce rapport ?");
        d.setPositiveText("Oui");
        d.setNegativeText("Non");
        d.setPositiveListener((x, y, b) ->
        {
            if (b == 0) deleteSelectedReport();
        });
        openDialog(d);
    }

    private void deleteSelectedReport()
    {
        int i = reportList.getSelectedIndex();
        if (i != -1)
        {
            TaskManager.sendTask(new TaskDeletePoliceReport(i));
            loadReports();
        }
    }

    /* ------------------------------------------------------------------
     * Persistance
     * ------------------------------------------------------------------ */
    @Override
    public void load(NBTTagCompound tag)
    {
        // La persistance est maintenant gérée via PoliceReportManager
    }

    @Override
    public void save(NBTTagCompound tag)
    {
        // La persistance est maintenant gérée via PoliceReportManager
    }
}
