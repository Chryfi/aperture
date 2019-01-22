package mchorse.aperture.client.gui;

import mchorse.aperture.camera.CameraProfile;
import mchorse.aperture.camera.FixtureRegistry;
import mchorse.aperture.camera.fixtures.AbstractFixture;
import mchorse.aperture.camera.fixtures.PathFixture;
import mchorse.aperture.client.gui.panels.GuiAbstractFixturePanel;
import mchorse.mclib.client.gui.framework.GuiTooltip;
import mchorse.mclib.client.gui.framework.elements.GuiElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/**
 * GUI playback scrub
 *
 * This class is responsible for rendering and controlling the playback
 */
public class GuiPlaybackScrub extends GuiElement
{
    /**
     * Vanilla buttons resource location
     */
    public static final ResourceLocation VANILLA_BUTTONS = new ResourceLocation("textures/gui/widgets.png");

    public boolean scrubbing;
    public int value;
    public int min;
    public int max;
    public int index;

    public GuiCameraEditor editor;
    public CameraProfile profile;

    private int lastX;
    private boolean dragging;
    private boolean resize;
    private AbstractFixture start;
    private AbstractFixture end;

    public GuiPlaybackScrub(Minecraft mc, GuiCameraEditor editor, CameraProfile profile)
    {
        super(mc);

        this.editor = editor;
        this.profile = profile;
    }

    /* Public API methods  */

    /**
     * Set profile and update values which depends on camera profile
     */
    public void setProfile(CameraProfile profile)
    {
        this.profile = profile;
        this.index = -1;

        this.max = profile == null ? 0 : (int) profile.getDuration();
        this.value = MathHelper.clamp_int(this.value, this.min, this.max);
    }

    /**
     * Set the value of the scrubber. Also, if the value has changed notify
     * the listener.
     */
    public void setValue(int value, boolean fromScrub)
    {
        int old = this.value;

        this.value = value;
        this.value = MathHelper.clamp_int(this.value, this.min, this.max);

        if (this.value != old)
        {
            this.editor.scrubbed(this, this.value, fromScrub);
        }
    }

    /**
     * Set the value of the scrubb using API
     */
    public void setValue(int value)
    {
        this.setValue(value, false);
    }

    /**
     * Set the value of the scrubber from scrub
     */
    public void setValueFromScrub(int value)
    {
        this.setValue(value, true);
    }

    /**
     * Calculate value from given mouse X
     */
    public int calcValueFromMouse(int mouseX)
    {
        float factor = (float) (mouseX + 1 - this.area.x) / (float) this.area.w;

        return (int) (factor * (this.max - this.min)) + this.min;
    }

    /**
     * Calculate mouse X from given value
     */
    public int calcMouseFromValue(int value)
    {
        float factor = (value - this.min) / (float) (this.max - this.min);

        return (int) (factor * this.area.w) + this.area.x - 1;
    }

    /* GUI interactions */

    /**
     * Mouse was clicked
     */
    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (this.area.isInside(mouseX, mouseY))
        {
            if (mouseButton == 0)
            {
                this.scrubbing = true;
                this.setValueFromScrub(this.calcValueFromMouse(mouseX));

                return true;
            }
            else if (mouseButton == 1 && this.profile != null)
            {
                int tick = this.calcValueFromMouse(mouseX);
                AbstractFixture fixture = this.profile.atTick(tick);
                long offset = this.profile.calculateOffset(fixture);

                if (fixture != null)
                {
                    boolean left = Math.abs(this.calcMouseFromValue((int) offset) - mouseX) < 5;
                    boolean right = Math.abs(this.calcMouseFromValue((int) (offset + fixture.getDuration())) - mouseX) < 5;

                    if (left || right)
                    {
                        int index = this.profile.getAll().indexOf(fixture);

                        if (left && index > 0)
                        {
                            this.dragging = true;
                            this.lastX = mouseX;
                            this.start = this.profile.get(index - 1);
                            this.end = fixture;
                        }
                        else if (right)
                        {
                            this.dragging = true;
                            this.lastX = mouseX;
                            this.start = fixture;
                            this.end = this.profile.has(index + 1) ? this.profile.get(index + 1) : null;
                        }
                    }
                }

                /* Select camera fixture */
                int index = this.profile.getAll().indexOf(fixture);

                this.editor.pickCameraFixture(fixture, tick - offset);
                this.index = index;

                return true;
            }
        }

        return false;
    }

    /**
     * Mouse was released
     */
    @Override
    public void mouseReleased(int mouseX, int mouseY, int state)
    {
        if (this.resize)
        {
            this.profile.dirty();
            this.editor.updateValues();
        }

        this.scrubbing = false;
        this.resize = false;
        this.dragging = false;
    }

    /**
     * Draw scrub on the screen
     *
     * This scrub looks quite simple. The line part is inspired by Blender's
     * timeline thingy. Scrub also renders all of available camera fixtures.
     */
    @Override
    public void draw(GuiTooltip tooltip, int mouseX, int mouseY, float partialTicks)
    {
        if (this.scrubbing)
        {
            this.setValueFromScrub(this.calcValueFromMouse(mouseX));
        }

        /* Visual duration resize */
        if (this.dragging && Math.abs(mouseX - this.lastX) > 6 && !this.resize)
        {
            this.resize = true;
        }

        if (this.resize && this.profile != null)
        {
            long start = this.profile.calculateOffset(this.start);
            long end = start + this.start.getDuration();

            if (this.end != null)
            {
                end += this.end.getDuration();
            }

            long value = this.calcValueFromMouse(mouseX);

            if (value >= start + 5 && (this.end == null ? true : value <= end - 5))
            {
                this.start.setDuration(value - start);

                if (this.end != null)
                {
                    this.end.setDuration(end - value);
                }

                /* Update the values */
                GuiAbstractFixturePanel<AbstractFixture> delegate = this.editor.panel.delegate;

                if (delegate != null)
                {
                    if (delegate.fixture == this.start)
                    {
                        delegate.duration.setValue(this.start.getDuration());
                    }
                    else if (delegate.fixture == this.end)
                    {
                        delegate.duration.setValue(this.end.getDuration());
                    }
                }
            }
        }

        int x = this.area.x;
        int y = this.area.y;
        int w = this.area.w;
        int h = this.area.h;

        /* Draw background */
        Gui.drawRect(x, y + h - 1, x + w, y + h, 0xffffffff);

        /* Calculate tick marker position and tick label width */
        String label = String.valueOf(this.value + "/" + this.max);
        float f = (float) (this.value - this.min) / (float) (this.max - this.min);
        int tx = x + 1 + (int) ((w - 4) * f);
        int width = this.font.getStringWidth(label) + 4;

        /* Draw fixtures */
        int pos = 0;
        int i = 0;
        boolean drawnMarker = false;

        for (AbstractFixture fixture : this.profile.getAll())
        {
            int color = FixtureRegistry.CLIENT.get(fixture.getClass()).color.getHex();

            boolean selected = i == this.index;
            float leftFactor = (float) (pos - this.min) / (float) (this.max - this.min);
            float rightFactor = (float) (pos + fixture.getDuration() - this.min) / (float) (this.max - this.min);
            int leftMargin = x + 1 + (int) ((w - 3) * leftFactor);
            int rightMargin = x + 1 + (int) ((w - 3) * rightFactor);

            /* Draw fixture's  */
            Gui.drawRect(leftMargin + 1, y + 15, rightMargin, y + h - 1, (selected ? 0xff000000 : 0x66000000) + color);
            Gui.drawRect(rightMargin, y + 1, rightMargin + 1, y + h - 1, 0xff000000 + color);

            String name = fixture.getName();

            /* Draw path's fixture separators */
            if (fixture instanceof PathFixture)
            {
                PathFixture path = (PathFixture) fixture;
                int c = path.getCount() - 1;

                if (c > 1)
                {
                    if (path.perPointDuration)
                    {
                        long duration = path.getDuration();
                        long frame = path.getPoint(0).getDuration();

                        for (int j = 1; j < c; j++)
                        {
                            int fract = (int) ((rightMargin - leftMargin) * ((float) frame / duration));
                            int px = leftMargin + fract;

                            Gui.drawRect(px, y + 5, px + 1, y + h - 1, 0xff000000 + color - 0x00181818);

                            frame += path.getPoint(j).getDuration();
                        }
                    }
                    else
                    {
                        int fract = (rightMargin - leftMargin) / c;

                        for (int j = 1; j < c; j++)
                        {
                            int px = leftMargin + fract * j;

                            Gui.drawRect(px, y + 5, px + 1, y + h - 1, 0xff000000 + color - 0x00181818);
                        }
                    }
                }
            }

            /* Draw resizing markers */
            if (this.area.isInside(mouseX, mouseY) && !this.resize && !drawnMarker)
            {
                boolean left = Math.abs(leftMargin - mouseX) < 5;
                boolean right = Math.abs(rightMargin - mouseX) < 5;

                if ((left || right) && !this.resize)
                {
                    int markerOffset = (left ? leftMargin : rightMargin);

                    Gui.drawRect(markerOffset - 4, this.area.y - 1, markerOffset + 5, this.area.y, 0xaaffffff);
                    Gui.drawRect(markerOffset - 5, this.area.y - 1 - 2, markerOffset - 4, this.area.y + 2, 0xaaffffff);
                    Gui.drawRect(markerOffset + 5, this.area.y - 1 - 2, markerOffset + 6, this.area.y + 2, 0xaaffffff);
                    drawnMarker = true;
                }
            }

            /* Draw fixture's title */
            if (!name.isEmpty())
            {
                int lw = this.font.getStringWidth(name);
                int textColor = selected ? 16777120 : 0xffffff;

                if (lw + 4 < rightMargin - leftMargin)
                {
                    this.font.drawStringWithShadow(name, leftMargin + 4, y + 6, textColor);
                }
                else
                {
                    this.font.drawStringWithShadow("...", leftMargin + 4, y + 6, textColor);
                }
            }

            pos += fixture.getDuration();
            i++;
        }

        /* Draw the marker */
        Gui.drawRect(tx, y + 1, tx + 2, y + h - 1, 0xff57f52a);

        /* Draw the "how far into fixture" tick */
        String offsetLabel = String.valueOf(this.value - this.profile.calculateOffset(this.value, false));
        int ow = this.font.getStringWidth(offsetLabel);

        this.font.drawStringWithShadow(offsetLabel, tx - ow / 2 + 1, y + h - this.font.FONT_HEIGHT * 3 - 1, 0xffffff);

        /* Move the tick line left, so it won't overflow the scrub */
        if (tx + 3 - x + width > w)
        {
            tx -= width + 2;
        }

        /* Draw the tick label */
        Gui.drawRect(tx + 2, y + h - 3 - this.font.FONT_HEIGHT, tx + 2 + width, y + h - 1, 0xff57f52a);
        this.font.drawStringWithShadow(label, tx + 4, y + h - this.font.FONT_HEIGHT - 1, 0xffffff);
    }

    /**
     * Scrub event listener
     */
    public static interface IScrubListener
    {
        public void scrubbed(GuiPlaybackScrub scrub, int value, boolean fromScrub);
    }
}