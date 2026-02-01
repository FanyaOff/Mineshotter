package com.fanya.mineshotter.mixin;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(Main.class)
public class AWTHackMixin {
    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void awtHack(CallbackInfo ci) {
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            System.out.println("[Mineshotter] Setting java.awt.headless to false");
            System.setProperty("java.awt.headless", "false");
        }
    }

}
