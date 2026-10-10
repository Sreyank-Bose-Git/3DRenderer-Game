
package handler;

import datatypes.Vector3;

public class CollisionHandler {

    /**
     * Checks collision between a triangle and an axis-aligned cuboid.
     *
     * @param a       First triangle vertex
     * @param b       Second triangle vertex
     * @param c       Third triangle vertex
     * @param axis    Axis being resolved: X, Y, or Z
     * @param pos     Center of the player's cuboid
     * @param length  Cuboid size along the Z axis
     * @param width   Cuboid size along the X axis
     * @param height  Cuboid size along the Y axis
     */
    public boolean checkCameraTriangleCollision(
            Vector3 a,
            Vector3 b,
            Vector3 c,
            String axis,
            Vector3 pos,
            double length,
            double width,
            double height) {

        double hx = width / 2.0;
        double hy = height / 2.0;
        double hz = length / 2.0;

        // Translate triangle vertices into cuboid-centered coordinates.
        double[][] v = {
                {a.x - pos.x, a.y - pos.y, a.z - pos.z},
                {b.x - pos.x, b.y - pos.y, b.z - pos.z},
                {c.x - pos.x, c.y - pos.y, c.z - pos.z}
        };

        // Triangle edges.
        double[][] edges = {
                {
                        v[1][0] - v[0][0],
                        v[1][1] - v[0][1],
                        v[1][2] - v[0][2]
                },
                {
                        v[2][0] - v[1][0],
                        v[2][1] - v[1][1],
                        v[2][2] - v[1][2]
                },
                {
                        v[0][0] - v[2][0],
                        v[0][1] - v[2][1],
                        v[0][2] - v[2][2]
                }
        };

        // Triangle face normal.
        double[] normal = cross(edges[0], edges[1]);

        // Reject degenerate triangles.
        if (dot(normal, normal) < 1e-18) {
            return false;
        }

        // Optional axis filtering, matching your original API.
        if (axis != null) {
            double normalLength = Math.sqrt(dot(normal, normal));

            if (axis.equalsIgnoreCase("X")
                    && Math.abs(normal[0] / normalLength) < 0.2) {
                return false;
            }

            if (axis.equalsIgnoreCase("Y")
                    && Math.abs(normal[1] / normalLength) < 0.2) {
                return false;
            }

            if (axis.equalsIgnoreCase("Z")
                    && Math.abs(normal[2] / normalLength) < 0.2) {
                return false;
            }
        }

        // Test the cuboid's three face normals.
        double[][] boxAxes = {
                {1, 0, 0},
                {0, 1, 0},
                {0, 0, 1}
        };

        for (double[] boxAxis : boxAxes) {
            if (separated(v, boxAxis, hx, hy, hz)) {
                return false;
            }
        }

        // Test the triangle's face normal.
        if (separated(v, normal, hx, hy, hz)) {
            return false;
        }

        // Test the nine axes formed by triangle edges crossed
        // with the cuboid's three axes.
        for (double[] edge : edges) {
            for (double[] boxAxis : boxAxes) {
                double[] testAxis = cross(edge, boxAxis);

                // Parallel axes do not provide a useful separating test.
                if (dot(testAxis, testAxis) < 1e-18) {
                    continue;
                }

                if (separated(v, testAxis, hx, hy, hz)) {
                    return false;
                }
            }
        }

        // No separating axis: the cuboid and triangle intersect.
        return true;
    }

    private boolean separated(
            double[][] vertices,
            double[] axis,
            double hx,
            double hy,
            double hz) {

        double p0 = dot(vertices[0], axis);
        double p1 = dot(vertices[1], axis);
        double p2 = dot(vertices[2], axis);

        double min = Math.min(p0, Math.min(p1, p2));
        double max = Math.max(p0, Math.max(p1, p2));

        // Project the cuboid onto the same axis.
        double radius =
                hx * Math.abs(axis[0])
                        + hy * Math.abs(axis[1])
                        + hz * Math.abs(axis[2]);

        return min > radius || max < -radius;
    }

    private double[] cross(double[] a, double[] b) {
        return new double[] {
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
        };
    }

    private double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
}
