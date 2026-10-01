package engines.graphicEngine.renderer;

import engines.graphicEngine.core.GraphicEngineContext;
import engines.graphicEngine.math.geometry.Vertex3D;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static engines.graphicEngine.renderer.Rasterizer.*;

public class Pipeline {
    private final Camera camera;
    private Scene scene;
    private final GraphicEngineContext graphicEngineContext;

    private Matrix viewMatrix;
    private List<Triangle> geometryProcessedTri;
    private List<Triangle> trisToRender;

    List<Triangle> clippedTrianglesBuffer = new ArrayList<>();
    private List<Triangle> listPing = new ArrayList<>();
    private List<Triangle> listPong = new ArrayList<>();

    private float[] depthBuffer;

    private final ExecutorService executor;
    private int numThreads;

    private Triangle[] geometryProcessingQueue; //SEUL?
    private int trianglesToProcessCount = 0;

    private long[] threadActiveTimes;

    private Tile[] tilesPool;

    public long[] telemetryPipelineValue = new long[2 + 2 + 16 * 2];

    private static final int MAX_THREADS_CAPACITY = 16;

    public final double[] geoThreadOccupancySum = new double[MAX_THREADS_CAPACITY];
    public final double[] rasterThreadOccupancySum = new double[MAX_THREADS_CAPACITY];

    public final int[] geoThreadFrameCount = new int[MAX_THREADS_CAPACITY];
    public final int[] rasterThreadFrameCount = new int[MAX_THREADS_CAPACITY];

    public double totalGeometryTimeMs = 0;
    public double totalRasterizationTimeMs = 0;

    public static class Tile {
        public final int minX, maxX;
        public final int minY, maxY;
        public final List<Triangle> triToRaster = new ArrayList<>();

        public Tile(int minX, int maxX, int minY, int maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }

        public void clear() {
            this.triToRaster.clear();
        }
    }

    public Pipeline(Camera camera, Scene scene, GraphicEngineContext graphicEngineContext) {
        this.camera = camera;
        this.scene = scene;
        this.graphicEngineContext = graphicEngineContext;
        this.depthBuffer = new float[graphicEngineContext.getWindowHeight() * graphicEngineContext.getWindowWidth()];
        this.updateViewMatrix();
        this.geometryProcessedTri = new ArrayList<>();
        this.trisToRender = new ArrayList<>();

        this.threadActiveTimes = new long[15];

        this.updateNumThreads(8);
        this.executor = Executors.newFixedThreadPool(this.numThreads);

        this.updateTilesPool(32);
    }

    public void execution(int[] pixels) {
        this.updateViewMatrix();
        this.clearDepthBuffer();
        this.updateGeometryProcessingQueue();

        long startTime = System.nanoTime();
            this.processAllGeometryMultiThreaded();
    //        this.processAllGeometry();
        long endTime = System.nanoTime();
        telemetryPipelineValue[0] += (endTime - startTime);


        startTime = System.nanoTime();
            this.rasterizePassMultiThreadedTilesBR(pixels);
    //        this.rasterizePassMultiThreaded(pixels);
    //        this.rasterizePass(pixels);
        endTime = System.nanoTime();
        telemetryPipelineValue[18] += (endTime - startTime);
    }

    public void updateViewMatrix() {
        this.viewMatrix = Matrix.createViewMatrix(this.camera.getCameraPosition(), this.camera.getCameraDirection(), this.camera.getCameraUp());
    }

    public void updateGeometryProcessingQueue() {
        int requiredCapacity = 0;
        for (GameObject obj : scene.getRenderQueue()) {
            if (obj.isRendered()) {
                requiredCapacity += obj.getPoolTriangle().length;
            }
        }
        if (requiredCapacity == 0) return;

        if (this.geometryProcessingQueue == null || this.geometryProcessingQueue.length < requiredCapacity) {
            this.geometryProcessingQueue = new Triangle[requiredCapacity + 5000];
        }
    }

    public void processAllGeometryMultiThreaded() {
        this.geometryProcessedTri.clear();
        this.trianglesToProcessCount = 0;

        for (GameObject obj : scene.getRenderQueue()) {
            obj.getWorldTransformMatrix();
            if (!obj.isRendered()) {
                continue;
            }

            Triangle[] poolTriangle = obj.getPoolTriangle();

            for (Triangle t : poolTriangle) {
                this.geometryProcessingQueue[this.trianglesToProcessCount] = t;
                this.trianglesToProcessCount++;
            }
        }



        if (this.trianglesToProcessCount == 0) return;

        Matrix projectionMatrix = this.camera.getProjectionMatrix();

        Plane frontClippingPlane = camera.getCameraFrontClippingPlane();
        Plane farClippingPlane = camera.getCameraFarClippingPlane();

        List<PointLight> lightQueue = scene.getLightQueue();

        if (trianglesToProcessCount < 300) {
            this.numThreads = 1;
        } else if (trianglesToProcessCount < 1500) {
            this.updateNumThreads(Math.min(8, Runtime.getRuntime().availableProcessors()));
        } else {
            this.updateNumThreads(1);
        }


        List<Future<List<Triangle>>> futures = new ArrayList<>();
        int chunkSize = this.trianglesToProcessCount / this.numThreads;


        long passStartTime = System.nanoTime();
        for (int i = 0; i < this.numThreads; i++) {
            final int startIdx = i * chunkSize;
            final int endIdx = (i == this.numThreads - 1) ? this.trianglesToProcessCount : (i + 1) * chunkSize;

            final int threadIndex = i;
            futures.add(executor.submit(() -> {
                long workerStartTime = System.nanoTime();

                List<Triangle> localProcessedTriBuffer = new ArrayList<>();
                List<Triangle> localClippedWorker = new ArrayList<>();

                for (int j = startIdx; j < endIdx; j++) {
                    Triangle pooledTri = this.geometryProcessingQueue[j];

                    Matrix worldTransformMatrix = pooledTri.getParentWorldTransformMatrix();
                    pooledTri.getParentTriangle().transformInPool(worldTransformMatrix, pooledTri);

                    boolean isFlipped = worldTransformMatrix.getDeterminant() < 0;
                    if (!isFacing(this.camera, isFlipped, pooledTri)) {
                        continue;
                    }

                    computeGouraudLighting(lightQueue, isFlipped, pooledTri);

                    pooledTri.transformVertexInPlace(this.viewMatrix);

                    this.clipAndProjectMultiThreaded(projectionMatrix, frontClippingPlane, farClippingPlane, pooledTri, localProcessedTriBuffer, localClippedWorker);
                }
                long workerEndTime = System.nanoTime();
                this.telemetryPipelineValue[threadIndex + 2] += (workerEndTime - workerStartTime);

                return localProcessedTriBuffer;
            }));
        }

        for (Future<List<Triangle>> future : futures) {
            try {
                this.geometryProcessedTri.addAll(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        long passEndTime = System.nanoTime();
        telemetryPipelineValue[1] += (passEndTime - passStartTime);
    }

    public void clipAndProjectMultiThreaded(Matrix projectionMatrix, Plane frontClippingPlane, Plane farClippingPlane, Triangle triTransformed, List<Triangle> localResultBuffer, List<Triangle> localClippedWorker) {
        localClippedWorker.clear();

        frontClippingPlane.clipTriangleAgainstPlane(triTransformed, localClippedWorker);

        int startIndex = localResultBuffer.size();

        for (Triangle frontTri : localClippedWorker) {
            farClippingPlane.clipTriangleAgainstPlane(frontTri, localResultBuffer);
        }

        int endIndex = localResultBuffer.size();

        for (int i = startIndex; i < endIndex; i++) {
            projectToScreenInPlace(projectionMatrix, graphicEngineContext.getWindowWidth(), graphicEngineContext.getWindowHeight(), localResultBuffer.get(i));
        }
    }

    public void processAllGeometry() {
        this.geometryProcessedTri.clear();
        this.trianglesToProcessCount = 0;

        Matrix projectionMatrix = this.camera.getProjectionMatrix();

        Plane frontClippingPlane = camera.getCameraFrontClippingPlane();
        Plane farClippingPlane = camera.getCameraFarClippingPlane();

        List<GameObject> renderQueue = scene.getRenderQueue();

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
////        pooledTri = triMeshClean.transformed(worldTransformMatrix, baseColor, texture);

        triMeshClean.transformInPool(worldTransformMatrix, pooledTri);

        boolean isFlipped = worldTransformMatrix.getDeterminant() < 0;

        if (!isFacing(this.camera, isFlipped, pooledTri)) {
            return;
        }

        computeGouraudLighting(lightQueue, isFlipped, pooledTri);

        pooledTri.transformVertexInPlace(viewMatrix);

        this.clipAndProject(projectionMatrix, frontClippingPlane, farClippingPlane, pooledTri);
    }

    public void clipAndProject(Matrix projectionMatrix, Plane frontClippingPlane, Plane farClippingPlane, Triangle triTransformed) {
        this.clippedTrianglesBuffer.clear();

        frontClippingPlane.clipTriangleAgainstPlane(triTransformed, this.clippedTrianglesBuffer);

        int startIndex = this.geometryProcessedTri.size();

        for (Triangle frontTri : this.clippedTrianglesBuffer) {
            farClippingPlane.clipTriangleAgainstPlane(frontTri, this.geometryProcessedTri);
        }

        int endIndex = this.geometryProcessedTri.size();

        for (int i = startIndex; i < endIndex; i++) {
            projectToScreenInPlace(projectionMatrix, graphicEngineContext.getWindowWidth(), graphicEngineContext.getWindowHeight(), this.geometryProcessedTri.get(i));
        }
    }

    public void updateTilesPool(int tilesSize) {
        int winWidth = graphicEngineContext.getWindowWidth();
        int winHeight = graphicEngineContext.getWindowHeight();

        int cols = (int) Math.ceil((double) winWidth  / tilesSize);
        int rows = (int) Math.ceil((double) winHeight / tilesSize);
        int totalTiles = cols * rows;

        if (this.tilesPool == null || totalTiles != this.tilesPool.length) {
            this.tilesPool = new Tile[totalTiles];
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    int minX = x * tilesSize;
                    int maxX = Math.min(winWidth - 1, (x + 1) * tilesSize - 1);
                    int minY = y * tilesSize;
                    int maxY = Math.min(winHeight - 1, (y + 1) * tilesSize - 1);

                    this.tilesPool[y * cols + x] = new Tile(minX, maxX, minY, maxY);
                }
            }
        } else {
            for (Tile tile : this.tilesPool) {
                tile.clear();
            }
        }
    }

    public void rasterizePassMultiThreadedTilesBR(int[] pixels) {
        int winWidth = graphicEngineContext.getWindowWidth();
        int winHeight = graphicEngineContext.getWindowHeight();

        int tilesSize = 32;
        this.updateTilesPool(tilesSize);
        int cols = (int) Math.ceil((double) winWidth  / tilesSize);
        int rows = (int) Math.ceil((double) winHeight / tilesSize);

        for (Triangle triToClip : geometryProcessedTri) {
            this.clipToScreen(triToClip);

            for (Triangle clippedTri : trisToRender) {
                Vertex3D[] vertices = clippedTri.getVertices();

                double minX = Math.min(vertices[0].x, Math.min(vertices[1].x, vertices[2].x));
                double maxX = Math.max(vertices[0].x, Math.max(vertices[1].x, vertices[2].x));
                double minY = Math.min(vertices[0].y, Math.min(vertices[1].y, vertices[2].y));
                double maxY = Math.max(vertices[0].y, Math.max(vertices[1].y, vertices[2].y));

                int startTileX = Math.max(0, (int) (minX / tilesSize));
                int endTileX   = Math.min(cols - 1, (int) (maxX / tilesSize));
                int startTileY = Math.max(0, (int) (minY / tilesSize));
                int endTileY   = Math.min(rows - 1, (int) (maxY / tilesSize));

                for (int ty = startTileY; ty <= endTileY; ty++) {
                    for (int tx = startTileX; tx <= endTileX; tx++) {
                        this.tilesPool[ty * cols + tx].triToRaster.add(clippedTri);
                    }
                }
            }
            graphicEngineContext.incrementTriangleCount(trisToRender.size());
        }

        long passStartTime = System.nanoTime();

        executeDynamicTiledRasterization(this.tilesPool, pixels, winWidth, depthBuffer);

        long passEndTime = System.nanoTime();
        telemetryPipelineValue[19] += (passEndTime - passStartTime);
    }

    private void executeDynamicTiledRasterization(Tile[] tiles, int[] pixels, int winWidth, float[] depthBuffer) {
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger nextTileIndex = new java.util.concurrent.atomic.AtomicInteger(0);

        long[] currentFrameWorkerTimes = new long[this.numThreads];
        long passStartTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            futures.add(executor.submit(() -> {
                long workerStartTime = System.nanoTime();

                int tileIdx;

                while ((tileIdx = nextTileIndex.getAndIncrement()) < tiles.length) {
                    Tile tile = tiles[tileIdx];
                    if (tile.triToRaster.isEmpty()) {
                        continue;
                    }

                    for (Triangle triToDraw : tile.triToRaster) {
                        drawTexturedTriangleTiled(
                                pixels, winWidth, depthBuffer,
                                tile.minX, tile.maxX, tile.minY, tile.maxY,
                                triToDraw
                        );
                    }
                }
                long workerEndTime = System.nanoTime();
                this.telemetryPipelineValue[threadIndex + 20] += (workerEndTime - workerStartTime);
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void rasterizePassMultiThreaded(int[] pixels) {
        int winWidth = graphicEngineContext.getWindowWidth();
        int winHeight = graphicEngineContext.getWindowHeight();

        int triangleCount = geometryProcessedTri.size();

        if (triangleCount < 300) {
            this.numThreads = 1;
        } else if (triangleCount < 1500) {
            this.numThreads = Math.min(4, Runtime.getRuntime().availableProcessors());
        } else {
            this.updateNumThreads(1);
        }

        int bandHeight = (int) Math.ceil((double) winHeight / numThreads);

        List<Triangle>[] threadBins = new ArrayList[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threadBins[i] = new ArrayList<>();
        }

        for (Triangle triToClip : geometryProcessedTri) {
            this.clipToScreen(triToClip);

            for (Triangle clippedTri : trisToRender) {
                Vertex3D[] vertices = clippedTri.getVertices();
                double minY = Math.min(vertices[0].y, Math.min(vertices[1].y, vertices[2].y));
                double maxY = Math.max(vertices[0].y, Math.max(vertices[1].y, vertices[2].y));

                int startBin = Math.max(0, (int) (minY / bandHeight));
                int endBin = Math.min(numThreads - 1, (int) (maxY / bandHeight));

                for (int i = startBin; i <= endBin; i++) {
                    threadBins[i].add(clippedTri);
                }
            }
        }
        graphicEngineContext.incrementTriangleCount(trisToRender.size());
        executeThreadedRasterization(threadBins, pixels, winWidth, bandHeight, numThreads, depthBuffer);
    }

    private void executeThreadedRasterization(List<Triangle>[] threadBins, int[] pixels, int winWidth, int bandHeight, int numThreads, float[] depthBuffer) {
        List<Future<?>> futures = new ArrayList<>();

        for (int threadIndex = 0; threadIndex < numThreads; threadIndex++) {
            final List<Triangle> localTrisToRender = threadBins[threadIndex];

            final int threadMinY = threadIndex * bandHeight;
            final int threadMaxY = (threadIndex == numThreads - 1) ? graphicEngineContext.getWindowHeight() - 1 : (threadIndex + 1) * bandHeight - 1;

            futures.add(executor.submit(() -> {
                for (Triangle triToDraw : localTrisToRender) {
                    drawTexturedTriangleMultiThreaded(pixels, winWidth, depthBuffer, threadMinY, threadMaxY, triToDraw);
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                this.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void rasterizePass(int[] pixels) {
        int winWidth = graphicEngineContext.getWindowWidth();

        for (Triangle triToClip : geometryProcessedTri) {
            this.clipToScreen(triToClip);

            this.drawBatch(graphicEngineContext, pixels, winWidth, depthBuffer);
        }
    }

    public void clipToScreen(Triangle triToClip) {
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
            drawTexturedTriangle(pixels, winWidth, depthBuffer, triToDraw);
        }

        graphicEngineContext.incrementTriangleCount(trisToRender.size());
    }

    public void clearDepthBuffer() {
        int newLength = graphicEngineContext.getWindowHeight() * graphicEngineContext.getWindowWidth();
        if (newLength != depthBuffer.length) {
            depthBuffer = new float[newLength];
        } else {
            java.util.Arrays.fill(depthBuffer, 0.0f);
        }
    }

    public void updateNumThreads(int threadLeft) {
        int cores = Runtime.getRuntime().availableProcessors();
        numThreads = Math.max(1, cores - threadLeft);
//        numThreads = 1;
    }

    public Matrix getViewMatrix() {
        return viewMatrix;
    }

    public List<Triangle> getGeometryProcessedTri() {
        return geometryProcessedTri;
    }

    public void setGeometryProcessingQueue(Triangle[] geometryProcessingQueue) {
        this.geometryProcessingQueue = geometryProcessingQueue;
    }

    public void printOccupancyTelemetry() {
        System.out.println("====== PIPELINE TELEMETRY (OCCUPATION) ======");

        long totalGeometryPassTime = telemetryPipelineValue[1];
        System.out.printf("Temps global parallélisé Géométrie : %.3f ms\n", totalGeometryPassTime / 1_000_000.0);

        if (totalGeometryPassTime > 0) {
            for (int i = 0; i < 16; i++) {
                long threadTime = telemetryPipelineValue[i + 2];
                if (threadTime > 0) {
                    double rate = ((double) threadTime / totalGeometryPassTime) * 100.0;
                    System.out.printf("  -> Thread Géo %d : Taux d'occupation = %.2f%% (%.3f ms)\n",
                            i, rate, threadTime / 1_000_000.0);
                }
            }
        }

        long totalRasterizationPassTime = telemetryPipelineValue[19];
        System.out.printf("Temps global parallélisé Rasterization : %.3f ms\n", totalRasterizationPassTime / 1_000_000.0);

        if (totalRasterizationPassTime > 0) {
            for (int i = 0; i < 16; i++) {
                long threadTime = telemetryPipelineValue[i + 20];
                if (threadTime > 0) {
                    double rate = ((double) threadTime / totalRasterizationPassTime) * 100.0;
                    System.out.printf("  -> Thread Raster %d : Taux d'occupation = %.2f%% (%.3f ms)\n",
                            i, rate, threadTime / 1_000_000.0);
                }
            }
        }
        System.out.println("=============================================");
    }

    public void resetTelemetry() {
        java.util.Arrays.fill(this.geoThreadOccupancySum, 0.0);
        java.util.Arrays.fill(this.rasterThreadOccupancySum, 0.0);
        java.util.Arrays.fill(this.geoThreadFrameCount, 0);
        java.util.Arrays.fill(this.rasterThreadFrameCount, 0);

        this.totalGeometryTimeMs = 0;
        this.totalRasterizationTimeMs = 0;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public Scene getScene() {
        return scene;
    }
}
