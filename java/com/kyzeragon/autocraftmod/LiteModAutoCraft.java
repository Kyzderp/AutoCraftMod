package com.kyzeragon.autocraftmod;

import java.io.File;

import org.lwjgl.input.Keyboard;

import com.mumfrey.liteloader.JoinGameListener;
import com.mumfrey.liteloader.Tickable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

public class LiteModAutoCraft implements Tickable, JoinGameListener
{
	private AutoInventory autoInv;

	@Override
	public String getName() {return "AutoCraft";}

	@Override
	public String getVersion() {return "0.9.0";}

	@Override
	public void init(File configPath) 
	{
	}

	@Override
	public void upgradeSettings(String version, File configPath, File oldConfigPath) {}

	@Override
	public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock) 
	{
		// TODO: which key to use?
		if (inGame && minecraft.thePlayer.openContainer != null
				&& minecraft.thePlayer.openContainer.equals(minecraft.thePlayer.inventoryContainer)
				&& minecraft.currentScreen instanceof GuiInventory)
		{
//			System.out.println("Opened inventory");
			if (Keyboard.isKeyDown(Keyboard.KEY_RETURN))
			{
				if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
					this.autoInv.storeCrafting();
				else
					this.autoInv.craft();
			}
		}

	}
	
	@Override
	public void onJoinGame(INetHandler netHandler, S01PacketJoinGame joinGamePacket) 
	{
		this.autoInv = new AutoInventory();
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
