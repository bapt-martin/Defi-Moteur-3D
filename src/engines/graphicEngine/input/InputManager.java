package engines.graphicEngine.input;

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
    private final Camera camera;
    private final KeyboardInput keyboardInput;
    private final MouseInput mouseInput;
    private final MouseMotionInput mouseMotionInput;

    private Robot robot;
    private Point lastRobotPos = new Point(0, 0);
    private boolean isFirstMove = true;
    double accumulatedDeltaX = 0;
    double accumulatedDeltaY = 0;

    private boolean isHUDTogglePreviously = false;
    private boolean isBenchmarkTogglePreviously = false;
    private boolean isCameraSpotPressedPreviously = false;

    public InputManager(GraphicEngine graphicEngine, Camera camera) {
        this.graphicEngineContext = graphicEngine.getGraphicEngineContext();
        this.camera = camera;
        this.keyboardInput = new KeyboardInput(this);
        this.mouseInput = new MouseInput(graphicEngine, this);
        this.mouseMotionInput = new MouseMotionInput(graphicEngineContext,this);

        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
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

            if (!graphicEngineContext.isBenchmarkRunning()) {
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
        boolean isCameraSpotPressedNow = keyboardInput.getKeysPressed()[KeyEvent.VK_T];

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
        double deltaFrameTime = 1.0 / graphicEngineContext.getUPS_TARGET();
        double translationCameraSpeed = camera.getdTranslationCameraSpeed();
        double rotationCameraSpeed = camera.getdRotationCameraSpeed();

        this.handleTranslation(deltaFrameTime, translationCameraSpeed);
        this.handleRotation(deltaFrameTime, rotationCameraSpeed);
        this.applyMouseRotation(deltaFrameTime, rotationCameraSpeed);

        this.toggleBenchmarkMode();
        this.toggleHUD();
        this.toggleCameraSpotlight();
    }

    public void handleTranslation(double deltaFrameTime, double translationCameraSpeed) {
        double xDir = 0; // Strafe
        double zDir = 0; // Forward
        double yDir = 0; // Fly

        if (keyboardInput.getKeysPressed()[KeyEvent.VK_Z]) zDir += 1; // Forward
        if (keyboardInput.getKeysPressed()[KeyEvent.VK_S]) zDir -= 1; // Backward
        if (keyboardInput.getKeysPressed()[KeyEvent.VK_Q]) xDir -= 1; // Left strafe
        if (keyboardInput.getKeysPressed()[KeyEvent.VK_D]) xDir += 1; // Right strafe

        if (keyboardInput.getKeysPressed()[KeyEvent.VK_SPACE]) {
            if (keyboardInput.getKeysPressed()[KeyEvent.VK_SHIFT]) {
                yDir -= 1; // Down
            } else {
                yDir += 1; // Up
            }
        }

        if (xDir == 0 && yDir == 0 && zDir == 0) return;

        Vector3D moveX = new Vector3D(camera.getCameraRight());
        moveX.scaleInPlace(xDir);

        Vector3D moveY = new Vector3D(camera.getCameraUp());
        moveY.scaleInPlace(yDir);

        Vector3D moveZ = new Vector3D(camera.getCameraDirection());
        moveZ.scaleInPlace(zDir);
        
        Vector3D totalTranslation = moveX.add(moveZ).add(moveY);
        totalTranslation.normalizeInPlace();

        camera.translateCameraInPlace(totalTranslation, 1, translationCameraSpeed, deltaFrameTime);
    }

    public void handleRotation(double deltaFrameTime, double rotationCameraSpeed) {
        this.handlePitch(deltaFrameTime, rotationCameraSpeed);
        this.handleYaw(deltaFrameTime, rotationCameraSpeed);
        this.handleRoll(deltaFrameTime, rotationCameraSpeed);
    }

    public void handlePitch(double deltaFrameTime, double rotationCameraSpeed) {
        // UP = Anti-Clockwise X-Axis rotation Pitch
        if (keyboardInput.getKeysPressed()[KeyEvent.VK_UP]) {
            camera.setCamPitch(camera.getCamPitch() - rotationCameraSpeed * deltaFrameTime);
        }

        // DOWN = Clockwise X-Axis rotation Pitch
        if (keyboardInput.getKeysPressed()[KeyEvent.VK_DOWN]) {
            camera.setCamPitch(camera.getCamPitch() + rotationCameraSpeed * deltaFrameTime);
        }
    }

    public void handleYaw(double deltaFrameTime, double rotationCameraSpeed) {
        // RIGHT = Anti-Clockwise Y-Axis rotation Yaw
        if (keyboardInput.getKeysPressed()[KeyEvent.VK_RIGHT]) {
            camera.setCamYaw(camera.getCamYaw() + rotationCameraSpeed * deltaFrameTime);
        }

        // LEFT = Clockwise Y-Axis rotation Yaw
        if (keyboardInput.getKeysPressed()[KeyEvent.VK_LEFT]) {
            camera.setCamYaw(camera.getCamYaw() - rotationCameraSpeed * deltaFrameTime);
        }
    }

    public void handleRoll(double deltaFrameTime, double rotationCameraSpeed) {
        // A = Anti-Clockwise Z-Axis rotation Roll
        if (keyboardInput.getKeysPressed()[KeyEvent.VK_A]) {
            camera.setCamRoll(camera.getCamRoll() + rotationCameraSpeed * deltaFrameTime);
        }

        // E = Clockwise Y-Axis rotation Roll
        if (keyboardInput.getKeysPressed()[KeyEvent.VK_E]) {
            camera.setCamRoll(camera.getCamRoll() - rotationCameraSpeed * deltaFrameTime);
        }
    }

    private synchronized void applyMouseRotation(double deltaFrameTime, double rotationCameraSpeed) {
        if (this.accumulatedDeltaX != 0 || this.accumulatedDeltaY != 0) {
            double sensitivity = mouseMotionInput.getMouseSensitivity();

            camera.setCamYaw(camera.getCamYaw() + rotationCameraSpeed * this.accumulatedDeltaX * sensitivity);
            camera.setCamPitch(camera.getCamPitch() + rotationCameraSpeed * this.accumulatedDeltaY * sensitivity);

            this.accumulatedDeltaX = 0;
            this.accumulatedDeltaY = 0;
        }
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
        camera.setZoom(camera.getZoom() - e.getWheelRotation() * camera.getZoomFactor());
        System.out.println(camera.getZoom());
    }
}
