package mchorse.aperture.commands;

import mchorse.aperture.Aperture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;

/**
 * Command /load_chunks
 *
 * This client side command is responsible for loading all chunks in the render
 * distance.
 */
public class CommandLoadChunks extends CommandBase
{
    @Override
    public String getName()
    {
        return "load_chunks";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "aperture.commands.load_chunks";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length > 0)
        {
            Aperture.l10n.send(sender, this.getUsage(sender));

            return;
        }

        RenderGlobal render = Minecraft.getMinecraft().renderGlobal;
        Field frustumField = null;
        Field chunkyField = null;

        /* Find all fields */
        for (Field field : render.getClass().getDeclaredFields())
        {
            if (chunkyField == null && field.getType().equals(ChunkRenderDispatcher.class))
            {
                chunkyField = field;
                chunkyField.setAccessible(true);
            }

            if (frustumField == null && field.getType().equals(ViewFrustum.class))
            {
                frustumField = field;
                frustumField.setAccessible(true);
            }

            if (chunkyField != null && frustumField != null)
            {
                break;
            }
        }

        /* Force chunk loading */
        if (chunkyField != null && frustumField != null)
        {
            try
            {
                ChunkRenderDispatcher chunks = (ChunkRenderDispatcher) chunkyField.get(render);
                ViewFrustum frustum = (ViewFrustum) frustumField.get(render);

                for (RenderChunk chunk : frustum.renderChunks)
                {
                    if (chunk.getCompiledChunk() == CompiledChunk.DUMMY)
                    {
                        chunks.updateChunkNow(chunk);
                    }
                }

                Aperture.l10n.info(sender, "commands.load_chunks");
            }
            catch (Exception e)
            {
                /* I don't know what might happen code to go here */
                e.printStackTrace();

                Aperture.l10n.error(sender, "commands.load_chunks");
            }
        }
    }
}