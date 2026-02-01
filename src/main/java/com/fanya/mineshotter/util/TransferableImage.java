package com.fanya.mineshotter.util;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

public class TransferableImage implements Transferable {
    private final Image image;

    public TransferableImage(Image image) {
        this.image = image;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        return DataFlavor.imageFlavor.equals(flavor) ? image : null;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.imageFlavor};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.imageFlavor.equals(flavor);
    }
}
