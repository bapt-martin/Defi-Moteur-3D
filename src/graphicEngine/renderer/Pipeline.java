package graphicEngine.renderer;

import graphicEngine.core.GraphicEngineContext;
import graphicEngine.scene.GameObject;
import graphicEngine.scene.Scene;
import graphicEngine.math.geometry.Plane;
import graphicEngine.math.geometry.Triangle;
import graphicEngine.math.tools.Matrix;
import graphicEngine.math.tools.Vector3D;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Pipeline {
    private final Camera camera;
    private final Scene scene;
    private final GraphicEngineContext graphicEngineContext;

    private Matrix viewMatrix;
    private List<Triangle> processedTriangle;
    private List<Triangle> trisToRender;

    List<Triangle> clippedTrianglesBuffer = new ArrayList<>();
    private List<Triangle> listPing = new ArrayList<>();
    private List<Triangle> listPong = new ArrayList<>();

    private Vector3D lightDirection = new Vector3D(0, 0, 1);

    private float[] depthBuffer;

    public Pipeline(Camera camera, Scene scene, GraphicEngineContext graphicEngineContext) {
        this.camera = camera;
        this.scene = scene;
        this.graphicEngineContext = graphicEngineContext;
        this.depthBuffer = new float[graphicEngineContext.getWindowHeight()* graphicEngineContext.getWindowWidth()];
        this.updateViewMatrix();
        this.processedTriangle = new ArrayList<>();
        this.trisToRender = new ArrayList<>();
    }

    public void execution(Graphics g) {
        this.updateViewMatrix();

        this.clearDepthBuffer();

        this.processAllGeometry();

//        this.paintersAlgorithm();

        this.rasterizePass(g);
    }

    public void updateViewMatrix() {
        this.viewMatrix = Matrix.createViewMatrix(this.camera.getCameraPosition(), this.camera.getCameraDirection(), this.camera.getCameraUp());
    }

    public void processAllGeometry() {
        Matrix projectionMatrix = this.camera.getProjectionMatrix();

        Plane frontClippingPlane = camera.getCameraFrontClippingPlane();
        Plane farClippingPlane = camera.getCameraFarClippingPlane();

        this.processedTriangle.clear();

        List<GameObject> renderQueue = scene.getRenderQueue();
        for (GameObject gameObject : renderQueue) {
            if(!gameObject.isRendered()) {
                continue;
            }

            this.processGameObject(projectionMatrix, frontClippingPlane, farClippingPlane, lightDirection, gameObject);
        }
    }

    public void processGameObject(Matrix projectionMatrix, Plane frontClippingPlane, Plane farClippingPlane, Vector3D lightDirection, GameObject gameObject) {
        Matrix worldTransformMatrix = gameObject.getWorldTransformMatrix();
        List<Triangle> gameObjectTriangles = gameObject.getMesh().getMeshTriangle();
        Texture texture = gameObject.getTexture();

        for (Triangle triMeshClean : gameObjectTriangles) {
            this.processTriangle(projectionMatrix, frontClippingPlane, farClippingPlane, lightDirection, worldTransformMatrix, triMeshClean, texture);
        }
    }

    public void processTriangle(Matrix projectionMatrix, Plane frontClippingPlane, Plane farClippingPlane, Vector3D lightDirection, Matrix worldTransformMatrix, Triangle triMeshClean, Texture texture) { //Backface Culling
        Triangle triTransformed = triMeshClean.VertexTransformed(worldTransformMatrix);
        triTransformed.setTexture(texture);

        boolean isFlipped = worldTransformMatrix.getDeterminant() < 0;
        if (!triTransformed.isFacing(this.camera,isFlipped)) {
            return;
        }

        triTransformed.setLighting(lightDirection, isFlipped);

        triTransformed.transformVertexInPlace(viewMatrix);

        this.clipAndProject(projectionMatrix, frontClippingPlane, farClippingPlane, triTransformed);
    }

    public void clipAndProject(Matrix projectionMatrix, Plane frontClippingPlane, Plane farClippingPlane, Triangle triTransformed) {
        this.clippedTrianglesBuffer.clear();

        frontClippingPlane.clipTriangleAgainstPlane(triTransformed, this.clippedTrianglesBuffer);

        int startIndex = this.processedTriangle.size();

        for (Triangle frontTri : this.clippedTrianglesBuffer) {
            farClippingPlane.clipTriangleAgainstPlane(frontTri, this.processedTriangle);
        }

        int endIndex = this.processedTriangle.size();

        for (int i = startIndex; i < endIndex; i++) {
            this.processedTriangle.get(i).projectToScreenInPlace(projectionMatrix, graphicEngineContext.getWindowWidth(), graphicEngineContext.getWindowHeight());
        }
    }

    public void paintersAlgorithm() {
        this.processedTriangle.sort((t1, t2) -> {
            double dMeanZ1 = (t1.getVertices()[0].getZ() + t1.getVertices()[1].getZ() + t1.getVertices()[2].getZ()) / 3;
            double dMeanZ2 = (t2.getVertices()[0].getZ() + t2.getVertices()[1].getZ() + t2.getVertices()[2].getZ()) / 3;
            return Double.compare(dMeanZ2, dMeanZ1);
        });
    }

    public void rasterizePass(Graphics g) {
        int winWidth = graphicEngineContext.getWindowWidth();
        int winHeight = graphicEngineContext.getWindowHeight();

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);


        for (Triangle triToClip : processedTriangle) {
            this.clipToScreen(winWidth, winHeight, triToClip);

            this.drawBatch(graphicEngineContext, g2, winWidth, depthBuffer);
        }
    }

    public void clipToScreen(int iWinWidth, int iWinHeight, Triangle triToClip) {
        listPing.clear();
        listPing.add(triToClip);

        for (int p = 0; p < 4; p++) {
            listPong.clear();
            for (Triangle test : listPing) {
                graphicEngineContext.getWindowBorderPlanes()[p].clipTriangleAgainstPlane(test, listPong);
            }

            List<Triangle> temp = listPing;
            listPing = listPong;
            listPong = temp;
        }

        this.trisToRender = listPing;
    }

    public void drawBatch(GraphicEngineContext graphicEngineContext, Graphics g, int winWidth, float[] depthBuffer) {
        for (Triangle triToDraw : trisToRender) {
            triToDraw.drawTriangle(g, false, winWidth, depthBuffer);
        }

        graphicEngineContext.incrementTriangleCount(trisToRender.size());
    }

    public void clearDepthBuffer() {
        for (int i = 0; i < depthBuffer.length; i++) {
            depthBuffer[i] = (float) 0;
        }
    }

    public Matrix getViewMatrix() {
        return viewMatrix;
    }

    public List<Triangle> getProcessedTriangle() {
        return processedTriangle;
    }
}
