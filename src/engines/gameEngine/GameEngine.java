package engines.gameEngine;

import engines.graphicEngine.core.BenchmarkManager;
import engines.graphicEngine.core.GraphicEngine;
import engines.graphicEngine.core.GraphicEngineContext;
import engines.graphicEngine.io.userInput.CameraController;
import engines.graphicEngine.io.userInput.InputManager;
import engines.graphicEngine.overlay.HeadUpDisplay;
import engines.graphicEngine.renderer.Camera;
import engines.graphicEngine.renderer.Pipeline;
import engines.graphicEngine.scene.Scene;

import javax.swing.*;
import java.util.Locale;

public abstract class GameEngine implements Runnable {
    private Thread gameThread;

    protected final GraphicEngineContext graphicEngineContext;
    protected final GraphicEngine graphicEngine;
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

            this.inputManager = new InputManager(this.graphicEngine);

            this.pipeline = new Pipeline(this.graphicEngineContext);
            this.hud = new HeadUpDisplay(this.graphicEngineContext);
            this.benchmarkManager = new BenchmarkManager(this.graphicEngineContext, this.pipeline);
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

        double updateRequired = 0;

        long timer = System.currentTimeMillis();
        long startTime = System.nanoTime();
        long previousTimeFrame = startTime;

        while (graphicEngineContext.isRunning()) {
            Camera currentCamera =  this.graphicEngineContext.getCurrentScene().getMainCamera();

            long currentTimeFrame = System.nanoTime();
            double currentFrameDuration = (currentTimeFrame - previousTimeFrame) / 1_000_000_000.0;
            graphicEngineContext.setCurrentFrameDuration(currentFrameDuration);

            if (currentFrameDuration > 0.25) currentFrameDuration = 0.25;

            double elapsedTime = (currentTimeFrame - startTime) / 1_000_000_000.0;
            graphicEngineContext.setElapsedTime(elapsedTime);

            updateRequired += currentFrameDuration * graphicEngineContext.getUPS_TARGET();
            previousTimeFrame = currentTimeFrame;

            boolean needsRender = false;

            while (updateRequired >= 1) {
                this.update();
                currentUPS++;
                updateRequired--;
                needsRender = true;
            }

            if (needsRender) {
                this.benchmarkManager.captureData();

                if (!graphicEngineContext.isRunning()) {
                    break;
                }
                this.graphicEngine.render(this.pipeline, this.hud);

                currentFPS++;
                graphicEngineContext.incrementElapsedFrame();
            }

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                System.out.println(currentCamera.getCameraPosition().toString());
                graphicEngineContext.setCurrentUPS(currentUPS);
                graphicEngineContext.setCurrentFPS(currentFPS);
                currentUPS = 0;
                currentFPS = 0;
            }
        }
        this.stop();
    }

    private void update() {
        inputManager.processInputs();

        this.graphicEngineContext.getCurrentScene().getCameraController().update();

        Camera currentCamera =  this.graphicEngineContext.getCurrentScene().getMainCamera();
        currentCamera.updateWindowProjectionMatrix();
        currentCamera.updateCamReferentialMatrix();
        currentCamera.updateProjectionMatrix();


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

    public GraphicEngine getGraphicEngine() {
        return this.graphicEngine;
    }
}