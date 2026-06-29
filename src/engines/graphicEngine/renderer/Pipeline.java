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
    private final Scene scene;
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

    private Triangle[] geometryProcessingQueue;
    private int trianglesToProcessCount = 0;

    private long[] threadActiveTimes;

    private Tile[] tilesPool;

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

    public void execution(int[] pixels ) {
        this.updateViewMatrix();
        this.clearDepthBuffer();

        this.processAllGeometryMultiThreaded();
//        this.processAllGeometry();
        this.rasterizePassMultiThreadedTBR(pixels);
//        this.rasterizePassMultiThreaded(pixels);
//        this.rasterizePass(pixels);
    }

    public void updateViewMatrix() {
        this.viewMatrix = Matrix.createViewMatrix(this.camera.getCameraPosition(), this.camera.getCameraDirection(), this.camera.getCameraUp());
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


        List<Future<List<Triangle>>> futures = new ArrayList<>();
        int chunkSize = this.trianglesToProcessCount / this.numThreads;

        for (int i = 0; i < this.numThreads; i++) {
            final int startIdx = i * chunkSize;
            final int endIdx = (i == this.numThreads - 1) ? this.trianglesToProcessCount : (i + 1) * chunkSize;

            futures.add(executor.submit(() -> {

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

    public void rasterizePassMultiThreadedTBR(int[] pixels) {
        int winWidth = graphicEngineContext.getWindowWidth();
        int winHeight = graphicEngineContext.getWindowHeight();

        int tilesSize = 32;
        this.updateTilesPool(tilesSize);
        int cols = (int) Math.ceil((double) winWidth  / tilesSize);
        int rows = (int) Math.ceil((double) winHeight / tilesSize);


        int triangleCount = geometryProcessedTri.size();
        if (triangleCount < 300) {
            this.numThreads = 1;
        } else if (triangleCount < 1500) {
            this.numThreads = Math.min(4, Runtime.getRuntime().availableProcessors());
        } else {
            this.updateNumThreads(1);
        }


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
            graphicEngineContext.incrementTriangleCount(trisToRender.size());
            }
        }

        executeDynamicTiledRasterization(this.tilesPool, pixels, winWidth, depthBuffer);
    }

    private void executeDynamicTiledRasterization(Tile[] tiles, int[] pixels, int winWidth, float[] depthBuffer) {
        List<Future<?>> futures = new ArrayList<>();

        AtomicInteger nextTileIndex = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
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
        executeThreadedRasterizationVerbose(threadBins, pixels, winWidth, bandHeight, numThreads, depthBuffer);
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

    private void executeThreadedRasterizationVerbose(List<Triangle>[] threadBins, int[] pixels, int winWidth, int bandHeight, int numThreads, float[] depthBuffer) {
        List<Future<?>> futures = new ArrayList<>();

        long passStartTime = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            final List<Triangle> localTrisToRender = threadBins[i];

            final int threadMinY = threadIndex * bandHeight;
            final int threadMaxY = (threadIndex == numThreads - 1) ? graphicEngineContext.getWindowHeight() - 1 : (threadIndex + 1) * bandHeight - 1;

            futures.add(executor.submit(() -> {
                long workerStartTime = System.nanoTime();

                for (Triangle triToDraw : localTrisToRender) {
                    drawTexturedTriangleMultiThreaded(pixels, winWidth, depthBuffer, threadMinY, threadMaxY, triToDraw);
                }

                long workerEndTime = System.nanoTime();

                threadActiveTimes[threadIndex] = workerEndTime - workerStartTime;
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        long passEndTime = System.nanoTime();
        long passTotalTime = passEndTime - passStartTime;


        if (graphicEngineContext.getElapsedFrame() % 60 == 0) {
            printThreadOccupancy(numThreads, passTotalTime);
        }
    }

    private void printThreadOccupancy(int numThreads, long passTotalTime) {
        System.out.println("=== Profiling des " + numThreads + " Threads (Bandes Horizontales) ===");

        for (int i = 0; i < numThreads; i++) {
            double occupancyPercentage = (threadActiveTimes[i] / (double) passTotalTime) * 100.0;
            double activeTimeMs = threadActiveTimes[i] / 1_000_000.0;

            System.out.printf("Thread %02d : %5.1f%% d'activité (%5.2f ms)%n", i, occupancyPercentage, activeTimeMs);
        }
        System.out.printf("Temps total de la passe : %.2f ms%n", (passTotalTime / 1_000_000.0));
        System.out.println("==================================================\n");
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
}
