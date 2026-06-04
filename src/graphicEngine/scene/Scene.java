package graphicEngine.scene;

import graphicEngine.math.geometry.Mesh;
import graphicEngine.renderer.Texture;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scene {
    public record ObjectData(String name, String meshName) {}
    private final Map<String, Mesh> meshLibrary;
    private final Map<String, Texture> textureLibrary;
    private final Map<String, GameObject> gameObjectLibrary;
    private final List<GameObject> renderQueue;


    public Scene() {
        this.renderQueue = new ArrayList<>();
        this.meshLibrary = new HashMap<>();
        this.textureLibrary = new HashMap<>();
        this.gameObjectLibrary = new HashMap<>();
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
        gameObjectLibrary.put(objectName, gameObject);
        this.renderQueue.add(gameObject);
    }

    public void removeMultipleGameObject(List<String> nameList) {
        for (String name : nameList) {
            this.removeGameObject(name);
        }
    }

    private void removeGameObject(String objectName) {
        int lastIndex = this.renderQueue.size()-1;
        GameObject objectToRemove = this.gameObjectLibrary.get(objectName);

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

        this.gameObjectLibrary.remove(objectName);
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
        return this.gameObjectLibrary.get(name);
    }

    public Map<String, Mesh> getMeshLibrary() {
        return meshLibrary;
    }

    public Map<String, Texture> getTextureLibrary() {
        return textureLibrary;
    }

    public Map<String, GameObject> getGameObjectLibrary() {
        return gameObjectLibrary;
    }

    public List<GameObject> getRenderQueue() {
        return renderQueue;
    }
}
