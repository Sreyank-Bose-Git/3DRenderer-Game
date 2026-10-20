package exec.renderer;

import exec.extras.Collection;
import main.RendererPanel;
import types.datatypes.Vector2;
import types.datatypes.Vector3;
import types.things.Mesh;
import types.things.Light;
import types.things.Object;
import types.things.Thing;

import java.util.Arrays;

public class World {

    public Mesh[] meshes;
    public Light[] lights;
    public Camera[] cameras;

    public Collection[] collections;

    public int activeCameraIndex;
    public RendererPanel rendererPanel;

    public World(
            int meshesSize,
            int lightsSize,
            int camerasSize,
            int collectionsSize,
            RendererPanel rendererPanel) {

        meshes = new Mesh[Math.max(1, meshesSize)];
        lights = new Light[Math.max(1, lightsSize)];
        cameras = new Camera[Math.max(1, camerasSize)];
        collections = new Collection[collectionsSize];

        storeCollection("default");

        this.rendererPanel = rendererPanel;

        createBasicWorld();
    }

    // =========================================================
    // INITIAL WORLD
    // =========================================================

    private void createBasicWorld() {

        storeCamera(
                new Vector3(0, 50, -200),
                new Vector2(0, 0)
        );

        activeCameraIndex = 0;

        storeLight(
                "point",
                5.0,
                new Vector3(0, 50, -200),
                new Vector3(0, 0, 180)
        );

        storeObject("/types/objects/cube.obj", "IamFat");

        Mesh mesh = getMesh(0);

        if (mesh != null) {
            mesh.scale.x *= 1000;
            mesh.scale.y *= 10;
            mesh.scale.z *= 1000;
        }
    }

    // =========================================================
    // FIND AVAILABLE SLOTS
    // =========================================================

    private int findNextCollectionIndex() {
        for (int i = 0; i < collections.length; i++) {
            if (collections[i] == null) {
                return i;
            }
        }

        return -1;
    }

    private int findNextCameraIndex() {
        for (int i = 0; i < cameras.length; i++) {
            if (cameras[i] == null) {
                return i;
            }
        }

        return -1;
    }

    private int findNextLightIndex() {
        for (int i = 0; i < lights.length; i++) {
            if (lights[i] == null) {
                return i;
            }
        }

        return -1;
    }

    private int findNextMeshIndex() {
        for (int i = 0; i < meshes.length; i++) {
            if (meshes[i] == null) {
                return i;
            }
        }

        return -1;
    }

    // =========================================================
    // GROW ARRAYS
    // =========================================================

    private void growCollections() {
        collections = Arrays.copyOf(
                collections,
                Math.max(1, collections.length * 2)
        );
    }

    private void growCameras() {
        cameras = Arrays.copyOf(
                cameras,
                Math.max(1, cameras.length * 2)
        );
    }

    private void growLights() {
        lights = Arrays.copyOf(
                lights,
                Math.max(1, lights.length * 2)
        );
    }

    private void growMeshes() {
        meshes = Arrays.copyOf(
                meshes,
                Math.max(1, meshes.length * 2)
        );
    }

    public Collection storeCollection(String name) {

        int index = findNextCollectionIndex();

        if (index == -1) {
            growCollections();
            index = findNextCollectionIndex();
        }

        Collection collection = new Collection(name);

        collections[index] = collection;

        return collection;
    }

    public Collection storeCollection(String name, Thing[] things, Vector3 coordinates, Vector3 scale, Vector3 rotation) {

        int index = findNextCollectionIndex();

        if (index == -1) {
            growCollections();
            index = findNextCollectionIndex();
        }

        Collection collection = new Collection(name, things, coordinates, scale, rotation);

        collections[index] = collection;

        return collection;
    }

    // =========================================================
    // STORE CAMERA
    // =========================================================

    public Camera storeCamera(
            Vector3 coordinates,
            Vector2 rotation) {

        int index = findNextCameraIndex();

        if (index == -1) {
            growCameras();
            index = findNextCameraIndex();
        }

        Camera camera = new Camera(
                rendererPanel,
                coordinates,
                rotation
        );

        cameras[index] = camera;

        return camera;
    }

    // =========================================================
    // STORE LIGHT
    // =========================================================

    public Light storeLight(
            String lightType,
            double lightStrength,
            Vector3 coordinates,
            Vector3 rotation) {

        int index = findNextLightIndex();

        if (index == -1) {
            growLights();
            index = findNextLightIndex();
        }

        Light light = new Light(
                lightType,
                lightStrength,
                coordinates,
                rotation
        );

        lights[index] = light;

        return light;
    }

    // =========================================================
    // STORE OBJECT / MESH
    // =========================================================

    public Mesh storeObject(String objPath, String collectionName) {

        int index = findNextMeshIndex();

        if (index == -1) {
            growMeshes();
            index = findNextMeshIndex();
        }

        try {
            Mesh mesh = new Object(this, objPath, collectionName);
            mesh.generate();

            if(collectionName.equals("default")) {
                collections[0].storeThing(mesh);
            }
            else {
                Collection collection = storeCollection(collectionName);
                collection.storeThing(mesh);
            }

            meshes[index] = mesh;

            return mesh;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public int getCollectionIndex(String collectionName) {
        for(int i = 0; i < collections.length & collections[i] != null; i ++) {
            if(collections[i].name == collectionName) {
                return i;
            }
        }

        return -1;
    }

    public Mesh getMesh(int index) {
        if (index < 0 || index >= meshes.length) {
            return null;
        }

        return meshes[index];
    }

    public Camera getActiveCamera() {
        if (activeCameraIndex < 0
                || activeCameraIndex >= cameras.length) {
            return null;
        }

        return cameras[activeCameraIndex];
    }
}