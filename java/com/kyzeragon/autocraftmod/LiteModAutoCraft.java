package com.kyzeragon.autocraftmod;

import java.io.File;

import org.lwjgl.input.Keyboard;

import com.mumfrey.liteloader.JoinGameListener;
import com.mumfrey.liteloader.Tickable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

public class LiteModAutoCraft implements Tickable, JoinGameListener
{
	private AutoInventory autoInv;
	private AutoWorkbench autoBench;
	private int cooldown;
	private String message;
	private boolean isError;

	@Override
	public String getName() {return "AutoCraft";}

	@Override
	public String getVersion() {return "1.0.0";}

	@Override
	public void init(File configPath) 
	{
		this.cooldown = 20;
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
		// TODO: display error on top of screen
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
