package com.kyzeragon.autocraftmod;

import java.util.Hashtable;
import java.util.LinkedList;

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


	public AutoInventory(LiteModAutoCraft main)
	{
		this.inv = (ContainerPlayer) Minecraft.getMinecraft().thePlayer.inventoryContainer;
		this.stored = new int[4];
		this.meta = new int[4];
		this.main = main;
	}

	public void storeCrafting()
	{
		for (int i = 0; i < 4; i++)
		{
			if (this.inv.inventorySlots.get(i+1) != null)
			{
				ItemStack stack = ((Slot)this.inv.inventorySlots.get(i+1)).getStack();
				if (stack != null)
				{
					stored[i] = Item.getIdFromItem(stack.getItem());
					meta[i] = stack.getItemDamage();
				}
				else // nothing in the slot
				{
					stored[i] = -1;
					meta[i] = -1;
				}
			}
		}
		this.main.message("Stored current crafting recipe.", false);
		// TODO: error for invalid recipe (crafting output is empty)
	}

	public void craft()
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
					else if (stored[i] == -1) // there's something when there shouldn't be
					{
						this.shiftClick(i+1);
						continue;
					}
				}

				if (stored[i] == -1)
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
					if (!found)
					{
						ItemStack displayStack = new ItemStack(Item.getItemById(stored[i]));
						displayStack.setItemDamage(meta[i]);
						this.main.message("Insufficient material: " + displayStack.getDisplayName(), true); 
						return;
					}
				}
			}
		}
		this.shiftClick(0);
	}


	private void shiftClick(int slot)
	{//windowClick(int windowID, int slot, int data, int action, EntityPlayer par5EntityPlayer)
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
