package com.fanya.mineshotter.util;

import com.fanya.mineshotter.logic.DrawingManager;
import com.fanya.mineshotter.logic.SelectionManager;
import com.fanya.mineshotter.model.LineSegment;
import net.minecraft.client.texture.NativeImage;

import java.awt.image.BufferedImage;

public class ImageResultProcessor {

    public static BufferedImage createBufferedImage(NativeImage rawScreenshot, SelectionManager sel, DrawingManager draw, double scaleFactor) {
        NativeImage nativeImg = createFinalNativeImage(rawScreenshot, sel, draw, scaleFactor);
        int w = nativeImg.getWidth();
        int h = nativeImg.getHeight();

        BufferedImage awtImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int color = nativeImg.getColor(x, y);

                int a = (color >>> 24) & 0xFF;  // Alpha
                int b = (color >>> 16) & 0xFF;  // Blue
                int g = (color >>>  8) & 0xFF;  // Green
                int r = (color >>>  0) & 0xFF;  // Red

                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                awtImg.setRGB(x, y, argb);
            }
        }
        nativeImg.close();
        return awtImg;
    }

    public static NativeImage createFinalNativeImage(NativeImage rawScreenshot, SelectionManager sel, DrawingManager draw, double scaleFactor) {
        int x1 = Math.min(sel.selX1, sel.selX2), y1 = Math.min(sel.selY1, sel.selY2);
        int guiW = Math.abs(sel.selX2 - sel.selX1), guiH = Math.abs(sel.selY2 - sel.selY1);
        int px1 = (int) (x1 * scaleFactor), py1 = (int) (y1 * scaleFactor);
        int pW = (int) (guiW * scaleFactor), pH = (int) (guiH * scaleFactor);

        NativeImage cropped = new NativeImage(pW, pH, false);

        rawScreenshot.copyRect(cropped, px1, py1, 0, 0, pW, pH, false, false);

        int thickness = (int)(DrawingManager.LINE_THICKNESS * scaleFactor);
        for (LineSegment line : draw.lines) {
            int lx1 = (int) ((line.x1 - x1) * scaleFactor);
            int ly1 = (int) ((line.y1 - y1) * scaleFactor);
            int lx2 = (int) ((line.x2 - x1) * scaleFactor);
            int ly2 = (int) ((line.y2 - y1) * scaleFactor);

            DrawingManager.drawThickLineOnImage(cropped, lx1, ly1, lx2, ly2, line.color, thickness, pW, pH);
        }
        return cropped;
    }
}
