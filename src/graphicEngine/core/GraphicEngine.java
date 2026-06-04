package graphicEngine.core;

import graphicEngine.input.InputManager;
import graphicEngine.io.ObjLoader;
import graphicEngine.math.geometry.Mesh;
import graphicEngine.math.geometry.Plane;
import graphicEngine.math.geometry.Vertex3D;
import graphicEngine.math.tools.Vector3D;
import graphicEngine.overlay.HeadUpDisplay;
import graphicEngine.renderer.Camera;
import graphicEngine.renderer.Pipeline;
import graphicEngine.scene.GameObject;
import graphicEngine.scene.Scene;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.nio.file.Paths;

public class GraphicEngine extends Canvas implements Runnable {
    private Thread gameThread;
    private final Camera camera;
    private final InputManager inputManager;
    private final HeadUpDisplay headUpDisplay;
    private final Pipeline pipeline;
    private final Scene scene;
    private final GraphicEngineContext graphicEngineContext;
    private final BenchmarkManager benchmarkManager;

    private int currentFPS = 0;
    private int currentUPS = 0;

    private double angleTheta = 0;
    private double anglePhi = 0;


    public GraphicEngine(int widthInit, int heightInit) {
        this.graphicEngineContext = new GraphicEngineContext(this, widthInit, heightInit);
        this.benchmarkManager = new BenchmarkManager(graphicEngineContext);

        this.graphicEngineContext.setBenchmarkManager(this.benchmarkManager);

        this.camera = new Camera(0.1,1000,90, new Plane(new Vertex3D(0, 0, 0.1), new Vector3D(0, 0, 1)), graphicEngineContext);


        this.setBackground(new Color(150,150,200));


        this.setFocusable(true);
//        this.setFocusTraversalKeysEnabled(false);

        this.requestFocusInWindow();

        this.graphicEngineContext.setCamera(this.camera);

        this.inputManager = new InputManager(this,camera);
        inputManager.attachTo(this);

        this.scene = new Scene();

        Mesh teapot = ObjLoader.readObjFile(Paths.get("obj model\\teapot.obj"));
        Mesh axis = ObjLoader.readObjFile(Paths.get("obj model\\axis.obj"));
        Mesh centeredCube = ObjLoader.readObjFile(Paths.get("obj model\\cube.obj"));
        Mesh outCenteredCube = ObjLoader.readObjFile(Paths.get("obj model\\cube pas centré.obj"));
//        new Scene.MeshData("F1","obj model\\F1.obj")

        this.scene.addMesh("teapot", teapot);
        this.scene.addMesh("axis", axis);
        this.scene.addMesh("centeredCube", centeredCube);
        this.scene.addMesh("outCenteredCube", outCenteredCube);

//        this.scene.loadMeshes(
//                List.of(new Scene.MeshData("teapot","obj model\\teapot.obj"),
//                        new Scene.MeshData("axis","obj model\\axis.obj"),
//                        new Scene.MeshData("cube centré","obj model\\cube.obj"),
//                        new Scene.MeshData("cube pas centré","obj model\\cube.obj")
////                        new Scene.MeshData("F1","obj model\\F1.obj")
//        ));

        this.scene.addGameObject("teapot1", new GameObject(teapot));
        this.scene.addGameObject("teapot2", new GameObject(teapot));
        this.scene.addGameObject("teapot3", new GameObject(teapot));
        this.scene.addGameObject("teapot4", new GameObject(teapot));
        this.scene.addGameObject("axis1",   new GameObject(axis));
        this.scene.addGameObject("cube1",   new GameObject(centeredCube));
        this.scene.addGameObject("teapot5", new GameObject(teapot));
        this.scene.addGameObject("teapot6", new GameObject(teapot));
        this.scene.addGameObject("cube2", new GameObject(outCenteredCube));
        this.scene.addGameObject("teapot7", new GameObject(teapot));
        this.scene.addGameObject("teapot8", new GameObject(teapot));

//        this.scene.addMultipleGameObjects(
//                List.of(new Scene.ObjectData("teapot1", "teapot"),
//                        new Scene.ObjectData("teapot2", "teapot"),
//                        new Scene.ObjectData("teapot3", "teapot"),
//                        new Scene.ObjectData("teapot4", "teapot"),
//                        new Scene.ObjectData("axis1",   "axis"),
//                        new Scene.ObjectData("cube1",   "cube centré"),
//                        new Scene.ObjectData("teapot5", "teapot"),
//                        new Scene.ObjectData("teapot6", "teapot"),
//                        new Scene.ObjectData("cube2", "cube pas centré"),
//                        new Scene.ObjectData("teapot7", "teapot"),
//                        new Scene.ObjectData("teapot8", "teapot")
////                        new Scene.ObjectData("F1", "F1")
//        ));


//        scene.getGameObject(8).setScale(2.5, 2.5, 2.5);

        scene.getGameObject("axis1").setPosition(0, 0, 0);
        scene.getGameObject("axis1").setScale(-0.3, 0.3, 0.3);

        scene.getGameObject("cube1").setPosition(1, 1, 1);
        scene.getGameObject("cube2").setPosition(0, 0, 0);
        scene.getGameObject("cube1").setRendered(true);
        scene.getGameObject("cube2").setRendered(false);
        scene.getGameObject("axis1").setRendered(true);


        scene.getGameObject("teapot5").setRendered(false);
        scene.getGameObject("teapot6").setRendered(false);
        scene.getGameObject("teapot7").setRendered(false);
        scene.getGameObject("teapot8").setRendered(false);


        GameObject t1 = scene.getGameObject("teapot1");
        t1.setPosition(-6, 0, 8);
        t1.setRotation(0, 0, 0);
        t1.setScale(1, 1, 1);

        GameObject t2 = scene.getGameObject("teapot2");
        t2.setPosition(6, 0, 8);
        t2.setRotation(45, 0, 0);
        t2.setScale(1.5, 1.5, 1.5);

        GameObject t3 = scene.getGameObject("teapot3");
        t3.setPosition(0, 5, 8);
        t3.setRotation(0, 0, 180);
        t3.setScale(0.5, 0.5, 0.5);

        GameObject t4 = scene.getGameObject("teapot4");
        t4.setPosition(0, -5, 8);
        t4.setRotation(-45, 45, 0);
        t4.setScale(2, 0.6, 1.2);

        t1.setRendered(true);
        t2.setRendered(true);
        t3.setRendered(true);
        t4.setRendered(true);

        this.headUpDisplay = new HeadUpDisplay(graphicEngineContext);

        this.pipeline = new Pipeline(camera, scene, graphicEngineContext);

        this.repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();

        SwingUtilities.invokeLater(() -> {
            this.graphicEngineContext.updateWindowInformation();
            this.createBufferStrategy(3);
            this.requestFocusInWindow();
            inputManager.centerMouse();

            this.start();
        });
    }

    public synchronized void start() {
        if (graphicEngineContext.isRunning()) return;
        graphicEngineContext.setRunning(true);
        gameThread = new Thread(this, "EngineThread");
        gameThread.start();
    }

    public synchronized void stop() {
        try {graphicEngineContext.setRunning(false);
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
//            System.out.println(deltaTime);

            if (deltaTime > 0.25) {
                deltaTime = 0.25;
            }

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
                this.render();
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

//        scene.getGameObject("teapot2").rotate(10,0,0);
//        scene.getGameObject("teapot3").rotate(0,5,5);
//        scene.getGameObject("teapot4").rotate(7,5,3);


        angleTheta += 0.07;
        anglePhi += 0.01;

        double r = 11.0;

        double y = r * Math.sin(anglePhi);
        double hR = r * Math.cos(anglePhi);
        double x = hR * Math.cos(angleTheta);
        double z = hR * Math.sin(angleTheta);

        double sX = 1.0 + (0.5 * Math.sin(anglePhi));
        double sY = 1.0 + (0.5 * Math.sin(angleTheta));
        double sZ = 1.0 + (0.5 * Math.cos(anglePhi));

//        scene.getGameObject("cube1").setScale(sX, sY, sZ);
//        scene.getGameObject("cube1").rotate(0.5,1,1.5);
//        scene.getGameObject("cube1").setPosition(x, y, z);

        if (graphicEngineContext.isHUDActive()) {
            headUpDisplay.updateStats();
        }
    }

    private void render() {
        BufferStrategy bs = this.getBufferStrategy();

        Graphics g = bs.getDrawGraphics();
//        Graphics g = this.getGraphics();

        g.setColor(this.getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        camera.updateWindowProjectionMatrix();
        camera.updateProjectionMatrix();

        graphicEngineContext.resetNbRenderedTriangle();

        pipeline.execution(g);

        if (graphicEngineContext.isHUDActive()) {
            headUpDisplay.draw(g);
        }

        g.dispose();
        bs.show();
    }

    public static void main(String[] args) {
        JFrame window = new JFrame("Graphic Engine");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        int width = 800;
        int height = 600;
        window.setSize(width, height);

        GraphicEngine graphicEngine = new GraphicEngine(width, height);
        window.add(graphicEngine);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    public GraphicEngineContext getEngineContext() {
        return graphicEngineContext;
    }

    public Camera getCamera() {
        return camera;
    }

    public int getCurrentFPS() {
        return currentFPS;
    }

    public int getCurrentUPS() {
        return currentUPS;
    }

    public BenchmarkManager getBenchmarkManager() {
        return benchmarkManager;
    }
}


