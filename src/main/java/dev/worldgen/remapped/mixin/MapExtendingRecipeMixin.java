package dev.worldgen.remapped.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.worldgen.remapped.Remapped;
import dev.worldgen.remapped.map.RemappedState;
import dev.worldgen.remapped.map.RemappedUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.MapExtendingRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapExtendingRecipe.class)
public class MapExtendingRecipeMixin {
    @Inject(
        method = "matches(Lnet/minecraft/recipe/input/CraftingRecipeInput;Lnet/minecraft/world/World;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/FilledMapItem;getMapState(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;)Lnet/minecraft/item/map/MapState;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void remapped$fixMapExtendingRecipe(CraftingRecipeInput input, World world, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) ItemStack stack) {
        if (Remapped.disabled()) return;

        RemappedState state = RemappedUtils.getState(stack, world);
        if (state == null) {
            cir.setReturnValue(false);
        } else if (state.hasExplorationMapDecoration()) {
            cir.setReturnValue(false);
        } else {
            cir.setReturnValue(state.scale() < 4);
        }
    }
}
