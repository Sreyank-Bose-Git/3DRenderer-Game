
package exec.renderer;

import main.RendererPanel;
import types.datatypes.Vector2;
import types.datatypes.Vector3;
import types.things.Mesh;
import types.things.Mesh.TriangleData;

public class Camera {

    public Vector3 coordinates;
    public Vector2 rotation;

    private Vector3 tempCoords;

    private double maxClimbHeight = 5.0;
    private double climbStep = 1.0;

    private double velocity = 50;
    private double speed;

    private double jumpStrength = 40;

    private int gravityOn = 1;

    private double gravity = 150 * gravityOn;
    private double gravityAcceleration = 50 * gravityOn;
    private boolean onGround = false;
    private double maxFallPerFrame = 10 * gravityOn;

    private double playerWidth = 5;
    private double playerHeight = 20;
    private double playerLength = 5;

    private final RendererPanel rendererPanel;

    // Reusable collision vertices to reduce allocations.
    private final Vector3 collisionA = new Vector3(0, 0, 0);
    private final Vector3 collisionB = new Vector3(0, 0, 0);
    private final Vector3 collisionC = new Vector3(0, 0, 0);

    public Camera(
            RendererPanel rendererPanel,
            Vector3 coordinates,
            Vector2 rotation
    ) {
        this.rendererPanel = rendererPanel;
        this.coordinates = coordinates;
        this.rotation = rotation;
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

        double rY = Math.toRadians(rotation.y);
        double sinY = Math.sin(rY);
        double cosY = Math.cos(rY);

        speed = velocity * timeStep;

        tempCoords = new Vector3(
                coordinates.x,
                coordinates.y,
                coordinates.z
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
                coordinates.x + moveX,
                coordinates.y,
                coordinates.z + moveZ
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
                            coordinates.y + step,
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
                horizontalCoords.x = coordinates.x;
                horizontalCoords.z = coordinates.z;
                horizontalCoords.y = coordinates.y;
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
                tempCoords.y = coordinates.y;
                gravity = 0;
                onGround = true;
            } else {
                // Upward collision: hit a ceiling.
                tempCoords.y = coordinates.y;
                gravity = 0;
            }
        }

        // 6. Apply the final position.
        coordinates.x = tempCoords.x;
        coordinates.y = tempCoords.y;
        coordinates.z = tempCoords.z;
    }

    /**
     * Tests the player's cuboid against every triangle belonging
     * to every generated mesh in the world.
     *
     * Triangle vertices are transformed into world coordinates
     * using the same transform order as Renderer:
     * scale, rotate X, rotate Y, rotate Z, translate.
     */
    private boolean collidesAt(Vector3 playerPosition, String axis) {

        Mesh[] meshes = rendererPanel.world.meshes;

        if (meshes == null) {
            return false;
        }

        for (Mesh mesh : meshes) {

            if (mesh == null) {
                continue;
            }

            if (!mesh.isGenerated() || mesh.isDisposed()) {
                continue;
            }

            int triangleCount = mesh.getTriangleCount();

            for (int i = 0; i < triangleCount; i++) {

                TriangleData triangle = mesh.getTriangleData(i);

                if (triangle == null) {
                    continue;
                }

                // Skip malformed triangle indices.
                if (triangle.vertex1 < 0
                        || triangle.vertex2 < 0
                        || triangle.vertex3 < 0
                        || triangle.vertex1 >= mesh.getVertexCount()
                        || triangle.vertex2 >= mesh.getVertexCount()
                        || triangle.vertex3 >= mesh.getVertexCount()) {
                    continue;
                }

                Vector3 localA =
                        mesh.getVertexData(triangle.vertex1).position;

                Vector3 localB =
                        mesh.getVertexData(triangle.vertex2).position;

                Vector3 localC =
                        mesh.getVertexData(triangle.vertex3).position;

                transformToWorld(localA, mesh, collisionA);
                transformToWorld(localB, mesh, collisionB);
                transformToWorld(localC, mesh, collisionC);

                if (rendererPanel.collisionHandler
                        .checkCameraTriangleCollision(
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

        double x = vertex.x * mesh.scale.x;
        double y = vertex.y * mesh.scale.y;
        double z = vertex.z * mesh.scale.z;

        // Rotation X.
        double angle = Math.toRadians(mesh.rotation.x);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double nextY = y * cos - z * sin;
        double nextZ = y * sin + z * cos;

        y = nextY;
        z = nextZ;

        // Rotation Y.
        angle = Math.toRadians(mesh.rotation.y);
        cos = Math.cos(angle);
        sin = Math.sin(angle);

        double nextX = x * cos + z * sin;
        nextZ = -x * sin + z * cos;

        x = nextX;
        z = nextZ;

        // Rotation Z.
        angle = Math.toRadians(mesh.rotation.z);
        cos = Math.cos(angle);
        sin = Math.sin(angle);

        nextX = x * cos - y * sin;
        nextY = x * sin + y * cos;

        x = nextX;
        y = nextY;

        // Translation.
        out.x = x + mesh.coordinates.x;
        out.y = y + mesh.coordinates.y;
        out.z = z + mesh.coordinates.z;
    }

    public void playRotateHandler() {

        if (rendererPanel.mouseHandler.isLocked) {

            rotation.y += rendererPanel.mouseHandler.deltaYaw;
            rotation.x += rendererPanel.mouseHandler.deltaPitch;

            rendererPanel.mouseHandler.deltaYaw = 0;
            rendererPanel.mouseHandler.deltaPitch = 0;

            // Clamp pitch to prevent flipping.
            if (rotation.x > 89.0) {
                rotation.x = 89.0;
            }

            if (rotation.x < -89.0) {
                rotation.x = -89.0;
            }

            // Wrap yaw.
            rotation.y %= 360.0;

            if (rotation.y < 0) {
                rotation.y += 360.0;
            }
        }
    }
}
