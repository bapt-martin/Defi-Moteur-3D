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
    private int numThreads;

    private Triangle[] frameWorkQueue;
    private int trianglesToProcessCount = 0;

    private long[] threadActiveTimes;

    public Pipeline(Camera camera, Scene scene, GraphicEngineContext graphicEngineContext) {
        this.camera = camera;
        this.scene = scene;
        this.graphicEngineContext = graphicEngineContext;
        this.depthBuffer = new float[graphicEngineContext.getWindowHeight() * graphicEngineContext.getWindowWidth()];
        this.updateViewMatrix();
        this.processedTriangle = new ArrayList<>();
        this.trisToRender = new ArrayList<>();

        this.threadActiveTimes = new long[15];

        this.updateNumThreads(8);
        this.executor = Executors.newFixedThreadPool(this.numThreads);
    }

    public void execution(int[] pixels ) {
        this.updateViewMatrix();

        this.clearDepthBuffer();

        this.processAllGeometryMultiThreaded();
//        this.processAllGeometry();

        this.rasterizePassMultiThreaded(pixels);
//        this.rasterizePass(pixels);
    }

    public void updateViewMatrix() {
        this.viewMatrix = Matrix.createViewMatrix(this.camera.getCameraPosition(), this.camera.getCameraDirection(), this.camera.getCameraUp());
    }

    public void processAllGeometryMultiThreaded() {
        this.processedTriangle.clear();
        this.trianglesToProcessCount = 0;

        for (GameObject obj : scene.getRenderQueue()) {
            obj.getWorldTransformMatrix();
            if (!obj.isRendered()) {
                continue;
            }

            Triangle[] poolTriangle = obj.getPoolTriangle();

            for (Triangle t : poolTriangle) {
                this.frameWorkQueue[this.trianglesToProcessCount] = t;
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

                List<Triangle> bufferSingleThreadProcessedTriangle = new ArrayList<>();
                List<Triangle> localClippedWorker = new ArrayList<>();

                for (int j = startIdx; j < endIdx; j++) {

                    Triangle pooledTri = this.frameWorkQueue[j];

                    Matrix worldTransformMatrix = pooledTri.getParentWorldTransformMatrix();

                    pooledTri.getParentTriangle().transformInPool(worldTransformMatrix, pooledTri);

                    boolean isFlipped = worldTransformMatrix.getDeterminant() < 0;
                    if (!pooledTri.isFacing(this.camera, isFlipped)) {
                        continue;
                    }


                    pooledTri.setLighting(lightQueue, isFlipped);

                    pooledTri.transformVertexInPlace(this.viewMatrix);

                    this.clipAndProjectMultiThreaded(projectionMatrix, frontClippingPlane, farClippingPlane, pooledTri, bufferSingleThreadProcessedTriangle, localClippedWorker);
                }
                return bufferSingleThreadProcessedTriangle;
            }));
        }

        for (Future<List<Triangle>> future : futures) {
            try {
                this.processedTriangle.addAll(future.get());
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
            localResultBuffer.get(i).projectToScreenInPlace(projectionMatrix, graphicEngineContext.getWindowWidth(), graphicEngineContext.getWindowHeight());
        }
    }

    public void processAllGeometry() {
        this.processedTriangle.clear();
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

    public void rasterizePassMultiThreaded(int[] pixels) {
        int winWidth = graphicEngineContext.getWindowWidth();
        int winHeight = graphicEngineContext.getWindowHeight();

        this.updateNumThreads(1);

        int bandHeight = (int) Math.ceil((double) winHeight / numThreads);

        List<Triangle>[] threadBins = new ArrayList[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threadBins[i] = new ArrayList<>();
        }

        // 2. Le BINNING : On répartit les triangles post-clipping
        for (Triangle triToClip : processedTriangle) {
            this.clipToScreen(winWidth, winHeight, triToClip);

            for (Triangle clippedTri : trisToRender) {
                // Trouver le Y minimum et maximum du triangle
                Vertex3D[] vertices = clippedTri.getVertices();
                double minY = Math.min(vertices[0].y, Math.min(vertices[1].y, vertices[2].y));
                double maxY = Math.max(vertices[0].y, Math.max(vertices[1].y, vertices[2].y));

                // Calculer dans quels seaux ce triangle tombe
                int startBin = Math.max(0, (int) (minY / bandHeight));
                int endBin = Math.min(numThreads - 1, (int) (maxY / bandHeight));

                // Ajouter le triangle UNIQUEMENT dans les seaux concernés
                for (int i = startBin; i <= endBin; i++) {
                    threadBins[i].add(clippedTri);
                }
            }
        }

        // 3. Lancer l'assaut Multi-Threadé
//        executeThreadedRasterizationVerbose(threadBins, pixels, winWidth, bandHeight, numThreads, depthBuffer);
        executeThreadedRasterization(threadBins, pixels, winWidth, bandHeight, numThreads, depthBuffer);
    }

    private void executeThreadedRasterization(List<Triangle>[] threadBins, int[] pixels, int winWidth, int bandHeight, int numThreads, float[] depthBuffer) {
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            final List<Triangle> localTrisToRender = threadBins[i];

            // Définition des frontières de ce Thread
            final int threadMinY = threadIndex * bandHeight;
            final int threadMaxY = (threadIndex == numThreads - 1) ? graphicEngineContext.getWindowHeight() - 1 : (threadIndex + 1) * bandHeight - 1;

            futures.add(executor.submit(() -> {
                // Si la liste est vide (aucun triangle dans cette bande), le Thread ne fait RIEN ! (0% CPU)
                for (Triangle triToDraw : localTrisToRender) {
                    // On passe les limites Y à la méthode de dessin
                    triToDraw.drawTexturedTriangleMultiThreaded2(pixels, winWidth, depthBuffer, threadMinY, threadMaxY);
                }
            }));
        }

        // Attendre que tous les ouvriers aient fini la frame
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

        // 1. DÉPART DU CHRONO GLOBAL
        long passStartTime = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            final List<Triangle> localTrisToRender = threadBins[i];

            final int threadMinY = threadIndex * bandHeight;
            final int threadMaxY = (threadIndex == numThreads - 1) ? graphicEngineContext.getWindowHeight() - 1 : (threadIndex + 1) * bandHeight - 1;

            futures.add(executor.submit(() -> {
                long workerStartTime = System.nanoTime();

                for (Triangle triToDraw : localTrisToRender) {
                    triToDraw.drawTexturedTriangleMultiThreaded2(pixels, winWidth, depthBuffer, threadMinY, threadMaxY);
                }

                long workerEndTime = System.nanoTime();

                // On enregistre le temps actif en nanosecondes dans notre tableau global
                threadActiveTimes[threadIndex] = workerEndTime - workerStartTime;
            }));
        }

        // Attendre que tous les ouvriers aient fini la frame
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 4. FIN DU CHRONO GLOBAL
        long passEndTime = System.nanoTime();
        long passTotalTime = passEndTime - passStartTime;

        // 5. AFFICHAGE DU PROFILING (À faire tourner une fois de temps en temps)
        // Tu peux l'entourer d'un "if (frameCount % 60 == 0)" pour ne l'afficher qu'une fois par seconde
        printThreadOccupancy(numThreads, passTotalTime);
    }

    // Méthode utilitaire pour afficher le bilan proprement dans la console
    private void printThreadOccupancy(int numThreads, long passTotalTime) {
        System.out.println("=== Profiling des " + numThreads + " Threads (Bandes Horizontales) ===");

        for (int i = 0; i < numThreads; i++) {
            // Calcul du pourcentage : (Temps de travail / Temps total de la frame) * 100
            double occupancyPercentage = (threadActiveTimes[i] / (double) passTotalTime) * 100.0;
            double activeTimeMs = threadActiveTimes[i] / 1_000_000.0;

            // Affichage formaté (ex: "Thread 03 :  45.2% d'activité (3.20 ms)")
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

    public List<Triangle> getProcessedTriangle() {
        return processedTriangle;
    }

    public void setFrameWorkQueue(Triangle[] frameWorkQueue) {
        this.frameWorkQueue = frameWorkQueue;
    }
}
