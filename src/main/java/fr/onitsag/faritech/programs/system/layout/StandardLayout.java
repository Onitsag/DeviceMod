package fr.onitsag.faritech.programs.system.layout;

import fr.onitsag.faritech.api.app.Application;
import fr.onitsag.faritech.api.app.IIcon;
import fr.onitsag.faritech.api.app.Icons;
import fr.onitsag.faritech.api.app.Layout;
import fr.onitsag.faritech.api.app.component.*;
import fr.onitsag.faritech.api.app.listener.ClickListener;
import fr.onitsag.faritech.core.Laptop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import javax.annotation.Nullable;
import java.awt.Color;

/**
 * Author: MrCrayfish
 */
public class StandardLayout extends Layout
{
    private String title;
    protected Application app;
    private Layout previous;
    private IIcon icon;

    public StandardLayout(String title, int width, int height, Application app, @Nullable Layout previous)
    {
        super(width, height);
        this.title = title;
        this.app = app;
        this.previous = previous;
    }

    @Override
    public void init()
    {
        if(previous != null)
        {
            Button btnBack = new Button(2, 2, Icons.ARROW_LEFT);
            btnBack.setClickListener((mouseX, mouseY, mouseButton) ->
            {
                if(mouseButton == 0)
                {
                    app.setCurrentLayout(previous);
                }
            });
            this.addComponent(btnBack);
        }
    }

    @Override
    public void render(Laptop laptop, Minecraft mc, int x, int y, int mouseX, int mouseY, boolean windowActive, float partialTicks)
    {
        Color color = new Color(Laptop.getSystem().getSettings().getColorScheme().getHeaderColor());
        Gui.drawRect(x, y, x + width, y + 20, color.getRGB());
        Gui.drawRect(x, y + 20, x + width, y + 21, color.darker().getRGB());

        if(previous == null && icon != null)
        {
            icon.draw(mc, x + 5, y + 5);
        }
        mc.fontRenderer.drawString(title, x + 5 + (previous != null || icon != null ? 16 : 0), y + 7, Color.WHITE.getRGB(), true);

        super.render(laptop, mc, x, y, mouseX, mouseY, windowActive, partialTicks);
    }

    public void setIcon(IIcon icon)
    {
        this.icon = icon;
    }
}
