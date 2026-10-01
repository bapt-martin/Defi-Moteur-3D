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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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

    private int startFrameOfCurrentState = 0;

    private long startTriangleCount = 0;
    private long endTriangleCount = 0;

    private long startTime = 0;
    private long endTime = 0;

    private long previousFrameTime = 0;
    private long previousTriangleCount = 0;

    private double minTrianglesPerSecond = Double.MAX_VALUE;
    private double maxTrianglesPerSecond = 0.0;

    private double minFPS = Double.MAX_VALUE;
    private double maxFPS = 0.0;

    private final int WARMUP_FRAMES = 50;
    private final int MEASURE_FRAMES = 150;
    private final int COOLDOWN_FRAMES = 25;

    private double benchmarkDuration = 0;
    private double benchmarkAverageFPS = 0;
    private long benchmarkTotalTriangleCount = 0;

    int maxExpectedFrames = 3000;
    float[] frameTimesBuffer = new float[maxExpectedFrames];
    int totalFramesRendered = 0;

    boolean telemetryResetDone = false;

    private Scene benchmarkScene;
    private Scene previousScene;

    public BenchmarkManager(GraphicEngineContext graphicEngineContext, Pipeline pipeline) {
        this.graphicEngineContext = graphicEngineContext;
        this.pipeline = pipeline;
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
//        t1.setPosition(0, 0, 0);
        t1.setRotation(0, 0, 0);
        t1.setScale(10, 10, 10);

        t1.setRendered(true);
    }

    public void benchMarkSceneUpdate() {
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
        this.previousScene = this.pipeline.getScene();
        this.pipeline.setScene(benchmarkScene);

        System.out.println("--- BENCHMARK STARTED ---");
        System.out.println("--- WARM UP ---");
        this.currentState = State.WARMUP;

        this.startFrameOfCurrentState = graphicEngineContext.getElapsedFrame();

        this.benchmarkAverageFPS = 0;
        this.benchmarkDuration = 0;

        this.minTrianglesPerSecond = Double.MAX_VALUE;
        this.maxTrianglesPerSecond = 0.0;

        this.minFPS = Double.MAX_VALUE;
        this.maxFPS = 0.0;

    }

    public void cancel() {
        System.out.println("--- BENCHMARK CANCELLED ---");
        this.currentState = State.IDLE;
        graphicEngineContext.setBenchmarkRunning(this.isRunning());
    }

    public void update() {
        if (currentState == State.IDLE || currentState == State.FINISHED) return;

        int currentFrame = graphicEngineContext.getElapsedFrame();
        int framesInState = currentFrame - startFrameOfCurrentState;

        long currentTime = System.nanoTime();
        long currentTotalTriangles = graphicEngineContext.getTotalTriangleCount();

        switch (currentState) {
            case WARMUP:
                if (framesInState >= WARMUP_FRAMES) {
                    System.out.println("--- WARMUP DONE ---");
                    System.out.println("--- START OF MEASURE ---");

                    this.currentState = State.MEASURING;
                    graphicEngineContext.setBenchmarkRunning(this.isRunning());

                    this.startTime = currentTime;
                    this.startTriangleCount = currentTotalTriangles;

                    this.previousFrameTime = currentTime;
                    this.previousTriangleCount = currentTotalTriangles;

                    this.startFrameOfCurrentState = currentFrame;
                }
                break;

            case MEASURING:
                long timeDeltaNanoseconds = currentTime - previousFrameTime;
                long trianglesThisFrame = currentTotalTriangles - previousTriangleCount;

                if (timeDeltaNanoseconds > 0) {
                    double timeDeltaSeconds = timeDeltaNanoseconds / 1_000_000_000.0;
                    double currentTrianglesPerSecond = trianglesThisFrame / timeDeltaSeconds;
                    double currentFPS = 1.0 / timeDeltaSeconds;

                    if (currentTrianglesPerSecond < minTrianglesPerSecond) {
                        minTrianglesPerSecond = currentTrianglesPerSecond;
                    }
                    if (currentTrianglesPerSecond > maxTrianglesPerSecond) {
                        maxTrianglesPerSecond = currentTrianglesPerSecond;
                    }
                    if (currentFPS < minFPS) {
                        minFPS = currentFPS;
                    }
                    if (currentFPS > maxFPS) {
                        maxFPS = currentFPS;
                    }
                }

                this.previousFrameTime = currentTime;
                this.previousTriangleCount = currentTotalTriangles;

                if (framesInState >= MEASURE_FRAMES) {
                    System.out.println("--- MEASURE DONE ---");
                    System.out.println("--- START OF COOL DOWN ---");

                    this.endTime = currentTime;
                    this.endTriangleCount = currentTotalTriangles;

                    this.currentState = State.COOL_DOWN;
                    graphicEngineContext.setBenchmarkRunning(this.isRunning());

                    this.startFrameOfCurrentState = currentFrame;
                }
                break;

            case COOL_DOWN:
                if (framesInState >= COOLDOWN_FRAMES) {
                    System.out.println("--- COOL DOWN DONE ---");
                    finish();
                }
                break;

            case IDLE:
                System.out.println("--- BENCHMARK IDLE ---");
                this.benchmarkDuration = 0;
                this.benchmarkAverageFPS = 0;
                break;
            default:
                break;
        }
    }

    public void finish() {
        System.out.println("--- END OF BENCHMARK ---");
        this.pipeline.setScene(previousScene);

        this.benchmarkDuration = (endTime - startTime) / 1_000_000_000.0;
        this.benchmarkAverageFPS = MEASURE_FRAMES / benchmarkDuration;
        this.benchmarkTotalTriangleCount = endTriangleCount - startTriangleCount;

        if (minTrianglesPerSecond == Double.MAX_VALUE) {
            minTrianglesPerSecond = 0;
        }
        if (minFPS == Double.MAX_VALUE) {
            minFPS = 0;
        }

        saveResultsToCSV();

        this.currentState = State.FINISHED;
        graphicEngineContext.setBenchmarkRunning(this.isRunning());

        System.out.println("=== BENCHMARK RESULT ===");
        System.out.println("Avg FPS        : " + String.format(Locale.US, "%.2f", benchmarkAverageFPS));
        System.out.println("Min FPS        : " + String.format(Locale.US, "%.2f", minFPS));
        System.out.println("Max FPS        : " + String.format(Locale.US, "%.2f", maxFPS));
        System.out.println("Duration       : " + String.format(Locale.US, "%.2f", benchmarkDuration) + "s");
        System.out.println("Min Triangles/s: " + String.format(Locale.US, "%.0f", minTrianglesPerSecond));
        System.out.println("Max Triangles/s: " + String.format(Locale.US, "%.0f", maxTrianglesPerSecond));
    }

    private void saveResultsToCSV() {
        String filepath = "C:\\Users\\marti\\IdeaProjects\\Defi V5\\src\\engines\\graphicEngine\\io\\benchmarkResults.csv";
        File file = new File(filepath);
        boolean fileExists = file.exists();

        try (FileWriter fw = new FileWriter(file, true);
             BufferedWriter bw = new BufferedWriter(fw)) {

            if (!fileExists) {
                file.getParentFile().mkdirs();
                bw.write("Date;Average FPS;Min FPS;Max FPS;Duration (s);Frames Measured;Total Triangle Rendered;Average Triangle/Frame;Min Triangles/s;Max Triangles/s");
                bw.newLine();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String line = String.format(Locale.US, "%s;%.2f;%.2f;%.2f;%.4f;%d;%d;%d;%.2f;%.2f",
                    timestamp,
                    benchmarkAverageFPS,
                    minFPS,
                    maxFPS,
                    benchmarkDuration,
                    MEASURE_FRAMES,
                    benchmarkTotalTriangleCount,
                    benchmarkTotalTriangleCount / MEASURE_FRAMES,
                    minTrianglesPerSecond,
                    maxTrianglesPerSecond
            );

            bw.write(line);
            bw.newLine();

            System.out.println("-> Results saved in " + filepath);

        } catch (IOException e) {
            System.err.println("CSV save error : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        return currentState == State.WARMUP ||
                currentState == State.MEASURING ||
                currentState == State.COOL_DOWN;
    }

    public boolean isMeasuring() {
        return currentState == State.MEASURING;
    }

    public double getBenchmarkDuration() {
        return benchmarkDuration;
    }

    public double getBenchmarkAverageFPS() {
        return benchmarkAverageFPS;
    }

    public State getCurrentState() {
        return currentState;
    }

    public Scene getBenchmarkScene() {
        return benchmarkScene;
    }
}