package gameEngine; // À terme, tu mettras ça dans un package 'game'

import graphicEngine.io.ObjLoader;
import graphicEngine.math.geometry.Mesh;
import graphicEngine.math.tools.Vector3D;
import graphicEngine.renderer.Texture;
import graphicEngine.scene.GameObject;
import graphicEngine.scene.lightRelative.PointLight;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Paths;

// MonJeu "EST UN" GameEngine
public class GameTest extends GameEngine {
    private double angleTheta = 0;
    private double anglePhi = 0;

    public GameTest(int width, int height) {
        super(width, height);
    }

    @Override
    public void initGame() {
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

        System.out.println(this.scene.getMeshLibrary().get("texturedCube").getMeshTriangle().getFirst().getTextVertices()[0].toString());
        System.out.println(this.scene.getMeshLibrary().get("texturedCube").getMeshTriangle().getFirst().getVertices()[0].toString());

        Texture texturedCubeTexture = new Texture("obj model\\texture\\textureTest1.png");
        this.scene.addTexture("texturedCubeTexture",texturedCubeTexture);
        this.scene.getTextureLibrary().get("texturedCubeTexture").printPixelColor(0,1022);


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

        PointLight sun3 = new PointLight(
                new Vector3D(-10, 5, -10),
                0.05,
                new Color(255, 180, 50)
        );
        scene.addLight("sunLight3", sun3);
        scene.addGameObject("sun3", new GameObject(texturedSphere, texturedCubeTexture));
        scene.getGameObject("sun3").setPosition(-10, 5, -10);


        this.scene.addGameObject("teapot1", new GameObject(teapot, Color.WHITE));
        this.scene.addGameObject("teapot2", new GameObject(teapot));
        this.scene.addGameObject("teapot3", new GameObject(teapot));
        this.scene.addGameObject("teapot4", new GameObject(teapot));
        this.scene.addGameObject("axis1",   new GameObject(axis, Color.BLUE));
        this.scene.addGameObject("cubeCentered",   new GameObject(centeredCube));
        this.scene.addGameObject("teapot5", new GameObject(teapot));
        this.scene.addGameObject("teapot6", new GameObject(teapot));
        this.scene.addGameObject("cubeOutCentered", new GameObject(outCenteredCube));
        this.scene.addGameObject("teapot7", new GameObject(teapot));
        this.scene.addGameObject("teapot8", new GameObject(teapot));
        this.scene.addGameObject("cube3",   new GameObject(centeredCube));
        this.scene.addGameObject("cube4",   new GameObject(centeredCube));
        this.scene.addGameObject("cube5", new GameObject(outCenteredCube));
        this.scene.addGameObject("cube6", new GameObject(outCenteredCube));
        this.scene.addGameObject("texturedCube", new GameObject(texturedCube,texturedCubeTexture));
        this.scene.addGameObject("wall", new GameObject(texturedSphere,texturedCubeTexture));

        scene.getGameObject("wall").setRotation(-45, 45, 45);;
        scene.getGameObject("wall").setPosition(-25, 0, 0);
        scene.getGameObject("wall").setScale(10, 10, 10);

        System.out.println(this.scene.getGameObjectDirectory().get("texturedCube").getMesh().getMeshTriangle().getFirst().getTextVertices()[0].toString());
        System.out.println(this.scene.getGameObjectDirectory().get("texturedCube").getMesh().getMeshTriangle().getFirst().getVertices()[0].toString());

        scene.getGameObject("texturedCube").setRotation(45, 45, 45);;

        scene.getGameObject("axis1").setPosition(0, 0, 0);
        scene.getGameObject("axis1").setScale(-0.3, 0.3, 0.3);

        scene.getGameObject("cubeCentered").setPosition(0, 0, 0);
        scene.getGameObject("cubeOutCentered").setPosition(0, 0, 0);
        scene.getGameObject("cubeCentered").   setRendered(false);
        scene.getGameObject("cubeOutCentered").setRendered(false);
        scene.getGameObject("axis1").          setRendered(true);


        scene.getGameObject("teapot5").setRendered(false);
        scene.getGameObject("teapot6").setRendered(false);
        scene.getGameObject("teapot7").setRendered(false);
        scene.getGameObject("teapot8").setRendered(false);


        GameObject t1 = scene.getGameObject("teapot1");
        t1.setPosition(25, 0, 8);
        t1.setRotation(0, 0, 0);
        t1.setScale(10, 10, 10);

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
        t2.setRendered(false);
        t3.setRendered(false);
        t4.setRendered(false);

        scene.getGameObject("cube3").setPosition(6, 0, 5);
        scene.getGameObject("cube4").setPosition(-6, 0, 5);
        scene.getGameObject("cube5").setPosition(6, 0, -5);
        scene.getGameObject("cube6").setPosition(-6, 0, -5);

        scene.getGameObject("cube3").setRendered(false);
        scene.getGameObject("cube4").setRendered(false);
        scene.getGameObject("cube5").setRendered(false);
        scene.getGameObject("cube6").setRendered(false);
    }

    @Override
    public void updateGameLogic() {
        scene.getGameObject("teapot2").rotate(10,0,0);
        scene.getGameObject("teapot3").rotate(0,5,5);
        scene.getGameObject("teapot4").rotate(7,5,3);


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
        scene.getGameObject("sun3").setPosition(-x, 5, -z);

        scene.getLight("sunLight1").setOn(true);
        scene.getLight("sunLight2").setOn(false);
        scene.getLight("sunLight3").setOn(false);

        scene.linkLight("sun1", "sunLight1");
        scene.linkLight("sun2", "sunLight2");
        scene.linkLight("sun3", "sunLight3");
    }

    public static void main(String[] args) {
        JFrame window = new JFrame("EnginesTest");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        int width = 800;
        int height = 600;

        GameTest jeu = new GameTest(width, height);

        window.add(jeu.getGraphicEngine());
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);

        jeu.start();
    }
}