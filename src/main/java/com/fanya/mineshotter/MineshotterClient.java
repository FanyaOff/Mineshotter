package com.fanya.mineshotter;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.sound.PositionedSoundInstance;
import org.lwjgl.glfw.GLFW;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MineshotterClient implements ClientModInitializer {

    private static final Logger LOGGER = LogManager.getLogger();
    public static KeyBinding screenshotKey;
    private static boolean captureNextFrame = false;
    private static NativeImage pendingScreenshot = null;
    private static Screen captureParent = null;
    private static boolean capturedThisFrame = false;

    @Override
    public void onInitializeClient() {
        System.setProperty("java.awt.headless", "false");

        screenshotKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mineshotter.capture",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                "category.mineshotter"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            capturedThisFrame = false;

            while (screenshotKey.wasPressed()) {
                captureNextFrame = true;
                captureParent = client.currentScreen;
            }

            if (pendingScreenshot != null) {
                client.setScreen(new ScreenshotScreen(captureParent, pendingScreenshot));
                pendingScreenshot = null;
                captureParent = null;
                captureNextFrame = false;
            }
        });

        // Use HudRenderCallback to capture after the HUD is drawn (hotbar, crosshair, etc.)
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (captureNextFrame && !capturedThisFrame && MinecraftClient.getInstance().currentScreen == null) {
                capturedThisFrame = true;
                performCapture();
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof ScreenshotScreen) return;

            ScreenEvents.afterRender(screen).register((scr, context, mouseX, mouseY, tickDelta) -> {
                if (captureNextFrame && !capturedThisFrame) {
                    capturedThisFrame = true;
                    performCapture();
                }
            });

            ScreenKeyboardEvents.allowKeyPress(screen).register((currentScreen, key, scancode, modifiers) -> {
                if (screenshotKey.matchesKey(key, scancode)) {
                    captureNextFrame = true;
                    captureParent = currentScreen;
                    return false;
                }
                return true;
            });
        });
    }

    private static void performCapture() {
        MinecraftClient client = MinecraftClient.getInstance();

        try {
            if (pendingScreenshot != null) {
                pendingScreenshot.close();
            }
            net.minecraft.client.util.ScreenshotRecorder.takeScreenshot(
                client.getFramebuffer(),
                image -> {
                    pendingScreenshot = image;
                    client.execute(() -> client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0F)));
                }
            );
        } catch (Exception e) {
            LOGGER.error("Error during screenshot capture", e);
            captureNextFrame = false;
        }
    }
}
