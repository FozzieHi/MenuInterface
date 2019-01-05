package brian.menuinterface;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * MenuUtil contains Reflection & Utilities
 * Warning: Potentially unsafe when common sense is missing.
 *
 * @author made by Kangarko <3 Edited by OOP-778
 */
@SuppressWarnings("unchecked")
public class MenuUtil {

	private static Method getHandle;
	private static Field fieldPlayerConnection;
	private static Method sendPacket;

	private static String SERVER_VERSION;

	static {
		try {
			final String packageName = Bukkit.getServer() == null ? "" : Bukkit.getServer().getClass().getPackage().getName();
			SERVER_VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);

			getHandle = getOFCClass("entity.CraftPlayer").getMethod("getHandle");
			fieldPlayerConnection = getNMSClass("EntityPlayer").getField("playerConnection");
			sendPacket = getNMSClass("PlayerConnection").getMethod("sendPacket", getNMSClass("Packet"));

		} catch (final Throwable t) {
			System.out.println("Unable to find setup reflection. Plugin will still function.");
			System.out.println("Error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
			System.out.println("Ignore this if using Cauldron. Otherwise check if your server is compatibible.");

			fieldPlayerConnection = null;
			sendPacket = null;
			getHandle = null;
		}
	}

	/**
	 * Send a packet to the player.
	 *
	 * @param player
	 * @param packet, must be the NMS class
	 */
	public static void sendPacket(Player player, Object packet) {
		if (getHandle == null || fieldPlayerConnection == null || sendPacket == null) {
			System.out.println("Cannot send packet " + packet.getClass().getSimpleName() + " on your server sofware (known to be broken on Cauldron).");
			return;
		}

		try {
			final Object handle = getHandle.invoke(player);
			final Object playerConnection = fieldPlayerConnection.get(handle);

			sendPacket.invoke(playerConnection, packet);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException("Could not send " + packet.getClass().getSimpleName() + " to " + player.getName(), ex);
		}
	}

	/**
	 * Return class in the net.minecraft.server with the current version prefix included.
	 */
	public static Class<?> getNMSClass(String name) {
		return lookupClass("net.minecraft.server." + SERVER_VERSION + "." + name);
	}

	/**
	 * Return class in the org.bukkit.craftbukkit with the current version prefix included.
	 */
	public static Class<?> getOFCClass(String name) {
		return lookupClass("org.bukkit.craftbukkit." + SERVER_VERSION + "." + name);
	}

	/**
	 * Find a class and cast it to a specific type.
	 */
	public static <T> Class<T> lookupClass(String path, Class<T> type) {
		return (Class<T>) lookupClass(path);
	}

	// Find a class with the fully qualified name.
	// Throws error when not found.
	private static Class<?> lookupClass(String path) {
		try {
			return Class.forName(path);

		} catch (final ClassNotFoundException ex) {
			throw new ReflectionException("Could not find class: " + path);
		}
	}


	public static void changeTitle(Player pl, String title, String inventoryType) {
		try {

			if(title.length() > 32){
				title = title.substring(0,32);
			}

			final Object entityPlayer = pl.getClass().getMethod("getHandle").invoke(pl);

			final Constructor<?> packetConst = getNMSClass("PacketPlayOutOpenWindow").getConstructor(int.class, String.class, getNMSClass("IChatBaseComponent"), int.class);


			final Object activeContainer = entityPlayer.getClass().getField("activeContainer").get(entityPlayer);
			final Constructor<?> chatMessageConst = getNMSClass("ChatMessage").getConstructor(String.class, Object[].class);

			final Object windowId = activeContainer.getClass().getField("windowId").get(activeContainer);
			final Object chatMessage = chatMessageConst.newInstance(colorize(title), new Object[0]);

			final Object packet = packetConst.newInstance(windowId, inventoryType, chatMessage, pl.getOpenInventory().getTopInventory().getSize() );
			sendPacket(pl, packet);

			entityPlayer.getClass().getMethod("updateInventory", getNMSClass("Container")).invoke(entityPlayer, activeContainer);
		} catch (final ReflectiveOperationException ex) {

			ex.printStackTrace();

		}
	}



	public static class ReflectionException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public ReflectionException(String msg) {
			super(msg);
		}

		public ReflectionException(String msg, Exception ex) {
			super(msg, ex);
		}
	}

	public static void out(String text){
		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', text));
	}

	static String colorize(String text){
		return ChatColor.translateAlternateColorCodes('&', text);
	}
}
