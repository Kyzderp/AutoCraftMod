package io.github.kyzderp.autocraftmod;

public class Click 
{
	private int windowID;
	private int slot;
	private int data;
	private int action;
	
	public int getWindowID() { return windowID; }
	public int getSlot() { return slot;	}
	public int getData() { return data; }
	public int getAction() { return action;	}

	public Click(int windowID, int slot, int data, int action)
	{
		this.windowID = windowID;
		this.slot = slot;
		this.data = data;
		this.action = action;
	}
	
	@Override
	public String toString()
	{
		return "window: " + windowID + " slot: " + slot;
	}
}
