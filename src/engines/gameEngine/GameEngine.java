package engines.gameEngine;

import engines.graphicEngine.core.BenchmarkManager;
import engines.graphicEngine.core.GraphicEngine;
import engines.graphicEngine.core.GraphicEngineContext;
import engines.graphicEngine.io.userInput.InputManager;
import engines.graphicEngine.overlay.HeadUpDisplay;
import engines.graphicEngine.renderer.Camera;
import engines.graphicEngine.renderer.Pipeline;
import engines.graphicEngine.scene.Scene;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

public abstract class GameEngine implements Runnable {
    private Thread gameThread;

    protected final GraphicEngineContext graphicEngineContext;
    protected final GraphicEngine graphicEngine;
    protected final Scene scene;
    protected final Camera camera;
    protected final InputManager inputManager;

    protected final Pipeline pipeline;
    private final HeadUpDisplay hud;
    private final BenchmarkManager benchmarkManager;

    private int currentFPS = 0;
    private int currentUPS = 0;

    public GameEngine(int width, int height) {
        JFrame window = new JFrame("EnginesTest");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        long startEngineConstruct = System.nanoTime();
            this.graphicEngine = new GraphicEngine(width, height);
            this.graphicEngineContext = graphicEngine.getGraphicEngineContext();

            this.scene = new Scene();
            this.camera = new Camera(graphicEngine.getGraphicEngineContext());

            this.inputManager = new InputManager(this.graphicEngine, this.camera);

            this.pipeline = new Pipeline(this.camera, this.scene, this.graphicEngineContext);
            this.hud = new HeadUpDisplay(this.graphicEngineContext);
            this.benchmarkManager = new BenchmarkManager(this.graphicEngineContext, this.pipeline);
            this.graphicEngineContext.setBenchmarkManager(this.benchmarkManager);
        long endEngineConstruct = System.nanoTime();
        System.out.printf("-> [PROFILE] Sub GameEngine systems allocated in %.2f ms%n",
                (endEngineConstruct - startEngineConstruct) / 1_000_000.0);

        window.add(this.getGraphicEngine());
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    public abstract void initGame();
    public abstract void updateGameLogic();

    public synchronized void start() {
        if (graphicEngineContext.isRunning()) return;

        System.out.println("-> [PROFILE] Engine initialisation...");
        long startGlobalInit = System.nanoTime();
            graphicEngineContext.setRunning(true);

            long startGraphics = System.nanoTime();
                this.graphicEngine.initGraphics();
            long endGraphics = System.nanoTime();
            System.out.printf("     |-- Window and contextual graph initialised in %.2f ms%n",
                    (endGraphics - startGraphics) / 1_000_000.0);

            this.inputManager.centerMouse();

            long startUserInit = System.nanoTime();
                this.initGame();
            long endUserInit = System.nanoTime();
            System.out.printf("     |-- game initialization in %.2f ms%n",
                    (endUserInit - startUserInit) / 1_000_000.0);

        long endGlobalInit = System.nanoTime();
        System.out.println("==================================================\n");
        System.out.printf("-> [PROFILE] Global initialisation success in %.2f ms %n",
                (endGlobalInit - startGlobalInit) / 1_000_000.0);
        System.out.println("==================================================\n");

        gameThread = new Thread(this, "GameThread");
        gameThread.start();
    }

    public synchronized void stop() {
        if (!graphicEngineContext.isRunning()) return;

        graphicEngineContext.setRunning(false);

        if (gameThread != null && Thread.currentThread() != gameThread) {
            try {
                gameThread.join(2000);
            } catch (InterruptedException e) {
                System.err.println("-> [ERROR] Interruption while waiting for game thread");
                Thread.currentThread().interrupt();
            }
        }
        this.pipeline.shutdown();
        System.exit(0);
    }

    @Override
    public void run() {
        this.graphicEngineContext.updateWindowInformation();

        double deltaU = 0;

        long timer = System.currentTimeMillis();
        long startTime = System.nanoTime();
        long previousTime = startTime;

        int maxExpectedFrames = 3000;
        float[] frameTimesBuffer = new float[maxExpectedFrames];
        int totalFramesRendered = 0;

        double warmUpPeriod = 1.0;

        boolean telemetryResetDone = false;

        while (graphicEngineContext.isRunning()) {
            benchmarkManager.update();

            long currentTime = System.nanoTime();
            double deltaTime = (currentTime - previousTime) / 1_000_000_000.0;
            graphicEngineContext.setDeltaTime(deltaTime);

            if (deltaTime > 0.25) deltaTime = 0.25;

            double elapsedTime = (currentTime - startTime) / 1_000_000_000.0;
            graphicEngineContext.setElapsedTime(elapsedTime);

            deltaU += deltaTime * graphicEngineContext.getUPS_TARGET();
            previousTime = currentTime;

            boolean needsRender = false;

            while (deltaU >= 1) {
                this.update();
                currentUPS++;
                deltaU--;
                needsRender = true;
            }

            if (needsRender) {
                this.graphicEngine.render(this.camera, this.pipeline, this.hud);

                if (benchmarkManager.isMeasuring()) {
                    if (totalFramesRendered < maxExpectedFrames) {
                        frameTimesBuffer[totalFramesRendered] = (float) deltaTime;
                    }
                    totalFramesRendered++;
                }

                currentFPS++;
                graphicEngineContext.incrementElapsedFrame();
            }

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                graphicEngineContext.setCurrentUPS(currentUPS);
                graphicEngineContext.setCurrentFPS(currentFPS);
                currentUPS = 0;
                currentFPS = 0;
            }

            if (elapsedTime >= 3.0) {
                break;
            }

            if (!telemetryResetDone && elapsedTime >= warmUpPeriod) {
                this.pipeline.resetTelemetry();
                telemetryResetDone = true;
            }
        }

        java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(this.graphicEngine);
        if (parentWindow != null) {
            parentWindow.dispose();
        }

        printPerformanceTelemetry(totalFramesRendered, frameTimesBuffer, graphicEngineContext.getElapsedTime(), warmUpPeriod);

        this.stop();
    }

    private void update() {
        inputManager.processInputs();
        camera.updateWindowProjectionMatrix();
        camera.updateCamReferentialMatrix();
        camera.updateProjectionMatrix();

        this.updateGameLogic();

        if (graphicEngineContext.isHUDActive()) {
            this.hud.updateStats();
        }
    }

    private String formatFloatArray(double[] array) {
        if (array == null || array.length == 0) return "\"\"";
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (int i = 0; i < array.length; i++) {
            sb.append(String.format(Locale.US, "%.2f", array[i]));
            if (i < array.length - 1) {
                sb.append(";");
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private void printPerformanceTelemetry(int totalFrames, float[] frameTimes, double totalDuration, double warmUpDuration) {
        if (totalFrames == 0) {
            System.out.println("\n-> [PROFILE] No data collected");
            return;
        }

        float sum = 0;
        float maxFrame = 0;
        float minFrame = Float.MAX_VALUE;

        int sampleLimit = Math.min(totalFrames, frameTimes.length);
        for (int i = 0; i < sampleLimit; i++) {
            float ft = frameTimes[i];
            sum += ft;
            if (ft > maxFrame) maxFrame = ft;
            if (ft < minFrame) minFrame = ft;
        }

        float avgFrameTime = sum / sampleLimit;
        float avgFPS = 1.0f / avgFrameTime;

        java.util.Arrays.sort(frameTimes, 0, sampleLimit);
        int onePercentCount = Math.max(1, sampleLimit / 100);

        float sumWorst = 0;
        for (int i = sampleLimit - onePercentCount; i < sampleLimit; i++) {
            sumWorst += frameTimes[i];
        }
        float avgWorstFrameTime = sumWorst / onePercentCount;
        float onePercentLowFPS = 1.0f / avgWorstFrameTime;

        float sumBest = 0;
        for (int i = 0; i < onePercentCount; i++) {
            sumBest += frameTimes[i];
        }
        float avgBestFrameTime = sumBest / onePercentCount;
        float onePercentHighFPS = 1.0f / avgBestFrameTime;

        double telemetryDuration = totalDuration - warmUpDuration;

        System.out.println("\n==================================================");
        System.out.println("-> [PROFILE] Perf report");
        System.out.println("--------------------------------------------------");
        System.out.printf("     |-- Warm-up : %.3f s%n", warmUpDuration);
        System.out.printf("     |-- Telemetry Duration : %.3f s%n", telemetryDuration);
        System.out.printf("     |-- Total frame analyzed : %d%n", totalFrames);
        System.out.println("--------------------------------------------------");
        System.out.printf("     |-- Mean FPS : %.2f FPS%n", avgFPS);
        System.out.printf("     |-- Average Frame Time    : %.3f ms%n", avgFrameTime * 1000.0);
        System.out.printf("     |-- Best frame : %.3f ms (équiv. %.1f FPS)%n", minFrame * 1000.0, minFrame > 0 ? 1.0 / minFrame : 0.0);
        System.out.printf("     |-- Worst Frame      : %.3f ms (équiv. %.1f FPS)%n", maxFrame * 1000.0, maxFrame > 0 ? 1.0 / maxFrame : 0.0);
        System.out.println("--------------------------------------------------");
        System.out.printf("     |-- 1%% High FPS       : %.2f FPS%n", onePercentHighFPS);
        System.out.printf("     |-- 1%% Low FPS        : %.2f FPS%n", onePercentLowFPS);
        System.out.println("==================================================\n");

        java.util.Scanner scanner = new java.util.Scanner(System.in);
        System.out.print("-> [BENCHMARK] Commentary : ");
        String commentary = scanner.nextLine();

        float geometryTime = (float) this.pipeline.telemetryPipelineValue[0] / 1_000_000;
        float rasterizationTime = (float) this.pipeline.telemetryPipelineValue[18] / 1_000_000;

        double[] geometryThreadOccupation = new double[16];
        double[] rasterizationThreadOccupation = new double[16];

        long totalGeoNs = this.pipeline.telemetryPipelineValue[1];
        if (totalGeoNs > 0) {
            for (int i = 2; i <= 17; i++) {
                geometryThreadOccupation[i - 2] = ((double) this.pipeline.telemetryPipelineValue[i] / totalGeoNs) * 100.0;
            }
        }

        long totalRasterNs = this.pipeline.telemetryPipelineValue[19];
        if (totalRasterNs > 0) {
            for (int i = 20; i <= 35; i++) {
                rasterizationThreadOccupation[i - 20] = ((double) this.pipeline.telemetryPipelineValue[i] / totalRasterNs) * 100.0;
            }
        }

        float geometryRatio = (geometryTime/(geometryTime + rasterizationTime)) * 100;
        float rasterizationRatio = (rasterizationTime/(geometryTime + rasterizationTime)) * 100;

        float avgFrameTimeMs = avgFrameTime * 1000.0f;
        float avgBestFrameTimeMs = avgBestFrameTime * 1000.0f;
        float avgWorstFrameTimeMs = avgWorstFrameTime * 1000.0f;

        exportToCSV(commentary, telemetryDuration, totalFrames, avgFPS, avgFrameTimeMs, avgBestFrameTimeMs, avgWorstFrameTimeMs, geometryTime, rasterizationTime, geometryRatio, rasterizationRatio, geometryThreadOccupation, rasterizationThreadOccupation);
    }

    private void exportToCSV(String commentary, double telemetryDuration, int totalFrames, float avgFPS, float avgFrameTimeMs, float top1PercentMs, float bottom1PercentMs, float geometryTime, float rasterizationTime, float geometryRatio, float rasterizationRatio, double[] geometryThreadOccupation,  double[] rasterizationThreadOccupation) {
        String csvFileName = "performance_metrics_ver_5.csv";
        File file = new File(csvFileName);
        boolean exists = file.exists();

        try (FileWriter fw = new FileWriter(file, true);
             PrintWriter pw = new PrintWriter(fw)) {

            if (!exists) {
                StringBuilder header = new StringBuilder("commentary, telemetryDuration, totalFrames, avgFPS, avgFrameTimeMs, avgBestFrameTimeMs, avgWorstFrameTimeMs, geometryTime, rasterizationTime, geometryRatio, rasterizationRatio");

                // Colonnes individuelles pour chaque cœur en géométrie
                for (int i = 0; i < 16; i++) {
                    header.append(",geometry_thread_").append(i+1);
                }
                // Colonnes individuelles pour chaque cœur en rastérisation
                for (int i = 0; i < 16; i++) {
                    header.append(",rasterization_thread_").append(i+1);
                }
                pw.println(header);
            }

            String cleanCommentary = "\"" + commentary.replace("\"", "\"\"") + "\"";

            StringBuilder row = new StringBuilder();
            row.append(String.format(Locale.US, "%s,%.3f,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f",
                    cleanCommentary,
                    telemetryDuration,
                    totalFrames,
                    avgFPS,
                    avgFrameTimeMs,
                    top1PercentMs,
                    bottom1PercentMs,
                    geometryTime,
                    rasterizationTime,
                    geometryRatio,
                    rasterizationRatio
            ));

            for (double occ : geometryThreadOccupation) {
                row.append(String.format(Locale.US, ",%.2f", occ));
            }

            for (double occ : rasterizationThreadOccupation) {
                row.append(String.format(Locale.US, ",%.2f", occ));
            }

            pw.println(row);
            System.out.println("-> [TELEMETRY] data successfully exported in : " + csvFileName);

        } catch (IOException e) {
            System.err.println("-> [ERROR] Exportation impossible : " + e.getMessage());
        }
    }

    public GraphicEngine getGraphicEngine() {
        return this.graphicEngine;
    }
}