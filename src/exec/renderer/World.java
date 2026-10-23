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

    public Thing[] things;

    public Collection[] collections;

    public int activeCameraIndex;
    public RendererPanel rendererPanel;

    public World(
            int thingsSize,
            int collectionsSize,
            RendererPanel rendererPanel) {

        things = new Thing[thingsSize];
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
                new Vector3(0, 500, -200),
                new Vector2(0, 0)
        );

        activeCameraIndex = 0;

        storeLight(
                "point",
                5.0,
                new Vector3(0, 50, -200),
                new Vector3(0, 0, 180),
                "default"
        );

        storeObject("/types/objects/cube.obj", "default");

        storeObject("/types/objects/cube.obj", "default");

        Thing mesh = things[2];

        if (mesh != null) {
            mesh.localScale.x *= 1000;
            mesh.localScale.y *= 10;
            mesh.localScale.z *= 1000;
        }

        mesh = things[3];

        if (mesh != null) {
            mesh.localScale.x *= 10;
            mesh.localScale.y *= 10;
            mesh.localScale.z *= 10;

            mesh.localCoordinates.y = 10;
            mesh.localCoordinates.z = -50;
        }
    }

    public void update() {

        collectionInclusion();


    }

    void collectionInclusion() {
        // things
        for(int i = 0; i < things.length && things[i] != null; i++) {

            things[i].actualCoordinates.x = things[i].localCoordinates.x + collections[getCollectionIndex(things[i].collectionName)].coordinates.x;
            things[i].actualCoordinates.y = things[i].localCoordinates.y + collections[getCollectionIndex(things[i].collectionName)].coordinates.y;
            things[i].actualCoordinates.z = things[i].localCoordinates.z + collections[getCollectionIndex(things[i].collectionName)].coordinates.z;

            things[i].actualRotation.x = things[i].localRotation.x + collections[getCollectionIndex(things[i].collectionName)].rotation.x;
            things[i].actualRotation.y = things[i].localRotation.y + collections[getCollectionIndex(things[i].collectionName)].rotation.y;
            things[i].actualRotation.z = things[i].localRotation.z + collections[getCollectionIndex(things[i].collectionName)].rotation.z;

            things[i].actualScale.x = things[i].localScale.x + collections[getCollectionIndex(things[i].collectionName)].scale.x;
            things[i].actualScale.y = things[i].localScale.y + collections[getCollectionIndex(things[i].collectionName)].scale.y;
            things[i].actualScale.z = things[i].localScale.z + collections[getCollectionIndex(things[i].collectionName)].scale.z;
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

    private int findNextThingIndex() {
        for (int i = 0; i < things.length; i++) {
            if (things[i] == null) {
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

    private void growThings() {
        things = Arrays.copyOf(
                things,
                Math.max(1, things.length * 2)
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

        int index = findNextThingIndex();

        if (index == -1) {
            growThings();
            index = findNextThingIndex();
        }

        Camera camera = new Camera(
                rendererPanel,
                coordinates,
                rotation,
                "default"
        );

        things[index] = camera;

    }

    // =========================================================
    // STORE LIGHT
    // =========================================================

    public void storeLight(
            String lightType,
            double thingstrength,
            Vector3 coordinates,
            Vector3 rotation,
            String collectionName) {

        int index = findNextThingIndex();

        if (index == -1) {
            growThings();
            index = findNextThingIndex();
        }

        Light light = new Light(
                lightType,
                thingstrength,
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

        things[index] = light;

    }

    // =========================================================
    // STORE OBJECT / MESH
    // =========================================================

    public void storeObject(String objPath, String collectionName) {

        int index = findNextThingIndex();

        if (index == -1) {
            growThings();
            index = findNextThingIndex();
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

            things[index] = mesh;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public int getCollectionIndex(String collectionName) {
        if (collectionName == null) {
            return -1;
        }

        for (int i = 0; i < collections.length; i++) {
            Collection collection = collections[i];

            if (collection != null
                    && collectionName.equals(collection.name)) {
                return i;
            }
        }

        return -1;
    }
}