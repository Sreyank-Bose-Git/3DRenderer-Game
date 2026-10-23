
package exec.renderer;

import main.RendererPanel;
import types.datatypes.Vector2;
import types.datatypes.Vector3;
import types.things.Mesh;
import types.things.Mesh.TriangleData;
import types.things.Thing;

public class Camera extends Thing {

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

    public double playerWidth = 5;
    public double playerHeight = 20;
    public double playerLength = 5;

    // Reusable collision vertices to reduce allocations.
    public final Vector3 collisionA = new Vector3(0, 0, 0);
    public final Vector3 collisionB = new Vector3(0, 0, 0);
    public final Vector3 collisionC = new Vector3(0, 0, 0);

    private final RendererPanel rendererPanel;

    public Camera(
            RendererPanel rendererPanel,
            Vector3 coordinates,
            Vector2 rotation,
            String collectionName
    ) {
        this.collectionName = collectionName;

        this.rendererPanel = rendererPanel;
        this.localCoordinates = coordinates;

        this.localRotation.x = rotation.x;
        this.localRotation.y = rotation.y;

        this.tempCoords = coordinates;
    }

    public void update() {
        if (rendererPanel.gameState.equals("play")) {
            playRotateHandler();
            playMovementHandler();
        }
    }

    public void playMovementHandler() {

        double timeStep =
                rendererPanel.frameInterval / 1_000_000_000.0;

        double rY = Math.toRadians(localRotation.y);
        double sinY = Math.sin(rY);
        double cosY = Math.cos(rY);

        speed = velocity * timeStep;

        tempCoords = new Vector3(
                localCoordinates.x,
                localCoordinates.y,
                localCoordinates.z
        );

        // 1. Calculate horizontal movement.
        double moveX = 0;
        double moveZ = 0;

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
        double movementLength = Math.sqrt(
                moveX * moveX + moveZ * moveZ
        );

        if (movementLength > speed && movementLength > 0) {
            double factor = speed / movementLength;
            moveX *= factor;
            moveZ *= factor;
        }

        // 2. Jump.
        if (rendererPanel.keyHandler.jumpPressed && onGround) {
            gravity = -jumpStrength;
            onGround = false;
        }

        // 3. Gravity.
        gravity += gravityAcceleration * timeStep;

        double fallAmount = gravity * timeStep;

        fallAmount = Math.clamp(fallAmount,
                -maxFallPerFrame, maxFallPerFrame);

        // 4. Horizontal movement and step climbing.
        Vector3 horizontalCoords = new Vector3(
                actualCoordinates.x + moveX,
                actualCoordinates.y,
                actualCoordinates.z + moveZ
        );

        boolean horizontalBlocked =
                collidesAt(horizontalCoords, "X")
                        || collidesAt(horizontalCoords, "Z");

        if (horizontalBlocked) {

            boolean climbed = false;

            // Only climb when grounded and moving downwards or level.
            if (gravity >= 0 && onGround) {

                for (double step = climbStep;
                     step <= maxClimbHeight;
                     step += climbStep) {

                    Vector3 candidate = new Vector3(
                            horizontalCoords.x,
                            actualCoordinates.y + step,
                            horizontalCoords.z
                    );

                    boolean blocked =
                            collidesAt(candidate, "X")
                                    || collidesAt(candidate, "Y")
                                    || collidesAt(candidate, "Z");

                    if (!blocked) {
                        horizontalCoords.y = candidate.y;
                        climbed = true;
                        break;
                    }
                }
            }

            if (!climbed) {
                horizontalCoords.x = localCoordinates.x;
                horizontalCoords.z = localCoordinates.z;
                horizontalCoords.y = localCoordinates.y;
            }
        }

        tempCoords.x = horizontalCoords.x;
        tempCoords.y = horizontalCoords.y;
        tempCoords.z = horizontalCoords.z;

        // 5. Resolve vertical movement independently.
        tempCoords.y -= fallAmount;
        onGround = false;

        if (collidesAt(tempCoords, "Y")) {

            // Downward collision: land on the ground.
            if (gravity >= 0) {
                tempCoords.y = localCoordinates.y;
                gravity = 0;
                onGround = true;
            } else {
                // Upward collision: hit a ceiling.
                tempCoords.y = localCoordinates.y;
                gravity = 0;
            }
        }

        // 6. Apply the final position.
        localCoordinates.x = tempCoords.x;
        localCoordinates.y = tempCoords.y;
        localCoordinates.z = tempCoords.z;
    }

    /**
     * Tests the player's cuboid against every triangle belonging
     * to every generated mesh in the world.
     *
     * Triangle vertices are transformed into world localCoordinates
     * using the same transform order as Renderer:
     * scale, rotate X, rotate Y, rotate Z, translate.
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

            for (TriangleData triangle : mesh.triangles) {

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

                if (rendererPanel.collisionHandler.checkCameraTriangleCollision(
                        collisionA,
                        collisionB,
                        collisionC,
                        axis,
                        playerPosition,
                        playerLength,
                        playerWidth,
                        playerHeight)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Applies the exact mesh transform order used by Renderer.
     * Writes into an existing Vector3 to avoid allocating one
     * for each triangle vertex.
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

        // Translation.
        out.x = x + mesh.actualCoordinates.x;
        out.y = y + mesh.actualCoordinates.y;
        out.z = z + mesh.actualCoordinates.z;
    }

    public void playRotateHandler() {

        if (rendererPanel.mouseHandler.isLocked) {

            localRotation.y += rendererPanel.mouseHandler.deltaYaw;
            localRotation.x += rendererPanel.mouseHandler.deltaPitch;

            rendererPanel.mouseHandler.deltaYaw = 0;
            rendererPanel.mouseHandler.deltaPitch = 0;

            // Clamp pitch to prevent flipping.
            if (localRotation.x > 89.0) {
                localRotation.x = 89.0;
            }

            if (localRotation.x < -89.0) {
                localRotation.x = -89.0;
            }

            // Wrap yaw.
            localRotation.y %= 360.0;

            if (localRotation.y < 0) {
                localRotation.y += 360.0;
            }
        }
    }
}
