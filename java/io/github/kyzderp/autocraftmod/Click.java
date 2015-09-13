package io.github.kyzderp.autocraftmod;

public class Click 
{
	private int windowID;
	private int slot;
	private int data;
	private int action;
	private boolean doNext;
	
	public int getWindowID() { return windowID; }
	public int getSlot() { return slot;	}
	public int getData() { return data; }
	public int getAction() { return action;	}
	public boolean getDoNext() { return doNext; }

	public Click(int windowID, int slot, int data, int action, boolean doNext)
	{
		this.windowID = windowID;
		this.slot = slot;
		this.data = data;
		this.action = action;
		this.doNext = doNext;
	}

	@Override
	public String toString()
	{
		return "window: " + windowID + " slot: " + slot;
	}
}
