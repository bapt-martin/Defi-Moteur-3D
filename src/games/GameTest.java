package games;

import engines.gameEngine.GameEngine;
import engines.graphicEngine.io.ObjLoader;
import engines.graphicEngine.math.geometry.Mesh;
import engines.graphicEngine.math.geometry.Plane;
import engines.graphicEngine.math.geometry.Vertex3D;
import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.renderer.Camera;
import engines.graphicEngine.renderer.Texture;
import engines.graphicEngine.scene.GameObject;
import engines.graphicEngine.scene.lightRelative.PointLight;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Paths;


public class GameTest extends GameEngine {
    private double angleTheta = 0;
    private double anglePhi = 0;

    public GameTest(int width, int height) {
        super(width, height);
    }

    @Override
    public void initGame() {
        this.camera.updateParameters(new Vertex3D(0, 0, 23),
                new Camera.CameraRotation(0, 0, 0),
                new Vector3D(0, 0, 1),
                new Vector3D(0, 1, 0),
                new Plane(new Vertex3D(0, 0, 0.1), new Vector3D(0, 0, 1)),
                new Plane(new Vertex3D(0, 0, 100), new Vector3D(0, 0, -1)),
                0.1,50,90,
                0.005, Color.WHITE, 15);

        scene.addLight("cameraSpotLight", this.camera.getCameraSpot());

        Mesh teapot = ObjLoader.readObjFile(Paths.get("obj model\\objTextureLess\\teapot.obj"));
        Mesh axis = ObjLoader.readObjFile(Paths.get("obj model\\objTextureLess\\axis.obj"));
        Mesh centeredCube = ObjLoader.readObjFile(Paths.get("obj model\\objTextureLess\\cube.obj"));
        Mesh outCenteredCube = ObjLoader.readObjFile(Paths.get("obj model\\objTextureLess\\cube pas centré.obj"));
        Mesh texturedCube = ObjLoader.readObjFile(Paths.get("obj model\\objWithTexture\\cubeTexture.obj"));
        Mesh texturedSphere = ObjLoader.readObjFile(Paths.get("obj model\\objWithTexture\\sphereTexture.obj"));
//        new Scene.MeshData("F1","obj model\\F1.obj")

        this.scene.addMesh("teapot", teapot);
        this.scene.addMesh("axis", axis);
        this.scene.addMesh("centeredCube", centeredCube);
        this.scene.addMesh("outCenteredCube", outCenteredCube);
        this.scene.addMesh("texturedCube", texturedCube);
        this.scene.addMesh("texturedSphere", texturedSphere);

//        System.out.println(this.scene.getMeshLibrary().get("texturedCube").getMeshTriangle().getFirst().getTextVertices()[0].toString());
//        System.out.println(this.scene.getMeshLibrary().get("texturedCube").getMeshTriangle().getFirst().getVertices()[0].toString());

        Texture texturedCubeTexture = new Texture("obj model\\texture\\textureTest1.png");
        Texture photoTexture = new Texture("obj model\\texture\\photo.png");
        this.scene.addTexture("texturedCubeTexture",texturedCubeTexture);
        this.scene.addTexture("photoTexture",photoTexture);


        PointLight sun1 = new PointLight(
                new Vector3D(10, 5, 0),
                0.05,
                new Color(255, 180, 50)
        );
        scene.addLight("sunLight1", sun1);
        scene.addGameObject("sun1", new GameObject(texturedSphere, texturedCubeTexture));
        scene.getGameObject("sun1").setPosition(10, 5, 10);

        PointLight sun2 = new PointLight(
                new Vector3D(-10, 5, 0),
                0.05,
                new Color(255, 180, 50)
        );
        scene.addLight("sunLight2", sun2);
        scene.addGameObject("sun2", new GameObject(texturedSphere, texturedCubeTexture));
        scene.getGameObject("sun2").setPosition(-10, 5, 10);

        this.scene.addGameObject("teapot1", new GameObject(teapot, Color.WHITE));
        this.scene.addGameObject("teapot2", new GameObject(teapot, Color.WHITE));
        this.scene.addGameObject("teapot3", new GameObject(teapot, Color.WHITE));
        this.scene.addGameObject("teapot4", new GameObject(teapot, Color.WHITE));
        this.scene.addGameObject("teapot5", new GameObject(teapot, Color.WHITE));
        this.scene.addGameObject("teapot6", new GameObject(teapot, Color.WHITE));
        this.scene.addGameObject("teapot7", new GameObject(teapot, Color.WHITE));
        this.scene.addGameObject("teapot8", new GameObject(teapot, Color.WHITE));
        this.scene.addGameObject("teapot9", new GameObject(teapot, Color.WHITE));
        this.scene.addGameObject("teapot10", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot11", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot12", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot13", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot14", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot15", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot16", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot17", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot18", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot19", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot20", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot21", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot22", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot23", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot24", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot25", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot26", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot27", new GameObject(teapot, Color.WHITE));
//        this.scene.addGameObject("teapot28", new GameObject(teapot, Color.WHITE));



        this.scene.addGameObject("axis1",   new GameObject(axis, Color.BLUE));


        this.scene.addGameObject("texturedCube", new GameObject(texturedCube,texturedCubeTexture));
        this.scene.addGameObject("wall", new GameObject(texturedCube,photoTexture));


        scene.getGameObject("wall").setRotation(0, 180, 0);;
        scene.getGameObject("wall").setPosition(-25, 0, 0);
        scene.getGameObject("wall").setScale(10, 10, 10);


        scene.getGameObject("texturedCube").setRotation(45, 45, 45);;


        scene.getGameObject("axis1").setPosition(0, 0, 0);
        scene.getGameObject("axis1").setScale(-0.3, 0.3, 0.3);




        GameObject t1 = scene.getGameObject("teapot1");
        t1.setPosition(35, 0, 8);
//        t1.setPosition(0, 0, 0);
        t1.setRotation(0, 0, 0);
        t1.setScale(10, 10, 10);

        t1.setRendered(true);
    }

    @Override
    public void updateGameLogic() {
        boolean isFlashlightOn = graphicEngineContext.isCameraSpotOn();
        camera.getCameraSpot().setOn(isFlashlightOn);

        angleTheta += 0.07;
        anglePhi += 0.01;

        double r = 11.0;

        double hR = r * Math.cos(anglePhi);
        double x = hR * Math.cos(angleTheta/2);
        double y = r * Math.sin(anglePhi);
        double z = hR * Math.sin(angleTheta);

        double sX = 1.0 + (0.5 * Math.sin(anglePhi));
        double sY = 1.0 + (0.5 * Math.sin(angleTheta));
        double sZ = 1.0 + (0.5 * Math.cos(anglePhi));

        scene.getGameObject("texturedCube").setScale(sX, sY, sZ);
        scene.getGameObject("texturedCube").rotate(0.5,1,1.5);
        scene.getGameObject("texturedCube").setPosition(x, y, z);

        scene.getGameObject("sun1").setPosition(x, 5, z);
        scene.getGameObject("sun2").setPosition(-x, 5, z);

        scene.getLight("sunLight1").setOn(true);
        scene.getLight("sunLight1").setLightColor(Color.GREEN);
        scene.getLight("sunLight2").setOn(true);
        scene.getLight("sunLight2").setLightColor(Color.BLUE);

        scene.linkLight("sun1", "sunLight1");
        scene.linkLight("sun2", "sunLight2");

//        int cores = Runtime.getRuntime().availableProcessors();
//        System.out.println("Cœurs disponibles : " + cores);
    }

    public static void main(String[] args) {
        JFrame window = new JFrame("EnginesTest");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        int width = 800;
        int height = 600;

        GameTest game1 = new GameTest(width, height);

        window.add(game1.getGraphicEngine());
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);

        game1.start();
    }
}