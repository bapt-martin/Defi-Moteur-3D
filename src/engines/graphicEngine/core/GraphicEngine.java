package engines.graphicEngine.core;

import engines.graphicEngine.overlay.HeadUpDisplay;
import engines.graphicEngine.renderer.Camera;
import engines.graphicEngine.renderer.Pipeline;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

public class GraphicEngine extends Canvas {
    private final GraphicEngineContext graphicEngineContext;
    private BufferedImage frameBuffer;
    private int[] pixels;


    public GraphicEngine(int widthInit, int heightInit) {
        this.graphicEngineContext = new GraphicEngineContext(this, widthInit, heightInit);

        this.setPreferredSize(new Dimension(widthInit, heightInit));

        this.setBackground(new Color(150,150,200));
//        this.setBackground(Color.BLACK);
        this.setFocusable(true);
//        this.setFocusTraversalKeysEnabled(false);
        this.requestFocusInWindow();

        this.repaint();
    }


    public void initGraphics() {
        this.frameBuffer = new BufferedImage(graphicEngineContext.getWindowWidth(), graphicEngineContext.getWindowHeight(), BufferedImage.TYPE_INT_RGB);
        this.pixels = ((java.awt.image.DataBufferInt) frameBuffer.getRaster().getDataBuffer()).getData();
        this.createBufferStrategy(3);
    }

    public void render(Camera camera, Pipeline pipeline, HeadUpDisplay hud) {
        if (this.getBufferStrategy() == null) return;

        this.checkAndResizeBuffer();

        java.util.Arrays.fill(pixels, this.getBackground().getRGB());

        camera.updateWindowProjectionMatrix();
        camera.updateProjectionMatrix();

        graphicEngineContext.resetNbRenderedTriangle();

        pipeline.execution(pixels);

        BufferStrategy bs = this.getBufferStrategy();
        Graphics g = bs.getDrawGraphics();

        g.drawImage(frameBuffer, 0, 0, graphicEngineContext.getWindowWidth(), graphicEngineContext.getWindowHeight(), null);

        if (graphicEngineContext.isHUDActive()) {
            hud.draw(g);
            g.setColor(Color.GREEN);
            int centerX = graphicEngineContext.getWindowWidth() / 2;
            int centerY = graphicEngineContext.getWindowHeight() / 2;
            g.drawLine(centerX - 10, centerY, centerX + 10, centerY);
            g.drawLine(centerX, centerY - 10, centerX, centerY + 10);
        }

        g.dispose();
        bs.show();
    }

    private void checkAndResizeBuffer() {
        int currentWidth = this.getWidth();
        int currentHeight = this.getHeight();

        if (currentWidth <= 0 || currentHeight <= 0) return;

        if (frameBuffer.getWidth() != currentWidth || frameBuffer.getHeight() != currentHeight) {

            graphicEngineContext.updateWindowInformation();

            this.frameBuffer = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_RGB);
            System.out.println("Buffer : " + frameBuffer.getWidth() + "x" + frameBuffer.getHeight() +
                    " | Pixels[] : " + pixels.length);

            this.pixels = ((java.awt.image.DataBufferInt) frameBuffer.getRaster().getDataBuffer()).getData();

            this.createBufferStrategy(3);
        }
    }

    public GraphicEngineContext getGraphicEngineContext() {
        return graphicEngineContext;
    }
}


