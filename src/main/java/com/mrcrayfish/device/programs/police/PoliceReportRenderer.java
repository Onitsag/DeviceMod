package com.mrcrayfish.device.programs.police;

import com.mrcrayfish.device.api.app.renderer.ListItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

public class PoliceReportRenderer extends ListItemRenderer<PoliceReport> {
    public PoliceReportRenderer() {
        super(35); // Réduit la hauteur de chaque élément
    }

    @Override
    public void render(PoliceReport report, Gui gui, Minecraft mc, int x, int y, int width, int height, boolean selected) {
        // Fond de l'élément
        if (selected) {
            gui.drawRect(x, y, x + width, y + height, 0xFF555555);
        } else {
            gui.drawRect(x, y, x + width, y + height, 0xFF333333);
        }

        // Nom du suspect
        mc.fontRenderer.drawString(report.getSuspectName(), x + 5, y + 5, 0xFFFFFF);

        // Date et lieu sur la même ligne
        mc.fontRenderer.drawString(report.getDate() + " - " + report.getLocation(), x + 5, y + 20, 0xCCCCCC);
    }
} 