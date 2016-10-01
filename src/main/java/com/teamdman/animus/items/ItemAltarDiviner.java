package com.teamdman.animus.items;


import WayofTime.bloodmagic.api.BlockStack;
import WayofTime.bloodmagic.api.altar.AltarComponent;
import WayofTime.bloodmagic.api.altar.EnumAltarComponent;
import WayofTime.bloodmagic.api.altar.EnumAltarTier;
import WayofTime.bloodmagic.api.altar.IBloodAltar;
import WayofTime.bloodmagic.block.base.BlockString;
import WayofTime.bloodmagic.client.IVariantProvider;
import WayofTime.bloodmagic.item.block.ItemBlockBloodRune;
import WayofTime.bloodmagic.registry.ModBlocks;
import WayofTime.bloodmagic.tile.TileAltar;
import WayofTime.bloodmagic.util.ChatUtil;
import WayofTime.bloodmagic.util.Utils;
import com.teamdman.animus.registry.AnimusBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by TeamDman on 2015-08-30.
 */
public class ItemAltarDiviner extends Item implements IVariantProvider {
	@Override
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
		tooltip.add("Shift-rightclick on an altar to show altar outline");
		tooltip.add("Keep using to automatically place altar blocks");
		tooltip.add("Use in off hand to display max tier altar outline");
	}
	private boolean stackEqualExact(ItemStack stack1, ItemStack stack2)
	{
		if (stack1.getItem() instanceof ItemBlockBloodRune && stack2.getItem() instanceof ItemBlockBloodRune)
			return true;
		return stack1.getItem() == stack2.getItem() && (!stack1.getHasSubtypes() || stack1.getMetadata() == stack2.getMetadata()) && ItemStack.areItemStackTagsEqual(stack1, stack2);
	}

	private int getSlotFor(ItemStack stack, EntityPlayer player) {
		for (int i = 0; i < player.inventory.mainInventory.length; ++i)
		{
			if (player.inventory.mainInventory[i] != null && stackEqualExact(stack, player.inventory.mainInventory[i]))
			{
				return i;
			}
		}
		return -1;
	}

	@Override
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		IBlockState asd = world.getBlockState(pos);
		System.out.println(asd);
		if (world.getTileEntity(pos) == null || !(world.getTileEntity(pos) instanceof IBloodAltar))
			return EnumActionResult.PASS;
		TileAltar altar = (TileAltar) world.getTileEntity(pos);
		altar.checkTier();
		if (!player.isSneaking() || altar == null || altar.getTier().toInt() >= EnumAltarTier.MAXTIERS) {
			return EnumActionResult.PASS;
		}

		for (AltarComponent altarComponent : EnumAltarTier.values()[hand.compareTo(EnumHand.OFF_HAND)==0?EnumAltarTier.MAXTIERS-1:altar.getTier().toInt()].getAltarComponents()) {
			BlockPos componentPos = pos.add(altarComponent.getOffset());
			BlockStack worldBlock = new BlockStack(world.getBlockState(componentPos).getBlock(), world.getBlockState(componentPos).getBlock().getMetaFromState(world.getBlockState(componentPos)));
			if (world.isAirBlock(componentPos)) {
				world.setBlockState(componentPos, AnimusBlocks.blockPhantomBuilder.getDefaultState());
				world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
			}
		}

		String missingText="";
		for (AltarComponent altarComponent : EnumAltarTier.values()[altar.getTier().toInt()].getAltarComponents()) {
			BlockPos componentPos = pos.add(altarComponent.getOffset());
			BlockStack worldBlock = new BlockStack(world.getBlockState(componentPos).getBlock(), world.getBlockState(componentPos).getBlock().getMetaFromState(world.getBlockState(componentPos)));

			if (altarComponent.getComponent() != EnumAltarComponent.NOTAIR) {
				if (worldBlock.getBlock() == AnimusBlocks.blockPhantomBuilder) {
					int invSlot = getSlotFor(new ItemStack(Utils.getBlockForComponent(altarComponent.getComponent())), player);
					if (invSlot != -1 || player.capabilities.isCreativeMode) {
						if (!player.capabilities.isCreativeMode) {
							//world.setBlockState(componentPos,Block.getBlockFromItem(player.inventory.getStackInSlot(invSlot).getItem()).getDefaultState()); //Block.getBlockFromItem(player.inventory.getStackInSlot(invSlot).getItem()).getDefaultState());
							ItemStack _stack = player.inventory.getStackInSlot(invSlot);
							if (_stack.getItem() instanceof ItemBlock) {
								ItemBlock _item = (ItemBlock) player.inventory.getStackInSlot(invSlot).getItem();
								if (_item.getBlock() instanceof BlockString) {
									System.out.println("yee");
									IBlockState _state = ((BlockString) _item.getBlock()).getStateFromMeta(_item.getDamage(_stack));
									world.setBlockState(componentPos, _state);
								}
								world.setBlockState(componentPos, _item.getBlock().getDefaultState());
							}
						} else {
							world.setBlockState(componentPos, Utils.getBlockForComponent(altarComponent.getComponent()).getDefaultState());
						}
						world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.BLOCKS, 0.5F, 1.0F);
						if (!player.capabilities.isCreativeMode) {
							player.inventory.decrStackSize(invSlot, 1);
							return EnumActionResult.PASS;
						}
					} else {
						missingText=I18n.format("text.component.diviner.missing") + " " +
								(altarComponent.getComponent() == EnumAltarComponent.GLOWSTONE ? "Glowstone Block" :
										(I18n.format(new ItemStack(Utils.getBlockForComponent(altarComponent.getComponent())).getItem().getUnlocalizedName(new ItemStack(Utils.getBlockForComponent(altarComponent.getComponent()))) + ".name")));

					}
				}
			} else if (player.capabilities.isCreativeMode) {
				world.setBlockState(componentPos, Blocks.STONE.getDefaultState());
			}
		}
		System.out.println(missingText);
		if (missingText.length()>0) {
			ChatUtil.sendNoSpam(player, new TextComponentTranslation(missingText));
		}
		return EnumActionResult.PASS;
	}


	@Override
	public List<Pair<Integer, String>> getVariants() {
		List<Pair<Integer, String>> ret = new ArrayList<Pair<Integer, String>>();
		ret.add(new ImmutablePair<Integer, String>(0, "type=normal"));
		return ret;
	}
}