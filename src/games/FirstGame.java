package games;

import engines.gameEngine.GameEngine;
import engines.graphicEngine.core.BenchmarkManager;
import engines.graphicEngine.io.ObjLoader;
import engines.graphicEngine.math.geometry.Mesh;
import engines.graphicEngine.math.geometry.Plane;
import engines.graphicEngine.math.geometry.Vertex3D;
import engines.graphicEngine.math.tools.ColorUtils;
import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.renderer.Camera;
import engines.graphicEngine.renderer.Texture;
import engines.graphicEngine.scene.GameObject;
import engines.graphicEngine.scene.Scene;
import engines.graphicEngine.scene.lightRelative.PointLight;

import java.nio.file.Paths;


public class FirstGame extends GameEngine {
    private double angleTheta = 0;
    private double anglePhi = 0;

    public FirstGame(int width, int height) {
        super(width, height);
    }

    @Override
    public void initGame() {
//        Scene scene =  this.graphicEngineContext.getCurrentScene();
        Scene scene = new Scene(graphicEngineContext);
        scene.getMainCamera().updateParameters(new Vertex3D(10, 40, 0),
                new Camera.CameraRotation(0, 90, 0),
                new Vector3D(0, 0, 1),
                new Vector3D(0, 1, 0),
                new Plane(new Vertex3D(0, 0, 0.1f), new Vector3D(0, 0, 1)),
                new Plane(new Vertex3D(0, 0, 100), new Vector3D(0, 0, -1)),
                0.1f,50,90,
                0.005f, ColorUtils.WHITE, 55);
//        this.addLight("cameraSpotLight", mainCamera.getCameraSpot());

        long start = System.nanoTime();
            Mesh teapot          = ObjLoader.loadMesh(Paths.get("obj model\\objTextureLess\\teapot.obj"));
            Mesh axis            = ObjLoader.loadMesh(Paths.get("obj model\\objTextureLess\\axis.obj"));
            Mesh centeredCube    = ObjLoader.loadMesh(Paths.get("obj model\\objTextureLess\\cube.obj"));
            Mesh outCenteredCube = ObjLoader.loadMesh(Paths.get("obj model\\objTextureLess\\cube pas centré.obj"));
            Mesh texturedCube    = ObjLoader.loadMesh(Paths.get("obj model\\objWithTexture\\cubeTexture.obj"));
            Mesh texturedSphere  = ObjLoader.loadMesh(Paths.get("obj model\\objWithTexture\\sphereTexture.obj"));
        long end = System.nanoTime();
        System.out.printf("             |-- mesh loaded in %.2f ms%n", (end - start) / 1_000_000.0);


        scene.addMesh("teapot", teapot);
        scene.addMesh("axis", axis);
        scene.addMesh("centeredCube", centeredCube);
        scene.addMesh("outCenteredCube", outCenteredCube);
        scene.addMesh("texturedCube", texturedCube);
        scene.addMesh("texturedSphere", texturedSphere);

        Texture texturedCubeTexture = new Texture("obj model\\texture\\textureTest1.png");
        Texture photoTexture = new Texture("obj model\\texture\\photo.png");
        scene.addTexture("texturedCubeTexture",texturedCubeTexture);
        scene.addTexture("photoTexture",photoTexture);


        PointLight sun1 = new PointLight(
                new Vector3D(10, 5, 0),
                0.0005f,
                ColorUtils.getARGB(255, 255, 180, 50)
        );
        scene.addLight("sunLight1", sun1);
        scene.addGameObject("sun1", new GameObject(texturedSphere, texturedCubeTexture));
        scene.getGameObject("sun1").setPosition(10, 5, 10);



        scene.addGameObject("axis1",   new GameObject(axis, ColorUtils.BLUE));
        scene.getGameObject("axis1").setPosition(0, 0, 0);
        scene.getGameObject("axis1").setScale(-0.3f, 0.3f, 0.3f);


        scene.addGameObject("texturedCube", new GameObject(texturedCube,texturedCubeTexture));
        scene.getGameObject("texturedCube").setRotation(45, 45, 45);

//        scene.addGameObject("photoTexture", new GameObject(texturedCube,photoTexture));
//        scene.getGameObject("photoTexture").setRotation(45, 45, 45);
//        scene.getGameObject("photoTexture").setPosition(10, 10, 10);
//        scene.getGameObject("photoTexture").setScale(1, 1, 1);

        scene.addGameObject("teapot1", new GameObject(teapot, ColorUtils.YELLOW));
        GameObject t1 = scene.getGameObject("teapot1");
        t1.setPosition(35, 0, 8);
//        t1.setPosition(0, 0, 0);
        t1.setRotation(0, 0, 0);
        t1.setScale(1, 1, 1);

        t1.setRendered(true);
    }

    @Override
    public void updateGameLogic() {
        if (graphicEngineContext.getBenchmarkRunningState() == BenchmarkManager.State.MEASURING) {
            this.graphicEngineContext.getBenchmarkManager().updateBenchmarkScene();
        } else {
            Scene scene =  this.graphicEngineContext.getCurrentScene();
            scene.getMainCamera().getCameraSpot().setOn(graphicEngineContext.isCameraSpotOn());

            angleTheta += 0.07;
            anglePhi += 0.01;

            double r = 13.0;

            double hR = r * Math.cos(anglePhi);
            float x = (float) (hR * Math.cos(angleTheta / 2));
            float y = (float) (r * Math.sin(anglePhi));
            float z = (float) (hR * Math.sin(angleTheta));

            GameObject teapot = scene.getGameObject("teapot1");
            teapot.setPosition(x, y, z);

            scene.getGameObject("sun1").setPosition(-z, -x, -y);
            scene.getLight("sunLight1").setOn(true);
            scene.getLight("sunLight1").setLightColor(ColorUtils.RED);
            scene.linkLight("sun1", "sunLight1");
        }
    }

    public static void main(String[] args) {
        FirstGame game1 = new FirstGame(800 ,600);
        game1.start();
    }
}