package io.github.kyzderp.autocraftmod;

import java.util.List;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class Simulator 
{
	private int size;
	private ItemStack[] slots;
	private ItemStack held;
	
	public Simulator(List<Slot> inventorySlots, int size)
	{
		this.size = size;
		this.slots = new ItemStack[size];
		for (int i = 0; i < size; i++)
		{
			this.slots[i] = inventorySlots.get(i).getStack();
//			System.out.println("added slot " + i + " stack " + this.slots[i]);
		}
	}
	
	public ItemStack stackAt(int slot)
	{
		return this.slots[slot];
	}
	
	
	/**
	 * @param slot
	 */
	public void rightClick(int slot)
	{
		if (this.held != null)
			// TODO should not be holding anything. put it down later
			return;
		
		if (this.slots == null)
			return;
		
		ItemStack stack = this.slots[slot];
		int staying = stack.stackSize / 2;
		this.held = new ItemStack(stack.getItem(), stack.stackSize - staying);
		stack.stackSize = staying;
		if (stack.stackSize < 1)
			this.slots[slot] = null;
	}
	
	/**
	 * 
	 * @param slot
	 */
	public void leftClick(int slot)
	{
		// Put the items down in slot
		if (this.held != null)
		{
			if (this.slots[slot] != null)
				// TODO: It should be empty! Don't know what to do, abort
				return;
			
			this.slots[slot] = this.held;
			this.held = null;
		}
		else // Pick up entire stack
		{
			if (this.slots[slot] == null)
				// TODO: shouldn't be empty!
				return;
			
			this.held = this.slots[slot];
			this.slots[slot] = null;
		}
	}
	
	/**
	 * Put it in the first available slot
	 * Assume that they are only shift clicking on the crafting area
	 * @param slot
	 */
	public void shiftClick(int slot)
	{
		int destination = this.findFirstFreeSlot();
		if (destination == 0)
			// TODO: cannot find slot
			return;
		
		this.slots[destination] = this.slots[slot];
		this.slots[slot] = null;
	}
	
	/**
	 * Find the first free slot starting from the top left of inventory
	 * @return
	 */
	private int findFirstFreeSlot()
	{
		for (int i = this.size - 36; i < this.size; i++)
		{
			if (this.slots[i] == null)
				return i;
		}
		return 0;
	}
	
	// TODO: put held stack down
}
