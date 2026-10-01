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


public class FirstGameTest extends GameEngine {
    private double angleTheta = 0;
    private double anglePhi = 0;

    public FirstGameTest(int width, int height) {
        super(width, height);
    }

    @Override
    public void initGame() {
        this.camera.updateParameters(new Vertex3D(0, 0, 30),
                new Camera.CameraRotation(0, 0, 0),
                new Vector3D(0, 0, 1),
                new Vector3D(0, 1, 0),
                new Plane(new Vertex3D(0, 0, 0.1f), new Vector3D(0, 0, 1)),
                new Plane(new Vertex3D(0, 0, 100), new Vector3D(0, 0, -1)),
                0.1f,50,90,
                0.005f, Color.WHITE, 15);

        scene.addLight("cameraSpotLight", this.camera.getCameraSpot());

        long start = System.nanoTime();
            Mesh teapot          = ObjLoader.loadMesh(Paths.get("obj model\\objTextureLess\\teapot.obj"));
            Mesh axis            = ObjLoader.loadMesh(Paths.get("obj model\\objTextureLess\\axis.obj"));
            Mesh centeredCube    = ObjLoader.loadMesh(Paths.get("obj model\\objTextureLess\\cube.obj"));
            Mesh outCenteredCube = ObjLoader.loadMesh(Paths.get("obj model\\objTextureLess\\cube pas centré.obj"));
            Mesh texturedCube    = ObjLoader.loadMesh(Paths.get("obj model\\objWithTexture\\cubeTexture.obj"));
            Mesh texturedSphere  = ObjLoader.loadMesh(Paths.get("obj model\\objWithTexture\\sphereTexture.obj"));
    //        new Scene.MeshData("F1","obj model\\F1.obj")
        long end = System.nanoTime();
        System.out.printf("             |-- mesh loaded in %.2f ms%n", (end - start) / 1_000_000.0);


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
                0.05f,
                new Color(255, 180, 50)
        );
        scene.addLight("sunLight1", sun1);
        scene.addGameObject("sun1", new GameObject(texturedSphere, texturedCubeTexture));
        scene.getGameObject("sun1").setPosition(10, 5, 10);



        this.scene.addGameObject("axis1",   new GameObject(axis, Color.BLUE));


        this.scene.addGameObject("texturedCube", new GameObject(texturedCube,texturedCubeTexture));
        scene.getGameObject("texturedCube").setRotation(45, 45, 45);;


        scene.getGameObject("axis1").setPosition(0, 0, 0);
        scene.getGameObject("axis1").setScale(-0.3f, 0.3f, 0.3f);


        this.scene.addGameObject("teapot1", new GameObject(teapot, Color.WHITE));

        GameObject t1 = scene.getGameObject("teapot1");
        t1.setPosition(35, 0, 8);
//        t1.setPosition(0, 0, 0);
        t1.setRotation(0, 0, 0);
        t1.setScale(1, 1, 1);

        t1.setRendered(true);
    }

    @Override
    public void updateGameLogic() {
        if (graphicEngineContext.isBenchmarkRunning()) {
            this.graphicEngineContext.getBenchmarkManager().benchMarkSceneUpdate();
        } else {
            camera.getCameraSpot().setOn(true);

            angleTheta += 0.07;
            anglePhi += 0.01;

            double r = 13.0;

            double hR = r * Math.cos(anglePhi);
            float x = (float) (hR * Math.cos(angleTheta / 2));
            float y = (float) (r * Math.sin(anglePhi));
            float z = (float) (hR * Math.sin(angleTheta));

            GameObject teapot = scene.getGameObject("teapot1");
            teapot.setPosition(x, y, z);

            scene.getGameObject("sun1").setPosition(x, 5, z);
            scene.getLight("sunLight1").setOn(true);
            scene.getLight("sunLight1").setLightColor(Color.RED);
            scene.linkLight("sun1", "sunLight1");
        }
    }

    public static void main(String[] args) {
        FirstGameTest game1 = new FirstGameTest(800 ,600);
        game1.start();
    }
}