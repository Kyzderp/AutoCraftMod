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


	public AutoInventory()
	{
		this.inv = (ContainerPlayer) Minecraft.getMinecraft().thePlayer.inventoryContainer;
		this.stored = new int[4];
		this.meta = new int[4];
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
					System.out.println((i+1) + ": " + stored[i] + " " + meta[i]);
				}
				else // nothing in the slot
				{
					stored[i] = -1;
					meta[i] = -1;
				}
			}
		}
		System.out.println("Finished storing.");
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
								&& curr.getItemDamage() == meta[i]) // found the right item
						{
							this.rightClick(j);
							this.click(i+1); // move it to appropriate slot
							found = true;
							break;
						}
					}
					if (!found)
					{
						System.out.println("Could not find item ID " + stored[i] + ":" + meta[i]);
						return;
					}
				}
			}
		}
		this.shiftClick(0);
		System.out.println("Finished crafting.");
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
