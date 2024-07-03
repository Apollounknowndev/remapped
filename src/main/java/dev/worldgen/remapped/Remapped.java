package dev.worldgen.remapped;

import dev.worldgen.remapped.color.RemappedColor;
import dev.worldgen.remapped.item.EmptyTrackerlessMap;
import dev.worldgen.remapped.network.RemappedMapUpdatePacket;
import dev.worldgen.remapped.network.RemappedEnablePacket;
import dev.worldgen.remapped.network.RemappedReadyPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Remapped implements ModInitializer {
	public static final String MOD_ID = "remapped";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Item EMPTY_MAP = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "empty_map"), new EmptyTrackerlessMap(new Item.Settings()));
	private static boolean enabled = true;

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.addBefore(Items.MAP, EMPTY_MAP));

		DynamicRegistries.registerSynced(RemappedColor.REGISTRY_KEY, RemappedColor.CODEC);

		PayloadTypeRegistry.playS2C().register(RemappedMapUpdatePacket.ID, RemappedMapUpdatePacket.CODEC);
		PayloadTypeRegistry.configurationS2C().register(RemappedEnablePacket.ID, RemappedEnablePacket.CODEC);
		PayloadTypeRegistry.configurationC2S().register(RemappedReadyPacket.ID, RemappedReadyPacket.CODEC);

		ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
			if (ServerConfigurationNetworking.canSend(handler, RemappedEnablePacket.ID)) {
				handler.addTask(new RemappedEnablePacket.RemappedEnableTask());
			} else {
				handler.disconnect(Text.literal("Install the Remapped mod to play on this server."));
			}
		});

		ServerConfigurationNetworking.registerGlobalReceiver(RemappedReadyPacket.ID, (packet, context) -> {
			context.networkHandler().completeTask(RemappedEnablePacket.RemappedEnableTask.KEY);
		});
	}

	public static void set(boolean bl) {
		enabled = bl;
	}

	public static boolean enabled() {
		return enabled;
	}

	public static boolean disabled() {
		return !enabled;
	}
}