package fr.onitsag.faritech.programs.business.network;

import fr.onitsag.faritech.programs.business.ApplicationBusinessManager;
import fr.onitsag.faritech.programs.business.service.BusinessRepository;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageSyncBusiness implements IMessage, IMessageHandler<MessageSyncBusiness, IMessage>
{
    private NBTTagCompound payload;

    public MessageSyncBusiness() {}

    public MessageSyncBusiness(NBTTagCompound tag)
    {
        this.payload = tag;
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeTag(buf, payload);
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        payload = ByteBufUtils.readTag(buf);
    }

    @Override
    public IMessage onMessage(MessageSyncBusiness message, MessageContext ctx)
    {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            BusinessRepository.get().updateFromNetwork(message.payload);
            if(Minecraft.getMinecraft().currentScreen instanceof fr.onitsag.faritech.core.Laptop)
            {
                fr.onitsag.faritech.core.Laptop laptop = (fr.onitsag.faritech.core.Laptop) Minecraft.getMinecraft().currentScreen;
                for(fr.onitsag.faritech.core.Window window : laptop.getOpenWindows())
                {
                    if(window != null && window.getContent() instanceof ApplicationBusinessManager)
                    {
                        ((ApplicationBusinessManager) window.getContent()).onBusinessDataSynced();
                    }
                }
            }
        });
        return null;
    }
}

