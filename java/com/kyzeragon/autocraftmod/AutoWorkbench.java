package com.kyzeragon.autocraftmod;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class AutoWorkbench 
{
	private ContainerWorkbench inv;
	private int[] stored;
	private int[] meta;
	private LiteModAutoCraft main;


	public AutoWorkbench(LiteModAutoCraft main)
	{
		this.stored = new int[9];
		this.meta = new int[9];
		this.main = main;
	}

	public void storeCrafting()
	{
		this.inv = (ContainerWorkbench)Minecraft.getMinecraft().thePlayer.openContainer;
		if (!((Slot)this.inv.inventorySlots.get(0)).getHasStack())
		{ // Nothing in crafting output
			this.main.message("Invalid crafting recipe!", true);
			return;
		}
		ItemStack result = ((Slot)this.inv.inventorySlots.get(0)).getStack();
		for (int i = 0; i < 9; i++)
		{
			if (this.inv.inventorySlots.get(i+1) != null)
			{
				ItemStack stack = ((Slot)this.inv.inventorySlots.get(i+1)).getStack();
				if (stack != null)
				{
					this.stored[i] = Item.getIdFromItem(stack.getItem());
					this.meta[i] = stack.getItemDamage();
				}
				else // nothing in the slot
				{
					this.stored[i] = 0;
					this.meta[i] = 0;
				}
			}
		}
		this.main.message("Stored crafting recipe for "	+ result.getDisplayName(), false);
		// TODO: error for invalid recipe (crafting output is empty)
	}

	public void craft()
	{
		int n = 0;
		for (int i = 0; i < 9; i++) n += this.stored[i];
		if (n == 0)	return;
		
		this.inv = (ContainerWorkbench)Minecraft.getMinecraft().thePlayer.openContainer;
		for (int i = 0; i < 9; i++)
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
				for (int j = 10; j <= 45; j++) // search through inventory for matches
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
				if (!found)
				{
					for (int j = 1; j <=9; j++) // search the crafting matrix for extras
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
					if (!found) // cannot find, clear crafting matrix
					{
						ItemStack displayStack = new ItemStack(Item.getItemById(stored[i]));
						displayStack.setItemDamage(meta[i]);
						for (int k = 1; k <= 9; k++)
							this.shiftClick(k);
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

