package fr.onitsag.faritech.network.task;

import com.google.common.collect.ImmutableList;
import fr.onitsag.faritech.FariTechMod;
import fr.onitsag.faritech.api.ApplicationManager;
import fr.onitsag.faritech.object.AppInfo;
import fr.onitsag.faritech.proxy.CommonProxy;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.List;

/**
 * Author: MrCrayfish
 */
public class MessageSyncApplications implements IMessage, IMessageHandler<MessageSyncApplications, MessageSyncApplications>
{
    private List<AppInfo> allowedApps;

    public MessageSyncApplications() {}

    public MessageSyncApplications(List<AppInfo> allowedApps)
    {
        this.allowedApps = allowedApps;
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(allowedApps.size());
        for(AppInfo appInfo : allowedApps)
        {
            ByteBufUtils.writeUTF8String(buf, appInfo.getId().toString());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        int size = buf.readInt();
        ImmutableList.Builder<AppInfo> builder = new ImmutableList.Builder<>();
        for(int i = 0; i < size; i++)
        {
            String appId = ByteBufUtils.readUTF8String(buf);
            AppInfo info = ApplicationManager.getApplication(appId);
            if(info != null)
            {
                builder.add(info);
            }
            else
            {
                FariTechMod.getLogger().error("Missing application '" + appId + "'");
            }
        }
        allowedApps = builder.build();
    }

    @Override
    public MessageSyncApplications onMessage(MessageSyncApplications message, MessageContext ctx)
    {
        ReflectionHelper.setPrivateValue(CommonProxy.class, FariTechMod.proxy, message.allowedApps, "allowedApps");
        return null;
    }
}
