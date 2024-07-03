package dev.worldgen.remapped.mixin.client;

import dev.worldgen.remapped.duck.RemappedClientWorldAccess;
import dev.worldgen.remapped.duck.RemappedWorldAccess;
import dev.worldgen.remapped.map.RemappedState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.MapIdComponent;
import org.spongepowered.asm.mixin.Mixin;

import java.util.HashMap;
import java.util.Map;

@Mixin(ClientWorld.class)
public class ClientWorldMixin implements RemappedClientWorldAccess {
    private final Map<MapIdComponent, RemappedState> remapped$clientStates = new HashMap<>();
    @Override
    public RemappedState remapped$getState(MapIdComponent id) {
        return this.remapped$clientStates.get(id);
    }

    @Override
    public void remapped$setState(MapIdComponent id, RemappedState state) {
        this.remapped$clientStates.put(id, state);
    }

    @Override
    public Map<MapIdComponent, RemappedState> remapped$getStates() {
        return this.remapped$clientStates;
    }

    @Override
    public void remapped$setStates(Map<MapIdComponent, RemappedState> states) {
        this.remapped$clientStates.putAll(states);
    }
}