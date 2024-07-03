package dev.worldgen.remapped.duck;

import dev.worldgen.remapped.map.RemappedState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public interface RemappedWorldAccess {
    default RemappedState remapped$getState(MapIdComponent id) {
        throw new IllegalStateException("Implemented via mixin");
    }

    default void remapped$setState(MapIdComponent id, RemappedState state) {
        throw new IllegalStateException("Implemented via mixin");
    }
}
