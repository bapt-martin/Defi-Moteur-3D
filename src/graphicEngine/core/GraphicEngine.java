package graphicEngine.core;

import graphicEngine.input.InputManager;
import graphicEngine.io.ObjLoader;
import graphicEngine.math.geometry.Mesh;
import graphicEngine.math.geometry.Plane;
import graphicEngine.math.geometry.Vertex3D;
import graphicEngine.math.tools.Vector3D;
import graphicEngine.overlay.HeadUpDisplay;
import graphicEngine.renderer.Camera;
import graphicEngine.renderer.Pipeline;
import graphicEngine.renderer.Texture;
import graphicEngine.scene.GameObject;
import graphicEngine.scene.lightRelative.PointLight;
import graphicEngine.scene.Scene;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;

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
//        Graphics g = this.getGraphics();

//        g.setColor(this.getBackground());
//        g.fillRect(0, 0, getWidth(), getHeight());

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
            System.out.println("Redimensionnement dynamique réussi : " + currentWidth + "x" + currentHeight +" "+pixels.length);
        }
    }

    public GraphicEngineContext getGraphicEngineContext() {
        return graphicEngineContext;
    }
}


