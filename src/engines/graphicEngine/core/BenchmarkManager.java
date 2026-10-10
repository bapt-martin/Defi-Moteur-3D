    package engines.graphicEngine.core;
    
    import engines.graphicEngine.io.ObjLoader;
    import engines.graphicEngine.math.geometry.Mesh;
    import engines.graphicEngine.math.geometry.Plane;
    import engines.graphicEngine.math.geometry.Vertex3D;
    import engines.graphicEngine.math.tools.Vector3D;
    import engines.graphicEngine.renderer.Camera;
    import engines.graphicEngine.renderer.Pipeline;
    import engines.graphicEngine.renderer.Texture;
    import engines.graphicEngine.scene.GameObject;
    import engines.graphicEngine.scene.Scene;
    import engines.graphicEngine.scene.lightRelative.PointLight;
    
    import java.awt.*;
    import java.io.*;
    import java.nio.file.Paths;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.util.Locale;
    
    import static engines.graphicEngine.renderer.Pipeline.MAX_THREADS_CAPACITY;
    
    public class BenchmarkManager {
        public enum State {
            IDLE,
            WARMUP,
            MEASURING,
            COOL_DOWN,
            FINISHED
        }


        private double angleTheta = 0;
        private double anglePhi = 0;

        private State currentState = State.IDLE;

        private final GraphicEngineContext graphicEngineContext;
        private final Pipeline pipeline;
        private BenchmarkMetrics metrics;
        private Scene benchmarkScene;
        private Scene previousScene;

        private int startFrameOfCurrentState = 0;

        private final int WARMUP_FRAMES = 5;
        private final int MEASURE_FRAMES = 15;
        private final int COOLDOWN_FRAMES = 5;

        float[] frameTimesBuffer = new float[MEASURE_FRAMES];

        boolean telemetryResetDone = false;

        public long[] telemetryPipelineValue = new long[2 + 2 + 16 * 2];

        public final double[] geoThreadOccupancySum = new double[MAX_THREADS_CAPACITY];
        public final double[] rasterThreadOccupancySum = new double[MAX_THREADS_CAPACITY];

        public final int[] geoThreadFrameCount = new int[MAX_THREADS_CAPACITY];
        public final int[] rasterThreadFrameCount = new int[MAX_THREADS_CAPACITY];

        public double totalGeometryTimeMs = 0;
        public double totalRasterizationTimeMs = 0;

        public static class BenchmarkMetrics {
            // --- Meta / Context ---
            public String commentary;
            public int windowWidth;
            public int windowHeight;

            // --- Performance : Frame Time ---
            public int totalRenderedFrames = 0;
            public float averageFrameTime;
            public float minFrameTime;
            public float maxFrameTime;
            public float averageBestFrameTime;
            public float averageWorstFrameTime;
            public long benchmarkTimeStart = 0;
            public long benchmarkTimeEnd = 0;
            public long previousFrameTime = 0;

            // --- Performance : FPS ---
            public double benchmarkAverageFPS = 0;
            public double benchmarkDuration = 0;
            public float onePercentLowFPS;
            public float onePercentHighFPS;

            // --- Pipeline : Overhead ---
            public float pipelineOverheadDuration;

            // --- Pipeline : Geometry ---
            public double minTrianglesPerSecond = Double.MAX_VALUE;
            public double maxTrianglesPerSecond = 0.0;
            public long benchmarkTotalRenderedTriangleCount = 0;
            public float geometryPassDuration;
            public float geometryPassDurationRatio;
            public double averageTrianglesPerFrame;
            public long culledTrianglesPerFrame;
            public float cullingEfficiencyPercentage;
            public double[] geometryThreadOccupation = new double[16];

            // --- Pipeline : Rasterization ---
            public float rasterizationPassDuration;
            public float rasterizationPassDurationRatio;
            public long rasterizedTrianglesPerFrame;
            public long pixelsProcessedPerFrame;
            public float fillRateMegapixels;
            public float overdrawRatio;
            public long zBufferRejectionsPerFrame;
            public float zBufferRejectionPercentage;
            public double[] rasterizationThreadOccupation = new double[16];

            public float averageUsedMemoryMB;
        }

        public BenchmarkManager(GraphicEngineContext graphicEngineContext, Pipeline pipeline) {
            this.graphicEngineContext = graphicEngineContext;
            this.pipeline = pipeline;
            this.metrics = new BenchmarkMetrics();
            this.initBenchmarkScene();
        }
    
        public void initBenchmarkScene() {
            this.benchmarkScene = new Scene();
            this.graphicEngineContext.getCamera().updateParameters(new Vertex3D(0, 0, 30),
                    new Camera.CameraRotation(0, 0, 0),
                    new Vector3D(0, 0, 1),
                    new Vector3D(0, 1, 0),
                    new Plane(new Vertex3D(0, 0, 0.1f), new Vector3D(0, 0, 1)),
                    new Plane(new Vertex3D(0, 0, 100), new Vector3D(0, 0, -1)),
                    0.1f,50,90,
                    0.005f, Color.WHITE, 15);
    
            benchmarkScene.addLight("cameraSpotLight", this.graphicEngineContext.getCamera().getCameraSpot());
    
            //Mesh loading
            Mesh teapot          = ObjLoader.loadMesh(Paths.get("obj model\\objTextureLess\\teapot.obj"));
            Mesh axis            = ObjLoader.loadMesh(Paths.get("obj model\\objTextureLess\\axis.obj"));
            Mesh centeredCube    = ObjLoader.loadMesh(Paths.get("obj model\\objTextureLess\\cube.obj"));
            Mesh outCenteredCube = ObjLoader.loadMesh(Paths.get("obj model\\objTextureLess\\cube pas centré.obj"));
            Mesh texturedCube    = ObjLoader.loadMesh(Paths.get("obj model\\objWithTexture\\cubeTexture.obj"));
            Mesh texturedSphere  = ObjLoader.loadMesh(Paths.get("obj model\\objWithTexture\\sphereTexture.obj"));
    
            this.benchmarkScene.addMesh("teapot", teapot);
            this.benchmarkScene.addMesh("axis", axis);
            this.benchmarkScene.addMesh("centeredCube", centeredCube);
            this.benchmarkScene.addMesh("outCenteredCube", outCenteredCube);
            this.benchmarkScene.addMesh("texturedCube", texturedCube);
            this.benchmarkScene.addMesh("texturedSphere", texturedSphere);
    
    
            Texture texturedCubeTexture = new Texture("obj model\\texture\\textureTest1.png");
            Texture photoTexture = new Texture("obj model\\texture\\photo.png");
            this.benchmarkScene.addTexture("texturedCubeTexture",texturedCubeTexture);
            this.benchmarkScene.addTexture("photoTexture",photoTexture);
    
    
            for (int i = 0; i < 30; i++) {
                this.benchmarkScene.addGameObject("teapot" + i, new GameObject(teapot, Color.WHITE));
            }
    
            PointLight sun1 = new PointLight(
                    new Vector3D(10, 5, 0),
                    0.05f,
                    new Color(255, 180, 50)
            );
            benchmarkScene.addLight("sunLight1", sun1);
            benchmarkScene.addGameObject("sun1", new GameObject(texturedSphere, texturedCubeTexture));
            benchmarkScene.getGameObject("sun1").setPosition(10, 5, 10);
    
            PointLight sun2 = new PointLight(
                    new Vector3D(-10, 5, 0),
                    0.05f,
                    new Color(255, 180, 50)
            );
            benchmarkScene.addLight("sunLight2", sun2);
            benchmarkScene.addGameObject("sun2", new GameObject(texturedSphere, texturedCubeTexture));
            benchmarkScene.getGameObject("sun2").setPosition(-10, 5, 10);
    
            benchmarkScene.addGameObject("axis1",   new GameObject(axis, Color.BLUE));
    
    
            benchmarkScene.addGameObject("texturedCube", new GameObject(texturedCube,texturedCubeTexture));
            benchmarkScene.addGameObject("wall", new GameObject(texturedCube,photoTexture));
    
    
            benchmarkScene.getGameObject("wall").setRotation(0, 180, 0);;
            benchmarkScene.getGameObject("wall").setPosition(-25, 0, 0);
            benchmarkScene.getGameObject("wall").setScale(10, 10, 10);
    
    
            benchmarkScene.getGameObject("texturedCube").setRotation(45, 45, 45);;
    
    
            benchmarkScene.getGameObject("axis1").setPosition(0, 0, 0);
            benchmarkScene.getGameObject("axis1").setScale(-0.3f, 0.3f, 0.3f);
    
            GameObject t1 = benchmarkScene.getGameObject("teapot1");
            t1.setPosition(35, 0, 8);
            t1.setRotation(0, 0, 0);
            t1.setScale(10, 10, 10);
        }
    
        public void updateBenchmarkScene() {
            this.graphicEngineContext.getCamera().getCameraSpot().setOn(true);
    
            angleTheta += 0.07;
            anglePhi += 0.01;
    
            double r = 13.0;
    
            double hR = r * Math.cos(anglePhi);
            float x = (float) (hR * Math.cos(angleTheta / 2));
            float y = (float) (r * Math.sin(anglePhi));
            float z = (float) (hR * Math.sin(angleTheta));
    
            float sX = (float) (1.0 + (0.5 * Math.sin(anglePhi)));
            float sY = (float) (1.0 + (0.5 * Math.sin(angleTheta)));
            float sZ = (float) (1.0 + (0.5 * Math.cos(anglePhi)));
    
            int totalTeapots = 30;
            for (int i = 2; i <= totalTeapots + 1; i++) {
                GameObject teapot = benchmarkScene.getGameObject("teapot" + i);
                if (teapot == null) continue;
    
                double step = (2 * Math.PI) / totalTeapots;
                double individualTheta = angleTheta + (i * step);
                double individualPhi = anglePhi + (i * step);
    
                hR = r * Math.cos(anglePhi);
                x = (float) (r * Math.cos(individualTheta));
                y = (float) (hR * Math.sin(individualPhi));
                z = (float) (r * Math.sin(individualTheta));
                teapot.setPosition(x, y, z);
    
                sX = (float) (1.0 + (3 * Math.sin(individualPhi)));
                sY = (float) (1.0 + (3 * Math.sin(individualTheta)));
                sZ = (float) (1.0 + (3 * Math.cos(individualPhi)));
                teapot.setScale(sX, sY, sZ);
    
                float rx = (float) (individualTheta);
                float ry = (float) (individualTheta * (i % 2 == 0 ? 1 : -1));
                float rz = (float) (individualPhi);
                teapot.rotate(rx, ry, rz);
            }
    
            benchmarkScene.getGameObject("texturedCube").setScale(sX, sY, sZ);
            benchmarkScene.getGameObject("texturedCube").rotate(0.5f, 1, 1.5f);
            benchmarkScene.getGameObject("texturedCube").setPosition(x, y, z);
    
            benchmarkScene.getGameObject("sun1").setPosition(x, 5, z);
            benchmarkScene.getGameObject("sun2").setPosition(-x, 5, z);
    
            benchmarkScene.getLight("sunLight1").setOn(true);
            benchmarkScene.getLight("sunLight1").setLightColor(Color.GREEN);
            benchmarkScene.getLight("sunLight2").setOn(true);
            benchmarkScene.getLight("sunLight2").setLightColor(Color.BLUE);
    
            benchmarkScene.linkLight("sun1", "sunLight1");
            benchmarkScene.linkLight("sun2", "sunLight2");
        }
    
        public void start() {
            if (currentState == State.WARMUP || currentState == State.MEASURING) return;
            this.currentState = State.WARMUP;

//            this.graphicEngineContext.setWindowHeight(1500);
//            this.graphicEngineContext.setWindowWidth(600);

            System.out.println("--- BENCHMARK STARTED ---");
            System.out.println("--- WARM UP ---");
    
            this.previousScene = this.pipeline.getScene();
            this.pipeline.setScene(benchmarkScene);
    
            this.startFrameOfCurrentState = graphicEngineContext.getElapsedFrame();
    
            metrics.benchmarkAverageFPS = 0;
            metrics.benchmarkDuration = 0;
    
            metrics.minTrianglesPerSecond = Double.MAX_VALUE;
            metrics.maxTrianglesPerSecond = 0.0;
        }
    
        public void cancel() {
            System.out.println("--- BENCHMARK CANCELLED ---");
            this.currentState = State.IDLE;
            graphicEngineContext.setBenchmarkRunning(this.isRunning());
        }
    
        public void update() {
            if (currentState == State.IDLE || currentState == State.FINISHED) return;
    
            int currentFrame = graphicEngineContext.getElapsedFrame();
            int frameNumber = currentFrame - startFrameOfCurrentState - 1;
    
            long currentTime = System.nanoTime();

            switch (currentState) {
                case WARMUP:
                    if (frameNumber >= WARMUP_FRAMES) {
                        System.out.println("--- WARMUP DONE ---");
                        System.out.println("--- START OF MEASURE ---");
    
                        this.currentState = State.MEASURING;
                        graphicEngineContext.setBenchmarkRunning(this.isRunning());

                        metrics.previousFrameTime = currentTime;

                        this.startFrameOfCurrentState = currentFrame;

                        if (!telemetryResetDone) {
                            this.resetTelemetry();
                            telemetryResetDone = true;
                        }

                        metrics.benchmarkTimeStart = System.nanoTime();
                    }
                    break;
    
                case MEASURING:
                    System.out.println(frameNumber);
                    if (frameNumber >= MEASURE_FRAMES) {
                        System.out.println("--- MEASURE DONE ---");
                        System.out.println("--- START OF COOL DOWN ---");
                        this.currentState = State.COOL_DOWN;

                        metrics.benchmarkTimeEnd = currentTime;

                        graphicEngineContext.setBenchmarkRunning(this.isRunning());

                        this.startFrameOfCurrentState = currentFrame;
                        break;
                    }

                    long trianglesThisFrame = graphicEngineContext.getCurrentFrameRenderedTriangleCount();
                    metrics.benchmarkTotalRenderedTriangleCount += trianglesThisFrame;

                    System.out.println(metrics.benchmarkTotalRenderedTriangleCount);
                    System.out.println(trianglesThisFrame);

                    frameTimesBuffer[frameNumber] = (float) graphicEngineContext.getDeltaFrameTime();
                    metrics.totalRenderedFrames++;

                    if (currentTime - metrics.previousFrameTime > 0) {
                        double deltaFrameTime = graphicEngineContext.getDeltaFrameTime();
    
                        double currentTrianglesPerSecond = trianglesThisFrame / deltaFrameTime;

                        if (currentTrianglesPerSecond < metrics.minTrianglesPerSecond) {
                            metrics.minTrianglesPerSecond = currentTrianglesPerSecond;
                        }
                        if (currentTrianglesPerSecond > metrics.maxTrianglesPerSecond) {
                            metrics.maxTrianglesPerSecond = currentTrianglesPerSecond;
                        }
                    }

                    metrics.previousFrameTime = currentTime;

                    break;
    
                case COOL_DOWN:
                    if (frameNumber >= COOLDOWN_FRAMES) {
                        System.out.println("--- COOL DOWN DONE ---");
                        finish();
                    }
                    break;
    
                case IDLE:
                    System.out.println("--- BENCHMARK IDLE ---");
                    break;

                default:
                    break;
            }
        }
    
        public void finish() {
            System.out.println("--- END OF BENCHMARK ---");
            this.pipeline.setScene(previousScene);
            telemetryResetDone = false;
    
            java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(this.graphicEngineContext.getGraphicEngine());
            if (parentWindow != null) {
                parentWindow.dispose();
            }
            graphicEngineContext.setRunning(false);

            computeBenchmarkResults();
            printBenchmarkResult();
            saveBenchmarkResultToCSV();

            this.currentState = State.FINISHED;

            graphicEngineContext.setBenchmarkRunning(this.isRunning());
        }

        private void computeBenchmarkResults() {
            if (metrics.totalRenderedFrames == 0) {
                System.out.println("\n-> [PROFILE] No data collected");
                return;
            }

            metrics.benchmarkDuration = (metrics.benchmarkTimeEnd - metrics.benchmarkTimeStart) / 1_000_000_000.0;
            metrics.benchmarkAverageFPS = MEASURE_FRAMES / metrics.benchmarkDuration;

            if (metrics.minTrianglesPerSecond == Double.MAX_VALUE) {
                metrics.minTrianglesPerSecond = 0;
            }

            float sum = 0;
            metrics.maxFrameTime = 0;
            metrics.minFrameTime = Float.MAX_VALUE;

            int sampleLimit = Math.min(metrics.totalRenderedFrames, frameTimesBuffer.length);
            for (int i = 0; i < sampleLimit; i++) {
                float ft = frameTimesBuffer[i];
                sum += ft;
                if (ft > metrics.maxFrameTime) metrics.maxFrameTime = ft;
                if (ft < metrics.minFrameTime) metrics.minFrameTime = ft;
            }

            metrics.averageFrameTime = sum / sampleLimit;

            java.util.Arrays.sort(frameTimesBuffer, 0, sampleLimit);
            int onePercentCount = Math.max(1, sampleLimit / 100);

            float sumBest = 0;
            for (int i = 0; i < onePercentCount; i++) {
                sumBest += frameTimesBuffer[i];
            }
            metrics.averageBestFrameTime = sumBest / onePercentCount;
            metrics.onePercentHighFPS = 1.0f / metrics.averageBestFrameTime;


            float sumWorst = 0;
            for (int i = sampleLimit - onePercentCount; i < sampleLimit; i++) {
                sumWorst += frameTimesBuffer[i];
            }
            metrics.averageWorstFrameTime = sumWorst / onePercentCount;
            metrics.onePercentLowFPS = 1.0f / metrics.averageWorstFrameTime;

            metrics.averageTrianglesPerFrame = (float) metrics.benchmarkTotalRenderedTriangleCount / metrics.totalRenderedFrames;


            metrics.geometryPassDuration = (float) (this.pipeline.profilerRawData.telemetryPipelineValue[0] / 1_000_000_000.0f) / metrics.totalRenderedFrames;
            metrics.rasterizationPassDuration = (float) (this.pipeline.profilerRawData.telemetryPipelineValue[18] / 1_000_000_000.0f) / metrics.totalRenderedFrames;


            for (int i = 0; i < 16; i++) {
                int activeFrames = this.pipeline.profilerRawData.geoThreadFrameCount[i];
                if (activeFrames > 0) {
                    metrics.geometryThreadOccupation[i] = this.pipeline.profilerRawData.geoThreadOccupancySum[i] / activeFrames;
                } else {
                    metrics.geometryThreadOccupation[i] = 0.0;
                }
            }

            for (int i = 0; i < 16; i++) {
                int activeFrames = this.pipeline.profilerRawData.rasterThreadFrameCount[i];
                if (activeFrames > 0) {
                    metrics.rasterizationThreadOccupation[i] = this.pipeline.profilerRawData.rasterThreadOccupancySum[i] / activeFrames;
                } else {
                    metrics.rasterizationThreadOccupation[i] = 0.0;
                }
            }

            metrics.geometryPassDurationRatio = (metrics.geometryPassDuration /(metrics.geometryPassDuration + metrics.rasterizationPassDuration)) * 100;
            metrics.rasterizationPassDurationRatio = (metrics.rasterizationPassDuration /(metrics.geometryPassDuration + metrics.rasterizationPassDuration)) * 100;
        }

        private void printBenchmarkResult() {
            if (this.metrics == null) return;

            System.out.println("\n==================================================");
            System.out.println("-> [PROFILE] BENCHMARK REPORT");
            System.out.println("==================================================");

            System.out.println("[GENERAL]");
            System.out.printf("     |-- Benchmark Duration : %.3f s%n", metrics.benchmarkDuration);
            System.out.printf("     |-- Total frames       : %d%n", metrics.totalRenderedFrames);

            System.out.println("--------------------------------------------------");
            System.out.println("[PERFORMANCE & FLUIDITY]");
            System.out.printf("     |-- Mean FPS           : %.2f FPS%n", metrics.benchmarkAverageFPS);
            System.out.printf("     |-- 1%% High FPS       : %.2f FPS%n", metrics.onePercentHighFPS);
            System.out.printf("     |-- 1%% Low FPS        : %.2f FPS%n", metrics.onePercentLowFPS);
            System.out.printf("     |-- Average Frame Time : %.3f ms%n", metrics.averageFrameTime * 1000.0);
            System.out.printf("     |-- Best frame         : %.3f ms (équiv. %.1f FPS)%n", metrics.minFrameTime * 1000.0, metrics.minFrameTime > 0 ? 1.0 / metrics.minFrameTime : 0.0);
            System.out.printf("     |-- Worst Frame        : %.3f ms (équiv. %.1f FPS)%n", metrics.maxFrameTime * 1000.0, metrics.maxFrameTime > 0 ? 1.0 / metrics.maxFrameTime : 0.0);

            System.out.println("--------------------------------------------------");
            System.out.println("[PIPELINE RATIOS]");
            System.out.printf("     |-- Geometry           : %s (%.2f ms)%n", createProgressBar(metrics.geometryPassDurationRatio), metrics.geometryPassDuration);
            System.out.printf("     |-- Rasterization      : %s (%.2f ms)%n", createProgressBar(metrics.rasterizationPassDurationRatio), metrics.rasterizationPassDuration);

            System.out.println("--------------------------------------------------");
            System.out.println("[GEOMETRY WORKLOAD]");
            System.out.printf("     |-- Total Triangles    : %d%n", metrics.benchmarkTotalRenderedTriangleCount);
            System.out.printf("     |-- Avg Triangles/Frame: %.0f%n", metrics.averageTrianglesPerFrame);
            System.out.printf("     |-- Peak Throughput    : %.0f Tri/sec%n", metrics.maxTrianglesPerSecond);
            System.out.printf("     |-- Lowest Throughput  : %.0f Tri/sec%n", metrics.minTrianglesPerSecond);
            System.out.println("     |-- Top Active Cores   :");
            printTopThreads(metrics.geometryThreadOccupation, 4); // Affiche les 4 meilleurs

            System.out.println("--------------------------------------------------");
            System.out.println("[RASTERIZATION WORKLOAD]");
            System.out.printf("     |-- Triangles Drawn    : %d / frame%n", metrics.rasterizedTrianglesPerFrame);
            System.out.printf("     |-- Pixels Processed   : %d px / frame%n", metrics.pixelsProcessedPerFrame);
            System.out.printf("     |-- Fill Rate          : %.1f Megapixels/sec%n", metrics.fillRateMegapixels);
            System.out.printf("     |-- Overdraw Ratio     : %.2fx%n", metrics.overdrawRatio);
            System.out.printf("     |-- Z-Buffer Rejects   : %d px (%.1f%%)%n", metrics.zBufferRejectionsPerFrame, metrics.zBufferRejectionPercentage);
            System.out.println("     |-- Top Active Cores   :");
            printTopThreads(metrics.rasterizationThreadOccupation, 4);
            System.out.println("==================================================\n");

            java.util.Scanner scanner = new java.util.Scanner(System.in);
            System.out.print("-> [BENCHMARK] Commentary : ");
            metrics.commentary = scanner.nextLine();
        }
    
        private void saveBenchmarkResultToCSV() {
            String csvFileName = "performance_metrics_ver_7.csv";
            File file = new File(csvFileName);
            
            boolean exists = file.exists();
            try (FileWriter fw = new FileWriter(file, true);
                 PrintWriter pw = new PrintWriter(fw)) {
    
                if (!exists) {
                    StringBuilder header = new StringBuilder("Date,Commentary,Duration_s,Frames,Avg_FPS,1%_Low_FPS,1%_High_FPS,Avg_FrameTime_ms,Best_FrameTime_ms,Worst_FrameTime_ms,Total_Triangles,Triangles_Per_Frame,Min_Tri_s,Max_Tri_s,Geo_Pass_ms,Raster_Pass_ms,Geo_Ratio_%,Raster_Ratio_%");

                    // Colonnes individuelles pour chaque cœur en géométrie
                    for (int i = 0; i < 16; i++) {
                        header.append(",Geo_thread_").append(i+1);
                    }
                    // Colonnes individuelles pour chaque cœur en rastérisation
                    for (int i = 0; i < 16; i++) {
                        header.append(",Raster_thread_").append(i+1);
                    }
    
                    pw.println(header);
                }
                
                String cleanCommentary = "\"" + metrics.commentary.replace("\"", "\"\"") + "\"";
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                StringBuilder row = new StringBuilder();
                row.append(String.format(Locale.US, "%s,%s,%.3f,%d,%.2f,%.2f,%.2f,%.3f,%.3f,%.3f,%d,%d,%.0f,%.0f,%.3f,%.3f,%.2f,%.2f",
                        timestamp,                                     // Date
                        cleanCommentary,                               // Commentary
                        metrics.benchmarkDuration,                             // Duration_s
                        metrics.totalRenderedFrames,                                   // Frames
                        metrics.benchmarkAverageFPS,                           // Avg_FPS
                        metrics.onePercentLowFPS,                              // 1%_Low_FPS
                        metrics.onePercentHighFPS,                             // 1%_High_FPS
                        metrics.averageFrameTime * 1000.0f,                            // Avg_FrameTime_ms
                        metrics.averageBestFrameTime * 1000.0f,                        // Best_FrameTime_ms
                        metrics.averageWorstFrameTime * 1000.0f,                       // Worst_FrameTime_ms
                        metrics.benchmarkTotalRenderedTriangleCount,                   // Total_Triangles
                        metrics.benchmarkTotalRenderedTriangleCount / metrics.totalRenderedFrames,  // Triangles_Per_Frame
                        metrics.minTrianglesPerSecond,                         // Min_Tri_s
                        metrics.maxTrianglesPerSecond,                         // Max_Tri_s
                        metrics.geometryPassDuration * 1000.0f,                        // Geo_Pass_ms
                        metrics.rasterizationPassDuration * 1000.0f,                       // Raster_Pass_ms
                        metrics.geometryPassDurationRatio,                     // Geo_Ratio_%
                        metrics.rasterizationPassDurationRatio                 // Raster_Ratio_%
                ));
    
                for (double gOcc : metrics.geometryThreadOccupation) {
                    row.append(String.format(Locale.US, ",%.2f", gOcc));
                }
    
                for (double rOcc : metrics.rasterizationThreadOccupation) {
                    row.append(String.format(Locale.US, ",%.2f", rOcc));
                }
    
                pw.println(row);
                System.out.println("-> [TELEMETRY] data successfully exported in : " + csvFileName);
    
            } catch (IOException e) {
                System.err.println("-> [ERROR] Exportation impossible : " + e.getMessage());
            }
        }

        // Trie les cœurs et affiche uniquement les 4 plus actifs
        private void printTopThreads(double[] occupations, int maxDisplayedThreads) {
            Integer[] indices = new Integer[16];
            for (int i = 0; i < 16; i++) indices[i] = i;

            // Tri décroissant
            java.util.Arrays.sort(indices, (a, b) -> Double.compare(occupations[b], occupations[a]));

            int printed = 0;
            for (int i = 0; i < 16; i++) {
                int threadId = indices[i];
                double occ = occupations[threadId];
                if (occ > 0.01) { // On ignore les cœurs à 0%
                    System.out.printf("     |     Thread %-2d : %s%n", threadId, createProgressBar(occ));
                    printed++;
                }
                if (printed == maxDisplayedThreads) break;
            }
            if (printed == 0) {
                System.out.println("     |     Aucun thread actif mesuré.");
            }
        }

        private String createProgressBar(double percentage) {
            int segmentCount = 20;
            int solidBars = (int) (percentage * segmentCount / 100);
            if (percentage > 0 && solidBars == 0) solidBars = 1; // Au moins 1 barre si > 0%
            if (solidBars > segmentCount) solidBars = segmentCount;

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < segmentCount; i++) {
                sb.append(i < solidBars ? "█" : "░");
            }
            sb.append(String.format(Locale.US, "] %5.1f%%", percentage));
            return sb.toString();
        }

        public void resetTelemetry() {
            java.util.Arrays.fill(this.geoThreadOccupancySum, 0.0);
            java.util.Arrays.fill(this.rasterThreadOccupancySum, 0.0);
            java.util.Arrays.fill(this.geoThreadFrameCount, 0);
            java.util.Arrays.fill(this.rasterThreadFrameCount, 0);

            java.util.Arrays.fill(this.telemetryPipelineValue, 0L);

            this.totalGeometryTimeMs = 0;
            this.totalRasterizationTimeMs = 0;
        }

        public boolean isRunning() {
            return currentState == State.WARMUP ||
                    currentState == State.MEASURING ||
                    currentState == State.COOL_DOWN;
        }
    }