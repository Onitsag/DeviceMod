package fr.onitsag.faritech.programs.police.network;

import fr.onitsag.faritech.programs.police.PoliceReportManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageSyncPoliceReports implements IMessage, IMessageHandler<MessageSyncPoliceReports, IMessage> {
    private NBTTagCompound tag;

    public MessageSyncPoliceReports() {}

    public MessageSyncPoliceReports(NBTTagCompound tag) {
        this.tag = tag;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, tag);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        tag = ByteBufUtils.readTag(buf);
    }

    @Override
    public IMessage onMessage(MessageSyncPoliceReports message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            PoliceReportManager.INSTANCE.readFromNBT(message.tag);
            // Rafraîchit l'appli police si elle est ouverte
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if(mc.currentScreen instanceof fr.onitsag.faritech.core.Laptop) {
                fr.onitsag.faritech.core.Laptop laptop = (fr.onitsag.faritech.core.Laptop) mc.currentScreen;
                for (fr.onitsag.faritech.core.Window window : laptop.getOpenWindows()) {
                    if(window != null && window.getContent() instanceof fr.onitsag.faritech.programs.police.ApplicationPolice) {
                        ((fr.onitsag.faritech.programs.police.ApplicationPolice) window.getContent()).loadReports();
                    }
                }
            }
        });
        return null;
    }
} 
