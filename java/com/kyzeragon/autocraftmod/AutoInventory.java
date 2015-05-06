package com.kyzeragon.autocraftmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class AutoInventory 
{
	private ContainerPlayer inv;
	private int[] stored;
	private int[] meta;
	private LiteModAutoCraft main;
	private int durabilityRepair;

	public AutoInventory(LiteModAutoCraft main)
	{
		this.inv = (ContainerPlayer) Minecraft.getMinecraft().thePlayer.inventoryContainer;
		this.stored = new int[4];
		this.meta = new int[4];
		this.main = main;
		this.durabilityRepair = 0;
	}


	/**
	 * Stores the crafting matrix if the crafting output is valid
	 */
	public void storeCrafting()
	{//itemtool, itemarmor, itembow, itemsword, itemhoe, shears?

		if (!((Slot)this.inv.inventorySlots.get(0)).getHasStack())
		{ // Nothing in crafting output
			this.main.message("Invalid crafting recipe!", true);
			return;
		}
		ItemStack result = ((Slot)this.inv.inventorySlots.get(0)).getStack();
		this.durabilityRepair = Item.getIdFromItem(result.getItem());
		for (int i = 0; i < 4; i++)
		{
			if (this.inv.inventorySlots.get(i+1) != null)
			{
				ItemStack stack = ((Slot)this.inv.inventorySlots.get(i+1)).getStack();
				if (stack != null)
				{
					Item curr = stack.getItem();
					if (Item.getIdFromItem(curr) != this.durabilityRepair || !curr.isDamageable())
					{
						this.durabilityRepair = 0;
					}
					stored[i] = Item.getIdFromItem(curr);
					meta[i] = stack.getItemDamage();
				}
				else // nothing in the slot 
				{
					stored[i] = 0;
					meta[i] = 0;
				}
			}
		}
		this.main.message("Stored crafting recipe for "	+ result.getDisplayName(), false);
	}

	/**
	 * Crafts the stored recipe depending on normal recipe or repair.
	 */
	public void craft()
	{
		int n = 0;
		for (int i = 0; i < 4; i++)
			n += stored[i];
		if (n == 0)
			return;
		if (this.durabilityRepair != 0)
			this.repair();
		else
			this.craftNormal();
	}

	/**
	 * Repairs tool/armor/weapon/etc stuff that has durability
	 */
	private void repair()
	{
		for (int i = 1; i < 5; i++) // first clear out crafting matrix
			this.shiftClick(i);
		ArrayList<Slot> repairSlots = new ArrayList<Slot>();
		Item toRepair = Item.getItemById(this.durabilityRepair);
		for (int i = 9; i <= 44; i++) // Search inventory for all matching items
		{
			if (((Slot)this.inv.inventorySlots.get(i)).getHasStack())
			{
				ItemStack stack = ((Slot)this.inv.inventorySlots.get(i)).getStack();
				if (stack.getItem() == toRepair	&& stack.getItemDamage() > 0)
					repairSlots.add((Slot)this.inv.inventorySlots.get(i));
			}
		}
		// check that there are at least 2 that can be used for repair
		if (repairSlots.size() < 2)
		{
			this.main.message("Insufficient repair material: " 
					+ new ItemStack(toRepair).getDisplayName(), true);
			return;
		}
		// Sort according to durability
		Collections.sort(repairSlots, new DurabilityComparator());
		int target = toRepair.getMaxDamage() - repairSlots.get(0).getStack().getItemDamage() 
				+ toRepair.getMaxDamage() * 5 / 100;
		this.click(repairSlots.get(0).slotNumber); // move largest damage to crafting grid
		this.click(1);
		for (int i = 1; i < repairSlots.size(); i++) // find the largest damage that can combine and make full
		{
			int durability = repairSlots.get(i).getStack().getItemDamage();
			if (durability <= target)
			{
				this.click(repairSlots.get(i).slotNumber); // move to crafting grid
				this.click(2);
				this.shiftClick(0);
				return;
			}
		}
		this.click(repairSlots.get(1).slotNumber); // otherwise, take next smallest
		this.click(2);
		this.shiftClick(0);
	}

	/**
	 * Craft a normal stored recipe
	 */
	private void craftNormal()
	{
		for (int i = 0; i < 4; i++)
		{
			if (this.inv.inventorySlots.get(i+1) != null)
			{
				ItemStack stack = ((Slot)this.inv.inventorySlots.get(i+1)).getStack();
				if (stack != null)
				{
					int currID = Item.getIdFromItem(stack.getItem());
					int currMeta = stack.getItemDamage();
					if (currID == stored[i] && currMeta == meta[i])
						continue; // correct item in slot
					else if (stored[i] == 0) // there's something when there shouldn't be
					{
						this.shiftClick(i+1);
						continue;
					}
				}

				if (stored[i] == 0)
					continue;
				this.shiftClick(i+1); // empty the slot, could already be empty but watevzzz \o/
				boolean found = false;
				for (int j = 9; j <= 44; j++) // search through inventory for matches
				{
					ItemStack curr = ((Slot)this.inv.inventorySlots.get(j)).getStack();
					if (curr != null && Item.getIdFromItem(curr.getItem()) == stored[i]
							&& curr.getItemDamage() == meta[i]) // found the right item
					{
						this.click(j);
						this.click(i+1); // move it to appropriate slot
						found = true;
						break;
					}
				}
				if (!found) // can't find more in the inventory
				{// i is the slot it should go in
					for (int j = 1; j <=4; j++) // search the crafting matrix for extras
					{
						ItemStack curr = ((Slot)this.inv.inventorySlots.get(j)).getStack();
						if (curr != null && Item.getIdFromItem(curr.getItem()) == stored[i]
								&& curr.getItemDamage() == meta[i] && curr.stackSize > 1) // found the right item
						{
							this.rightClick(j);
							this.click(i+1); // move it to appropriate slot
							found = true;
							break;
						}
					}
					if (!found) // can't find anywhere, empty crafting matrix
					{
						ItemStack displayStack = new ItemStack(Item.getItemById(stored[i]));
						displayStack.setItemDamage(meta[i]);
						for (int k = 1; k < 5; k++)
							this.shiftClick(k);
						this.main.message("Insufficient material: " + displayStack.getDisplayName(), true); 
						return;
					}
				}
			}
		}
		this.shiftClick(0);
	}


	class DurabilityComparator implements Comparator
	{
		@Override
		public int compare(Object arg0, Object arg1) 
		{
			int dmg1 = ((Slot)arg0).getStack().getItemDamage();
			int dmg2 = ((Slot)arg1).getStack().getItemDamage();
			if (dmg1 > dmg2)
				return -1;
			if (dmg1 == dmg2)
				return 0;
			return 1;
		}
	}

	private void shiftClick(int slot)
	{
		Minecraft.getMinecraft().playerController.windowClick(this.inv.windowId,
				slot, 0, 1, Minecraft.getMinecraft().thePlayer);
	}

	private void click(int slot)
	{
		Minecraft.getMinecraft().playerController.windowClick(this.inv.windowId,
				slot, 0, 0, Minecraft.getMinecraft().thePlayer);
	}

	private void rightClick(int slot)
	{
		Minecraft.getMinecraft().playerController.windowClick(this.inv.windowId,
				slot, 1, 0, Minecraft.getMinecraft().thePlayer);
	}

	/* Action values (from invtweaks):
	 * 0: Standard Click
	 * 1: Shift-Click
	 * 2: Move item to/from hotbar slot (Depends on current slot and hotbar slot being full or empty)
	 * 3: Duplicate item (only while in creative)
	 * 4: Drop item
	 * 5: Spread items (Drag behavior)
	 * 6: Merge all valid items with held item
	 */
}
