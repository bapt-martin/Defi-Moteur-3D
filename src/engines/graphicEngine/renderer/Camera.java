package engines.graphicEngine.renderer;

import engines.graphicEngine.core.GraphicEngineContext;
import engines.graphicEngine.math.geometry.Plane;
import engines.graphicEngine.math.tools.Matrix;
import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.math.geometry.Vertex3D;
import engines.graphicEngine.scene.lightRelative.SpotLight;

import java.awt.*;

public class Camera {
    private GraphicEngineContext graphicEngineContext;

    private Vertex3D cameraPosition;
    private CameraRotation cameraRotation;
    private Vector3D cameraDirection;
    private Vector3D cameraUp;
    private Vector3D cameraRight;

    private SpotLight cameraSpot;

    private Matrix projectionMatrix;
    private Plane cameraFrontClippingPlane;
    private Plane cameraFarClippingPlane;
    private double near;
    private double far;
    private double fov;

    private double dTranslationCameraSpeed = 20;
    private double dRotationCameraSpeed = 0.5;

    private double zoom;
    private double zoomFactor;


    public Camera(GraphicEngineContext graphicEngineContext) {
        this.graphicEngineContext = graphicEngineContext;

        graphicEngineContext.setCamera(this);

        this.cameraPosition = new Vertex3D(0, 0, 0);
        this.cameraRotation = new CameraRotation(0, 0, 0);
        this.cameraDirection = new Vector3D(0, 0, 1);
        this.cameraUp = new Vector3D(0, 1, 0);
        this.cameraRight = new Vector3D(1, 0, 0);

        this.cameraFrontClippingPlane = new Plane(new Vertex3D(0, 0, 0.1), new Vector3D(0, 0, 1));
        this.cameraFarClippingPlane = new Plane(new Vertex3D(0, 0, 1000), new Vector3D(0, 0, -1));

        this.near = 0.1;
        this.far = 1000;
        this.fov = 90;
        this.zoom = 1;
        this.zoomFactor = 0.1;

        this.cameraSpot = new SpotLight(
                new Vector3D(0, 0, 0),
                0.005,
                Color.WHITE,
                new Vector3D(0, 0, 1),
                30
        );
    }

    public Camera(Vertex3D cameraPosition, CameraRotation cameraRotation, Vector3D cameraDirection, Vector3D cameraUp,
                  Plane cameraFrontClippingPlane, Plane cameraFarClippingPlane,
                  double near, double far, double fov,
                  GraphicEngineContext graphicEngineContext,
                  double cameraSpotFalloff, Color cameraSpotColor, double cameraSpotCutoffAngle) {

        this(graphicEngineContext);

        this.updateParameters(cameraPosition, cameraRotation, cameraDirection, cameraUp,
                cameraFrontClippingPlane, cameraFarClippingPlane,
                near, far, fov,
                cameraSpotFalloff, cameraSpotColor, cameraSpotCutoffAngle);
    }

    public void updateParameters(Vertex3D cameraPosition, CameraRotation cameraRotation, Vector3D cameraDirection, Vector3D cameraUp,
                                 Plane cameraFrontClippingPlane, Plane cameraFarClippingPlane,
                                 double near, double far, double fov,
                                 double cameraSpotFalloff, Color cameraSpotColor, double cameraSpotCutoffAngle) {

        this.cameraPosition.copyFrom(cameraPosition);

        this.cameraRotation = cameraRotation;

        this.cameraDirection.copyFrom(cameraDirection);
        this.cameraUp.copyFrom(cameraUp);

        this.cameraRight = this.cameraDirection.crossProduct(this.cameraUp);

        this.cameraFrontClippingPlane = cameraFrontClippingPlane;
        this.cameraFarClippingPlane = cameraFarClippingPlane;
        this.near = near;
        this.far = far;
        this.fov = fov;

        this.cameraSpot.setPosition(new Vector3D(this.cameraPosition.x, this.cameraPosition.y, this.cameraPosition.z));
        this.cameraSpot.setDirection(this.cameraDirection);
        this.cameraSpot.setFallOff(cameraSpotFalloff);
        this.cameraSpot.setLightColor(cameraSpotColor);
        this.cameraSpot.setCutOffAngle(Math.cos(Math.toRadians(cameraSpotCutoffAngle)));
    }

    public static class CameraRotation {
        public double yaw;
        public double pitch;
        public double roll;

        public CameraRotation(double yaw, double pitch, double roll) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
        }
    }

    public void updateWindowProjectionMatrix() {
        int lastWindowWidth = graphicEngineContext.getWindowWidth();
        int lastWindowHeight = graphicEngineContext.getWindowHeight();

        graphicEngineContext.updateWindowInformation();

        int currentWindowWidth = graphicEngineContext.getWindowWidth();
        int currentWindowHeight = graphicEngineContext.getWindowHeight();

        if (lastWindowWidth != currentWindowWidth || lastWindowHeight != currentWindowHeight) {
            this.updateProjectionMatrix();
        }
    }

    public void updateProjectionMatrix() {
        this.projectionMatrix = Matrix.createProjectionMatrix(this.far, this.near, this.fov, graphicEngineContext.getWindowWidth(), graphicEngineContext.getWindowHeight(), zoom);
    }

    public void updateCamReferentialMatrix() {
        Vector3D[] localAxes = {new Vector3D(0,0,-1),
                                new Vector3D(0,1,0),
                                new Vector3D(1,0,0)};

        Matrix matCameraRotYaw = Matrix.createRotationAroundAxis(cameraRotation.yaw,localAxes[1]);
        Vector3D.rotateBasisInPlace(matCameraRotYaw, localAxes);

        cameraRotation.pitch = Math.max(-89.0*Math.PI/180.0, Math.min(89.0*Math.PI/180.0, cameraRotation.pitch));
        Matrix matCameraRotPitch = Matrix.createRotationAroundAxis(cameraRotation.pitch,localAxes[2]);
        Vector3D.rotateBasisInPlace(matCameraRotPitch, localAxes);

        Matrix matCameraRotRoll = Matrix.createRotationAroundAxis(cameraRotation.roll,localAxes[0]);
        Vector3D.rotateBasisInPlace(matCameraRotRoll, localAxes);

        cameraDirection = localAxes[0];
        cameraUp = localAxes[1];
        cameraRight = localAxes[2];

        this.updateSpotLight();
    }

    public void updateSpotLight() {
        if (cameraSpot != null) {
            cameraSpot.setPosition(new Vector3D(cameraPosition.x, cameraPosition.y, cameraPosition.z));
            cameraSpot.setDirection(cameraDirection);
        }
    }

    public void translateCameraInPlace(Vector3D translationDirection, int sens, double translationSpeed, double deltaFrameTime) {
        translationDirection.scaleInPlace(sens * deltaFrameTime * translationSpeed);

        this.cameraPosition.translateInPlace(translationDirection);

        this.updateSpotLight();
    }

    public double rotateCameraInPlace(double currentRotationValue, double rotationSpeed, double deltaFrameTime) {
        return currentRotationValue + rotationSpeed * deltaFrameTime;
    }

    public Vertex3D getCameraPosition() {
        return cameraPosition;
    }


    public Vector3D getCameraDirection() {
        return cameraDirection;
    }

    public Vector3D getCameraUp() {
        return cameraUp;
    }

    public Vector3D getCameraRight() {
        return cameraRight;
    }

    public double getCamPitch() {
        return cameraRotation.pitch;
    }

    public void setCamPitch(double camPitch) {
        this.cameraRotation.pitch = camPitch;
    }

    public double getCamYaw() {
        return cameraRotation.yaw;
    }

    public void setCamYaw(double camYaw) {
        this.cameraRotation.yaw = camYaw;
    }

    public double getCamRoll() {
        return cameraRotation.roll;
    }

    public void setCamRoll(double camRoll) {
        this.cameraRotation.roll = camRoll;
    }

    public Matrix getProjectionMatrix() {
        return new Matrix(projectionMatrix);
    }

    public double getdTranslationCameraSpeed() {
        return dTranslationCameraSpeed;
    }

    public double getdRotationCameraSpeed() {
        return dRotationCameraSpeed;
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public CameraRotation getCameraRotation() {
        return cameraRotation;
    }

    public Plane getCameraFrontClippingPlane() {
        return cameraFrontClippingPlane;
    }

    public void setCameraFrontClippingPlane(Plane cameraFrontClippingPlane) {
        this.cameraFrontClippingPlane = cameraFrontClippingPlane;
    }

    public Plane getCameraFarClippingPlane() {
        return cameraFarClippingPlane;
    }

    public SpotLight getCameraSpot() {
        return cameraSpot;
    }
}


