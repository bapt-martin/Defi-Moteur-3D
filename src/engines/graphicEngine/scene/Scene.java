package engines.graphicEngine.scene;

import engines.graphicEngine.math.geometry.Mesh;
import engines.graphicEngine.math.geometry.Triangle;
import engines.graphicEngine.math.tools.Matrix;
import engines.graphicEngine.renderer.Texture;
import engines.graphicEngine.scene.lightRelative.PointLight;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scene {
    public record ObjectData(String name, String meshName) {}

    private final Map<String, Mesh> meshLibrary;
    private final Map<String, Texture> textureLibrary;

    private final Map<String, GameObject> gameObjectDirectory;
    private final List<GameObject> renderQueue;

    private final Map<String, PointLight> lightDirectory;
    private final List<PointLight> lightQueue;

    private Triangle[] globalOriginals;
    private Triangle[] globalPool;
    private int totalTriangles = 0;

    public Scene() {
        this.meshLibrary = new HashMap<>();
        this.textureLibrary = new HashMap<>();

        this.gameObjectDirectory = new HashMap<>();
        this.renderQueue = new ArrayList<>();

        this.lightDirectory = new HashMap<>();
        this.lightQueue = new ArrayList<>();
    }


    // Cette méthode est à appeler UNE SEULE FOIS quand ta scène a fini de charger
    public void buildGlobalBuffers() {
        // 1. On compte le nombre total de triangles dans la scène
        this.totalTriangles = 0;
        for (GameObject obj : renderQueue) {
            this.totalTriangles += obj.getMesh().getMeshTriangle().size();
        }

        // 2. On alloue les tableaux géants une bonne fois pour toutes
        this.globalOriginals = new Triangle[totalTriangles];
        this.globalPool = new Triangle[totalTriangles];

        int currentIndex = 0;

        // 3. On "aplatit" tous les objets
        for (GameObject obj : renderQueue) {
            List<Triangle> objTriangles = obj.getMesh().getMeshTriangle();
            Matrix refMatrix = obj.getWorldTransformMatrix();
            Color refColor = obj.getBasedColor();
            Texture refTexture = obj.getTexture();

            for (Triangle tri : objTriangles) {
                // On injecte le contexte de l'objet dans le triangle original
//                tri.setParentContext(refMatrix, refColor, refTexture);

                this.globalOriginals[currentIndex] = tri;

                // On crée le brouillon (Deep Clone) directement dans le grand pool
                this.globalPool[currentIndex] = tri.deepClone();

                currentIndex++;
            }
        }
    }

    public Triangle[] getGlobalOriginals() { return globalOriginals; }
    public Triangle[] getGlobalPool() { return globalPool; }
    public int getTotalTriangles() { return totalTriangles; }

    public int getTotalTrianglesCount() {
        int count = 0;
        for (GameObject obj : renderQueue) {
            count += obj.getPoolTriangle().length;
        }
        return count;
    }

    public void linkLight(String gameObjectName, String lightName) {
        GameObject sunObject = this.getGameObject(gameObjectName);
        PointLight sunLight = this.getLightDirectory().get(lightName);

        sunLight.setPosition(sunObject.getPosition());
    }

    public void addMultipleGameObjects(List<ObjectData> objectReferences) {
        for (ObjectData objectData : objectReferences) {
            GameObject gameObject = new GameObject(meshLibrary.get(objectData.meshName()));
            this.addGameObject(objectData.name(), gameObject);
        }
    }

    public void addGameObject(String objectName, GameObject gameObject) {
        gameObject.setName(objectName);
        gameObject.setId(this.renderQueue.size());
        gameObjectDirectory.put(objectName, gameObject);
        this.renderQueue.add(gameObject);
    }

    public void addLight(String lightName, PointLight pointLight) {
        pointLight.setName(lightName);
        pointLight.setId(this.lightQueue.size());
        lightDirectory.put(lightName, pointLight);
        this.lightQueue.add(pointLight);
    }

    public void removeMultipleGameObject(List<String> nameList) {
        for (String name : nameList) {
            this.removeGameObject(name);
        }
    }

    private void removeGameObject(String objectName) {
        int lastIndex = this.renderQueue.size()-1;
        GameObject objectToRemove = this.gameObjectDirectory.get(objectName);

        if (objectToRemove == null) {
            return;
        }

        int objectId = objectToRemove.getId();

        if (objectId!=lastIndex) {
            this.renderQueue.set(objectId,this.renderQueue.removeLast());
            this.renderQueue.get(objectId).setId(objectId);
        } else {
            this.renderQueue.removeLast();
        }

        this.gameObjectDirectory.remove(objectName);
    }

    public void addMesh(String meshName, Mesh mesh) {
        mesh.setMeshName(meshName);
        this.meshLibrary.put(meshName,mesh);
    }

    public void addTexture(String textureName, Texture texture) {
        texture.setTextureName(textureName);
        this.textureLibrary.put(textureName,texture);
    }

    public GameObject getGameObject(String name) {
        return this.gameObjectDirectory.get(name);
    }
    public PointLight getLight(String name) {
        return this.lightDirectory.get(name);
    }

    public Map<String, Mesh> getMeshLibrary() {
        return meshLibrary;
    }

    public Map<String, Texture> getTextureLibrary() {
        return textureLibrary;
    }

    public Map<String, GameObject> getGameObjectDirectory() {
        return gameObjectDirectory;
    }

    public Map<String, PointLight> getLightDirectory() {
        return lightDirectory;
    }

    public List<GameObject> getRenderQueue() {
        return renderQueue;
    }

    public List<PointLight> getLightQueue() {
        return lightQueue;
    }
}
