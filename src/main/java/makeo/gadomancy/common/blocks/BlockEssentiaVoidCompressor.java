package makeo.gadomancy.common.blocks;

import cpw.mods.fml.common.network.NetworkRegistry;
import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.blocks.tiles.TileEssentiaVoidCompressor;
import makeo.gadomancy.common.network.PacketHandler;
import makeo.gadomancy.common.network.packets.PacketStartAnimation;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import makeo.gadomancy.common.registration.RegisteredItems;
import makeo.gadomancy.common.registration.RegisteredRecipes;
import makeo.gadomancy.common.research.SimpleResearchItem;
import makeo.gadomancy.common.utils.ExplosionHelper;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.research.ResearchManager;

public class BlockEssentiaVoidCompressor extends BlockEssentiaCompressor {
	private IIcon icon;

	public BlockEssentiaVoidCompressor() {
		this.setHardness(5F);
		this.setResistance(20F);
		this.setHarvestLevel("axe", 1);
		this.setBlockBounds(0, 0, 0, 1, 1, 1);
		this.setCreativeTab(RegisteredItems.creativeTab);
	}

	@Override
	protected String getTextureName() {
		return "gadomancy:block_packed_void_compressor";
	}

	@Override
	public int getRenderType() {
		return RegisteredBlocks.rendererTransparentBlock;
	}

	@Override
	public void registerBlockIcons(IIconRegister ir) {
		this.icon = ir.registerIcon(Gadomancy.MODID + ":transparent");
		super.registerBlockIcons(ir);
	}

	@Override
	public IIcon getTransparentIcon() {
		return this.icon;
	}

	@Override
	public int getLightValue(IBlockAccess world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		if (te == null || !(te instanceof TileEssentiaVoidCompressor)) {
			return 0;
		} else {
			if (((TileEssentiaVoidCompressor) te).isMultiblockFormed() && ((TileEssentiaVoidCompressor) te).isMasterTile()) {
				return 8;
			}
		}
		return 0;
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		super.breakBlock(world, x, y, z, block, meta);

		TileEntity te = world.getTileEntity(x, y, z);
		if (te == null || !(te instanceof TileEssentiaVoidCompressor)) return;
		TileEssentiaVoidCompressor compressor = (TileEssentiaVoidCompressor) te;
		if (compressor.isMultiblockFormed()) {
			AspectList al = compressor.getAspects();
			if (al.visSize() > 0) {
				ExplosionHelper.taintplosion(world, x, y, z, true, 2, 2.0F, 4, 20);
				world.setBlockToAir(x, y, z);
			} else {
				ExplosionHelper.taintplosion(world, x, y, z, false, 2, 2.0F, 4, 20);
				world.setBlockToAir(x, y, z);
			}
		}
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		if (te == null || !(te instanceof TileEssentiaVoidCompressor)) {
			super.setBlockBoundsBasedOnState(world, x, y, z);
			return;
		}
		if (((TileEssentiaVoidCompressor) te).isMultiblockFormed()) {
			if (((TileEssentiaVoidCompressor) te).isMasterTile()) {
				this.setBlockBounds(0, 0, 0, 1, 3, 1); // master is lowest tile here.
			} else {
				int yM = ((TileEssentiaVoidCompressor) te).getMultiblockYIndex();
				this.setBlockBounds(0, -yM, 0, 1, 3 - yM, 1);
			}
		} else {
			this.setBlockBounds(0, 0, 0, 1, 1, 1);
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int these, float are,
	                                float some, float variables) { // LUL side, hitx, hity, hitz
		if (world.isRemote) return false;
		TileEntity te = world.getTileEntity(x, y, z);
		if (te == null || !(te instanceof TileEssentiaVoidCompressor)) return false;
		if (((TileEssentiaVoidCompressor) te).isMultiblockFormed()) {
			if (!((TileEssentiaVoidCompressor) te).isMasterTile()) {
				int yOffset = ((TileEssentiaVoidCompressor) te).getMultiblockYIndex();
				return RegisteredBlocks.blockEssentiaVoidCompressor
					.onBlockActivated(world, x, y - yOffset, z, player, these, are, some, variables);
			}
		} else {
			ItemStack heldItem = player.getHeldItem();
			if (heldItem != null && heldItem.getItem() instanceof ItemWandCasting
				&& ResearchManager.isResearchComplete(
				player.getCommandSenderName(),
				SimpleResearchItem.getFullName("ESSENTIA_VOID_COMPRESSOR"))) {
				ChunkCoordinates lowest = this.findLowestCompressorBlock(world, x, y, z);
				boolean canForm = lowest != null && this.isMuliblockPossible(world, lowest);
				if (canForm && ThaumcraftApiHelper.consumeVisFromWandCrafting(
					player.getCurrentEquippedItem(),
					player,
					RegisteredRecipes.costsCompressorMultiblock,
					true)) {
					int multiblockID = TileEssentiaVoidCompressor.getAndIncrementNewMultiblockId();
					TileEssentiaVoidCompressor compressor = (TileEssentiaVoidCompressor) world
						.getTileEntity(lowest.posX, lowest.posY, lowest.posZ);
					compressor.setInMultiblock(true, 0, multiblockID);
					PacketStartAnimation pkt = new PacketStartAnimation(
						PacketStartAnimation.ID_SPARKLE_SPREAD,
						lowest.posX,
						lowest.posY,
						lowest.posZ);
					NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(
						world.provider.dimensionId,
						lowest.posX,
						lowest.posY,
						lowest.posZ,
						32);
					PacketHandler.INSTANCE.sendToAllAround(pkt, point);
					compressor = (TileEssentiaVoidCompressor) world
						.getTileEntity(lowest.posX, lowest.posY + 1, lowest.posZ);
					compressor.setInMultiblock(false, 1, multiblockID);
					pkt = new PacketStartAnimation(
						PacketStartAnimation.ID_SPARKLE_SPREAD,
						lowest.posX,
						lowest.posY + 1,
						lowest.posZ);
					point = new NetworkRegistry.TargetPoint(
						world.provider.dimensionId,
						lowest.posX,
						lowest.posY + 1,
						lowest.posZ,
						32);
					PacketHandler.INSTANCE.sendToAllAround(pkt, point);
					compressor = (TileEssentiaVoidCompressor) world
						.getTileEntity(lowest.posX, lowest.posY + 2, lowest.posZ);
					compressor.setInMultiblock(false, 2, multiblockID);
					pkt = new PacketStartAnimation(
						PacketStartAnimation.ID_SPARKLE_SPREAD,
						lowest.posX,
						lowest.posY + 2,
						lowest.posZ);
					point = new NetworkRegistry.TargetPoint(
						world.provider.dimensionId,
						lowest.posX,
						lowest.posY + 2,
						lowest.posZ,
						32);
					PacketHandler.INSTANCE.sendToAllAround(pkt, point);
				}
			}
		}
		return false;
	}

	private boolean isMuliblockPossible(World world, ChunkCoordinates lowest) {
		return lowest != null && this.isMultiblockable(world, lowest.posX, lowest.posY, lowest.posZ)
			&& this.isMultiblockable(world, lowest.posX, lowest.posY + 1, lowest.posZ)
			&& this.isMultiblockable(world, lowest.posX, lowest.posY + 2, lowest.posZ);
	}

	private ChunkCoordinates findLowestCompressorBlock(World world, int x, int y, int z) {
		ChunkCoordinates result = null;
		for (int i = 0; i <= y; i++) {
			if (!this.isMultiblockable(world, x, y - i, z)) break;
			result = new ChunkCoordinates(x, y - i, z);
		}
		return result;
	}

	private boolean isMultiblockable(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		TileEntity te = world.getTileEntity(x, y, z);
		if (!block.equals(RegisteredBlocks.blockEssentiaVoidCompressor)) return false;
		if (te == null || !(te instanceof TileEssentiaVoidCompressor)) return false;
		TileEssentiaVoidCompressor compressor = (TileEssentiaVoidCompressor) te;
		return !compressor.isMultiblockFormed();
	}

	@Override
	public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
		return new TileEssentiaVoidCompressor();
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
}
