package engines.graphicEngine.io.userInput;

import engines.graphicEngine.core.GraphicEngineContext;
import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.renderer.Camera;

import java.awt.event.KeyEvent;

public class CameraController {
    private Camera targetCamera;
    private GraphicEngineContext graphicEngineContext;
    private InputManager input;


    public CameraController(Camera targetCamera, GraphicEngineContext graphicEngineContext) {
        this.targetCamera = targetCamera;
        this.graphicEngineContext = graphicEngineContext;
        this.input = graphicEngineContext.getInputManager();
    }

    public void update() {
        double frameTimeReference = 1.0 / graphicEngineContext.getUPS_TARGET();
        double translationCameraSpeed = targetCamera.getTranslationCameraSpeed();
        double rotationCameraSpeed = targetCamera.getRotationCameraSpeed();

        this.handleTranslation(frameTimeReference, translationCameraSpeed);
        this.handleRotation(frameTimeReference, rotationCameraSpeed);
        this.applyMouseRotation(frameTimeReference, rotationCameraSpeed);
        this.handleMouseWheelInput();

    }

    public void handleTranslation(double deltaFrameTime, double translationCameraSpeed) {
        double xDir = 0; // Strafe
        double zDir = 0; // Forward
        double yDir = 0; // Fly

        if (input.isKeyPressed(KeyEvent.VK_Z)) zDir += 1; // Forward
        if (input.isKeyPressed(KeyEvent.VK_S)) zDir -= 1; // Backward
        if (input.isKeyPressed(KeyEvent.VK_Q)) xDir -= 1; // Left strafe
        if (input.isKeyPressed(KeyEvent.VK_D)) xDir += 1; // Right strafe

        if (input.isKeyPressed(KeyEvent.VK_SPACE)) {
            if (input.isKeyPressed(KeyEvent.VK_SHIFT)) {
                yDir -= 1; // Down
            } else {
                yDir += 1; // Up
            }
        }

        if (xDir == 0 && yDir == 0 && zDir == 0) return;

        Vector3D moveX = new Vector3D(targetCamera.getCameraRight());
        moveX.scaleInPlace(xDir);

        Vector3D moveY = new Vector3D(targetCamera.getCameraUp());
        moveY.scaleInPlace(yDir);

        Vector3D moveZ = new Vector3D(targetCamera.getCameraDirection());
        moveZ.scaleInPlace(zDir);

        Vector3D totalTranslation = moveX.add(moveZ).add(moveY);
        totalTranslation.normalizeInPlace();

        targetCamera.translateCameraInPlace(totalTranslation, 1, translationCameraSpeed, deltaFrameTime);
    }

    public void handleRotation(double deltaFrameTime, double rotationCameraSpeed) {
        this.handlePitch(deltaFrameTime, rotationCameraSpeed);
        this.handleYaw(deltaFrameTime, rotationCameraSpeed);
        this.handleRoll(deltaFrameTime, rotationCameraSpeed);
    }

    public void handlePitch(double deltaFrameTime, double rotationCameraSpeed) {
        // UP = Anti-Clockwise X-Axis rotation Pitch
        if (input.isKeyPressed(KeyEvent.VK_UP)) {
            targetCamera.setCamPitch(targetCamera.getCamPitch() - rotationCameraSpeed * deltaFrameTime);
        }

        // DOWN = Clockwise X-Axis rotation Pitch
        if (input.isKeyPressed(KeyEvent.VK_DOWN)) {
            targetCamera.setCamPitch(targetCamera.getCamPitch() + rotationCameraSpeed * deltaFrameTime);
        }
    }

    public void handleYaw(double deltaFrameTime, double rotationCameraSpeed) {
        // RIGHT = Anti-Clockwise Y-Axis rotation Yaw
        if (input.isKeyPressed(KeyEvent.VK_RIGHT)) {
            targetCamera.setCamYaw(targetCamera.getCamYaw() + rotationCameraSpeed * deltaFrameTime);
        }

        // LEFT = Clockwise Y-Axis rotation Yaw
        if (input.isKeyPressed(KeyEvent.VK_LEFT)) {
            targetCamera.setCamYaw(targetCamera.getCamYaw() - rotationCameraSpeed * deltaFrameTime);
        }
    }

    public void handleRoll(double deltaFrameTime, double rotationCameraSpeed) {
        // A = Anti-Clockwise Z-Axis rotation Roll
        if (input.isKeyPressed(KeyEvent.VK_A)) {
            targetCamera.setCamRoll(targetCamera.getCamRoll() + rotationCameraSpeed * deltaFrameTime);
        }

        // E = Clockwise Y-Axis rotation Roll
        if (input.isKeyPressed(KeyEvent.VK_E)) {
            targetCamera.setCamRoll(targetCamera.getCamRoll() - rotationCameraSpeed * deltaFrameTime);
        }
    }

    private synchronized void applyMouseRotation(double deltaFrameTime, double rotationCameraSpeed) {
        if (input.accumulatedDeltaX != 0 || input.accumulatedDeltaY != 0) {
            double sensitivity = input.getMouseMotionInput().getMouseSensitivity();

            targetCamera.setCamYaw(targetCamera.getCamYaw() + rotationCameraSpeed * input.accumulatedDeltaX * sensitivity);
            targetCamera.setCamPitch(targetCamera.getCamPitch() + rotationCameraSpeed * input.accumulatedDeltaY * sensitivity);

            input.accumulatedDeltaX = 0;
            input.accumulatedDeltaY = 0;
        }
    }

    public void handleMouseWheelInput() {
        if (input.accumulatedScroll != 0) {
            targetCamera.setZoom(targetCamera.getZoom() - input.accumulatedScroll * targetCamera.getZoomFactor());
            input.accumulatedScroll = 0; // On reset après lecture !
        }
    }

    public Camera getTargetCamera() {
        return targetCamera;
    }

    public void setTargetCamera(Camera targetCamera) {
        this.targetCamera = targetCamera;
    }
}
