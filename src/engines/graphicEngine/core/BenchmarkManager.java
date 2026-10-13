    package engines.graphicEngine.core;
    
    import engines.graphicEngine.io.ObjLoader;
    import engines.graphicEngine.math.geometry.Mesh;
    import engines.graphicEngine.math.geometry.Plane;
    import engines.graphicEngine.math.geometry.Vertex3D;
    import engines.graphicEngine.math.tools.ColorUtils;
    import engines.graphicEngine.math.tools.Vector3D;
    import engines.graphicEngine.renderer.Camera;
    import engines.graphicEngine.renderer.Pipeline;
    import engines.graphicEngine.renderer.Texture;
    import engines.graphicEngine.scene.GameObject;
    import engines.graphicEngine.scene.Scene;
    import engines.graphicEngine.scene.lightRelative.PointLight;

    import java.io.*;
    import java.nio.file.Paths;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.util.Locale;

    public class BenchmarkManager {
        public enum State {
            IDLE,
            WARMUP,
            MEASURING,
            FINISHED
        }

        private State currentState = State.IDLE;
        private int startFrameOfCurrentState = 0;

        private final GraphicEngineContext graphicEngineContext;
        private final Pipeline pipeline;
        private BenchmarkMetrics metrics;

        private Scene benchmarkScene;
        private Scene previousScene;

        private Camera benchmarkCamera;
        private Camera previousCamera;

        private double angleTheta = 0;
        private double anglePhi = 0;



        private final int WARMUP_FRAMES = 15;
        private final int MEASURE_FRAMES = 85;

        boolean telemetryResetDone = false;

        public static class BenchmarkMetrics {
            // 1. META & CONTEXT
            public String commentary;
            public int windowWidth;
            public int windowHeight;

            // 2. GLOBAL PERFORMANCE
            public long benchmarkTimeStartNs = 0;
            public long benchmarkTimeEndNs = 0;
            public double benchmarkDurationSec = 0;
            public int totalBenchmarkedFrames = 0;

            public double benchmarkAverageFPS = 0;
            public float onePercentHighFPS;
            public float onePercentLowFPS;

            public float minFrameDurationSec;
            public float maxFrameDurationSec;
            public float averageFrameDurationSec;
            float[] framesDurationBuffer;
            public float averageBestFrameDurationSec;
            public float averageWorstFrameDurationSec;
            public long previousFrameTime = 0;

            public double minTrianglesPerSecond = Double.MAX_VALUE;
            public double maxTrianglesPerSecond = 0.0;

            // 3. PIPELINE TIMINGS REPARTITION
            public float averagePipelineOverheadDurationSec;

            public float averageGeometryPassTotalDurationSec;
            public float averageGeometryPassPureMathDurationSec;
            public float averageGeometryPassMergeDurationSec;
            public float geometryPassDurationRatioPct;

            public float averageRasterizationPassTotalDurationSec;
            public float averageRasterizationPassPureMathDurationSec;
            public float averageRasterizationPassMergeDurationSec = 0.0f;
            public float rasterizationPassDurationRatioPct;

            // 4. GEOMETRY DATA
            public long totalBenchmarkedTrianglesCount = 0;

            public double averageSubmittedTrianglesPerFrame;
            public long averageCulledTrianglesPerFrame;
            public float averageCullingEfficiencyPct;
            public long averageRasterizedTrianglesPerFrame;

            // 5. RASTERIZATION DATA
            public long averagePixelsProcessedPerFrame;
            public long averageZBufferRejectionsPerFrame;
            public float averageZBufferRejectionPct;

            public float averageOverdrawRatio;
            public float averageFillRateMegapixels;

            // 6. THREADING EFFICIENCY
            public double[] geometryThreadPctOccupation = new double[16];
            public double[] rasterizationThreadPctOccupation = new double[16];

            // 7. SYSTEM & MEMORY
            public float averageUsedMemoryMB;
            public float peakUsedMemoryMB;
            public int garbageCollectorTriggerCount;
        }

        public BenchmarkManager(GraphicEngineContext graphicEngineContext, Pipeline pipeline) {
            this.graphicEngineContext = graphicEngineContext;
            this.pipeline = pipeline;
            this.metrics = new BenchmarkMetrics();
            this.metrics.framesDurationBuffer = new float[MEASURE_FRAMES];
//            this.benchmarkCamera = new Camera(graphicEngineContext);
            this.initBenchmarkScene1();
        }
    
        public void initBenchmarkScene1() {
            this.benchmarkScene = new Scene();
//            this.benchmarkCamera.updateParameters(new Vertex3D(-20, 0, 30),
//                    new Camera.CameraRotation(0, 0, 0),
//                    new Vector3D(0, 0, 1),
//                    new Vector3D(0, 1, 0),
//                    new Plane(new Vertex3D(0, 0, 0.1f), new Vector3D(0, 0, 1)),
//                    new Plane(new Vertex3D(0, 0, 100), new Vector3D(0, 0, -1)),
//                    0.1f,50,90,
//                    0.005f, ColorUtils.WHITE, 15);

            System.out.println(graphicEngineContext.getCamera().getCameraPosition().toString());
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
                this.benchmarkScene.addGameObject("teapot" + i, new GameObject(teapot, ColorUtils.WHITE));
            }
    
            PointLight sun1 = new PointLight(
                    new Vector3D(10, 5, 0),
                    0.05f,
//                    ColorUtils.getARGB(255,255, 180, 50)
                    ColorUtils.getARGB(255, 255, 255, 255)
            );
            benchmarkScene.addLight("sunLight1", sun1);
            benchmarkScene.addGameObject("sun1", new GameObject(texturedSphere, texturedCubeTexture));
            benchmarkScene.getGameObject("sun1").setPosition(10, 5, 10);
    
            PointLight sun2 = new PointLight(
                    new Vector3D(-10, 5, 0),
                    0.05f,
//                    ColorUtils.getARGB(255, 255, 180, 50)
                    ColorUtils.getARGB(255, 255, 255, 255)
            );
            benchmarkScene.addLight("sunLight2", sun2);
            benchmarkScene.addGameObject("sun2", new GameObject(texturedSphere, texturedCubeTexture));
            benchmarkScene.getGameObject("sun2").setPosition(-10, 5, 10);
    
            benchmarkScene.addGameObject("axis1",   new GameObject(axis, ColorUtils.BLUE));
    
    
            benchmarkScene.addGameObject("texturedCube", new GameObject(texturedCube,texturedCubeTexture));
            benchmarkScene.getGameObject("texturedCube").setRotation(45, 45, 45);;

    
            benchmarkScene.getGameObject("axis1").setPosition(0, 0, 0);
            benchmarkScene.getGameObject("axis1").setScale(-0.3f, 0.3f, 0.3f);
        }

        public void initBenchmarkScene2() {
            this.benchmarkScene = new Scene();
            this.graphicEngineContext.getCamera().updateParameters(new Vertex3D(0, 0, 30),
                    new Camera.CameraRotation(0, 0, 0),
                    new Vector3D(0, 0, 1),
                    new Vector3D(0, 1, 0),
                    new Plane(new Vertex3D(0, 0, 0.1f), new Vector3D(0, 0, 1)),
                    new Plane(new Vertex3D(0, 0, 100), new Vector3D(0, 0, -1)),
                    0.1f,50,90,
                    0.005f, ColorUtils.WHITE, 15);

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
                float hue = i / 30.0f;
                int colorInt = ColorUtils.hsbToARGB(hue, 1.0f, 1.0f);

                this.benchmarkScene.addGameObject("teapot" + i, new GameObject(teapot, colorInt));
            }

            PointLight sun1 = new PointLight(
                    new Vector3D(10, 5, 0),
                    0.05f,
//                    ColorUtils.getARGB(255,255, 180, 50)
                    ColorUtils.getARGB(255,255, 255, 255)
            );
            benchmarkScene.addLight("sunLight1", sun1);
            benchmarkScene.addGameObject("sun1", new GameObject(texturedSphere, texturedCubeTexture));
            benchmarkScene.getGameObject("sun1").setPosition(10, 5, 10);

            PointLight sun2 = new PointLight(
                    new Vector3D(-10, 5, 0),
                    0.05f,
//                    ColorUtils.getARGB(255, 255, 180, 50)
                    ColorUtils.getARGB(255,255, 255, 255)
            );

            benchmarkScene.addLight("sunLight2", sun2);
            benchmarkScene.addGameObject("sun2", new GameObject(texturedSphere, texturedCubeTexture));
            benchmarkScene.getGameObject("sun2").setPosition(-10, 5, 10);

            benchmarkScene.addGameObject("axis1",   new GameObject(axis, ColorUtils.BLUE));


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
            for (int i = 0; i < totalTeapots; i++) {
                GameObject teapot = benchmarkScene.getGameObject("teapot" + i);
                if (teapot == null) continue;
    
                double step = (2 * Math.PI) / totalTeapots;
                double individualTheta = angleTheta + (i * step);
                double individualPhi = anglePhi + (i * step);
    
                hR = r * Math.cos(anglePhi);
                x = (float) (r * Math.cos(individualTheta));
                y = (float) (hR * Math.sin(individualPhi));
                z = (float) (r * Math.sin(individualTheta));
                teapot.setPosition(x+10, y+10, z+10);
    

                float rx = (float) (individualTheta);
                float ry = (float) (individualTheta * (i % 2 == 0 ? 1 : -1));
                float rz = (float) (individualPhi);
                teapot.rotate(rx, ry, rz);

                sX = (float) (1.0 + (3 * Math.sin(individualPhi)));
                sY = (float) (1.0 + (3 * Math.sin(individualTheta)));
                sZ = (float) (1.0 + (3 * Math.cos(individualPhi)));
                teapot.setScale(sX, sY, sZ);

                float hue = (float) ((individualTheta / (2 * Math.PI)) % 1.0);
                if (hue < 0) hue += 1.0f;
                int colorInt = ColorUtils.hsbToARGB(hue, 1.0f, 1.0f);
                teapot.setBasedColor(colorInt);
            }


            benchmarkScene.getGameObject("texturedCube").setPosition(y, z, x);
            benchmarkScene.getGameObject("texturedCube").rotate(0.5f, 1, 1.5f);
            benchmarkScene.getGameObject("texturedCube").setScale(sY, sZ, sX);

            benchmarkScene.getGameObject("sun1").setPosition(z, x, y);
            benchmarkScene.getGameObject("sun2").setPosition(-z, -x, -y);
    
            benchmarkScene.getLight("sunLight1").setOn(true);
//            benchmarkScene.getLight("sunLight1").setLightColor(ColorUtils.GREEN);
            benchmarkScene.getLight("sunLight2").setOn(true);
//            benchmarkScene.getLight("sunLight2").setLightColor(ColorUtils.BLUE);
    
            benchmarkScene.linkLight("sun1", "sunLight1");
            benchmarkScene.linkLight("sun2", "sunLight2");
        }
    
        public void start() {
            if (currentState == State.WARMUP || currentState == State.MEASURING) return;
            this.currentState = State.WARMUP;
            System.out.println("--- BENCHMARK STARTED ---");
            System.out.println("--- WARM UP ---");

            this.previousScene = this.pipeline.getScene();
            this.pipeline.setScene(benchmarkScene);

//            this.previousCamera = this.graphicEngineContext.getCamera();
//            this.graphicEngineContext.setCamera(this.benchmarkCamera);

            this.startFrameOfCurrentState = graphicEngineContext.getElapsedFrame();

            this.metrics = new BenchmarkMetrics();

            this.metrics.framesDurationBuffer = new float[MEASURE_FRAMES];
            this.metrics.minTrianglesPerSecond = Double.MAX_VALUE;
            this.metrics.maxTrianglesPerSecond = 0.0;
            this.metrics.benchmarkAverageFPS = 0;
            this.metrics.benchmarkDurationSec = 0;

            if (this.pipeline.profilerRawData != null) {
                this.pipeline.profilerRawData.resetTelemetry();
            }
        }
    
        public void cancel() {
            System.out.println("--- BENCHMARK CANCELLED ---");
            this.currentState = State.IDLE;
        }
    
        public void captureData() {
            if (currentState == State.IDLE || currentState == State.FINISHED) return;

            int currentFrame = graphicEngineContext.getElapsedFrame();
            int frameNumber = currentFrame - startFrameOfCurrentState;

            long currentTime = System.nanoTime();

            switch (currentState) {
                case WARMUP:
                    if (frameNumber >= WARMUP_FRAMES) {
                        System.out.println("--- WARMUP DONE ---");
                        System.out.println("--- START OF MEASURE ---");
                        this.currentState = State.MEASURING;



                        metrics.totalBenchmarkedFrames = 0;

                        metrics.previousFrameTime = currentTime;

                        this.startFrameOfCurrentState = currentFrame;

                        if (!telemetryResetDone) {
                            if (this.pipeline.profilerRawData != null) {
                                this.pipeline.profilerRawData.resetTelemetry();
                            }
                            telemetryResetDone = true;
                        }

                        metrics.benchmarkTimeStartNs = System.nanoTime();
                    }
                    break;

                case MEASURING:
                    long trianglesThisFrame = graphicEngineContext.getCurrentFrameRenderedTriangleCount();
                    metrics.totalBenchmarkedTrianglesCount += trianglesThisFrame;

                    metrics.framesDurationBuffer[metrics.totalBenchmarkedFrames] = (float) graphicEngineContext.getCurrentFrameDuration();

                    if (currentTime - metrics.previousFrameTime > 0) {
                        double deltaFrameTime = graphicEngineContext.getCurrentFrameDuration();

                        double currentTrianglesPerSecond = trianglesThisFrame / deltaFrameTime;

                        if (currentTrianglesPerSecond < metrics.minTrianglesPerSecond) {
                            metrics.minTrianglesPerSecond = currentTrianglesPerSecond;
                        }
                        if (currentTrianglesPerSecond > metrics.maxTrianglesPerSecond) {
                            metrics.maxTrianglesPerSecond = currentTrianglesPerSecond;
                        }
                    }

                    metrics.previousFrameTime = currentTime;

                    metrics.totalBenchmarkedFrames++;
                    if (frameNumber >= MEASURE_FRAMES) {
                        System.out.println("--- MEASURE DONE ---");

                        metrics.benchmarkTimeEndNs = currentTime;
                        this.startFrameOfCurrentState = currentFrame;

                        finish();
                        break;
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
            this.currentState = State.FINISHED;

            this.pipeline.setScene(previousScene);
//            this.graphicEngineContext.setCamera(this.benchmarkCamera);
            telemetryResetDone = false;

            metrics.windowHeight = graphicEngineContext.getWindowHeight();
            metrics.windowWidth = graphicEngineContext.getWindowWidth();

            java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(this.graphicEngineContext.getGraphicEngine());
            javax.swing.JFrame frame = null;

            if (parentWindow instanceof javax.swing.JFrame) {
                frame = (javax.swing.JFrame) parentWindow;
                frame.setExtendedState(javax.swing.JFrame.ICONIFIED);
            }
            computeBenchmarkResults();
            printBenchmarkResult();
            saveBenchmarkResultToCSV();

            if (frame != null) {
                frame.setExtendedState(javax.swing.JFrame.NORMAL);
                frame.toFront();
                frame.requestFocus();
            }
        }

        private void computeBenchmarkResults() {
            if (metrics.totalBenchmarkedFrames == 0) {
                System.out.println("\n-> [PROFILE] No data collected");
                return;
            }

//            2. GLOBAL PERFORMANCE
            metrics.benchmarkDurationSec = (metrics.benchmarkTimeEndNs - metrics.benchmarkTimeStartNs) / 1_000_000_000.0;
            metrics.benchmarkAverageFPS = metrics.totalBenchmarkedFrames / metrics.benchmarkDurationSec;

            if (metrics.minTrianglesPerSecond == Double.MAX_VALUE) {
                metrics.minTrianglesPerSecond = 0;
            }

            float sum = 0;
            metrics.maxFrameDurationSec = 0;
            metrics.minFrameDurationSec = Float.MAX_VALUE;

            int sampleLimit = Math.min(metrics.totalBenchmarkedFrames, metrics.framesDurationBuffer.length);
            for (int i = 0; i < sampleLimit; i++) {
                float ft = metrics.framesDurationBuffer[i];
                sum += ft;
                if (ft > metrics.maxFrameDurationSec) metrics.maxFrameDurationSec = ft;
                if (ft < metrics.minFrameDurationSec) metrics.minFrameDurationSec = ft;
            }

            metrics.averageFrameDurationSec = sum / sampleLimit;

            java.util.Arrays.sort(metrics.framesDurationBuffer, 0, sampleLimit);
            int onePercentCount = Math.max(1, sampleLimit / 100);

            float sumBest = 0;
            for (int i = 0; i < onePercentCount; i++) {
                sumBest += metrics.framesDurationBuffer[i];
            }
            metrics.averageBestFrameDurationSec = sumBest / onePercentCount;
            metrics.onePercentHighFPS = 1.0f / metrics.averageBestFrameDurationSec;


            float sumWorst = 0;
            for (int i = sampleLimit - onePercentCount; i < sampleLimit; i++) {
                sumWorst += metrics.framesDurationBuffer[i];
            }
            metrics.averageWorstFrameDurationSec = sumWorst / onePercentCount;
            metrics.onePercentLowFPS = 1.0f / metrics.averageWorstFrameDurationSec;


//             3. PIPELINE TIMINGS REPARTITION
            long totalPipelineTimeNs = this.pipeline.profilerRawData.totalPipelineExecutionTimeNs;
            long totalPassesTimeNs = this.pipeline.profilerRawData.geometryPassTotalDurationNs + this.pipeline.profilerRawData.rasterizationPassTotalDurationNs;
            long overheadNs = totalPipelineTimeNs - totalPassesTimeNs;

            metrics.averagePipelineOverheadDurationSec = (overheadNs / 1_000_000_000.0f) / metrics.totalBenchmarkedFrames;

//            Geometry
            metrics.averageGeometryPassTotalDurationSec = (this.pipeline.profilerRawData.geometryPassTotalDurationNs / 1_000_000_000.0f) / metrics.totalBenchmarkedFrames;
            metrics.averageGeometryPassPureMathDurationSec = (this.pipeline.profilerRawData.geometryPassPureMathDurationNs / 1_000_000_000.0f) / metrics.totalBenchmarkedFrames;
            metrics.averageGeometryPassMergeDurationSec = (this.pipeline.profilerRawData.geometryPassMergeDurationNs / 1_000_000_000.0f) / metrics.totalBenchmarkedFrames;

//            Rasterization
            metrics.averageRasterizationPassTotalDurationSec = (this.pipeline.profilerRawData.rasterizationPassTotalDurationNs / 1_000_000_000.0f) / metrics.totalBenchmarkedFrames;
            metrics.averageRasterizationPassPureMathDurationSec = (this.pipeline.profilerRawData.rasterizationPassPureMathDurationNs / 1_000_000_000.0f) / metrics.totalBenchmarkedFrames;
            metrics.averageRasterizationPassMergeDurationSec = 0.0f;


//            Ratios
            metrics.geometryPassDurationRatioPct = (metrics.averageGeometryPassTotalDurationSec /(metrics.averageGeometryPassTotalDurationSec + metrics.averageRasterizationPassTotalDurationSec)) * 100;
            metrics.rasterizationPassDurationRatioPct = (metrics.averageRasterizationPassTotalDurationSec /(metrics.averageGeometryPassTotalDurationSec + metrics.averageRasterizationPassTotalDurationSec)) * 100;


//            4. GEOMETRY DATA

            long totalSubmitted = this.pipeline.profilerRawData.totalSubmittedTriangles;
            long totalSurviving = this.pipeline.profilerRawData.totalSurvivingGeoTriangles;
            long culledTriangles = totalSubmitted - totalSurviving;

            metrics.averageSubmittedTrianglesPerFrame = (float) totalSubmitted / metrics.totalBenchmarkedFrames;
            metrics.averageCulledTrianglesPerFrame = culledTriangles / metrics.totalBenchmarkedFrames;
            metrics.averageCullingEfficiencyPct = (totalSubmitted > 0) ? ((float) culledTriangles / totalSubmitted) * 100.0f : 0.0f;
            metrics.averageRasterizedTrianglesPerFrame = this.pipeline.profilerRawData.totalRasterizedTriangles / metrics.totalBenchmarkedFrames;

//            5. RASTERIZATION DATA
            long totalPixels = 0;
            long totalZRejects = 0;

            for (int i = 0; i < 16; i++) {
                totalPixels += this.pipeline.profilerRawData.pixelsProcessedPerThread[i];
                totalZRejects += this.pipeline.profilerRawData.zBufferRejectionsPerThread[i];
            }

            metrics.averagePixelsProcessedPerFrame = totalPixels / metrics.totalBenchmarkedFrames;
            metrics.averageZBufferRejectionsPerFrame = totalZRejects / metrics.totalBenchmarkedFrames;
            metrics.averageZBufferRejectionPct = (totalPixels > 0) ? ((float) totalZRejects / totalPixels) * 100.0f : 0.0f;


            long screenPixels = (long) metrics.windowWidth * metrics.windowHeight;
            if (screenPixels > 0) {
                metrics.averageOverdrawRatio = (float) metrics.averagePixelsProcessedPerFrame / screenPixels;
            } else {
                metrics.averageOverdrawRatio = 0.0f;
            }


            metrics.averageFillRateMegapixels = (float) (metrics.averagePixelsProcessedPerFrame * metrics.benchmarkAverageFPS) / 1_000_000.0f;

//            6. THREADING EFFICIENCY
            long geometryPassPureMathDurationNs = this.pipeline.profilerRawData.geometryPassPureMathDurationNs;
            long rasterizationPassPureMathDurationNs = this.pipeline.profilerRawData.rasterizationPassPureMathDurationNs;

            for (int i = 0; i < 16; i++) {
                if (geometryPassPureMathDurationNs > 0) {
                    long geoThreadTotalTimeNs = this.pipeline.profilerRawData.geometryThreadTimesNs[i];
                    double geoOccupancy = ((double) geoThreadTotalTimeNs / geometryPassPureMathDurationNs) * 100.0;

                    metrics.geometryThreadPctOccupation[i] = Math.min(geoOccupancy, 100.0);
                } else {
                    metrics.geometryThreadPctOccupation[i] = 0.0;
                }

                if (rasterizationPassPureMathDurationNs > 0) {
                    long rasterThreadTotalTimeNs = this.pipeline.profilerRawData.rasterizationThreadTimesNs[i];
                    double rasterOccupancy = ((double) rasterThreadTotalTimeNs / rasterizationPassPureMathDurationNs) * 100.0;

                    metrics.rasterizationThreadPctOccupation[i] = Math.min(rasterOccupancy, 100.0);
                } else {
                    metrics.rasterizationThreadPctOccupation[i] = 0.0;
                }
            }

//            7. SYSTEM & MEMORY
            metrics.averageUsedMemoryMB = (float) (this.pipeline.profilerRawData.totalUsedMemoryBytesSum / metrics.totalBenchmarkedFrames) / (1024.0f * 1024.0f);
            metrics.peakUsedMemoryMB = (float) this.pipeline.profilerRawData.peakUsedMemoryBytes / (1024.0f * 1024.0f);
            metrics.garbageCollectorTriggerCount = this.pipeline.profilerRawData.garbageCollectorTriggerCount;
        }

        private void printBenchmarkResult() {
            if (this.metrics == null) return;

            System.out.println("\n==================================================");
            System.out.println("-> [PROFILE] BENCHMARK REPORT");
            System.out.println("==================================================");

            System.out.println("[GENERAL]");
            System.out.printf("     |-- Benchmark Duration : %.3f s%n", metrics.benchmarkDurationSec);
            // AJOUT: %,d pour les frames (au cas où tu fasses un gros benchmark)
            System.out.printf("     |-- Total Benchmark frames     : %,d%n", metrics.totalBenchmarkedFrames);

            System.out.println("--------------------------------------------------");
            System.out.println("[PERFORMANCE & FLUIDITY]");
            System.out.printf("     |-- Mean FPS                   : %.2f FPS%n", metrics.benchmarkAverageFPS);
            System.out.printf("     |-- 1%% High FPS               : %.2f FPS%n", metrics.onePercentHighFPS);
            System.out.printf("     |-- 1%% Low FPS                : %.2f FPS%n", metrics.onePercentLowFPS);
            System.out.printf("     |-- Average Frame Duration     : %.3f ms%n", metrics.averageFrameDurationSec * 1000.0);
            System.out.printf("     |-- Best Frame Duration        : %.3f ms (équiv. %.1f FPS)%n", metrics.minFrameDurationSec * 1000.0, metrics.minFrameDurationSec > 0 ? 1.0 / metrics.minFrameDurationSec : 0.0);
            System.out.printf("     |-- Worst Frame Duration       : %.3f ms (équiv. %.1f FPS)%n", metrics.maxFrameDurationSec * 1000.0, metrics.maxFrameDurationSec > 0 ? 1.0 / metrics.maxFrameDurationSec : 0.0);
            // AJOUT: %,.0f pour le Throughput
            System.out.printf("     |-- Peak Triangle Throughput   : %,.0f Tri/sec%n", metrics.maxTrianglesPerSecond);
            System.out.printf("     |-- Lowest Triangle Throughput : %,.0f Tri/sec%n", metrics.minTrianglesPerSecond);

            System.out.println("--------------------------------------------------");
            System.out.println("[PIPELINE TIMINGS (Avg/Frame)]");
            System.out.printf("     |-- Pipeline Overhead  : %.3f ms (Setup & Clear)%n", metrics.averagePipelineOverheadDurationSec * 1000.0);
            System.out.printf("     |-- Geometry Pass      : %s (%.2f ms)%n", createProgressBar(metrics.geometryPassDurationRatioPct), metrics.averageGeometryPassTotalDurationSec * 1000.0);
            System.out.printf("          |-- Pure Math     : %.3f ms%n", metrics.averageGeometryPassPureMathDurationSec * 1000.0);
            System.out.printf("          |-- Merge (addAll): %.3f ms%n", metrics.averageGeometryPassMergeDurationSec * 1000.0);
            System.out.printf("     |-- Rasterization Pass : %s (%.2f ms)%n", createProgressBar(metrics.rasterizationPassDurationRatioPct), metrics.averageRasterizationPassTotalDurationSec * 1000.0);
            System.out.printf("          |-- Pure Math     : %.3f ms%n", metrics.averageRasterizationPassPureMathDurationSec * 1000.0);
            System.out.printf("          |-- Merge (Array) : %.3f ms%n", metrics.averageRasterizationPassMergeDurationSec * 1000.0);

            System.out.println("--------------------------------------------------");
            System.out.println("[GEOMETRY WORKLOAD]");
            // AJOUT: %,d et %,.0f pour les volumes de triangles
            System.out.printf("     |-- Total Triangles    : %,d%n", metrics.totalBenchmarkedTrianglesCount);
            System.out.printf("     |-- Submitted          : %,.0f Tri / frame%n", metrics.averageSubmittedTrianglesPerFrame);
            System.out.printf("     |-- Culled             : %,d Tri / frame (%.1f%% rejected)%n", metrics.averageCulledTrianglesPerFrame, metrics.averageCullingEfficiencyPct);
            System.out.println("     |-- Top Active Cores   :");
            printTopThreads(metrics.geometryThreadPctOccupation, 16);

            System.out.println("--------------------------------------------------");
            System.out.println("[RASTERIZATION WORKLOAD]");
            // AJOUT: %,d pour les gros volumes de pixels et Z-Buffer
            System.out.printf("     |-- Triangles Drawn    : %,d / frame%n", metrics.averageRasterizedTrianglesPerFrame);
            System.out.printf("     |-- Pixels Processed   : %,d px / frame%n", metrics.averagePixelsProcessedPerFrame);
            System.out.printf("     |-- Fill Rate          : %.1f Megapixels/sec%n", metrics.averageFillRateMegapixels);
            System.out.printf("     |-- Overdraw Ratio     : %.2fx%n", metrics.averageOverdrawRatio);
            System.out.printf("     |-- Z-Buffer Rejects   : %,d px (%.1f%%)%n", metrics.averageZBufferRejectionsPerFrame, metrics.averageZBufferRejectionPct);
            System.out.println("     |-- Top Active Cores   :");
            printTopThreads(metrics.rasterizationThreadPctOccupation, 16);

            System.out.println("--------------------------------------------------");
            System.out.println("[SYSTEM & MEMORY]");
            System.out.printf("     |-- RAM Used (Avg)     : %.2f MB%n", metrics.averageUsedMemoryMB);
            System.out.printf("     |-- RAM Used (Peak)    : %.2f MB%n", metrics.peakUsedMemoryMB);
            if (metrics.garbageCollectorTriggerCount > 0) {
                System.out.printf("     |-- GC Triggers        : %d strike(s)%n", metrics.garbageCollectorTriggerCount);
            } else {
                System.out.printf("     |-- GC Triggers        : 0 (Zero Allocation Master 🏆)%n");
            }
            System.out.println("==================================================\n");

            java.util.Scanner scanner = new java.util.Scanner(System.in);
            System.out.print("-> [BENCHMARK] Commentary : ");
            metrics.commentary = scanner.nextLine();
        }
    
        private void saveBenchmarkResultToCSV() {
            String folderPath = "C:\\Users\\marti\\IdeaProjects\\Defi V5\\src\\engines\\graphicEngine\\io\\benchmarkResults\\";
            String csvFileName = "performance_metrics_ver_9.csv";
            File file = new File(folderPath + csvFileName);

            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            boolean exists = file.exists();

            try (FileWriter fw = new FileWriter(file, true);
                 PrintWriter pw = new PrintWriter(fw)) {

                if (!exists) {
                    StringBuilder header = new StringBuilder(
                            "Date,Commentary,Duration_s,Frames,Avg_FPS,1%_Low_FPS,1%_High_FPS," +
                                    "Avg_FrameTime_ms,Best_FrameTime_ms,Worst_FrameTime_ms," +
                                    "Pipeline_Overhead_ms," +
                                    "Geo_Pass_ms,Geo_PureMath_ms,Geo_Merge_ms,Geo_Ratio_%," +
                                    "Raster_Pass_ms,Raster_PureMath_ms,Raster_Merge_ms,Raster_Ratio_%," +
                                    "Total_Triangles,Avg_Submitted_Tri_Frame,Avg_Culled_Tri_Frame,Culling_Efficiency_%," +
                                    "Avg_Rasterized_Tri_Frame,Min_Tri_s,Max_Tri_s," +
                                    "Avg_Pixels_Frame,Avg_ZRejects_Frame,ZRejection_%," +
                                    "Overdraw_Ratio,Fill_Rate_Mpx_s," +
                                    "Avg_RAM_Used_MB,Peak_RAM_Used_MB,GC_Triggers_Count"
                    );

                    for (int i = 0; i < 16; i++) {
                        header.append(",Geo_thread_").append(i+1);
                    }
                    for (int i = 0; i < 16; i++) {
                        header.append(",Raster_thread_").append(i+1);
                    }
    
                    pw.println(header);
                }
                
                String cleanCommentary = "\"" + metrics.commentary.replace("\"", "\"\"") + "\"";
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                StringBuilder row = new StringBuilder();
                row.append(String.format(Locale.US,
                        // Le format string (32 variables)
                        "%s,%s,%.3f,%d,%.2f,%.2f,%.2f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%.3f,%.3f,%.3f,%.2f,%d,%.0f,%d,%.1f,%d,%.0f,%.0f,%d,%d,%.1f,%.2f,%.1f,%.2f,%.2f,%d",
                        timestamp,                                                // 1
                        cleanCommentary,                                          // 2
                        metrics.benchmarkDurationSec,                             // 3
                        metrics.totalBenchmarkedFrames,                           // 4
                        metrics.benchmarkAverageFPS,                              // 5
                        metrics.onePercentLowFPS,                                 // 6
                        metrics.onePercentHighFPS,                                // 7
                        metrics.averageFrameDurationSec * 1000.0f,                // 8
                        metrics.averageBestFrameDurationSec * 1000.0f,            // 9
                        metrics.averageWorstFrameDurationSec * 1000.0f,           // 10
                        metrics.averagePipelineOverheadDurationSec * 1000.0f,     // 11
                        metrics.averageGeometryPassTotalDurationSec * 1000.0f,    // 12
                        metrics.averageGeometryPassPureMathDurationSec * 1000.0f, // 13
                        metrics.averageGeometryPassMergeDurationSec * 1000.0f,    // 14
                        metrics.geometryPassDurationRatioPct,                        // 15
                        metrics.averageRasterizationPassTotalDurationSec * 1000.0f, // 16
                        metrics.averageRasterizationPassPureMathDurationSec * 1000.0f, // 17
                        metrics.averageRasterizationPassMergeDurationSec * 1000.0f, // 18
                        metrics.rasterizationPassDurationRatioPct,                   // 19
                        metrics.totalBenchmarkedTrianglesCount,                   // 20
                        metrics.averageSubmittedTrianglesPerFrame,                // 21
                        metrics.averageCulledTrianglesPerFrame,                   // 22
                        metrics.averageCullingEfficiencyPct,                      // 23
                        metrics.averageRasterizedTrianglesPerFrame,               // 24
                        metrics.minTrianglesPerSecond,                            // 25
                        metrics.maxTrianglesPerSecond,                            // 26
                        metrics.averagePixelsProcessedPerFrame,                   // 27
                        metrics.averageZBufferRejectionsPerFrame,                 // 28
                        metrics.averageZBufferRejectionPct,                       // 29
                        metrics.averageOverdrawRatio,                             // 30
                        metrics.averageFillRateMegapixels,                        // 31
                        metrics.averageUsedMemoryMB,                              // 32
                        metrics.peakUsedMemoryMB,                                 // 33
                        metrics.garbageCollectorTriggerCount                      // 34
                ));
    
                for (double gOcc : metrics.geometryThreadPctOccupation) {
                    row.append(String.format(Locale.US, ",%.2f", gOcc));
                }
                for (double rOcc : metrics.rasterizationThreadPctOccupation) {
                    row.append(String.format(Locale.US, ",%.2f", rOcc));
                }
    
                pw.println(row);
                System.out.println("-> [TELEMETRY] data successfully exported in : " + csvFileName);
    
            } catch (IOException e) {
                System.err.println("-> [ERROR] Exportation impossible : " + e.getMessage());
            }
        }

        private void printTopThreads(double[] occupations, int maxDisplayedThreads) {
            Integer[] indices = new Integer[16];
            for (int i = 0; i < 16; i++) indices[i] = i;

            java.util.Arrays.sort(indices, (a, b) -> Double.compare(occupations[b], occupations[a]));

            int printed = 0;
            for (int i = 0; i < 16; i++) {
                int threadId = indices[i];
                double occ = occupations[threadId];
                if (occ > 0.01) {
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
            int segmentCount = 30;
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

        public State getCurrentState() {
            return currentState;
        }
    }