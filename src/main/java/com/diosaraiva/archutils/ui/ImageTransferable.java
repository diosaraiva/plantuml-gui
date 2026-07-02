package com.diosaraiva.archutils.ui;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * A {@link Transferable} wrapper that exposes an {@link Image} to the system
 * clipboard via {@link DataFlavor#imageFlavor}.
 */
public class ImageTransferable implements Transferable {

    private final Image image;

    /**
     * Creates a transferable for the given image.
     *
     * @param image the image to place on the clipboard
     */
    public ImageTransferable(Image image) {
        this.image = image;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.imageFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.imageFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!DataFlavor.imageFlavor.equals(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return image;
    }
}
