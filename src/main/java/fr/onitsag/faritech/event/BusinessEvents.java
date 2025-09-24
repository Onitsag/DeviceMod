package fr.onitsag.faritech.event;

import fr.onitsag.faritech.programs.business.data.BusinessData;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.IOException;

public class BusinessEvents
{
    private static final String FILE_NAME = "business.dat";

    @SubscribeEvent
    public void load(WorldEvent.Load event)
    {
        if(event.getWorld().provider.getDimension() == 0)
        {
            // Nettoyer les données existantes avant de charger le nouveau monde
            BusinessData.INSTANCE.clear();
            
            try
            {
                // Utiliser le dossier spécifique au monde pour séparer solo/multi
                File data = new File(event.getWorld().getSaveHandler().getWorldDirectory(), FILE_NAME);
                
                if(!data.exists())
                {
                    return;
                }

                NBTTagCompound nbt = CompressedStreamTools.read(data);
                if(nbt != null)
                {
                    BusinessData.INSTANCE.load(nbt);
                }
            }
            catch (IOException e)
            {
                System.err.println("[ERROR] BusinessEvents.load() - Erreur lors de la lecture: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public void save(WorldEvent.Save event)
    {
        if(event.getWorld().provider.getDimension() == 0)
        {
            // Sauvegarde asynchrone pour éviter de bloquer le thread principal
            Thread saveThread = new Thread(() -> {
                try
                {
                    // Utiliser le dossier spécifique au monde pour séparer solo/multi
                    File data = new File(event.getWorld().getSaveHandler().getWorldDirectory(), FILE_NAME);
                    
                    if(!data.exists())
                    {
                        data.createNewFile();
                    }

                    NBTTagCompound nbt = new NBTTagCompound();
                    BusinessData.INSTANCE.save(nbt);
                    
                    CompressedStreamTools.write(nbt, data);
                }
                catch (IOException e)
                {
                    System.err.println("[ERROR] BusinessEvents.save() - Erreur lors de l'écriture: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            saveThread.setName("BusinessData-Save");
            saveThread.setDaemon(true);
            saveThread.start();
        }
    }
}


