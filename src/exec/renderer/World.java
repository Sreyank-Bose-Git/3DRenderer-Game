package exec.renderer;

import exec.extras.Collection;
import main.RendererPanel;
import types.datatypes.Vector2;
import types.datatypes.Vector3;
import types.things.Mesh;
import types.things.Light;
import types.things.meshes.Object;
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
                new Vector3(0, 0, 180),
                "IamFat"
        );

        storeObject("/types/objects/cube.obj", "IamFat");

        Mesh mesh = meshes[0];

        if (mesh != null) {
            mesh.scale.x *= 1000;
            mesh.scale.y *= 10;
            mesh.scale.z *= 1000;
        }
    }

    public void update() {

        // Meshes
        for(int i = 0; i < meshes.length && meshes[i] != null; i++) {

            meshes[i].actualCoordinates.x = meshes[i].localCoordinates.x + collections[getCollectionIndex(meshes[i].collectionName)].coordinates.x;
            meshes[i].actualCoordinates.y = meshes[i].localCoordinates.y + collections[getCollectionIndex(meshes[i].collectionName)].coordinates.y;
            meshes[i].actualCoordinates.z = meshes[i].localCoordinates.z + collections[getCollectionIndex(meshes[i].collectionName)].coordinates.z;

            meshes[i].actualRotation.x = meshes[i].localRotation.x + collections[getCollectionIndex(meshes[i].collectionName)].rotation.x;
            meshes[i].actualRotation.y = meshes[i].localRotation.y + collections[getCollectionIndex(meshes[i].collectionName)].rotation.y;
            meshes[i].actualRotation.z = meshes[i].localRotation.z + collections[getCollectionIndex(meshes[i].collectionName)].rotation.z;

        }

        // Lights
        for(int i = 0; i < lights.length && lights[i] != null; i++) {

            lights[i].actualCoordinates.x = lights[i].localCoordinates.x + collections[getCollectionIndex(lights[i].collectionName)].coordinates.x;
            lights[i].actualCoordinates.y = lights[i].localCoordinates.y + collections[getCollectionIndex(lights[i].collectionName)].coordinates.y;
            lights[i].actualCoordinates.z = lights[i].localCoordinates.z + collections[getCollectionIndex(lights[i].collectionName)].coordinates.z;

            lights[i].actualRotation.x = lights[i].localRotation.x + collections[getCollectionIndex(lights[i].collectionName)].rotation.x;
            lights[i].actualRotation.y = lights[i].localRotation.y + collections[getCollectionIndex(lights[i].collectionName)].rotation.y;
            lights[i].actualRotation.z = lights[i].localRotation.z + collections[getCollectionIndex(lights[i].collectionName)].rotation.z;

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

    public void storeCamera(
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

    }

    // =========================================================
    // STORE LIGHT
    // =========================================================

    public void storeLight(
            String lightType,
            double lightStrength,
            Vector3 coordinates,
            Vector3 rotation,
            String collectionName) {

        int index = findNextLightIndex();

        if (index == -1) {
            growLights();
            index = findNextLightIndex();
        }

        Light light = new Light(
                lightType,
                lightStrength,
                coordinates,
                rotation,
                collectionName
        );

        if(collectionName.equals("default")) {
            collections[0].storeThing(light);
        }
        else {
            Collection collection = storeCollection(collectionName);
            collection.storeThing(light);
        }

        lights[index] = light;

    }

    // =========================================================
    // STORE OBJECT / MESH
    // =========================================================

    public void storeObject(String objPath, String collectionName) {

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

        } catch (Exception e) {
            e.printStackTrace();
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
}