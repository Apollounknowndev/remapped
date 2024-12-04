package dev.worldgen.remapped.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import dev.worldgen.remapped.Remapped;
import dev.worldgen.remapped.render.RemappedCartographyScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CartographyTableScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CartographyTableScreen.class)
public abstract class CartographyTableScreenMixin extends HandledScreen<CartographyTableScreenHandler> {
    @Shadow
    @Final
    private MapRenderState mapRenderState;

    public CartographyTableScreenMixin(CartographyTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(
        method = "drawBackground",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void remapped$fixMapRendering(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci, @Local(ordinal = 0) ItemStack secondSlot, @Local(ordinal = 1) ItemStack firstSlot) {
        if (Remapped.disabled()) return;

        RemappedCartographyScreen.render(context, this.x, this.y, this.client, this.mapRenderState, firstSlot, secondSlot);
        ci.cancel();
    }
}
