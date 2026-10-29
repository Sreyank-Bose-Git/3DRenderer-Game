

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

    // Maximum body turning speed in degrees per second.
    public double bodyTurnSpeed = 100.0;

    public String objectsCollectionName;
    public Collection objectsCollection;

    public Thing head;

    // Camera aim is independent of the body's facing direction.
    private double cameraYaw;
    private double cameraPitch;
    private boolean cameraAimInitialized;

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

        this.localRotation.x = 0.0;
        this.localRotation.y = rotation.y;

        this.cameraYaw = rotation.y;
        this.cameraPitch = rotation.x;
        this.cameraAimInitialized = true;

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

        if (!cameraAimInitialized) {
            cameraYaw = actualRotation.y;
            cameraPitch = 0.0;
            cameraAimInitialized = true;
        }

        playRotateHandler();
        playMovementHandler();

        Collection defaultCollection =
                rendererPanel.world.getCollection("default");

        // The visual body follows the player's position and body yaw only.
        if (objectsCollection != defaultCollection) {
            objectsCollection.localCoordinates.x = localCoordinates.x;
            objectsCollection.localCoordinates.y = localCoordinates.y;
            objectsCollection.localCoordinates.z = localCoordinates.z;
            objectsCollection.localRotation.y = localRotation.y;
            objectsCollection.localRotation.x = 0.0;
            objectsCollection.localRotation.z = 0.0;
        }

        Camera camera = getActiveCamera();
        if (camera == null || head == null) {
            return;
        }

        // Head aim is relative to the body's yaw, so looking around does
        // not rotate the body. Pitch is applied to the head only.
        head.localRotation.x = cameraPitch;
        head.localRotation.y = normalizeDegrees(
                cameraYaw - objectsCollection.actualRotation.y);
        head.localRotation.z = 0.0;

        double yaw = Math.toRadians(cameraYaw);
        double pitch = Math.toRadians(cameraPitch);
        double horizontalDistance = distance * Math.cos(pitch);

        double cameraX = head.actualCoordinates.x
                - Math.sin(yaw) * horizontalDistance;
        double cameraY = head.actualCoordinates.y
                + Math.sin(pitch) * distance;
        double cameraZ = head.actualCoordinates.z
                - Math.cos(yaw) * horizontalDistance;

        Collection cameraParent = camera.collectionName == null
                ? null
                : rendererPanel.world.getCollection(camera.collectionName);

        // Convert world position to parent-local coordinates with inverse
        // parent rotation as well as inverse translation.
        worldToParentLocal(cameraX, cameraY, cameraZ, cameraParent,
                camera.localCoordinates);

        // Camera orientation tracks the independent view direction.
        camera.localRotation.x = -cameraPitch
                - (cameraParent == null ? 0.0 : cameraParent.actualRotation.x);
        camera.localRotation.y = cameraYaw
                - (cameraParent == null ? 0.0 : cameraParent.actualRotation.y);
        camera.localRotation.z = 0.0;
    }

    private static double normalizeDegrees(double angle) {
        angle %= 360.0;
        return angle < 0.0 ? angle + 360.0 : angle;
    }

    /** Rotates from the current angle toward the target by at most maxDelta. */
    private static double rotateTowardsDegrees(
            double current, double target, double maxDelta) {
        double difference = (target - current + 540.0) % 360.0 - 180.0;

        if (Math.abs(difference) <= maxDelta) {
            return normalizeDegrees(target);
        }

        return normalizeDegrees(current + Math.copySign(maxDelta, difference));
    }

    /**
     * Converts a world-space point to coordinates relative to a parent,
     * undoing the parent's translation and X/Y/Z rotations.
     */
    private void worldToParentLocal(
            double worldX, double worldY, double worldZ,
            Collection parent, Vector3 out) {

        if (parent == null) {
            out.x = worldX;
            out.y = worldY;
            out.z = worldZ;
            return;
        }

        double x = worldX - parent.actualCoordinates.x;
        double y = worldY - parent.actualCoordinates.y;
        double z = worldZ - parent.actualCoordinates.z;

        // Inverse rotation Z.
        double angle = Math.toRadians(-parent.actualRotation.z);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double nextX = x * cos - y * sin;
        double nextY = x * sin + y * cos;
        x = nextX;
        y = nextY;

        // Inverse rotation Y.
        angle = Math.toRadians(-parent.actualRotation.y);
        cos = Math.cos(angle);
        sin = Math.sin(angle);
        nextX = x * cos + z * sin;
        double nextZ = -x * sin + z * cos;
        x = nextX;
        z = nextZ;

        // Inverse rotation X.
        angle = Math.toRadians(-parent.actualRotation.x);
        cos = Math.cos(angle);
        sin = Math.sin(angle);
        nextY = y * cos - z * sin;
        nextZ = y * sin + z * cos;

        out.x = nextX;
        out.y = nextY;
        out.z = nextZ;
    }

    public void playMovementHandler() {
        if (rendererPanel == null || rendererPanel.world == null) {
            return;
        }

        double timeStep =
                rendererPanel.frameInterval / 1_000_000_000.0;

        // Read the player's current world-space yaw.
        double rY = Math.toRadians(cameraYaw);
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

        // 6. Turn the body only when movement has a direction.
        // Camera yaw controls movement direction; body yaw follows movement.
        if (Math.abs(moveX) > 1e-9 || Math.abs(moveZ) > 1e-9) {
            double worldBodyYaw =
                    Math.toDegrees(Math.atan2(moveX, moveZ));

            Collection playerParent = collectionName == null
                    ? null
                    : rendererPanel.world.getCollection(collectionName);

            double parentYaw = playerParent == null
                    ? 0.0
                    : playerParent.actualRotation.y;

            double targetLocalYaw = normalizeDegrees(worldBodyYaw - parentYaw);
            double maxTurn = Math.max(0.0, bodyTurnSpeed) * timeStep;
            localRotation.y = rotateTowardsDegrees(
                    normalizeDegrees(localRotation.y),
                    targetLocalYaw,
                    maxTurn);
        }

        // Convert the resolved world position to parent-local coordinates,
        // accounting for the parent's rotation as well as translation.
        Collection parent = collectionName == null
                ? null
                : rendererPanel.world.getCollection(collectionName);

        worldToParentLocal(
                tempCoords.x, tempCoords.y, tempCoords.z,
                parent, localCoordinates);
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

        // Mouse controls the view, not the body.
        cameraYaw += rendererPanel.mouseHandler.deltaYaw;
        cameraPitch += rendererPanel.mouseHandler.deltaPitch;

        rendererPanel.mouseHandler.deltaYaw = 0.0;
        rendererPanel.mouseHandler.deltaPitch = 0.0;

        cameraPitch = Math.clamp(cameraPitch, -80.0, 80.0);
        cameraYaw = normalizeDegrees(cameraYaw);
    }
}
