package engines.graphicEngine.io.userInput;

import engines.graphicEngine.core.BenchmarkManager;
import engines.graphicEngine.core.GraphicEngineContext;
import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.renderer.Camera;
import engines.graphicEngine.core.GraphicEngine;
import engines.graphicEngine.math.geometry.Vertex3D;

import java.awt.*;
import java.awt.event.*;

public class InputManager {
    private final GraphicEngineContext graphicEngineContext;
    private final KeyboardInput keyboardInput;
    private final MouseInput mouseInput;
    private final MouseMotionInput mouseMotionInput;

    private Robot robot;
    private Point lastRobotPos = new Point(0, 0);
    private boolean isFirstMove = true;

    public double accumulatedDeltaX = 0;
    public double accumulatedDeltaY = 0;
    public double accumulatedScroll = 0;

    private boolean isHUDTogglePreviously = false;
    private boolean isBenchmarkTogglePreviously = false;
    private boolean isCameraSpotPressedPreviously = false;

    public InputManager(GraphicEngine graphicEngine) {
        this.graphicEngineContext = graphicEngine.getGraphicEngineContext();
        this.graphicEngineContext.setInputManager(this);
        this.keyboardInput = new KeyboardInput(this);
        this.mouseInput = new MouseInput(graphicEngine, this);
        this.mouseMotionInput = new MouseMotionInput(graphicEngineContext,this);

        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        this.attachTo(graphicEngine);
    }

    public void attachTo(Component c) {
        c.addKeyListener(this.keyboardInput);
        c.addMouseListener(this.mouseInput);
        c.addMouseWheelListener(this.mouseInput);
        c.addMouseMotionListener(this.mouseMotionInput);
        c.setFocusable(true);
        c.requestFocus();
    }

    public void toggleBenchmarkMode() {
        boolean isBPressedNow = keyboardInput.getKeysPressed()[KeyEvent.VK_B];
        if (isBPressedNow && !isBenchmarkTogglePreviously) {
            BenchmarkManager bm = graphicEngineContext.getBenchmarkManager();

            if (graphicEngineContext.getBenchmarkRunningState() == BenchmarkManager.State.IDLE) {
                bm.start();
            }
            else {
                System.out.println("--- FORCED STOP ---");
                bm.cancel();
            }
        }
        isBenchmarkTogglePreviously = isBPressedNow;
    }

    public void toggleHUD() {
        boolean isHUDToggledNow = keyboardInput.getKeysPressed()[KeyEvent.VK_H];

        if(isHUDToggledNow && !isHUDTogglePreviously) {
            boolean newState = !graphicEngineContext.isHUDActive();
            graphicEngineContext.setHUDActive(newState);

            if (newState) {
                System.out.println("--- HUD ON ---");
            } else {
                System.out.println("--- HUD OFF ---");
            }
        }

        isHUDTogglePreviously = isHUDToggledNow;
    }

    public void toggleCameraSpotlight() {
        boolean isCameraSpotPressedNow = keyboardInput.getKeysPressed()[KeyEvent.VK_L];

        if(isCameraSpotPressedNow && !isCameraSpotPressedPreviously) {
            boolean newState = !graphicEngineContext.isCameraSpotOn();
            graphicEngineContext.setCameraSpotOn(newState);

            if (newState) {
                System.out.println("--- Camera SpotLight ON ---");
            } else {
                System.out.println("--- Camera SpotLight OFF ---");
            }
        }

        isCameraSpotPressedPreviously = isCameraSpotPressedNow;
    }

    public void processInputs() {
        this.toggleBenchmarkMode();
        this.toggleHUD();
        this.toggleCameraSpotlight();
    }

    public boolean isKeyPressed(int keyCode) {
        return keyboardInput.getKeysPressed()[keyCode];
    }

    public synchronized void handleMouseMoving(MouseEvent e) {
        Point currentPosGlobal = e.getLocationOnScreen();

        if (isFirstMove) {
            lastRobotPos.setLocation(currentPosGlobal);
            centerMouse();
            isFirstMove = false;
            return;
        }

        if (currentPosGlobal.x == lastRobotPos.x && currentPosGlobal.y == lastRobotPos.y) {
            return;
        }

        this.accumulatedDeltaX += (currentPosGlobal.x - lastRobotPos.x);
        this.accumulatedDeltaY += (currentPosGlobal.y - lastRobotPos.y);

        centerMouse();
    }

    public void centerMouse() {
        Vertex3D globalCenter = graphicEngineContext.getCanvasCenter();
        if (globalCenter == null) return;

        int targetX = (int) globalCenter.getX();
        int targetY = (int) globalCenter.getY();

        robot.mouseMove(targetX, targetY);

        lastRobotPos.setLocation(targetX, targetY);
    }

    public void handleMouseWheelInput(MouseWheelEvent e) {
        this.accumulatedScroll += e.getWheelRotation();
    }

    public MouseMotionInput getMouseMotionInput() {
        return mouseMotionInput;
    }
}
