package graphicEngine.renderer;

import graphicEngine.core.GraphicEngineContext;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Rasterizer {
    private BufferedImage screenBuffer;
    private int[] pixelData;
    private int width;
    private int height;
    private GraphicEngineContext graphicEngineContext;

    public Rasterizer(GraphicEngineContext graphicEngineContext) {
        this.graphicEngineContext = graphicEngineContext;
        width = graphicEngineContext.getWindowWidth();
        height = graphicEngineContext.getWindowHeight();

        screenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        pixelData = ((DataBufferInt) screenBuffer.getRaster().getDataBuffer()).getData();
    }

    public void setPixel(int x, int y, int color) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            pixelData[y * width + x] = color;
        }
    }
}
