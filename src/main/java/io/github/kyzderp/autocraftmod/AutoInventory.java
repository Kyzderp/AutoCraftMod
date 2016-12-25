package io.github.kyzderp.autocraftmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class AutoInventory 
{
	private ContainerPlayer inv;
	private int[] stored;
	private int[] meta;
	private String output;
	private LiteModAutoCraft main;
	private int durabilityRepair;
	private LinkedList<Click> toSend;
	
	private Simulator simulator;

	public AutoInventory(LiteModAutoCraft main)
	{
		this.inv = (ContainerPlayer) Minecraft.getMinecraft().thePlayer.inventoryContainer;
		this.toSend = new LinkedList<Click>();
		this.stored = new int[4];
		this.meta = new int[4];
		this.output = "";
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
		this.output = result.getDisplayName();
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
		this.toSend.clear();
		for (int i = 1; i < 5; i++) // first clear out crafting matrix
			if (((Slot)this.inv.inventorySlots.get(i)).getHasStack())
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
			this.toSend.clear();
			this.sendQueue();
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
				this.sendQueue();
				return;
			}
		}
		this.click(repairSlots.get(1).slotNumber); // otherwise, take next smallest
		this.click(2);
		this.shiftClick(0);
		this.sendQueue();
	}

	/**
	 * Craft a normal stored recipe
	 */
	private void craftNormal()
	{
		this.toSend.clear();
		
		// Put down items
		this.depositHeld();
		
		// Not enough materials
		if (!this.checkMaterials())
			return;
		
		// Simulator to avoid stupid things
		this.simulator = new Simulator(this.inv.inventorySlots, 45);
		
		// Do the crafting
		String ignore = "-1";
		for (int i = 0; i < 4; i++)
		{
			if (this.inv.inventorySlots.get(i+1) != null)
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
					else // wrong item
						this.shiftClick(i+1);
				}

				if (stored[i] == 0)
					continue;
				if (this.simulator.stackAt(i+1) != null)
					this.shiftClick(i+1); // empty the slot
				boolean found = false;
				for (int j = 9; j <= 44; j++) // search through inventory for matches
				{
					if (("" + j).matches("^(" + ignore + ")$"))
						continue;
					ItemStack curr = this.simulator.stackAt(j);
					if (curr != null && Item.getIdFromItem(curr.getItem()) == stored[i]
							&& curr.getItemDamage() == meta[i]) // found the right item
					{
						this.click(j);
						this.click(i+1); // move it to appropriate slot
						ignore += "|" + j;
						found = true;
						break;
					}
				}
				if (!found) // can't find more in the inventory
				{// i is the slot it should go in
					for (int j = 1; j <=4; j++) // search the crafting matrix for extras
					{
						ItemStack curr = this.simulator.stackAt(j);
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
//						System.out.println("Insufficient material for " + this.output + ": " 
//								+ displayStack.getDisplayName());
						this.sendQueue();
						return;
					}
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
		for (int i = 9; i < 45; i++)
		{
			if (!((Slot)this.inv.inventorySlots.get(i)).getHasStack())
			{
				Minecraft.getMinecraft().playerController.windowClick(this.inv.windowId,
						i, 0, ClickType.PICKUP, Minecraft.getMinecraft().thePlayer);
				return;
			}
		}
		
		// If there isn't one, try the crafting grid
		for (int i = 1; i < 5; i++)
		{
			if (!((Slot)this.inv.inventorySlots.get(i)).getHasStack())
			{
				Minecraft.getMinecraft().playerController.windowClick(this.inv.windowId,
						i, 0, ClickType.PICKUP, Minecraft.getMinecraft().thePlayer);
				return;
			}
		}
		// If there still isn't one, oh well.
	}
	
	/**
	 * Check if there are sufficient materials to craft
	 * @return
	 */
	private boolean checkMaterials()
	{
		HashMap<String, Integer> needed = new HashMap<String, Integer>();
		for (int i = 0; i < 4; i++)
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
		for (int i = 1; i <= 44; i++)
		{
			if (i > 4 && i < 9)
				continue;
			if (((Slot)this.inv.inventorySlots.get(i)) == null)
				continue;
			ItemStack stack = ((Slot)this.inv.inventorySlots.get(i)).getStack();
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
			for (int k = 1; k < 5; k++)
				if (((Slot)this.inv.inventorySlots.get(k)).getHasStack())
					this.shiftClick(k);
			this.main.message("Insufficient material for " + this.output + ": " 
					+ displayStack.getDisplayName(), true);
			this.sendQueue();
			return false;
		}
		return true;
	}


	class DurabilityComparator implements Comparator<Slot>
	{
		@Override
		public int compare(Slot arg0, Slot arg1) 
		{
			int dmg1 = arg0.getStack().getItemDamage();
			int dmg2 = arg1.getStack().getItemDamage();
			if (dmg1 > dmg2)
				return -1;
			if (dmg1 == dmg2)
				return 0;
			return 1;
		}
	}

	private void shiftClick(int slot)
	{
		this.toSend.addLast(new Click(this.inv.windowId, slot, 0, ClickType.QUICK_MOVE));
		if (this.simulator != null)
			this.simulator.shiftClick(slot);
	}

	private void click(int slot)
	{
		this.toSend.addLast(new Click(this.inv.windowId, slot, 0, ClickType.PICKUP));
		if (this.simulator != null)
			this.simulator.leftClick(slot);
	}

	private void rightClick(int slot)
	{
		this.toSend.addLast(new Click(this.inv.windowId, slot, 1, ClickType.PICKUP));
		if (this.simulator != null)
			this.simulator.rightClick(slot);
	}

	private void sendQueue()
	{
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
