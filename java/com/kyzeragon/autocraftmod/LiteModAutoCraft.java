package com.kyzeragon.autocraftmod;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import org.lwjgl.input.Keyboard;

import com.mumfrey.liteloader.ChatFilter;
import com.mumfrey.liteloader.JoinGameListener;
import com.mumfrey.liteloader.OutboundChatListener;
import com.mumfrey.liteloader.Tickable;

public class LiteModAutoCraft implements Tickable, JoinGameListener, OutboundChatListener, ChatFilter
{
	private AutoInventory autoInv;
	private AutoWorkbench autoBench;
	private int cooldown;
	private String message;
	private boolean isError;
	private boolean sentCmd;

	@Override
	public String getName() {return "AutoCraft";}

	@Override
	public String getVersion() {return "1.1.2";}

	@Override
	public void init(File configPath) 
	{
		this.cooldown = 20;
		this.sentCmd = false;
	}

	@Override
	public void upgradeSettings(String version, File configPath, File oldConfigPath) {}

	@Override
	public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock) 
	{
		if (this.cooldown > 0)
		{
			this.cooldown--;
			this.displayMessage(this.message, this.isError);
		}
		if (inGame && minecraft.thePlayer.openContainer != null
				&& minecraft.currentScreen instanceof GuiInventory)
		{
			if (Keyboard.isKeyDown(Keyboard.KEY_RETURN))
			{
				if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
					this.autoInv.storeCrafting();
				else
					this.autoInv.craft();
			}
		}
		else if (inGame && minecraft.thePlayer.openContainer != null
				&& minecraft.currentScreen instanceof GuiCrafting)
		{
			if (Keyboard.isKeyDown(Keyboard.KEY_RETURN))
			{
				if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
					this.autoBench.storeCrafting();
				else
					this.autoBench.craft();
			}
		}
		// TODO: grab correct proportions of items from containers?
		// TODO: craft 1 at a time with ctrl+enter?
		// TODO: delay
		// TODO: auto-combine stuff like armor, must calculate best proportion
	}
	
	@Override
	public void onSendChatMessage(C01PacketChatMessage packet, String message) 
	{
		String[] tokens = message.split(" ");
		if (tokens[0].equalsIgnoreCase("/ac") || tokens[0].equalsIgnoreCase("/autocraft"))
		{
			this.sentCmd = true;
			if (tokens.length == 1)
			{
				this.logMessage("§2" + this.getName() + " §8[§2v" + this.getVersion() + "§8] §aby Kyzeragon", false);
				this.logMessage("Type §2/ac help $afor commands.", false);
			}
		}
	}
	
	@Override
	public boolean onChat(S02PacketChat chatPacket, IChatComponent chat, String message) 
	{
		if (message.matches(".*nknown.*ommand.*") && this.sentCmd)
		{
			this.sentCmd = false;
			return false;
		}
		return true;
	}
	
	@Override
	public void onJoinGame(INetHandler netHandler, S01PacketJoinGame joinGamePacket) 
	{
		this.autoInv = new AutoInventory(this);
		this.autoBench = new AutoWorkbench(this);
	}
	
	public void message(String message, boolean isError)
	{
		this.message = message;
		this.isError = isError;
		this.cooldown = 20;
	}
	
	private void displayMessage(String message, boolean isError)
	{
		int color = 0xFF5555;
		if (!isError)
			color = 0x55FF55;
		FontRenderer fontRender = Minecraft.getMinecraft().fontRenderer;
		fontRender.drawStringWithShadow(message, 
				Minecraft.getMinecraft().displayWidth/4 - fontRender.getStringWidth(message)/2, 
				Minecraft.getMinecraft().displayHeight/4 - 100, color);
	}
	
	/**
	 * Logs the message to the user
	 * @param message The message to log
	 * @param addPrefix Whether to add the mod-specific prefix or not
	 */
	public static void logMessage(String message, boolean addPrefix)
	{// "§8[§2§8] §a"
		if (addPrefix)
			message = "§8[§2AutoCraft§8] §a" + message;
		ChatComponentText displayMessage = new ChatComponentText(message);
		displayMessage.setChatStyle((new ChatStyle()).setColor(EnumChatFormatting.GREEN));
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}

	/**
	 * Logs the error message to the user
	 * @param message The error message to log
	 */
	public static void logError(String message)
	{
		ChatComponentText displayMessage = new ChatComponentText("§8[§4!§8] §c" + message + " §8[§4!§8]");
		displayMessage.setChatStyle((new ChatStyle()).setColor(EnumChatFormatting.RED));
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}
}
