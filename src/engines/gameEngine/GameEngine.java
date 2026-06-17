package engines.gameEngine;

import engines.graphicEngine.core.BenchmarkManager;
import engines.graphicEngine.core.GraphicEngine;
import engines.graphicEngine.core.GraphicEngineContext;
import engines.graphicEngine.input.InputManager;
import engines.graphicEngine.math.geometry.Plane;
import engines.graphicEngine.math.geometry.Vertex3D;
import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.overlay.HeadUpDisplay;
import engines.graphicEngine.renderer.Camera;
import engines.graphicEngine.renderer.Pipeline;
import engines.graphicEngine.scene.Scene;

import java.awt.*;

public abstract class GameEngine implements Runnable{
    private Thread gameThread;

    protected final GraphicEngineContext graphicEngineContext;
    protected final GraphicEngine graphicEngine;
    protected final Scene scene;
    protected final Camera camera;
    protected final InputManager inputManager;

    private final Pipeline pipeline;
    private final HeadUpDisplay hud;
    private final BenchmarkManager benchmarkManager;

    private int currentFPS = 0;
    private int currentUPS = 0;

    public GameEngine(int width, int height) {

        this.graphicEngine = new GraphicEngine(width, height);
        this.graphicEngineContext = graphicEngine.getGraphicEngineContext();

        this.scene = new Scene();
        this.camera = new Camera(graphicEngine.getGraphicEngineContext());

        this.graphicEngine.getGraphicEngineContext().setCamera(this.camera);

        this.inputManager = new InputManager(this.graphicEngine, this.camera);
        inputManager.attachTo(this.graphicEngine);

        this.pipeline = new Pipeline(this.camera, this.scene, this.graphicEngineContext);
        this.hud = new HeadUpDisplay(this.graphicEngineContext);
        this.benchmarkManager = new BenchmarkManager(this.graphicEngineContext);
        this.graphicEngineContext.setBenchmarkManager(this.benchmarkManager);
    }

    public abstract void initGame();
    public abstract void updateGameLogic();


    public GraphicEngine getGraphicEngine() {
        return this.graphicEngine;
    }

    public synchronized void start() {
        if (graphicEngineContext.isRunning()) return;
        graphicEngineContext.setRunning(true);
        this.graphicEngine.initGraphics();
        this.inputManager.centerMouse();

        this.initGame();

        gameThread = new Thread(this, "GameThread");
        gameThread.start();
    }

    @Override
    public void run() {
        this.graphicEngineContext.updateWindowInformation();
        double deltaU = 0;
        long timer = System.currentTimeMillis();
        long startTime = System.nanoTime();
        long previousTime = startTime;

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
        }
    }

    private void update() {
        inputManager.processInputs();
        camera.updateWindowProjectionMatrix();
        camera.updateCamReferentialMatrix();
        camera.updateProjectionMatrix();

        boolean isFlashlightOn = graphicEngineContext.isCameraSpotOn();
        camera.getCameraSpot().setOn(isFlashlightOn);

        this.updateGameLogic();

        if (graphicEngineContext.isHUDActive()) {
            this.hud.updateStats();
        }
    }
}

