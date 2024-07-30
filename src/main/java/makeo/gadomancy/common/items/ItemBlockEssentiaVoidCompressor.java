package makeo.gadomancy.common.items;

import makeo.gadomancy.common.registration.RegisteredItems;
import net.minecraft.block.Block;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;

public class ItemBlockEssentiaVoidCompressor extends ItemBlockEssentiaCompressor {
    public ItemBlockEssentiaVoidCompressor(Block block) {
        super(block);
    }

    @Override
    public EnumRarity getRarity(ItemStack p_77613_1_) {
        return RegisteredItems.raritySacred;
    }
}
