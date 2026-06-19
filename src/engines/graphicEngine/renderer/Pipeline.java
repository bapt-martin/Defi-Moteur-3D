package engines.graphicEngine.renderer;

import engines.graphicEngine.core.GraphicEngineContext;
import engines.graphicEngine.scene.GameObject;
import engines.graphicEngine.scene.lightRelative.PointLight;
import engines.graphicEngine.scene.Scene;
import engines.graphicEngine.math.geometry.Plane;
import engines.graphicEngine.math.geometry.Triangle;
import engines.graphicEngine.math.tools.Matrix;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    private float[] depthBuffer;

    private final ExecutorService executor;
    private final int numThreads;

    private Triangle[] frameWorkQueue;
    private int trianglesToProcessCount = 0;

    public Pipeline(Camera camera, Scene scene, GraphicEngineContext graphicEngineContext) {
        this.camera = camera;
        this.scene = scene;
        this.graphicEngineContext = graphicEngineContext;
        this.depthBuffer = new float[graphicEngineContext.getWindowHeight() * graphicEngineContext.getWindowWidth()];
        this.updateViewMatrix();
        this.processedTriangle = new ArrayList<>();
        this.trisToRender = new ArrayList<>();

        int cores = Runtime.getRuntime().availableProcessors();
//        this.numThreads = Math.max(1, cores - 8);
        this.numThreads = 1;
        this.executor = Executors.newFixedThreadPool(this.numThreads);

        this.frameWorkQueue = new Triangle[scene.getTotalTrianglesCount()];
    }

    public void execution(int[] pixels ) {
        this.updateViewMatrix();

        this.clearDepthBuffer();

        this.processAllGeometry();

        this.rasterizePass(pixels);
    }

    public void updateViewMatrix() {
        this.viewMatrix = Matrix.createViewMatrix(this.camera.getCameraPosition(), this.camera.getCameraDirection(), this.camera.getCameraUp());
    }

//    public void processAllGeometryMultiThreaded() {
//        this.processedTriangle.clear();
//        this.trianglesToProcessCount = 0;
//
//        for (GameObject obj : scene.getRenderQueue()) {
//            if (!obj.isRendered()) {
//                continue;
//            }
//
//            Triangle[] poolTriangle = obj.getPoolTriangle();
//
//            for (Triangle t : poolTriangle) {
//                this.frameWorkQueue[this.trianglesToProcessCount] = t;
//                this.trianglesToProcessCount++;
//            }
//        }
//
//        if (this.trianglesToProcessCount == 0) return;
//
//        Matrix projectionMatrix = this.camera.getProjectionMatrix();
//
//        Plane frontClippingPlane = camera.getCameraFrontClippingPlane();
//        Plane farClippingPlane = camera.getCameraFarClippingPlane();
//
//        List<GameObject> renderQueue = scene.getRenderQueue(); //habunai
//
//        List<PointLight> lightQueue = scene.getLightQueue();
//
//        List<Future<List<Triangle>>> futures = new ArrayList<>();
//        int chunkSize = this.trianglesToProcessCount / this.numThreads;
//
//        for (GameObject gameObject : renderQueue) {
//            this.processGameObject(projectionMatrix, frontClippingPlane, farClippingPlane, lightQueue, gameObject);
//        }
//
//        for (int i = 0; i < this.numThreads; i++) {
//            final int startIdx = i * chunkSize;
//            final int endIdx = (i == this.numThreads - 1) ? this.trianglesToProcessCount : (i + 1) * chunkSize;
//
//            futures.add(executor.submit(() -> {
//                List<Triangle> localBuffer = new ArrayList<>();
//
//                for (int j = startIdx; j < endIdx; j++) {
//                    Triangle pooledTri = this.frameWorkQueue[j];
//
//                    Matrix worldMat = pooledTri.getParentWorldTransformMatrix();
//
//                    boolean isFlipped = worldMat.getDeterminant() < 0;
//                    if (!pooledTri.isFacing(this.camera, isFlipped)) {
//                        continue;
//                    }
//
//                    pooledTri.transformVertexInPlace(worldMat);
//                    pooledTri.transformVertexInPlace(this.viewMatrix);
//
//                    pooledTri.setLighting(lightQueue, isFlipped);
//
//                    this.clipAndProject(projectionMatrix, frontClippingPlane, farClippingPlane, pooledTri, localBuffer);
//                }
//                return localBuffer;
//            }));
//        }
//
//        // ==========================================
//        // 3. LA RÉCOLTE
//        // ==========================================
//        for (Future<List<Triangle>> future : futures) {
//            try {
//                this.processedTriangle.addAll(future.get());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public void processAllGeometry() {
        this.processedTriangle.clear();
        this.trianglesToProcessCount = 0;

        Matrix projectionMatrix = this.camera.getProjectionMatrix();

        Plane frontClippingPlane = camera.getCameraFrontClippingPlane();
        Plane farClippingPlane = camera.getCameraFarClippingPlane();

        List<GameObject> renderQueue = scene.getRenderQueue(); //habunai

        List<PointLight> lightQueue = scene.getLightQueue();

        for (GameObject gameObject : renderQueue) {
            if(!gameObject.isRendered()) {
                continue;
            }

            this.processGameObject(projectionMatrix, frontClippingPlane, farClippingPlane, lightQueue, gameObject);
        }
    }

    public void processGameObject(Matrix projectionMatrix, Plane frontClippingPlane, Plane farClippingPlane, List<PointLight> lightQueue, GameObject gameObject) {
        Matrix worldTransformMatrix = gameObject.getWorldTransformMatrix();
        Color baseColor = gameObject.getBasedColor();
        Texture texture = gameObject.getTexture();

        Triangle[] gameObjectPoolTriangles = gameObject.getPoolTriangle();
        List<Triangle> gameObjectTriangles = gameObject.getMesh().getMeshTriangle();

        int size = gameObjectTriangles.size();
        for (int i=0; i<size; i++) {
            this.processTriangle(projectionMatrix, frontClippingPlane, farClippingPlane, lightQueue, worldTransformMatrix, gameObjectTriangles.get(i), gameObjectPoolTriangles[i], texture, baseColor);
        }
    }

    public void processTriangle(Matrix projectionMatrix, Plane frontClippingPlane, Plane farClippingPlane, List<PointLight> lightQueue, Matrix worldTransformMatrix, Triangle triMeshClean, Triangle pooledTri, Texture texture,Color baseColor) { //Backface Culling
//        pooledTri = triMeshClean.transformed(worldTransformMatrix, baseColor, texture);

        triMeshClean.transformInPool(worldTransformMatrix, baseColor, texture, pooledTri);

        boolean isFlipped = worldTransformMatrix.getDeterminant() < 0;

        if (!pooledTri.isFacing(this.camera,isFlipped)) {
            return;
        }

        pooledTri.setLighting(lightQueue, isFlipped);

        pooledTri.transformVertexInPlace(viewMatrix);

        this.clipAndProject(projectionMatrix, frontClippingPlane, farClippingPlane, pooledTri);
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

    public void rasterizePass(int[] pixels) {
        int winWidth = graphicEngineContext.getWindowWidth();
        int winHeight = graphicEngineContext.getWindowHeight();

//        Graphics2D g2 = (Graphics2D) g;
//        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        for (Triangle triToClip : processedTriangle) {
            this.clipToScreen(winWidth, winHeight, triToClip);

            this.drawBatch(graphicEngineContext, pixels, winWidth, depthBuffer);
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

    public void drawBatch(GraphicEngineContext graphicEngineContext, int[] pixels, int winWidth, float[] depthBuffer) {
        for (Triangle triToDraw : trisToRender) {
            triToDraw.drawTriangle(pixels, false, winWidth, depthBuffer);
        }

        graphicEngineContext.incrementTriangleCount(trisToRender.size());
    }

    public void clearDepthBuffer() {
        int newLength = graphicEngineContext.getWindowHeight() * graphicEngineContext.getWindowWidth();
        if (newLength != depthBuffer.length) {
            java.util.Arrays.fill(depthBuffer, 0.0f);
        } else {
            depthBuffer = new float[newLength];
        }
    }

    public Matrix getViewMatrix() {
        return viewMatrix;
    }

    public List<Triangle> getProcessedTriangle() {
        return processedTriangle;
    }
}
