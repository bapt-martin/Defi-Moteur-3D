package engines.graphicEngine.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    private State currentState = State.IDLE;
    private final GraphicEngineContext graphicEngineContext;

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

    public BenchmarkManager(GraphicEngineContext graphicEngineContext) {
        this.graphicEngineContext = graphicEngineContext;
    }

    public void start() {
        if (currentState == State.WARMUP || currentState == State.MEASURING) return;

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

    public double getBenchmarkDuration() {
        return benchmarkDuration;
    }

    public double getBenchmarkAverageFPS() {
        return benchmarkAverageFPS;
    }

    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }
}