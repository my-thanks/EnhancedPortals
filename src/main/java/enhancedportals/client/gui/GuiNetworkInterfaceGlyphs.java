package enhancedportals.client.gui;

import enhancedportals.EnhancedPortals;
import enhancedportals.client.gui.elements.ElementGlyphSelector;
import enhancedportals.client.gui.elements.ElementGlyphViewer;
import enhancedportals.client.gui.tabs.TabTip;
import enhancedportals.inventory.ContainerNetworkInterfaceGlyphs;
import enhancedportals.network.GuiHandler;
import enhancedportals.network.packet.PacketGuiData;
import enhancedportals.network.packet.PacketRequestGui;
import enhancedportals.portal.GlyphIdentifier;
import enhancedportals.tile.TileController;
import enhancedportals.utility.Localization;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import java.util.Random;

public class GuiNetworkInterfaceGlyphs extends BaseGui
{
    public static final int CONTAINER_SIZE = 135;
    TileController controller;
    GuiButton buttonCancel, buttonSave;
    ElementGlyphSelector selector;

    public GuiNetworkInterfaceGlyphs(TileController c, EntityPlayer p)
    {
        super(new ContainerNetworkInterfaceGlyphs(c, p.inventory), CONTAINER_SIZE);
        controller = c;
        name = "gui.networkInterface";
        setHidePlayerInventory();
    }

    @Override
    public void initGui()
    {
        super.initGui();
        int buttonWidth = 80;
        buttonCancel = new GuiButton(0, guiLeft + 7, guiTop + containerSize - 27, buttonWidth, 20, Localization.get("gui.cancel"));
        buttonSave = new GuiButton(1, guiLeft + xSize - buttonWidth - 7, guiTop + containerSize - 27, buttonWidth, 20, Localization.get("gui.save"));
        buttonList.add(buttonCancel);
        buttonList.add(buttonSave);
        addTab(new TabTip(this, "glyphs"));
        selector = new ElementGlyphSelector(this, 7, 52);
        selector.setIdentifierTo(controller.getIdentifierNetwork());
        addElement(selector);
        addElement(new ElementGlyphViewer(this, 7, 29, selector));
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (isShiftKeyDown())
        {
            if (button.id == buttonCancel.id)
            {
                selector.reset();
            }
            else if (button.id == buttonSave.id) // Random
            {
                Random random = new Random();
                GlyphIdentifier iden = new GlyphIdentifier();

                for (int i = 0; i < (isCtrlKeyDown() ? 9 : random.nextInt(8) + 1); i++)
                {
                    iden.addGlyph(random.nextInt(27));
                }

                selector.setIdentifierTo(iden);
            }
        }
        else if (button.id == buttonCancel.id)
        {
            EnhancedPortals.packetPipeline.sendToServer(new PacketRequestGui(controller, GuiHandler.NETWORK_INTERFACE_A));
        }
        else if (button.id == buttonSave.id) // Save Changes
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("nid", selector.getGlyphIdentifier().getGlyphString());
            EnhancedPortals.packetPipeline.sendToServer(new PacketGuiData(tag));
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int par1, int par2)
    {
        super.drawGuiContainerForegroundLayer(par1, par2);
        getFontRenderer().drawString(Localization.get("gui.networkIdentifier"), 7, 19, 0x404040);
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();

        if (isShiftKeyDown())
        {
            buttonCancel.displayString = EnumChatFormatting.AQUA + Localization.get("gui.clear");
            buttonSave.displayString = (isCtrlKeyDown() ? EnumChatFormatting.GOLD : EnumChatFormatting.AQUA) + Localization.get("gui.random");
        }
        else
        {
            buttonCancel.displayString = Localization.get("gui.cancel");
            buttonSave.displayString = Localization.get("gui.save");
        }
    }
}
