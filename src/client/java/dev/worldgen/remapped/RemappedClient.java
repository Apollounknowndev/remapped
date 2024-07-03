package dev.worldgen.remapped;

import dev.worldgen.remapped.duck.RemappedRendererAccess;
import dev.worldgen.remapped.map.RemappedState;
import dev.worldgen.remapped.network.RemappedMapUpdatePacket;
import dev.worldgen.remapped.network.RemappedEnablePacket;
import dev.worldgen.remapped.network.RemappedReadyPacket;
import dev.worldgen.remapped.render.RemappedMapRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.MapIdComponent;

import java.util.List;

public class RemappedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Remapped.set(false);

		ClientPlayNetworking.registerGlobalReceiver(RemappedMapUpdatePacket.ID, (payload, context) -> {
			MinecraftClient client = context.client();
			RemappedMapRenderer renderer = ((RemappedRendererAccess)client.gameRenderer).remapped$getRenderer();
			MapIdComponent id = payload.mapId();
			RemappedState state = client.world.remapped$getState(id);
			if (state == null) {
				state = RemappedState.of(payload.scale(), payload.locked(), client.world.getRegistryKey());
				client.world.remapped$setState(id, state);
			}

			payload.apply(state);
			renderer.updateTexture(id, state);
		});

		ClientConfigurationNetworking.registerGlobalReceiver(RemappedEnablePacket.ID, ((payload, context) -> {
			Remapped.set(true);
			context.responseSender().sendPacket(RemappedReadyPacket.INSTANCE);
		}));
	}
}