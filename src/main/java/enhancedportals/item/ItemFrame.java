package enhancedportals.item;

import enhancedportals.block.BlockFrame;
import enhancedportals.utility.Localization;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ItemFrame extends ItemBlockWithMetadata
{
    public static String[] unlocalizedName = new String[]{"frame", "controller", "redstone", "network_interface", "dial_device", "program_interface", "upgrade", "fluid", "item", "energy"};

    public ItemFrame(Block b)
    {
        super(b, BlockFrame.instance);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
    {
        int damage = stack.getItemDamage();

        if (damage > 0)
        {
            list.add(Localization.get("block.portalFramePart"));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void getSubItems(Item par1, CreativeTabs par2CreativeTabs, List list)
    {
        for (int i = 0; i < BlockFrame.FRAME_TYPES; i++)
        {
            list.add(new ItemStack(par1, 1, i));
        }
    }

    @Override
    public String getUnlocalizedName(ItemStack stack)
    {
        return super.getUnlocalizedName() + "." + unlocalizedName[stack.getItemDamage()];
    }
}
