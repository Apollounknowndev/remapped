package dev.worldgen.remapped;

import dev.worldgen.remapped.duck.RemappedClientWorldAccess;
import dev.worldgen.remapped.duck.RemappedRendererAccess;
import dev.worldgen.remapped.map.RemappedState;
import dev.worldgen.remapped.network.s2c.MapUpdatePacket;
import dev.worldgen.remapped.network.s2c.EnablePacket;
import dev.worldgen.remapped.network.c2s.ReadyPacket;
import dev.worldgen.remapped.render.RemappedMapRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.MapIdComponent;

public class RemappedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Remapped.set(false);

		ClientPlayNetworking.registerGlobalReceiver(MapUpdatePacket.ID, (payload, context) -> {
			MinecraftClient client = context.client();
			RemappedMapRenderer renderer = ((RemappedRendererAccess)client).remapped$getRenderer();
			MapIdComponent id = payload.mapId();
			RemappedState state = client.world.remapped$getState(id);
			if (state == null) {
				state = RemappedState.of(payload.scale(), payload.locked(), client.world.getRegistryKey());
				RemappedClientWorldAccess accessor = (RemappedClientWorldAccess) client.world;
				accessor.remapped$setClientSideState(id, state);
			}

			payload.apply(state);
			renderer.getTextureManager().setNeedsUpdate(id, state);
		});

		ClientConfigurationNetworking.registerGlobalReceiver(EnablePacket.ID, ((payload, context) -> {
			Remapped.set(true);
			context.responseSender().sendPacket(ReadyPacket.INSTANCE);
		}));
	}
}