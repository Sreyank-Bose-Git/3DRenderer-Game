package renderer;

import datatypes.Vector2;
import datatypes.Vector3;
import main.RendererPanel;

public class Camera {

    public Vector3 coordinates;
    public Vector2 rotation;

    Vector3 tempCoords;

    double maxClimbHeight = 5.0;
    double climbStep = 1.0;

    double velocity = 50;
    double speed;

    double jumpStrength = 40;

    double gravity = 150;
    double gravityAcceleration = 50;
    boolean onGround = false;
    double maxFallPerFrame = 10;

    double playerWidth = 5;
    double playerHeight = 20;
    double playerLength = 5;

    RendererPanel rendererPanel;

    public Camera(RendererPanel rendererPanel) {

        this.rendererPanel = rendererPanel;
        this.coordinates = new Vector3(0, 50, -200);
        this.rotation = new Vector2(0, 0);
        this.tempCoords = this.coordinates;
    }

    public void update() {

        rotateHandler();

        movementHandler();
    }

    public void movementHandler() {

        double timeStep = rendererPanel.frameInterval / 1000000000.0;
        double rY = Math.toRadians(rotation.y);

        double sinY = Math.sin(rY);
        double cosY = Math.cos(rY);

        speed = velocity * timeStep;

        // Start with the current position
        tempCoords = new Vector3(
                coordinates.x,
                coordinates.y,
                coordinates.z
        );

        // 1. Calculate horizontal movement
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

        // 2. Handle jumping independently of horizontal movement
        if (rendererPanel.keyHandler.jumpPressed && onGround) {
            gravity = -jumpStrength;
            onGround = false;
        }

        // 3. Apply gravity
        gravity += gravityAcceleration * timeStep;

        double fallAmount = gravity * timeStep;
        fallAmount = Math.min(fallAmount, maxFallPerFrame);

        // 4. Resolve horizontal movement, allowing step climbing

        Vector3 horizontalCoords = new Vector3(
                coordinates.x + moveX,
                coordinates.y,
                coordinates.z + moveZ
        );

        boolean horizontalBlocked = false;

        for (int i = 0; rendererPanel.world.triangles[i] != null; i++) {

            var tri = rendererPanel.world.triangles[i];

            Vector3 a = rendererPanel.world.vertices[tri.index1].coordinates;
            Vector3 b = rendererPanel.world.vertices[tri.index2].coordinates;
            Vector3 c = rendererPanel.world.vertices[tri.index3].coordinates;

            boolean collisionX =
                    rendererPanel.collisionHandler.checkCameraTriangleCollision(
                            a, b, c, "X", horizontalCoords,
                            playerLength, playerWidth, playerHeight);

            boolean collisionZ =
                    rendererPanel.collisionHandler.checkCameraTriangleCollision(
                            a, b, c, "Z", horizontalCoords,
                            playerLength, playerWidth, playerHeight);

            if (collisionX || collisionZ) {

                boolean climbed = false;

                // Do not force a climb while the player is jumping upward
                if (gravity >= 0 && onGround) {

                    for (double step = climbStep;
                         step <= maxClimbHeight;
                         step += climbStep) {

                        Vector3 candidate = new Vector3(
                                horizontalCoords.x,
                                coordinates.y + step,
                                horizontalCoords.z
                        );

                        boolean blocked = false;

                        for (int j = 0;
                             rendererPanel.world.triangles[j] != null;
                             j++) {

                            var other = rendererPanel.world.triangles[j];

                            Vector3 oa =
                                    rendererPanel.world.vertices[other.index1].coordinates;
                            Vector3 ob =
                                    rendererPanel.world.vertices[other.index2].coordinates;
                            Vector3 oc =
                                    rendererPanel.world.vertices[other.index3].coordinates;

                            if (rendererPanel.collisionHandler.checkCameraTriangleCollision(
                                    oa, ob, oc, "X", candidate,
                                    playerLength, playerWidth, playerHeight)
                                    ||
                                    rendererPanel.collisionHandler.checkCameraTriangleCollision(
                                            oa, ob, oc, "Y", candidate,
                                            playerLength, playerWidth, playerHeight)
                                    ||
                                    rendererPanel.collisionHandler.checkCameraTriangleCollision(
                                            oa, ob, oc, "Z", candidate,
                                            playerLength, playerWidth, playerHeight)) {

                                blocked = true;
                                break;
                            }
                        }

                        if (!blocked) {
                            horizontalCoords.y = candidate.y;
                            climbed = true;
                            break;
                        }
                    }
                }

                if (!climbed) {
                    horizontalBlocked = true;
                    break;
                }
            }
        }

        // If blocked, preserve the old horizontal position
        if (!horizontalBlocked) {
            tempCoords.x = horizontalCoords.x;
            tempCoords.z = horizontalCoords.z;
            tempCoords.y = horizontalCoords.y;
        } else {
            tempCoords.x = coordinates.x;
            tempCoords.z = coordinates.z;
        }

        // 5. Resolve vertical movement separately
        tempCoords.y -= fallAmount;
        onGround = false;

        for (int i = 0; rendererPanel.world.triangles[i] != null; i++) {

            var tri = rendererPanel.world.triangles[i];

            Vector3 a = rendererPanel.world.vertices[tri.index1].coordinates;
            Vector3 b = rendererPanel.world.vertices[tri.index2].coordinates;
            Vector3 c = rendererPanel.world.vertices[tri.index3].coordinates;

            if (rendererPanel.collisionHandler.checkCameraTriangleCollision(
                    a, b, c, "Y", tempCoords,
                    playerLength, playerWidth, playerHeight)) {

                // Only cancel downward movement.
                // Upward movement must remain possible for jumping.
                if (gravity >= 0) {
                    tempCoords.y = coordinates.y;
                    gravity = 0;
                    onGround = true;
                } else {
                    // If the player's head hits a ceiling, stop upward movement.
                    tempCoords.y = coordinates.y;
                    gravity = 0;
                }
            }
        }

        // 6. Apply the final position
        coordinates = new Vector3(
                tempCoords.x,
                tempCoords.y,
                tempCoords.z
        );
    }

    public void rotateHandler() {
        if (rendererPanel.mouseHandler.isLocked) {
            // 1. Add mouse deltas directly to camera rotation
            rotation.y += rendererPanel.mouseHandler.deltaYaw; // Yaw (Horizontal look)
            rotation.x += rendererPanel.mouseHandler.deltaPitch; // Pitch (Vertical look)

            // 2. Clear deltas after applying (CRITICAL: prevents infinite spinning)
            rendererPanel.mouseHandler.deltaYaw = 0;
            rendererPanel.mouseHandler.deltaPitch = 0;

            // 3. Clamp Pitch to prevent camera flipping upside down
            if (rotation.x > 89.0) {
                rotation.x = 89.0;
            }
            if (rotation.x < -89.0) {
                rotation.x = -89.0;
            }

            // 4. Wrap Yaw angle between 0 and 360 degrees
            rotation.y %= 360.0;
            if (rotation.y < 0) {
                rotation.y += 360.0;
            }
        }
    }
}
