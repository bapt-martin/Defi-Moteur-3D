package graphicEngine.renderer;

import graphicEngine.core.GraphicEngineContext;
import graphicEngine.math.geometry.Plane;
import graphicEngine.math.tools.Matrix;
import graphicEngine.math.tools.Vector3D;
import graphicEngine.math.geometry.Vertex3D;

public class Camera {
    private GraphicEngineContext graphicEngineContext;

    private Vertex3D cameraPosition;
    private CameraRotation cameraRotation;
    private Vector3D cameraDirection;
    private Vector3D cameraUp;
    private Vector3D cameraRight;

    private Matrix projectionMatrix;
    private Plane cameraClippingPlane;
    private double near;
    private double far;
    private double fov;

    private double dTranslationCameraSpeed = 10;
    private double dRotationCameraSpeed = 0.5;

    private double zoom;
    private double zoomFactor;


    public Camera(GraphicEngineContext graphicEngineContext) {
        this.cameraPosition = new Vertex3D(0, 0, 23);
        this.cameraRotation = new CameraRotation(0, 0, 0);
        this.cameraDirection = new Vector3D(0, 0, 1);
        this.cameraUp = new Vector3D(0, 1, 0);
        this.cameraRight = new Vector3D(1, 0, 0);
        this.cameraClippingPlane = new Plane(new Vertex3D(0, 0, 0.1), new Vector3D(0, 0, 1));
        this.near = 0.1;
        this.far = 1000;
        this.fov = 90;
        this.zoom = 1;
        this.zoomFactor = 0.1;
        this.graphicEngineContext = graphicEngineContext;
    }

    public Camera(double near, double far, double fov, Plane cameraClippingPlane, GraphicEngineContext graphicEngineContext) {
        this(graphicEngineContext);
        this.cameraClippingPlane = cameraClippingPlane;
        this.near = near;
        this.far = far;
        this.fov = fov;
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
    }

    public void translateCameraInPlace(Vector3D translationDirection, int sens, double translationSpeed, double deltaFrameTime) {
        translationDirection.scaleInPlace(sens * deltaFrameTime * translationSpeed);

        this.cameraPosition.translateInPlace(translationDirection);
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

    public Plane getCameraClippingPlane() {
        return cameraClippingPlane;
    }

    public void setCameraClippingPlane(Plane cameraClippingPlane) {
        this.cameraClippingPlane = cameraClippingPlane;
    }
}


