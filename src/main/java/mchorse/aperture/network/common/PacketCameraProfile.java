package mchorse.aperture.network.common;

import io.netty.buffer.ByteBuf;
import mchorse.aperture.camera.CameraProfile;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketCameraProfile implements IMessage
{
    public boolean play;
    public String filename;
    public CameraProfile profile;

    public PacketCameraProfile()
    {}

    public PacketCameraProfile(String filename, CameraProfile profile)
    {
        this(filename, profile, false);
    }

    public PacketCameraProfile(String filename, CameraProfile profile, boolean play)
    {
        this.play = play;
        this.filename = filename;
        this.profile = profile;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.play = buf.readBoolean();
        this.filename = ByteBufUtils.readUTF8String(buf);
        this.profile = new CameraProfile(null);
        this.profile.fromBytes(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeBoolean(this.play);
        ByteBufUtils.writeUTF8String(buf, this.filename);
        this.profile.toBytes(buf);
    }
}