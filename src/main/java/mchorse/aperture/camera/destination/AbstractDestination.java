package mchorse.aperture.camera.destination;

import mchorse.aperture.ClientProxy;
import mchorse.aperture.camera.CameraProfile;
import mchorse.mclib.utils.OpHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Abstract destination class
 * 
 * This class is going to be used by the camera profile to abstract away where 
 * the camera is going to be stored and saved to.
 */
public abstract class AbstractDestination
{
    protected String filename;

    /**
     * Create destination from resource location
     */
    public static AbstractDestination fromResourceLocation(ResourceLocation resource)
    {
        if (resource.getResourceDomain().equals("client"))
        {
            return new ClientDestination(resource.getResourcePath());
        }

        return new ServerDestination(resource.getResourcePath());
    }

    /**
     * Create an abstract destination based on the game's state (i.e. 
     * whether it's single or multiplayer game). 
     */
    @SideOnly(Side.CLIENT)
    public static AbstractDestination create(String filename)
    {
        return ClientProxy.server && OpHelper.isPlayerOp() ? new ServerDestination(filename) : new ClientDestination(filename);
    }

    /**
     * Abstract fixture's constructor. It accepts only the filename of a fixture. 
     */
    public AbstractDestination(String filename)
    {
        this.setFilename(filename);
    }

    public String getFilename()
    {
        return this.filename;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof AbstractDestination)
        {
            return this.filename.equals(((AbstractDestination) obj).getFilename());
        }

        return super.equals(obj);
    }

    /**
     * Rename camera profile 
     */
    public abstract void rename(String name);

    /**
     * Save given camera profile
     */
    public abstract void save(CameraProfile profile);

    /**
     * Reload camera profile 
     */
    public abstract void load();

    /**
     * Remove a camera profile 
     */
    public abstract void remove();

    /**
     * Create a resource location out of destination 
     */
    public abstract ResourceLocation toResourceLocation();
}