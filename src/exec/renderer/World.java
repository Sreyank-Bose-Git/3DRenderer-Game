
package exec.renderer;

import entities.ordinary.Player;
import exec.extras.Collection;
import main.RendererPanel;
import types.datatypes.Vector2;
import types.datatypes.Vector3;
import types.things.Light;
import types.things.Thing;
import types.things.meshes.Mesh;
import types.things.meshes.Object;

import java.util.Arrays;

public class World {

    public RendererPanel rendererPanel;
    public Thing[] things;
    public int activePlayerIndex = -1;

    public World(int thingsSize, int collectionsSize,
                 RendererPanel rendererPanel) {
        this.rendererPanel = rendererPanel;
        things = new Thing[Math.max(1, thingsSize)];

        storeCollection("default");
        storeCollection("player");

        createBasicWorld();
    }

    private void createBasicWorld() {
        storeCamera(new Vector3(0, 0, 0), new Vector2(0, 0));

        storeObject("/types/objects/cube.obj", "player");
        storeObject("/types/objects/cube.obj", "player");

        int defaultCubeIndex = findLastMeshInCollection("player");

        if(defaultCubeIndex > 0) {
            Thing mesh = things[defaultCubeIndex];
            mesh.localScale.y = 3;
            mesh.localScale.x = 5;
            mesh.localCoordinates.y = -7;
        }

        storePlayer(
                2, new Vector3(0, 500, -200),
                new Vector2(0, 0), "default", "player"
        );

        activePlayerIndex = findPlayerIndex();

        storeLight(
                "point", 5.0,
                new Vector3(0, 50, -200),
                new Vector3(0, 0, 180),
                "default"
        );

        storeObject("/types/objects/cube.obj", "default");

        storeCamera(
                new Vector3(0, 500, -200),
                new Vector2(0, 0)
        );

        // Avoid hard-coded array indices when modifying entities.
        defaultCubeIndex = findLastMeshInCollection("default");

        if (defaultCubeIndex >= 0) {
            Thing mesh = things[defaultCubeIndex];
            mesh.localScale.x *= 1000;
            mesh.localScale.y *= 10;
            mesh.localScale.z *= 1000;
        }
    }

    public void update() {
        collectionInclusion();

        for (int i = 0; i < things.length; i++) {
            if (things[i] != null) {
                things[i].update();
            }
        }
    }

    private void collectionInclusion() {
        // Collections must be processed before their children.
        // Repeat passes to support nested collections.
        for (int pass = 0; pass < things.length; pass++) {
            boolean changed = false;

            for (Thing thing : things) {
                if (thing == null) continue;

                Collection parent = getCollection(thing.collectionName);

                // A root collection has no parent.
                if (thing instanceof Collection collection
                        && collection.collectionName == null) {
                    setActualTransform(collection, null);
                    changed = true;
                    continue;
                }

                if (parent == null || parent == thing) {
                    continue;
                }

                setActualTransform(thing, parent);
                changed = true;
            }

            if (!changed) break;
        }
    }

    private void setActualTransform(Thing thing, Collection parent) {
        if (parent == null) {
            thing.actualCoordinates.x = thing.localCoordinates.x;
            thing.actualCoordinates.y = thing.localCoordinates.y;
            thing.actualCoordinates.z = thing.localCoordinates.z;

            thing.actualRotation.x = thing.localRotation.x;
            thing.actualRotation.y = thing.localRotation.y;
            thing.actualRotation.z = thing.localRotation.z;

            thing.actualScale.x = thing.localScale.x;
            thing.actualScale.y = thing.localScale.y;
            thing.actualScale.z = thing.localScale.z;
            return;
        }

        thing.actualCoordinates.x =
                thing.localCoordinates.x + parent.actualCoordinates.x;
        thing.actualCoordinates.y =
                thing.localCoordinates.y + parent.actualCoordinates.y;
        thing.actualCoordinates.z =
                thing.localCoordinates.z + parent.actualCoordinates.z;

        thing.actualRotation.x =
                thing.localRotation.x + parent.actualRotation.x;
        thing.actualRotation.y =
                thing.localRotation.y + parent.actualRotation.y;
        thing.actualRotation.z =
                thing.localRotation.z + parent.actualRotation.z;

        thing.actualScale.x =
                thing.localScale.x * parent.actualScale.x;
        thing.actualScale.y =
                thing.localScale.y * parent.actualScale.y;
        thing.actualScale.z =
                thing.localScale.z * parent.actualScale.z;
    }

    private int findNextThingIndex() {
        for (int i = 0; i < things.length; i++) {
            if (things[i] == null) return i;
        }
        return -1;
    }

    private void growThings() {
        things = Arrays.copyOf(things, Math.max(1, things.length * 2));
    }

    private int addThing(Thing thing) {
        int index = findNextThingIndex();

        if (index < 0) {
            growThings();
            index = findNextThingIndex();
        }

        things[index] = thing;
        return index;
    }

    public Collection storeCollection(String name) {
        Collection existing = getCollection(name);
        if (existing != null) return existing;

        Collection collection = new Collection(name);

        // The default collection is the root of the hierarchy.
        collection.collectionName =
                name.equals("default") ? null : "default";

        addThing(collection);

        if (collection.collectionName != null) {
            Collection parent = getCollection(collection.collectionName);
            if (parent != null) parent.storeThing(collection);
        }

        return collection;
    }

    public Collection storeCollection(
            String name, Thing[] children,
            Vector3 coordinates, Vector3 scale, Vector3 rotation) {

        Collection collection = storeCollection(name);
        collection.things = children;
        collection.localCoordinates = coordinates;
        collection.localScale = scale;
        collection.localRotation = rotation;

        return collection;
    }

    private void addToCollection(Thing thing, String collectionName) {
        Collection collection = getCollection(collectionName);

        if (collection == null) {
            collection = storeCollection(collectionName);
        }

        collection.storeThing(thing);
    }

    public void storeCamera(Vector3 coordinates, Vector2 rotation) {
        Camera camera = new Camera(
                rendererPanel, coordinates, rotation, "default"
        );

        addToCollection(camera, "default");
        addThing(camera);
    }

    public void storeLight(
            String lightType, double strength,
            Vector3 coordinates, Vector3 rotation,
            String collectionName) {

        Light light = new Light(
                lightType, strength, coordinates, rotation, collectionName
        );

        addToCollection(light, collectionName);
        addThing(light);
    }

    public void storeObject(String objPath, String collectionName) {
        try {
            Mesh mesh = new Object(this, objPath, collectionName);
            mesh.generate();

            addToCollection(mesh, collectionName);
            addThing(mesh);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void storePlayer(
            int activeCameraIndex, Vector3 coordinates,
            Vector2 rotation, String collectionName,
            String objectsCollectionName) {

        Player player = new Player(
                rendererPanel, activeCameraIndex,
                coordinates, rotation, collectionName,
                objectsCollectionName
        );

        addToCollection(player, collectionName);
        activePlayerIndex = addThing(player);
    }

    public Collection getCollection(String name) {
        if (name == null) return null;

        for (Thing thing : things) {
            if (thing instanceof Collection collection
                    && name.equals(collection.name)) {
                return collection;
            }
        }

        return null;
    }

    public int getCollectionIndex(String name) {
        if (name == null) return -1;

        for (int i = 0; i < things.length; i++) {
            if (things[i] instanceof Collection collection
                    && name.equals(collection.name)) {
                return i;
            }
        }

        return -1;
    }

    public Player getActivePlayer() {
        if (activePlayerIndex < 0
                || activePlayerIndex >= things.length
                || !(things[activePlayerIndex] instanceof Player player)) {
            return null;
        }

        return player;
    }

    private int findPlayerIndex() {
        for (int i = 0; i < things.length; i++) {
            if (things[i] instanceof Player) return i;
        }
        return -1;
    }

    private int findLastMeshInCollection(String collectionName) {
        for (int i = things.length - 1; i >= 0; i--) {
            if (things[i] instanceof Mesh mesh
                    && collectionName.equals(mesh.collectionName)) {
                return i;
            }
        }
        return -1;
    }

    public void prevCam() {
        Player player = getActivePlayer();
        if (player == null) return;

        for (int i = player.activeCameraIndex - 1; i >= 0; i--) {
            if (things[i] instanceof Camera) {
                player.activeCameraIndex = i;
                return;
            }
        }
    }

    public void nextCam() {
        Player player = getActivePlayer();
        if (player == null) return;

        for (int i = player.activeCameraIndex + 1;
             i < things.length; i++) {
            if (things[i] instanceof Camera) {
                player.activeCameraIndex = i;
                return;
            }
        }
    }
}
