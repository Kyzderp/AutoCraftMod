package io.github.kyzderp.autocraftmod;

import java.io.File;
import java.util.LinkedList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.inventory.ClickType;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.SPacketJoinGame;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import org.lwjgl.input.Keyboard;

import com.mojang.realmsclient.dto.RealmsServer;
import com.mumfrey.liteloader.JoinGameListener;
import com.mumfrey.liteloader.OutboundChatFilter;
import com.mumfrey.liteloader.Tickable;

public class LiteModAutoCraft implements Tickable, JoinGameListener, OutboundChatFilter
{
	private AutoInventory autoInv;
	private AutoWorkbench autoBench;
	private CraftSettings settings;
	
	private int msgcooldown;
	private String message;
	private boolean isError;
	private LinkedList<Click> clickQueue;
	private int currClickCooldown;
	private int maxClickCooldown;

	@Override
	public String getName() {return "AutoCraft";}

	@Override
	public String getVersion() {return "1.4.0";}

	@Override
	public void init(File configPath) 
	{
		this.settings = new CraftSettings();
		this.maxClickCooldown = this.settings.getMaxClickCooldown();
		this.currClickCooldown = 0;
		this.msgcooldown = 40;
		this.clickQueue = new LinkedList<Click>();
	}

	@Override
	public void upgradeSettings(String version, File configPath, File oldConfigPath) {}

	@Override
	public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock) 
	{
		if (!inGame)
			return;
		
		if (this.msgcooldown > 0)
		{
			this.msgcooldown--;
			this.displayMessage(this.message, this.isError);
		}
		
		while (!this.clickQueue.isEmpty() && this.currClickCooldown == 0)
		{
			Click currClick = this.clickQueue.pop();
			
			/*String thing = "";
			if (currClick.getAction() == 0)
			{
				if (currClick.getData() == 0)
					thing = "left Click";
				else
					thing = "right click";
			}*/
			
			Minecraft.getMinecraft().playerController.windowClick(currClick.getWindowID(),
					currClick.getSlot(), currClick.getData(), currClick.getAction(), 
					Minecraft.getMinecraft().thePlayer);

			this.currClickCooldown = this.maxClickCooldown;
			// For right clicks to not mess up
			if (this.maxClickCooldown < 2 && currClick.getData() == 1
					&& currClick.getAction() == ClickType.PICKUP)
				this.currClickCooldown = 2;
		}
		if (this.currClickCooldown > 0)
			this.currClickCooldown--;
		if (minecraft.thePlayer.openContainer != null
				&& minecraft.currentScreen instanceof GuiInventory)
		{
			if (Keyboard.isKeyDown(Keyboard.KEY_RETURN))
			{
				if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
					this.autoInv.storeCrafting();
				else if (this.clickQueue.isEmpty())
					this.autoInv.craft();
			}
		}
		else if (minecraft.thePlayer.openContainer != null
				&& minecraft.currentScreen instanceof GuiCrafting)
		{
			if (Keyboard.isKeyDown(Keyboard.KEY_RETURN))
			{
				if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
					this.autoBench.storeCrafting();
				else if (this.clickQueue.isEmpty())
					this.autoBench.craft();
			}
		}
		// TODO: delay?
		// TODO: auto-combine stuff still needs better algorithm
	}

	@Override
	public boolean onSendChatMessage(String message) 
	{
		String[] tokens = message.split(" ");
		if (tokens[0].equalsIgnoreCase("/autocraft"))
		{
			if (tokens.length == 1)
			{
				LiteModAutoCraft.logMessage("\u00A72" + this.getName() + " \u00A78[\u00A72v" + this.getVersion() + "\u00A78] \u00A7aby Kyzeragon", false);
				LiteModAutoCraft.logMessage("Type \u00A72/autocraft help \u00A7afor commands.", false);
			}
			else if (tokens[1].equalsIgnoreCase("delay"))
			{
				if (tokens.length == 2)
					LiteModAutoCraft.logMessage("Current crafting delay is " + this.maxClickCooldown, true);
				else if (!tokens[2].matches("[0-9]+"))
					LiteModAutoCraft.logError("Must be an integer. Recommended 0~4");
				else
				{
					this.maxClickCooldown = Integer.parseInt(tokens[2]);
					this.settings.setMaxClickCooldown(this.maxClickCooldown);
					LiteModAutoCraft.logMessage("Crafting delay set to " + this.maxClickCooldown, true);
				}
			}
			else if (tokens[1].equalsIgnoreCase("help"))
			{
				String[] commands = {"delay <number> - Sets the delay (in ticks) for craft clicking",
						"help - Displays this help message"};
				LiteModAutoCraft.logMessage("\u00A72" + this.getName() + " \u00A78[\u00A72v" + this.getVersion() + "\u00A78] \u00A7acommands:", false);
				for (String command: commands)
					LiteModAutoCraft.logMessage("/autocraft " + command, false);
			}
			else
			{
				LiteModAutoCraft.logMessage("\u00A72" + this.getName() + " \u00A78[\u00A72v" + this.getVersion() + "\u00A78] \u00A7aby Kyzeragon", false);
				LiteModAutoCraft.logMessage("Type \u00A72/autocraft help \u00A7afor commands.", false);
			}
			return false;
		}
		return true;
	}

	@Override
	public void onJoinGame(INetHandler netHandler,
			SPacketJoinGame joinGamePacket, ServerData serverData,
			RealmsServer realmsServer) 
	{
		this.autoInv = new AutoInventory(this);
		this.autoBench = new AutoWorkbench(this);
	}

	public void message(String message, boolean isError)
	{
		this.message = message;
		this.isError = isError;
		this.msgcooldown = 40;
	}

	private void displayMessage(String message, boolean isError)
	{
		int color = 0xFF5555;
		if (!isError)
			color = 0x55FF55;
		FontRenderer fontRender = Minecraft.getMinecraft().fontRendererObj;
		fontRender.drawStringWithShadow(message, 
				Minecraft.getMinecraft().displayWidth/4 - fontRender.getStringWidth(message)/2, 
				Minecraft.getMinecraft().displayHeight/4 - 100, color);
	}
	
	/**
	 * Queues a click to be run later
	 * @param click
	 */
	public void queueClicks(LinkedList<Click> queue)
	{
		this.clickQueue.addAll(queue);
	}

	/**
	 * Logs the message to the user
	 * @param message The message to log
	 * @param addPrefix Whether to add the mod-specific prefix or not
	 */
	public static void logMessage(String message, boolean addPrefix)
	{// "\u00A78[\u00A72\u00A78] \u00A7a"
		if (addPrefix)
			message = "\u00A78[\u00A72AutoCraft\u00A78] \u00A7a" + message;
		TextComponentString displayMessage = new TextComponentString(message);
		displayMessage.setStyle((new Style()).setColor(TextFormatting.GREEN));
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}

	/**
	 * Logs the error message to the user
	 * @param message The error message to log
	 */
	public static void logError(String message)
	{
		TextComponentString displayMessage = new TextComponentString("\u00A78[\u00A74!\u00A78] \u00A7c" + message + " \u00A78[\u00A74!\u00A78]");
		displayMessage.setStyle((new Style()).setColor(TextFormatting.RED));
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}
}
