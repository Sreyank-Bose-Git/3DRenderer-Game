
package entities.ordinary;

import exec.extras.Collection;
import exec.renderer.Camera;
import main.RendererPanel;
import types.datatypes.Vector2;
import types.datatypes.Vector3;
import types.things.Thing;
import types.things.meshes.Mesh;

public class Player extends Thing {

    public RendererPanel rendererPanel;
    public int activeCameraIndex;

    public Vector3 tempCoords;

    public double maxClimbHeight = 5.0;
    public double climbStep = 1.0;

    public double velocity = 50;
    public double speed;

    public double jumpStrength = 40;

    public int gravityOn = 1;

    public double gravity = 150 * gravityOn;
    public double gravityAcceleration = 50 * gravityOn;
    public boolean onGround = false;
    public double maxFallPerFrame = 10 * gravityOn;

    // Reusable collision vertices.
    public final Vector3 collisionA = new Vector3(0, 0, 0);
    public final Vector3 collisionB = new Vector3(0, 0, 0);
    public final Vector3 collisionC = new Vector3(0, 0, 0);

    // Reusable temporary vectors.
    private final Vector3 horizontalCoords = new Vector3(0, 0, 0);
    private final Vector3 candidateCoords = new Vector3(0, 0, 0);

    public double playerHeight = 25;
    public double playerWidth = 5;
    public double playerLength = 5;

    public double distance = 20.0;

    public String objectsCollectionName;
    public Collection objectsCollection;

    public Thing head;

    public Player(
            RendererPanel rendererPanel,
            int activeCameraIndex,
            Vector3 coordinates,
            Vector2 rotation,
            String collectionName,
            String objectsCollectionName
    ) {
        this.rendererPanel = rendererPanel;
        this.activeCameraIndex = activeCameraIndex;
        this.collectionName = collectionName;

        this.localCoordinates = new Vector3(
                coordinates.x,
                coordinates.y,
                coordinates.z
        );

        this.localRotation.x = rotation.x;
        this.localRotation.y = rotation.y;

        this.objectsCollectionName = objectsCollectionName;

        this.tempCoords = new Vector3(
                coordinates.x,
                coordinates.y,
                coordinates.z
        );

        initializeObjectsCollection();
    }

    private void initializeObjectsCollection() {
        if (rendererPanel == null
                || rendererPanel.world == null
                || objectsCollection != null) {
            return;
        }

        objectsCollection =
                rendererPanel.world.getCollection(objectsCollectionName);

        if (objectsCollection != null
                && objectsCollection.things != null
                && objectsCollection.things.length > 0) {
            head = objectsCollection.things[0];
        }
    }

    private Camera getActiveCamera() {
        if (rendererPanel == null
                || rendererPanel.world == null
                || rendererPanel.world.things == null
                || activeCameraIndex < 0
                || activeCameraIndex >= rendererPanel.world.things.length) {
            return null;
        }

        Thing thing =
                rendererPanel.world.things[activeCameraIndex];

        return thing instanceof Camera camera ? camera : null;
    }

    @Override
    public void update() {
        initializeObjectsCollection();

        if (rendererPanel == null
                || rendererPanel.world == null
                || !"play".equals(rendererPanel.gameState)
                || objectsCollection == null) {
            return;
        }

        // Read world transforms through actual*.
        // Apply player changes through local*.
        playRotateHandler();
        playMovementHandler();

        // Synchronize the visual body only if it is a separate collection.
        // Never move or rotate the root "default" collection.
        Collection defaultCollection =
                rendererPanel.world.getCollection("default");

        if (objectsCollection != defaultCollection) {
            objectsCollection.localCoordinates.x = localCoordinates.x;
            objectsCollection.localCoordinates.y = localCoordinates.y;
            objectsCollection.localCoordinates.z = localCoordinates.z;

            objectsCollection.localRotation.y = localRotation.y;
        }

        // Apply head pitch locally; read its resulting world rotation below.
        if (head != null) {
            head.localRotation.x = localRotation.x;
            head.localRotation.z = 0.0;
        }

        Camera camera = getActiveCamera();

        if (camera == null || head == null) {
            return;
        }

        // Read world-space head position and orientation from actual*.
        double yaw = Math.toRadians(objectsCollection.actualRotation.y);
        double pitch = Math.toRadians(head.actualRotation.x);

        double horizontalDistance = distance * Math.cos(pitch);

        double cameraX = head.actualCoordinates.x
                - Math.sin(yaw) * horizontalDistance;

        double cameraY = head.actualCoordinates.y
                + Math.sin(pitch) * distance;

        double cameraZ = head.actualCoordinates.z
                - Math.cos(yaw) * horizontalDistance;

        // Convert the desired world position to the camera parent's
        // local position. This assumes the parent has no rotation or scale.
        Collection cameraParent = camera.collectionName == null
                ? null
                : rendererPanel.world.getCollection(camera.collectionName);

        if (cameraParent != null) {
            camera.localCoordinates.x =
                    cameraX - cameraParent.actualCoordinates.x;
            camera.localCoordinates.y =
                    cameraY - cameraParent.actualCoordinates.y;
            camera.localCoordinates.z =
                    cameraZ - cameraParent.actualCoordinates.z;
        } else {
            camera.localCoordinates.x = cameraX;
            camera.localCoordinates.y = cameraY;
            camera.localCoordinates.z = cameraZ;
        }

        // Apply camera rotation locally.
        camera.localRotation.x = -head.actualRotation.x;
        camera.localRotation.y = objectsCollection.actualRotation.y;
        camera.localRotation.z = 0.0;
    }

    public void playMovementHandler() {
        if (rendererPanel == null || rendererPanel.world == null) {
            return;
        }

        double timeStep =
                rendererPanel.frameInterval / 1_000_000_000.0;

        // Read the player's current world-space yaw.
        double rY = Math.toRadians(actualRotation.y);
        double sinY = Math.sin(rY);
        double cosY = Math.cos(rY);

        speed = velocity * timeStep;

        // Start from the current world-space position.
        tempCoords.x = actualCoordinates.x;
        tempCoords.y = actualCoordinates.y;
        tempCoords.z = actualCoordinates.z;

        // 1. Calculate horizontal movement.
        double moveX = 0.0;
        double moveZ = 0.0;

        if (rendererPanel.keyHandler.forward) {
            moveX += speed * sinY;
            moveZ += speed * cosY;
        }

        if (rendererPanel.keyHandler.backward) {
            moveX -= speed * sinY;
            moveZ -= speed * cosY;
        }

        if (rendererPanel.keyHandler.leftward) {
            moveX -= speed * cosY;
            moveZ += speed * sinY;
        }

        if (rendererPanel.keyHandler.rightward) {
            moveX += speed * cosY;
            moveZ -= speed * sinY;
        }

        // Prevent faster diagonal movement.
        double movementLength =
                Math.sqrt(moveX * moveX + moveZ * moveZ);

        if (movementLength > speed && movementLength > 0.0) {
            double factor = speed / movementLength;
            moveX *= factor;
            moveZ *= factor;
        }

        // 2. Jump.
        if (rendererPanel.keyHandler.jumpPressed && onGround) {
            gravity = -jumpStrength;
            onGround = false;
        }

        // 3. Gravity: positive gravity moves the player downward.
        gravity += gravityAcceleration * timeStep;

        double fallAmount = Math.clamp(
                gravity * timeStep,
                -maxFallPerFrame,
                maxFallPerFrame
        );

        // 4. Horizontal movement and step climbing.
        horizontalCoords.x = actualCoordinates.x + moveX;
        horizontalCoords.y = actualCoordinates.y;
        horizontalCoords.z = actualCoordinates.z + moveZ;

        boolean horizontalBlocked =
                collidesAt(horizontalCoords, "X")
                        || collidesAt(horizontalCoords, "Z");

        if (horizontalBlocked) {
            boolean climbed = false;

            if (gravity >= 0.0 && onGround && climbStep > 0.0) {
                for (double step = climbStep;
                     step <= maxClimbHeight;
                     step += climbStep) {

                    candidateCoords.x = horizontalCoords.x;
                    candidateCoords.y = actualCoordinates.y + step;
                    candidateCoords.z = horizontalCoords.z;

                    boolean blocked =
                            collidesAt(candidateCoords, "X")
                                    || collidesAt(candidateCoords, "Y")
                                    || collidesAt(candidateCoords, "Z");

                    if (!blocked) {
                        horizontalCoords.y = candidateCoords.y;
                        climbed = true;
                        break;
                    }
                }
            }

            if (!climbed) {
                // Reject the horizontal movement.
                horizontalCoords.x = actualCoordinates.x;
                horizontalCoords.y = actualCoordinates.y;
                horizontalCoords.z = actualCoordinates.z;
            }
        }

        tempCoords.x = horizontalCoords.x;
        tempCoords.y = horizontalCoords.y;
        tempCoords.z = horizontalCoords.z;

        // 5. Resolve vertical movement independently.
        tempCoords.y -= fallAmount;
        onGround = false;

        if (collidesAt(tempCoords, "Y")) {
            tempCoords.y = actualCoordinates.y;

            if (gravity >= 0.0) {
                // Downward collision: landed on a surface.
                onGround = true;
            }
            // Upward collision: hit a ceiling, not the ground.

            gravity = 0.0;
        }

        // 6. Convert the resolved world position back to local space.
        // Read the parent's world position through actualCoordinates.
        Collection parent = collectionName == null
                ? null
                : rendererPanel.world.getCollection(collectionName);

        double parentX =
                parent == null ? 0.0 : parent.actualCoordinates.x;
        double parentY =
                parent == null ? 0.0 : parent.actualCoordinates.y;
        double parentZ =
                parent == null ? 0.0 : parent.actualCoordinates.z;

        // Apply the result through localCoordinates.
        localCoordinates.x = tempCoords.x - parentX;
        localCoordinates.y = tempCoords.y - parentY;
        localCoordinates.z = tempCoords.z - parentZ;
    }

    /**
     * Tests the player's cuboid against generated mesh triangles.
     * Mesh vertices and transforms are read in world space.
     */
    private boolean collidesAt(Vector3 playerPosition, String axis) {
        Thing[] things = rendererPanel.world.things;

        if (things == null) {
            return false;
        }

        for (Thing thing : things) {
            if (!(thing instanceof Mesh mesh)) {
                continue;
            }

            if (!mesh.generated || mesh.disposed) {
                continue;
            }

            // Do not collide with the player's own visual body.
            if (isPlayerBodyThing(thing)) {
                continue;
            }

            for (Mesh.TriangleData triangle : mesh.triangles) {
                if (triangle == null) {
                    continue;
                }

                if (triangle.vertex1 < 0
                        || triangle.vertex2 < 0
                        || triangle.vertex3 < 0
                        || triangle.vertex1 >= mesh.vertices.size()
                        || triangle.vertex2 >= mesh.vertices.size()
                        || triangle.vertex3 >= mesh.vertices.size()) {
                    continue;
                }

                Vector3 localA =
                        mesh.vertices.get(triangle.vertex1).position;
                Vector3 localB =
                        mesh.vertices.get(triangle.vertex2).position;
                Vector3 localC =
                        mesh.vertices.get(triangle.vertex3).position;

                transformToWorld(localA, mesh, collisionA);
                transformToWorld(localB, mesh, collisionB);
                transformToWorld(localC, mesh, collisionC);

                if (rendererPanel.collisionHandler.checkTriangleCollision(
                        collisionA,
                        collisionB,
                        collisionC,
                        axis,
                        playerPosition,
                        playerLength,
                        playerWidth,
                        playerHeight
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isPlayerBodyThing(Thing thing) {
        if (objectsCollection == null || objectsCollection.things == null) {
            return false;
        }

        for (Thing bodyThing : objectsCollection.things) {
            if (bodyThing == thing) {
                return true;
            }
        }

        return false;
    }

    /**
     * Reads actual world transforms and writes the transformed vertex
     * into the reusable output vector.
     */
    private void transformToWorld(
            Vector3 vertex,
            Mesh mesh,
            Vector3 out
    ) {
        double x = vertex.x * mesh.actualScale.x;
        double y = vertex.y * mesh.actualScale.y;
        double z = vertex.z * mesh.actualScale.z;

        // Rotation X.
        double angle = Math.toRadians(mesh.actualRotation.x);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double nextY = y * cos - z * sin;
        double nextZ = y * sin + z * cos;

        y = nextY;
        z = nextZ;

        // Rotation Y.
        angle = Math.toRadians(mesh.actualRotation.y);
        cos = Math.cos(angle);
        sin = Math.sin(angle);

        double nextX = x * cos + z * sin;
        nextZ = -x * sin + z * cos;

        x = nextX;
        z = nextZ;

        // Rotation Z.
        angle = Math.toRadians(mesh.actualRotation.z);
        cos = Math.cos(angle);
        sin = Math.sin(angle);

        nextX = x * cos - y * sin;
        nextY = x * sin + y * cos;

        x = nextX;
        y = nextY;

        // Translation into world space.
        out.x = x + mesh.actualCoordinates.x;
        out.y = y + mesh.actualCoordinates.y;
        out.z = z + mesh.actualCoordinates.z;
    }

    public void playRotateHandler() {
        if (rendererPanel == null
                || rendererPanel.mouseHandler == null
                || !rendererPanel.mouseHandler.isLocked) {
            return;
        }

        // Apply mouse rotation locally.
        localRotation.y += rendererPanel.mouseHandler.deltaYaw;
        localRotation.x += rendererPanel.mouseHandler.deltaPitch;

        // Consume mouse input.
        rendererPanel.mouseHandler.deltaYaw = 0.0;
        rendererPanel.mouseHandler.deltaPitch = 0.0;

        // Prevent vertical camera flipping.
        localRotation.x = Math.clamp(
                localRotation.x,
                -80.0,
                80.0
        );

        // Normalize yaw to [0, 360).
        localRotation.y %= 360.0;

        if (localRotation.y < 0.0) {
            localRotation.y += 360.0;
        }
    }
}
