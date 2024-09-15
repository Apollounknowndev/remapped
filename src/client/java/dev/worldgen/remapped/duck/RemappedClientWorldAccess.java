package dev.worldgen.remapped.duck;

import dev.worldgen.remapped.map.RemappedState;
import net.minecraft.component.type.MapIdComponent;

import java.util.Map;

public interface RemappedClientWorldAccess extends RemappedWorldAccess {

    default void remapped$setClientSideState(MapIdComponent id, RemappedState state) {
        throw new IllegalStateException("Implemented via mixin");
    }

    default Map<MapIdComponent, RemappedState> remapped$getStates() {
        throw new IllegalStateException("Implemented via mixin");
    }

    default void remapped$setStates(Map<MapIdComponent, RemappedState> states) {
        throw new IllegalStateException("Implemented via mixin");
    }
}
