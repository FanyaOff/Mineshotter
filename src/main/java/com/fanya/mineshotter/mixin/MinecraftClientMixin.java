package com.fanya.mineshotter.mixin;

import com.fanya.mineshotter.MineshotterClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderEnd(boolean tick, CallbackInfo ci) {
        MineshotterClient.onRenderEnd();
    }
}