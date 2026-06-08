package engines.graphicEngine.core;

import engines.graphicEngine.math.geometry.Plane;
import engines.graphicEngine.math.geometry.Vertex3D;
import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.renderer.Camera;

import javax.swing.*;
import java.awt.*;

public class GraphicEngineContext {
    private final GraphicEngine graphicEngine;
    private BenchmarkManager benchmarkManager;

    private volatile boolean isRunning;

    private boolean isHUDActive = true;
    private boolean isBenchmarkRunning = false;
    private boolean isCameraSpotOn = false;

    private int windowWidth;
    private int windowHeight;
    private Vertex3D windowPosition;
    private Vertex3D canvasCenter;

    private Plane[] windowBorderPlanes;

    private int triangleCountFrame;
    private long totalTriangleCount = 0;

    private double deltaTime;
    private double elapsedTime = 0;
    private int elapsedFrame = 0;
    private int currentFPS = 0;
    private int currentUPS = 0;

    private final int UPS_TARGET = 60;
    private final int FPS_TARGET = 60;

    private Camera camera;



    public GraphicEngineContext(GraphicEngine graphicEngine, int windowWidth, int windowHeight) {
        this.graphicEngine = graphicEngine;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.windowPosition = new Vertex3D(0, 0, 0);
        this.triangleCountFrame = 0;
        this.deltaTime = 0.1;
    }

    public void updateWindowInformation() {
        this.windowWidth = graphicEngine.getWidth();
        this.windowHeight = graphicEngine.getHeight();
//        System.out.println(windowHeight + " " + windowWidth);
        if (graphicEngine.isShowing()) {
            Point loc = graphicEngine.getLocationOnScreen();
            this.windowPosition = new Vertex3D(loc.x, loc.y, 0);

            int localCenterX = this.windowWidth / 2;
            int localCenterY = this.windowHeight / 2;
            Point centerPoint = new Point(localCenterX, localCenterY);

            SwingUtilities.convertPointToScreen(centerPoint, graphicEngine);

            this.canvasCenter = new Vertex3D(centerPoint.x, centerPoint.y, 0);
            camera.updateProjectionMatrix();
        }

        this.windowBorderPlanes = new Plane[] {
                new Plane(new Vertex3D(0, 0, 0), new Vector3D(0, 1, 0)),
                new Plane(new Vertex3D(0, windowHeight - 1, 0),  new Vector3D(0, -1, 0)),
                new Plane(new Vertex3D(0, 0, 0), new Vector3D(1, 0, 0)),
                new Plane(new Vertex3D(windowWidth - 1, 0, 0),   new Vector3D(-1, 0, 0))
        };
    }

    public void incrementTriangleCount(int count) {
        this.triangleCountFrame += count;
        this.totalTriangleCount += count;
    }

    public void resetNbRenderedTriangle() {
        this.setTriangleCountFrame(0);
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public int getTriangleCountFrame() {
        return triangleCountFrame;
    }

    public void setTriangleCountFrame(int triangleCountFrame) {
        this.triangleCountFrame = triangleCountFrame;
    }

    public double getDeltaTime() {
        return deltaTime;
    }

    public Vertex3D getWindowPosition() {
        return windowPosition;
    }

    public GraphicEngine getGraphicEngine() {
        return graphicEngine;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public void setDeltaTime(double deltaTime) {
        this.deltaTime = deltaTime;
    }

    public double getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(double elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public int getUPS_TARGET() {
        return UPS_TARGET;
    }

    public int getFPS_TARGET() {
        return FPS_TARGET;
    }

    public int getCurrentFPS() {
        return currentFPS;
    }

    public void setCurrentFPS(int currentFPS) {
        this.currentFPS = currentFPS;
    }

    public int getCurrentUPS() {
        return currentUPS;
    }

    public void setCurrentUPS(int currentUPS) {
        this.currentUPS = currentUPS;
    }

    public Vertex3D getCanvasCenter() {
        return canvasCenter;
    }

    public int getElapsedFrame() {
        return elapsedFrame;
    }

    public void setElapsedFrame(int elapsedFrame) {
        this.elapsedFrame = elapsedFrame;
    }

    public void incrementElapsedFrame() {
        this.elapsedFrame += 1;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public boolean isHUDActive() {
        return isHUDActive;
    }

    public void setHUDActive(boolean HUDActive) {
        isHUDActive = HUDActive;
    }

    public boolean isBenchmarkRunning() {
        return isBenchmarkRunning;
    }

    public void setBenchmarkRunning(boolean benchmarkRunning) {
        isBenchmarkRunning = benchmarkRunning;
    }

    public boolean isCameraSpotOn() {
        return isCameraSpotOn;
    }

    public void setCameraSpotOn(boolean cameraSpotOn) {
        isCameraSpotOn = cameraSpotOn;
    }

    public BenchmarkManager getBenchmarkManager() {
        return benchmarkManager;
    }

    public void setBenchmarkManager(BenchmarkManager benchmarkManager) {
        this.benchmarkManager = benchmarkManager;
    }

    public long getTotalTriangleCount() {
        return totalTriangleCount;
    }

    public Plane[] getWindowBorderPlanes() {
        return windowBorderPlanes;
    }

    public void setWindowBorderPlanes(Plane[] windowBorderPlanes) {
        this.windowBorderPlanes = windowBorderPlanes;
    }
}
