package com.fanya.mineshotter;

import com.fanya.mineshotter.gui.EditorUI;
import com.fanya.mineshotter.logic.DrawingManager;
import com.fanya.mineshotter.logic.SelectionManager;
import com.fanya.mineshotter.model.LineSegment;
import com.fanya.mineshotter.util.ImageResultProcessor;
import com.fanya.mineshotter.util.TransferableImage;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.sound.PositionedSoundInstance;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.io.File;

public class ScreenshotScreen extends Screen implements EditorUI.EditorUIActions {

    private final NativeImage rawScreenshot;
    private NativeImageBackedTexture texture;
    private Identifier textureId;
    private final Screen parent;

    private final SelectionManager selectionManager = new SelectionManager();
    private final DrawingManager drawingManager = new DrawingManager();
    private final EditorUI editorUI = new EditorUI();

    public ScreenshotScreen(Screen parent, NativeImage image) {
        super(Text.translatable("mineshotter.screen.title"));
        this.parent = parent;
        this.rawScreenshot = image;
    }

    @Override
    protected void init() {
        texture = new NativeImageBackedTexture(() -> "screenshot_texture", rawScreenshot);
        textureId = Identifier.of("mineshotter", "screenshot");
        client.getTextureManager().registerTexture(textureId, texture);
        drawingManager.init(client);
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        context.drawTexture(RenderPipelines.GUI_TEXTURED, textureId, 0, 0, 0, 0, sw, sh, sw, sh);
        selectionManager.renderDimming(context, sw, sh, textRenderer);
        drawingManager.renderLines(context, client);

        if (selectionManager.selectionConfirmed) {
            editorUI.render(context, mouseX, mouseY, selectionManager, client, textRenderer);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            fullReset();
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);

        if (!selectionManager.selectionConfirmed) {
            if (selectionManager.selX1 == -1) {
                selectionManager.selX1 = selectionManager.selX2 = mx;
                selectionManager.selY1 = selectionManager.selY2 = my;
            }
            selectionManager.isSelecting = true;
            return true;
        }

        if (editorUI.handleClick(mx, my, selectionManager, client, this)) return true;

        if (selectionManager.moveMode) {
            if (selectionManager.beginMoveResize(mx, my)) return true;
            return false;
        }

        if (selectionManager.isInSelection(mx, my)) {
            drawingManager.isDrawing = true;
            drawingManager.lastDrawX = mx;
            drawingManager.lastDrawY = my;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        int mx = (int) mouseX; int my = (int) mouseY;

        if (selectionManager.isSelecting) { selectionManager.selX2 = mx; selectionManager.selY2 = my; return true; }
        if (selectionManager.selectionConfirmed && selectionManager.moveMode && selectionManager.isDragging) {
            selectionManager.applyMoveResize(mx, my);
            return true;
        }

        if (selectionManager.selectionConfirmed && !selectionManager.moveMode && drawingManager.isDrawing) {
            if (selectionManager.isInSelection(mx, my)) {
                drawingManager.lines.add(new LineSegment(drawingManager.lastDrawX, drawingManager.lastDrawY, mx, my, editorUI.getCurrentColor()));
                drawingManager.linesDirty = true;
                drawingManager.lastDrawX = mx; drawingManager.lastDrawY = my;
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (selectionManager.isSelecting) {
            selectionManager.isSelecting = false;
            selectionManager.confirmSelection();
            return true;
        }
        drawingManager.isDrawing = false;
        selectionManager.isDragging = false;
        selectionManager.dragType = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!selectionManager.selectionConfirmed) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                selectionManager.confirmSelection();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_M) {
            selectionManager.moveMode = !selectionManager.moveMode;
            editorUI.colorPickerOpen = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (editorUI.colorPickerOpen) { editorUI.colorPickerOpen = false; return true; }
            fullReset(); return true;
        }
        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_C) { copy(); close(); return true; }
        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_Z && !selectionManager.moveMode && !drawingManager.lines.isEmpty()) {
            drawingManager.lines.remove(drawingManager.lines.size() - 1);
            drawingManager.linesDirty = true;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void fullReset() {
        selectionManager.resetSelection();
        drawingManager.lines.clear();
        drawingManager.linesDirty = true;
        editorUI.colorPickerOpen = false;
        selectionManager.moveMode = true;
    }

    @Override public void copy() {
        try {
            BufferedImage awtImg = ImageResultProcessor.createBufferedImage(rawScreenshot, selectionManager, drawingManager, client.getWindow().getScaleFactor());
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(new TransferableImage(awtImg), null);
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
            client.inGameHud.getChatHud().addMessage(Text.translatable("mineshotter.chat.copied"));
            close();
        } catch (Exception e) {
            e.printStackTrace();
            client.inGameHud.getChatHud().addMessage(Text.translatable("mineshotter.chat.copy_failed", e.getMessage()));
        }
    }

    @Override public void upload() {
        try {
            NativeImage finalImage = ImageResultProcessor.createFinalNativeImage(rawScreenshot, selectionManager, drawingManager, client.getWindow().getScaleFactor());
            File tempFile = File.createTempFile("mineshotter_", ".png");
            finalImage.writeTo(tempFile.toPath());
            finalImage.close();

            String link = Uploader.uploadFile(tempFile);
            if (link != null) {
                Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                cb.setContents(new java.awt.datatransfer.StringSelection(link), null);

                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));

                client.inGameHud.getChatHud().addMessage(Text.translatable("mineshotter.chat.uploaded", link));
                close();
            } else {
                client.inGameHud.getChatHud().addMessage(Text.translatable("mineshotter.chat.upload_failed"));
            }

            tempFile.delete();
        } catch (Exception e) {
            client.inGameHud.getChatHud().addMessage(Text.translatable("mineshotter.chat.upload_error"));
        }
    }

    @Override public void back() {
        fullReset();
    }


    @Override
    public void close() {
        if (texture != null) {
            client.getTextureManager().destroyTexture(textureId);
            texture.close();
            texture = null;
        }
        if (rawScreenshot != null) {
            rawScreenshot.close();
        }
        drawingManager.close(client);
        client.setScreen(parent);
    }
}
