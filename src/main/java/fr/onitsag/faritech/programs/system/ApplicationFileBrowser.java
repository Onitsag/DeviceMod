package fr.onitsag.faritech.programs.system;


import fr.onitsag.faritech.api.ApplicationManager;
import fr.onitsag.faritech.api.app.Icons;
import fr.onitsag.faritech.core.Laptop;
import fr.onitsag.faritech.core.io.FileSystem;
import fr.onitsag.faritech.object.AppInfo;
import fr.onitsag.faritech.object.TrayItem;
import fr.onitsag.faritech.programs.system.component.FileBrowser;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

public class ApplicationFileBrowser extends SystemApplication
{
	private FileBrowser browser;
	
	public ApplicationFileBrowser()
	{
		this.setDefaultWidth(211);
		this.setDefaultHeight(145);
	}

	@Override
	public void init(@Nullable NBTTagCompound intent)
	{
		browser = new FileBrowser(0, 0, this, FileBrowser.Mode.FULL);
		browser.openFolder(FileSystem.DIR_HOME);
		this.addComponent(browser);
	}

	@Override
	public void load(NBTTagCompound tagCompound)
	{

	}

	@Override
	public void save(NBTTagCompound tagCompound) 
	{
		
	}

	public static class FileBrowserTrayItem extends TrayItem
	{
		public FileBrowserTrayItem()
		{
			super(Icons.FOLDER);
		}

		@Override
		public void handleClick(int mouseX, int mouseY, int mouseButton)
		{
			AppInfo info = ApplicationManager.getApplication("faritech:file_browser");
			if(info != null)
			{
				Laptop.getSystem().openApplication(info);
			}
		}
	}
}
