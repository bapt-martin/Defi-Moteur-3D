package graphicEngine.scene;

import graphicEngine.math.geometry.Mesh;
import graphicEngine.renderer.Texture;
import graphicEngine.scene.lightRelative.PointLight;

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

    public Scene() {
        this.meshLibrary = new HashMap<>();
        this.textureLibrary = new HashMap<>();

        this.gameObjectDirectory = new HashMap<>();
        this.renderQueue = new ArrayList<>();

        this.lightDirectory = new HashMap<>();
        this.lightQueue = new ArrayList<>();
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
