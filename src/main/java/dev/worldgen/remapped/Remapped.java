package dev.worldgen.remapped;

import dev.worldgen.remapped.color.RemappedColor;
import dev.worldgen.remapped.config.ConfigHandler;
import dev.worldgen.remapped.item.EmptyTrackerlessMap;
import dev.worldgen.remapped.network.s2c.MapUpdatePacket;
import dev.worldgen.remapped.network.s2c.EnablePacket;
import dev.worldgen.remapped.network.c2s.ReadyPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Remapped implements ModInitializer {
	public static final String MOD_ID = "remapped";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final RegistryKey<Item> EMPTY_MAP_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "empty_map"));
	public static final Item EMPTY_MAP_ITEM = Registry.register(Registries.ITEM, EMPTY_MAP_KEY, new EmptyTrackerlessMap(
		new Item.Settings().registryKey(EMPTY_MAP_KEY).component(DataComponentTypes.ITEM_MODEL, Identifier.ofVanilla("map"))
	));
	public static final ComponentType<Unit> SCALE_FROM_CENTER = Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(MOD_ID, "scale_from_center"),
		ComponentType.<Unit>builder().codec(Unit.CODEC).packetCodec(PacketCodec.unit(Unit.INSTANCE)).build()
	);

	private static boolean enabled = true;

	@Override
	public void onInitialize() {
		ConfigHandler.load();

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.addBefore(Items.MAP, EMPTY_MAP_ITEM));

		DynamicRegistries.registerSynced(RemappedColor.REGISTRY_KEY, RemappedColor.CODEC);

		PayloadTypeRegistry.playS2C().register(MapUpdatePacket.ID, MapUpdatePacket.CODEC);
		PayloadTypeRegistry.configurationS2C().register(EnablePacket.ID, EnablePacket.CODEC);
		PayloadTypeRegistry.configurationC2S().register(ReadyPacket.ID, ReadyPacket.CODEC);

		ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
			if (ServerConfigurationNetworking.canSend(handler, EnablePacket.ID)) {
				handler.addTask(new EnablePacket.RemappedEnableTask());
			} else {
				handler.disconnect(Text.literal("Install the Remapped mod to play on this server."));
			}
		});

		ServerConfigurationNetworking.registerGlobalReceiver(ReadyPacket.ID, (packet, context) -> {
			context.networkHandler().completeTask(EnablePacket.RemappedEnableTask.KEY);
		});
	}

	public static void set(boolean bl) {
		enabled = bl;
	}

	public static boolean disabled() {
		return !enabled;
	}
}