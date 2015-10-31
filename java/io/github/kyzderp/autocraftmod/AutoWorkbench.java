package io.github.kyzderp.autocraftmod;

import java.util.HashMap;
import java.util.LinkedList;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

public class AutoWorkbench 
{
	private ContainerWorkbench inv;
	private int[] stored;
	private int[] meta;
	private String output;
	private LiteModAutoCraft main;
	private LinkedList<Click> toSend;

	private Simulator simulator;

	public AutoWorkbench(LiteModAutoCraft main) // TODO: when searching crafting table for extra materials take from highest
	{
		this.stored = new int[9];
		this.meta = new int[9];
		this.main = main;
		this.output = "";
		this.toSend = new LinkedList<Click>();
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
		this.output = result.getDisplayName();
		this.main.message("Stored crafting recipe for "	+ result.getDisplayName(), false);
	}

	public void craft()
	{
		this.toSend.clear();
		this.inv = (ContainerWorkbench)Minecraft.getMinecraft().thePlayer.openContainer;

		// Nothing to craft
		int n = 0;
		for (int i = 0; i < 9; i++) n += this.stored[i];
		if (n == 0)	return;
		
		// Put held items down, if any
		this.depositHeld();
		
		// Simulator to avoid stupid things
		this.simulator = new Simulator(this.inv.inventorySlots, 46);
		
		// Check if enough materials; if not, exit immediately
		if (!this.checkMaterials())
			return;
		
		// Actually craft
		for (int i = 0; i < 9; i++)
		{
			ItemStack stack = this.simulator.stackAt(i+1);
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
				else // the item is wrong
					this.shiftClick(i+1);
			}

			if (stored[i] == 0)
				continue;
			boolean found = false;
			for (int j = 10; j <= 45; j++) // search through inventory for matches
			{
				ItemStack curr = this.simulator.stackAt(j);
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
				int amount = 0;
				int slot = -1;
				for (int j = 1; j <=9; j++) // search the crafting matrix for extras, find highest
				{
					ItemStack curr = this.simulator.stackAt(j);
					if (curr != null && Item.getIdFromItem(curr.getItem()) == stored[i]
							&& curr.getItemDamage() == meta[i] && curr.stackSize > amount) // found the right item
					{
						amount = curr.stackSize;
						slot = j;
						found = true;
					}
				}
				if (found)
				{
					this.rightClick(slot);
					this.click(i+1);
				}
				if (!found) // cannot find, clear crafting matrix
				{
					ItemStack displayStack = new ItemStack(Item.getItemById(stored[i]));
					displayStack.setItemDamage(meta[i]);
//					System.out.println("Insufficient material: " + displayStack.getDisplayName());
					this.sendQueue();
					return;
				}
			}
		}
		this.shiftClick(0);
		this.sendQueue();
	}

	/**
	 * Deposits held items
	 */
	private void depositHeld() 
	{
		// First go through inventory portion to find empty slot
		for (int i = 10; i < 46; i++)
		{
			if (!((Slot)this.inv.inventorySlots.get(i)).getHasStack())
			{
				Minecraft.getMinecraft().playerController.windowClick(this.inv.windowId,
						i, 0, 0, Minecraft.getMinecraft().thePlayer);
				return;
			}
		}
		
		// If there isn't one, try the crafting grid
		for (int i = 1; i < 10; i++)
		{
			if (!((Slot)this.inv.inventorySlots.get(i)).getHasStack())
			{
				Minecraft.getMinecraft().playerController.windowClick(this.inv.windowId,
						i, 0, 0, Minecraft.getMinecraft().thePlayer);
				return;
			}
		}
		// If there still isn't one, oh well.
	}

	/**
	 * Checks if enough materials
	 * @return true if enough materials
	 */
	private boolean checkMaterials()
	{
		HashMap<String, Integer> needed = new HashMap<String, Integer>();
		for (int i = 0; i < 9; i++)
		{
			if (this.stored[i] != 0)
			{
				Integer count = needed.get(stored[i] + ":" + meta[i]);
				if (count == null)
					needed.put(stored[i] + ":" + meta[i], 1);
				else
					needed.put(stored[i] + ":" + meta[i], count + 1);
			}
		}
		for (int i = 1; i <= 45; i++)
		{
			this.inv = (ContainerWorkbench)Minecraft.getMinecraft().thePlayer.openContainer;
			if (this.simulator.stackAt(i) == null)
				continue;
			ItemStack stack = this.simulator.stackAt(i);
			if (stack == null)
				continue;
			String item = Item.getIdFromItem(stack.getItem()) + ":" + stack.getItemDamage();
			Integer count = needed.get(item);
			if (count == null)
				continue;
			else
			{
				if (stack.stackSize >= count)
					needed.remove(item);
				else
					needed.put(item, count - stack.stackSize);
			}
			if (needed.size() == 0)
				break;
		}

		if (needed.size() > 0) // Not enough materials, exit immediately
		{
			String item = needed.keySet().iterator().next();
			ItemStack displayStack = new ItemStack(Item.getItemById(Integer.parseInt(item.split(":")[0])));
			displayStack.setItemDamage(Integer.parseInt(item.split(":")[1]));
			this.toSend.clear();
			for (int k = 1; k < 10; k++)
				if (this.simulator.stackAt(k) != null)
					this.shiftClick(k);
			this.main.message("Insufficient material for " + this.output + ": " 
					+ displayStack.getDisplayName(), true);
			this.sendQueue();
			return false;
		}
		return true;
	}

	private void shiftClick(int slot)
	{
//		System.out.println("shift click slot " + slot);
		this.toSend.addLast(new Click(this.inv.windowId, slot, 0, 1));
		this.simulator.shiftClick(slot);
	}

	private void click(int slot)
	{
//		System.out.println("left click slot " + slot);
		this.toSend.addLast(new Click(this.inv.windowId, slot, 0, 0));
		this.simulator.leftClick(slot);
	}

	private void rightClick(int slot)
	{
//		System.out.println("right click slot " + slot);
		this.toSend.addLast(new Click(this.inv.windowId, slot, 1, 0));
		this.simulator.rightClick(slot);
	}

	private void sendQueue()
	{
//		System.out.println("sendqueue");
		this.main.queueClicks(this.toSend);
		this.toSend.clear();
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

