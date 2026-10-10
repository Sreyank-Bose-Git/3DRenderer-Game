package renderer;

import datatypes.Vector2;
import datatypes.Vector3;
import main.RendererPanel;

public class Camera {

    public Vector3 coordinates;
    public Vector2 rotation;

    Vector3 tempCoords;

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

        tempCoords = new Vector3(coordinates.x, coordinates.y, coordinates.z);

        // 1. Movement inputs (Horizontal)
        if(rendererPanel.keyHandler.forward) {
            tempCoords.x += speed * sinY;
            tempCoords.z += speed * cosY;
        }
        if(rendererPanel.keyHandler.backward) {
            tempCoords.x -= speed * sinY;
            tempCoords.z -= speed * cosY;
        }
        if(rendererPanel.keyHandler.leftward) {
            tempCoords.x -= speed * cosY;
            tempCoords.z += speed * sinY;
        }
        if(rendererPanel.keyHandler.rightward) {
            tempCoords.x += speed * cosY;
            tempCoords.z -= speed * sinY;
        }

        // 2. Handle Jump Input *before* applying gravity/collisions
        // (If your engine treats Up as positive Y, jump should decrease gravity or add directly to tempCoords.y)
        if(rendererPanel.keyHandler.jumpPressed && onGround) {

            // Option B: Direct position shift override for testing purposes:
            gravity -= jumpStrength;

            onGround = false;
            System.out.println("JUMP TRIGGERED!");
        }

        // 3. Gravity calculation & Safety clamp
        gravity += gravityAcceleration * timeStep;
        double fallAmount = gravity * timeStep;
        if (fallAmount > maxFallPerFrame) {
            fallAmount = maxFallPerFrame;
        }

        tempCoords.y -= fallAmount;

        // Assume airborne until a collision proves otherwise this frame
        onGround = false;

        // 4. Evaluate collisions across world triangles
        for(int i = 0; rendererPanel.world.triangles[i] != null; i++) {
            var tri = rendererPanel.world.triangles[i];
            Vector3 a = rendererPanel.world.vertices[tri.index1].coordinates;
            Vector3 b = rendererPanel.world.vertices[tri.index2].coordinates;
            Vector3 c = rendererPanel.world.vertices[tri.index3].coordinates;

            if(rendererPanel.collisionHandler.checkCameraTriangleCollision(a, b, c, "X", tempCoords,
                    playerLength, playerWidth, playerHeight)) {
                tempCoords.x = coordinates.x;
            }

            if(rendererPanel.collisionHandler.checkCameraTriangleCollision(a, b, c, "Y", tempCoords,
                    playerLength, playerWidth, playerHeight)) {
                // If we were falling down onto a floor
                if (fallAmount > 0) {
                    gravity = 0;
                    onGround = true;
                }
                tempCoords.y = coordinates.y;
            }

            if(rendererPanel.collisionHandler.checkCameraTriangleCollision(a, b, c, "Z", tempCoords,
                    playerLength, playerWidth, playerHeight)) {
                tempCoords.z = coordinates.z;
            }
        }

        coordinates = new Vector3(tempCoords.x, tempCoords.y, tempCoords.z);
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
