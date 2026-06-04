package graphicEngine.overlay;

import graphicEngine.core.GraphicEngineContext;
import graphicEngine.math.geometry.Vertex3D;
import graphicEngine.renderer.Camera;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HeadUpDisplay {
    private String fpsText      = "FPS: -";
    private String upsText      = "UPS: -";
    private String perfText     = "Perf: -";
    private String positionText = "Pos: -";
    private String rotationText = "Rot: -";
    private String triText      = "Tri: -";
    private String timeText     = "Time : -";
    private String frameText    = "Frame : -";

    private final List<Double> frameTimeHistory = new ArrayList<>();
    private final List<Integer> triCountHistory = new ArrayList<>();
    private final int MAX_HISTORY = 440;
    private int[] yMem = new int[MAX_HISTORY];

    private Color perfColor = Color.WHITE;

    private final GraphicEngineContext graphicEngineContext;

    private double lastPerfUpdate = 0;
    private double lastUiUpdate = 0;
    private final double PERF_UPDATE_INTERVAL = 0.25;
    private final double UI_UPDATE_INTERVAL;

    private final Font font = new Font("Consolas", Font.BOLD, 14);
    private final Color backgroundColor = new Color(0, 0, 0, 150);

    public HeadUpDisplay(GraphicEngineContext graphicEngineContext) {
        this.graphicEngineContext = graphicEngineContext;
        this.UI_UPDATE_INTERVAL = 1.0/graphicEngineContext.getUPS_TARGET();

        for (int i = 0; i < MAX_HISTORY; i++) {
            frameTimeHistory.add(0.0);
            triCountHistory.add(0);
        }
    }

    public void updateStats() {
        double now = graphicEngineContext.getElapsedTime();

        if (now - lastUiUpdate < UI_UPDATE_INTERVAL) {
            return;
        }
        lastUiUpdate = now;

        double deltaTime = graphicEngineContext.getDeltaTime();

        this.captureFrameMetrics();

        this.updateTransform();
        this.updateElapsedTime();
        this.updateFrameCounter();

        if (now - lastPerfUpdate < PERF_UPDATE_INTERVAL) {
            return;
        }
        lastPerfUpdate = now;

        if (deltaTime <= 0) return;

        this.updateFpsUps(deltaTime);
        this.updatePerfScore(deltaTime);
    }

    private void updateFrameCounter() {
        this.frameText = String.format("Frame : %d", graphicEngineContext.getElapsedFrame());
    }

    private void updateElapsedTime() {
        this.timeText = String.format("Time : %.1f", graphicEngineContext.getElapsedTime());
    }

    private void captureFrameMetrics() {
        double deltaTime = graphicEngineContext.getDeltaTime();
        addToHistory(frameTimeHistory, deltaTime * 1000);

        int nbTri = graphicEngineContext.getTriangleCountFrame();
        addToHistory(triCountHistory, nbTri);

        this.triText = String.format("Triangles: %d", nbTri);
    }

    private <T> void addToHistory(List<T> list, T value) {
        list.add(value);
        if (list.size() > MAX_HISTORY) {
            list.removeFirst();
        }
    }

    public void updateFpsUps(double deltaTime) {
        double currentFps = graphicEngineContext.getCurrentFPS();
        if (currentFps > 10000) {
            this.fpsText = "FPS: >10000";
        } else {
            this.fpsText = String.format("FPS: %.0f", currentFps);
        }

        this.upsText = String.format("UPS: %d", graphicEngineContext.getCurrentUPS());
    }

    public void updatePerfScore(double deltaTime) {
        double rawScore = ((1.0 / graphicEngineContext.getFPS_TARGET()) / deltaTime) * 100;
        double score = Math.min(200, rawScore);
        this.perfText = String.format("Perf: %.0f%% (%.1f ms)", score, deltaTime * 1000);
        this.perfColor = getScoreColor(score);
    }

    public void updateTransform() {
        this.updatePosition();
        this.updateRotation();
    }

    public void updatePosition() {
        Vertex3D cameraPosition = graphicEngineContext.getCamera().getCameraPosition();
        this.positionText = String.format("Pos: [X:%.1f Y:%.1f Z:%.1f]",
                cameraPosition.getX(),
                cameraPosition.getY(),
                cameraPosition.getZ()
        );
    }

    public void updateRotation() {
        Camera.CameraRotation cameraRotation = graphicEngineContext.getCamera().getCameraRotation();

        double deg = (180 / Math.PI)   ;
        this.rotationText = String.format("Rot: [Y:%.1f P:%.1f R:%.1f]",
                cameraRotation.yaw * deg,
                cameraRotation.pitch * deg,
                cameraRotation.roll * deg
        );
    }

    public void draw(Graphics g) {
        if (g instanceof Graphics2D) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        g.setFont(font);

        g.setColor(backgroundColor);
        g.fillRect(10, 10, 470, 150);


        int startX = 25;
        int startY = 35;
        int lineHeight = 20;

        g.setColor(perfColor);
        g.drawString(perfText, startX, startY);

        g.setColor(Color.WHITE);
        g.drawString(positionText, startX, startY + lineHeight);

        g.drawString(rotationText, startX, startY + lineHeight*2);

        g.drawString(fpsText, startX + 230, startY);

        g.drawString(upsText, startX + 230, startY + lineHeight);

        g.setColor(Color.CYAN);
        g.drawString(triText, startX + 230, startY + lineHeight*2);

        g.setColor(Color.WHITE);
        g.drawString(frameText, startX + 350, startY);

        g.drawString(timeText, startX + 350, startY + lineHeight);

        drawGraph(g, startX, startY + lineHeight*3, MAX_HISTORY, 50);
    }

    private void drawGraph(Graphics g, int x, int y, int w, int h) {
        final int FPS_TARGET = graphicEngineContext.getFPS_TARGET();
        double maxMs = (1.0 / FPS_TARGET) * 2 * 1000;
        double targetMs = maxMs / 2;

        this.drawScaledLine(frameTimeHistory, maxMs, null, g, x, y, w, h);

        int targetY = y + h - (int) ((targetMs / maxMs) * h);
        g.setColor(Color.GREEN);
        g.drawLine(x, targetY, x + w, targetY);

        g.setColor(Color.RED);
        g.drawLine(x, y, x + w, y);


        int maxTri = Collections.max(triCountHistory);
        if (maxTri == 0) maxTri = 1;

        this.drawScaledLine(triCountHistory, maxTri, Color.CYAN, g, x, y, w, h);
    }

    public void drawScaledLine(List<? extends Number> valueList, double max, Color color, Graphics g, int x, int y, int w, int h) {
        int size = valueList.size();
        if (size < 2) return;

        if (color != null) {
            g.setColor(color);
        }

        double startVal = valueList.getFirst().doubleValue();
        int startY = y + h - (int) ((startVal / max) * h);

        for (int i = 0; i < size - 1; i++) {
            double endVal = valueList.get(i + 1).doubleValue();

            int endY = y + h - (int) ((endVal / max) * h);

            if (color == null) {
                double localScore = ((max/2/endVal) * 100);
                g.setColor(getScoreColor(localScore));
            }

            g.drawLine(x + i, startY, x + i + 1, endY);
            startY = endY;
        }
    }

    public static Color getScoreColor(double score) {
        double clampedScore = Math.max(0, Math.min(100, score));

        float hue = (float) (clampedScore / 100.0 * 0.33); // Chromatic circle hue

        return Color.getHSBColor(hue, 1.0f, 1.0f);
    }
}
