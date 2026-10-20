package main;

import types.datatypes.Pixel;
import types.datatypes.Vector2;
import types.datatypes.Vector3;
import types.parts.Line;
import types.parts.Triangle;
import types.parts.Vertex;
import exec.renderer.Camera;
import types.visuals.Texture;

import java.awt.*;
import java.util.ArrayList;

public class Scripts {

    RendererPanel rendererPanel;

    private double[] frameNormalSums;
    private Vector3[] frameNormalCache;
    private boolean[] frameNormalReady;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Scripts(RendererPanel rendererPanel) {
        this.rendererPanel = rendererPanel;
    }

    /** Build vertex-normal sums once per frame in O(triangle count). */
    public void beginFrame() {
        if (rendererPanel.world == null || rendererPanel.renderer.currentVertices == null) {
            frameNormalSums = null;
            frameNormalCache = null;
            frameNormalReady = null;
            return;
        }

        Vertex[] vertices = rendererPanel.renderer.currentVertices;
        int count = vertices.length;
        frameNormalSums = new double[Math.multiplyExact(count, 3)];
        frameNormalCache = new Vector3[count];
        frameNormalReady = new boolean[count];

        Triangle[] triangles = rendererPanel.renderer.currentTriangles;
        if (triangles == null) return;

        for (Triangle t : triangles) {
            if (t == null) break;
            int i1 = t.index1, i2 = t.index2, i3 = t.index3;
            if (i1 < 0 || i2 < 0 || i3 < 0 ||
                    i1 >= count || i2 >= count || i3 >= count) continue;

            Vertex a = vertices[i1], b = vertices[i2], c = vertices[i3];
            if (a == null || b == null || c == null ||
                    a.coordinates == null || b.coordinates == null || c.coordinates == null) continue;

            double abx = b.coordinates.x - a.coordinates.x;
            double aby = b.coordinates.y - a.coordinates.y;
            double abz = b.coordinates.z - a.coordinates.z;
            double acx = c.coordinates.x - a.coordinates.x;
            double acy = c.coordinates.y - a.coordinates.y;
            double acz = c.coordinates.z - a.coordinates.z;
            double nx = aby * acz - abz * acy;
            double ny = abz * acx - abx * acz;
            double nz = abx * acy - aby * acx;
            if (nx * nx + ny * ny + nz * nz < 1e-20) continue;

            int b1 = i1 * 3, b2 = i2 * 3, b3 = i3 * 3;
            frameNormalSums[b1] += nx; frameNormalSums[b1 + 1] += ny; frameNormalSums[b1 + 2] += nz;
            frameNormalSums[b2] += nx; frameNormalSums[b2 + 1] += ny; frameNormalSums[b2 + 2] += nz;
            frameNormalSums[b3] += nx; frameNormalSums[b3 + 1] += ny; frameNormalSums[b3 + 2] += nz;
        }
    }


    // =========================================================
    // PUT PIXEL
    // =========================================================

    public void putPixel(
            int x,
            int y,
            double z,
            Color color) {

        if (x < 0 ||
                x >= rendererPanel.screenWidth ||
                y < 0 ||
                y >= rendererPanel.screenHeight) {

            return;
        }

        int index =
                y * rendererPanel.screenWidth + x;

        if (color == null || !Double.isFinite(z) || z <= 0.0) return;

        if (z < rendererPanel.renderer.zBuffer[index]) {
            rendererPanel.renderer.zBuffer[index] = z;
            rendererPanel.renderer.cBuffer[index] = color;
        }
    }


    // =========================================================
    // DRAW VERTEX
    // =========================================================

    public void drawVertex(
            Vertex vertex,
            Camera camera) {

        if (!vertex.opaque) {
            return;
        }

        Vector3 cameraVertex =
                worldToCamera(
                        vertex.coordinates,
                        camera
                );

        if (cameraVertex.z <
                rendererPanel.renderer.nearPlane) {

            return;
        }

        if (cameraVertex.z >
                rendererPanel.renderer.farPlane) {

            return;
        }

        Vector2 ndc =
                perspectiveProjection(cameraVertex);

        if (ndc == null) {
            return;
        }

        Pixel pixel =
                pixelatedCoordinates(ndc);

        putPixel(
                pixel.x,
                pixel.y,
                cameraVertex.z,
                vertex.color
        );
    }


    // =========================================================
    // DRAW LINE
    // =========================================================

    public void drawLine(
            Line line,
            Camera camera) {

        if (!line.opaque) {
            return;
        }

        Vertex[] vertices = rendererPanel.renderer.currentVertices;
        if (vertices == null || line.index1 < 0 || line.index2 < 0
                || line.index1 >= vertices.length || line.index2 >= vertices.length) {
            return;
        }

        Vertex vertex1 = vertices[line.index1];
        Vertex vertex2 = vertices[line.index2];

        if (vertex1 == null ||
                vertex2 == null) {

            return;
        }

        Vector3 cV1 =
                worldToCamera(
                        vertex1.coordinates,
                        camera
                );

        Vector3 cV2 =
                worldToCamera(
                        vertex2.coordinates,
                        camera
                );

        // -----------------------------------------------------
        // Near clipping
        // -----------------------------------------------------

        Vector3[] clipped =
                clipLine(
                        cV1,
                        cV2,
                        rendererPanel.renderer.nearPlane,
                        true
                );

        if (clipped == null) {
            return;
        }

        // -----------------------------------------------------
        // Far clipping
        // -----------------------------------------------------

        clipped =
                clipLine(
                        clipped[0],
                        clipped[1],
                        rendererPanel.renderer.farPlane,
                        false
                );

        if (clipped == null) {
            return;
        }

        cV1 = clipped[0];
        cV2 = clipped[1];

        // Clip side planes too, preventing off-screen lines from producing
        // extremely long rasterization loops.
        clipped = clipLineFrustum(cV1, cV2);
        if (clipped == null) return;
        cV1 = clipped[0];
        cV2 = clipped[1];

        // -----------------------------------------------------
        // Projection
        // -----------------------------------------------------

        Vector2 ndc1 =
                perspectiveProjection(cV1);

        Vector2 ndc2 =
                perspectiveProjection(cV2);

        if (ndc1 == null ||
                ndc2 == null) {

            return;
        }

        double x1 =
                ndcToScreenX(ndc1.x);

        double y1 =
                ndcToScreenY(ndc1.y);

        double x2 =
                ndcToScreenX(ndc2.x);

        double y2 =
                ndcToScreenY(ndc2.y);

        // -----------------------------------------------------
        // Horizontal line
        // -----------------------------------------------------

        if (Math.abs(y2 - y1) < 1e-9) {

            if (x1 > x2) {

                double temp = x1;
                x1 = x2;
                x2 = temp;

                Vector3 tempV = cV1;
                cV1 = cV2;
                cV2 = tempV;
            }

            int startX =
                    Math.max(
                            0,
                            (int) Math.ceil(x1 - 0.5)
                    );

            int endX =
                    Math.min(
                            rendererPanel.screenWidth - 1,
                            (int) Math.floor(x2 - 0.5)
                    );

            double dx =
                    x2 - x1;

            for (int x = startX;
                 x <= endX;
                 x++) {

                double z;

                if (Math.abs(dx) < 1e-9) {

                    z = cV1.z;

                } else {

                    double t =
                            (x + 0.5 - x1) / dx;

                    z =
                            cV1.z +
                                    (cV2.z - cV1.z) * t;
                }

                putPixel(
                        x,
                        (int) Math.round(y1),
                        z,
                        line.color
                );
            }

            return;
        }

        // -----------------------------------------------------
        // Sort by Y
        // -----------------------------------------------------

        if (y1 > y2) {

            double temp;

            temp = x1;
            x1 = x2;
            x2 = temp;

            temp = y1;
            y1 = y2;
            y2 = temp;

            Vector3 tempV = cV1;
            cV1 = cV2;
            cV2 = tempV;
        }

        double dy =
                y2 - y1;

        double dx =
                x2 - x1;

        double dz =
                cV2.z - cV1.z;

        int startY =
                Math.max(
                        0,
                        (int) Math.ceil(y1 - 0.5)
                );

        int endY =
                Math.min(
                        rendererPanel.screenHeight - 1,
                        (int) Math.floor(y2 - 0.5)
                );

        if (startY > endY) {
            return;
        }

        for (int y = startY;
             y <= endY;
             y++) {

            double sampleY =
                    y + 0.5;

            double t =
                    (sampleY - y1) / dy;

            t =
                    Math.clamp(
                            t,
                            0.0,
                            1.0
                    );

            double x =
                    x1 + dx * t;

            double z =
                    cV1.z + dz * t;

            putPixel(
                    (int) Math.round(x),
                    y,
                    z,
                    line.color
            );
        }
    }


    // =========================================================
    // LINE FRUSTUM REJECTION
    // =========================================================

    private Vector3[] clipLineFrustum(Vector3 a, Vector3 b) {
        double aspect = (double) rendererPanel.screenWidth / rendererPanel.screenHeight;
        double focal = rendererPanel.renderer.focalLength;
        if (!(focal > 0.0) || !Double.isFinite(focal)) return null;

        double t0 = 0.0, t1 = 1.0;
        for (int plane = 0; plane < 4; plane++) {
            double fa = frustumValue(a, plane, aspect, focal);
            double fb = frustumValue(b, plane, aspect, focal);
            if (!Double.isFinite(fa) || !Double.isFinite(fb)) return null;
            if (fa < 0.0 && fb < 0.0) return null;
            if ((fa < 0.0) != (fb < 0.0)) {
                double denominator = fa - fb;
                if (Math.abs(denominator) < 1e-15) return null;
                double t = fa / denominator;
                if (fa < 0.0) t0 = Math.max(t0, t);
                else t1 = Math.min(t1, t);
                if (t0 > t1) return null;
            }
        }

        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        return new Vector3[] {
                new Vector3(a.x + dx * t0, a.y + dy * t0, a.z + dz * t0),
                new Vector3(a.x + dx * t1, a.y + dy * t1, a.z + dz * t1)
        };
    }

    // =========================================================
    // DRAW TRIANGLE
    // =========================================================



    public void drawTriangle(Triangle triangle, Camera camera) {
        if (!triangle.opaque) {
            return;
        }

        Vertex[] vertices = rendererPanel.renderer.currentVertices;
        if (triangle.index1 < 0 || triangle.index2 < 0 || triangle.index3 < 0 ||
                triangle.index1 >= vertices.length ||
                triangle.index2 >= vertices.length ||
                triangle.index3 >= vertices.length) return;

        Vertex vertex1 = vertices[triangle.index1];
        Vertex vertex2 = vertices[triangle.index2];
        Vertex vertex3 = vertices[triangle.index3];

        if (vertex1 == null || vertex2 == null || vertex3 == null ||
                vertex1.coordinates == null || vertex2.coordinates == null ||
                vertex3.coordinates == null) {
            return;
        }

        Texture texture = triangle.texture;
        if (texture == null) {
            return;
        }

        Vector3 w1 = vertex1.coordinates;
        Vector3 w2 = vertex2.coordinates;
        Vector3 w3 = vertex3.coordinates;

        // Smooth normals averaged from adjacent triangles.
        Vector3 n1 = calculateSmoothVertexNormal(triangle.index1);
        Vector3 n2 = calculateSmoothVertexNormal(triangle.index2);
        Vector3 n3 = calculateSmoothVertexNormal(triangle.index3);

        Vector3 cV1 = worldToCamera(w1, camera);
        Vector3 cV2 = worldToCamera(w2, camera);
        Vector3 cV3 = worldToCamera(w3, camera);

        // Each vertex stores:
        // camera XYZ, UV, world XYZ, normal XYZ.
        double[][] polygon = {
                {cV1.x, cV1.y, cV1.z, triangle.u1, triangle.v1,
                        w1.x, w1.y, w1.z, n1.x, n1.y, n1.z},

                {cV2.x, cV2.y, cV2.z, triangle.u2, triangle.v2,
                        w2.x, w2.y, w2.z, n2.x, n2.y, n2.z},

                {cV3.x, cV3.y, cV3.z, triangle.u3, triangle.v3,
                        w3.x, w3.y, w3.z, n3.x, n3.y, n3.z}
        };

        polygon = clipTexturedZ(
                polygon,
                rendererPanel.renderer.nearPlane,
                true
        );

        if (polygon == null || polygon.length < 3) {
            return;
        }

        polygon = clipTexturedZ(
                polygon,
                rendererPanel.renderer.farPlane,
                false
        );

        if (polygon == null || polygon.length < 3) {
            return;
        }

        polygon = clipTexturedFrustum(polygon);

        if (polygon == null || polygon.length < 3) {
            return;
        }

        // Render each clipped triangle with per-pixel lighting.
        for (int i = 1; i < polygon.length - 1; i++) {
            rasterizeTexturedTriangle(
                    polygon[0],
                    polygon[i],
                    polygon[i + 1],
                    texture
            );
        }
    }

    private Vector3 calculateSmoothVertexNormal(int vertexIndex) {
        if (frameNormalSums == null || vertexIndex < 0 ||
                vertexIndex >= frameNormalSums.length) {
            return new Vector3(0, 1, 0);
        }
        if (frameNormalReady[vertexIndex]) return frameNormalCache[vertexIndex];

        int base = vertexIndex * 3;
        double nx = frameNormalSums[base];
        double ny = frameNormalSums[base + 1];
        double nz = frameNormalSums[base + 2];
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        Vector3 normal = length < 1e-10
                ? new Vector3(0, 1, 0)
                : new Vector3(nx / length, ny / length, nz / length);

        frameNormalCache[vertexIndex] = normal;
        frameNormalReady[vertexIndex] = true;
        return normal;
    }

    // =========================================================
    // TEXTURED TRIANGLE CLIPPING (parallel UV values; no extra classes)
    // Each camera-space point is [x, y, z, u, v].
    // =========================================================

    private double[][] clipTexturedZ(double[][] input, double plane, boolean keepGreater) {
        if (input == null || input.length == 0) return null;
        ArrayList<double[]> output = new ArrayList<>();
        for (int i = 0; i < input.length; i++) {
            double[] current = input[i];
            double[] previous = input[(i + input.length - 1) % input.length];
            boolean ci = keepGreater ? current[2] >= plane : current[2] <= plane;
            boolean pi = keepGreater ? previous[2] >= plane : previous[2] <= plane;
            if (!pi && ci) {
                output.add(interpolateTextured(previous, current,
                        (plane - previous[2]) / (current[2] - previous[2]), plane));
                output.add(current);
            } else if (pi && ci) {
                output.add(current);
            } else if (pi) {
                output.add(interpolateTextured(previous, current,
                        (plane - previous[2]) / (current[2] - previous[2]), plane));
            }
        }
        return output.size() < 3 ? null : output.toArray(new double[0][]);
    }

    private double[][] clipTexturedFrustum(double[][] polygon) {
        double aspectRatio = (double) rendererPanel.screenWidth / rendererPanel.screenHeight;
        double focalLength = rendererPanel.renderer.focalLength;
        if (polygon == null || polygon.length < 3 ||
                !(focalLength > 0.0) || !Double.isFinite(focalLength)) return null;
        for (int plane = 0; plane < 4; plane++) {
            polygon = clipTexturedFrustumPlane(polygon, plane, aspectRatio, focalLength);
            if (polygon == null) return null;
        }
        return polygon;
    }

    private double[][] clipTexturedFrustumPlane(double[][] polygon, int plane,
                                                double aspectRatio, double focalLength) {
        if (polygon == null || polygon.length == 0) return null;
        ArrayList<double[]> output = new ArrayList<>();
        for (int i = 0; i < polygon.length; i++) {
            double[] current = polygon[i];
            double[] previous = polygon[(i + polygon.length - 1) % polygon.length];
            double cv = frustumValue(new Vector3(current[0], current[1], current[2]),
                    plane, aspectRatio, focalLength);
            double pv = frustumValue(new Vector3(previous[0], previous[1], previous[2]),
                    plane, aspectRatio, focalLength);
            boolean ci = cv >= 0;
            boolean pi = pv >= 0;
            if (!pi && ci) {
                output.add(interpolateTextured(previous, current, pv / (pv - cv), Double.NaN));
                output.add(current);
            } else if (pi && ci) {
                output.add(current);
            } else if (pi) {
                output.add(interpolateTextured(previous, current, pv / (pv - cv), Double.NaN));
            }
        }
        return output.size() < 3 ? null : output.toArray(new double[0][]);
    }


    private double[] interpolateTextured(
            double[] a, double[] b, double t, double forcedZ) {

        double[] result = new double[11];

        for (int i = 0; i < 11; i++) {
            result[i] = a[i] + (b[i] - a[i]) * t;
        }

        if (!Double.isNaN(forcedZ)) {
            result[2] = forcedZ;
        }

        return result;
    }


    // Perspective-correct scanline rasterization. Screen points store [x,y,z,u,v].


    private void rasterizeTexturedTriangle(
            double[] p0,
            double[] p1,
            double[] p2,
            Texture texture) {

        Vector2 n0 = perspectiveProjection(
                new Vector3(p0[0], p0[1], p0[2])
        );

        Vector2 n1 = perspectiveProjection(
                new Vector3(p1[0], p1[1], p1[2])
        );

        Vector2 n2 = perspectiveProjection(
                new Vector3(p2[0], p2[1], p2[2])
        );

        if (n0 == null || n1 == null || n2 == null) {
            return;
        }

        double[] va = screenTexturedPoint(n0, p0);
        double[] vb = screenTexturedPoint(n1, p1);
        double[] vc = screenTexturedPoint(n2, p2);

        if (va[1] > vb[1]) { double[] t = va; va = vb; vb = t; }
        if (va[1] > vc[1]) { double[] t = va; va = vc; vc = t; }
        if (vb[1] > vc[1]) { double[] t = vb; vb = vc; vc = t; }

        if (Math.abs(vc[1] - va[1]) < 1e-9) {
            return;
        }

        int startY = Math.max(
                0,
                (int) Math.ceil(va[1] - 0.5)
        );

        int endY = Math.min(
                rendererPanel.screenHeight - 1,
                (int) Math.floor(vc[1] - 0.5)
        );

        for (int y = startY; y <= endY; y++) {
            double sy = y + 0.5;

            ArrayList<double[]> hits = new ArrayList<>(2);

            addTexturedEdgeHit(va, vb, sy, hits);
            addTexturedEdgeHit(vb, vc, sy, hits);
            addTexturedEdgeHit(vc, va, sy, hits);

            if (hits.size() < 2) {
                continue;
            }

            double[] left = hits.get(0);
            double[] right = hits.get(1);

            if (left[0] > right[0]) {
                double[] t = left;
                left = right;
                right = t;
            }

            drawTexturedScanline(
                    y,
                    left,
                    right,
                    texture
            );
        }
    }




    private double[] screenTexturedPoint(Vector2 ndc, double[] cameraPoint) {
        double z = cameraPoint[2];
        double invZ = 1.0 / z;

        return new double[] {
                (ndc.x + 1.0) * 0.5 * (rendererPanel.screenWidth - 1),
                (1.0 - ndc.y) * 0.5 * (rendererPanel.screenHeight - 1),
                z,
                cameraPoint[3] * invZ,
                cameraPoint[4] * invZ,
                invZ,
                cameraPoint[5] * invZ,
                cameraPoint[6] * invZ,
                cameraPoint[7] * invZ,
                cameraPoint[8] * invZ,
                cameraPoint[9] * invZ,
                cameraPoint[10] * invZ
        };
    }



    private void addTexturedEdgeHit(
            double[] a,
            double[] b,
            double y,
            ArrayList<double[]> hits) {

        if (Math.abs(a[1] - b[1]) < 1e-9) {
            return;
        }

        boolean intersects =
                (a[1] <= y && y < b[1])
                        || (b[1] <= y && y < a[1]);

        if (!intersects) {
            return;
        }

        double t = (y - a[1]) / (b[1] - a[1]);

        double[] hit = new double[12];

        for (int i = 0; i < 12; i++) {
            hit[i] = a[i] + (b[i] - a[i]) * t;
        }

        hits.add(hit);
    }




    private void drawTexturedScanline(
            int y,
            double[] left,
            double[] right,
            Texture texture) {

        if (y < 0 || y >= rendererPanel.screenHeight) {
            return;
        }

        if (left[0] > right[0]) {
            double[] temp = left;
            left = right;
            right = temp;
        }

        int startX = Math.max(
                0,
                (int) Math.ceil(left[0] - 0.5)
        );

        int endX = Math.min(
                rendererPanel.screenWidth - 1,
                (int) Math.floor(right[0] - 0.5)
        );

        if (startX > endX) {
            return;
        }

        double dx = right[0] - left[0];
        double step = Math.abs(dx) < 1e-12 ? 0 : 1.0 / dx;

        double t = Math.abs(dx) < 1e-12
                ? 0
                : (startX + 0.5 - left[0]) * step;

        for (int x = startX; x <= endX; x++, t += step) {

            double invZ = left[5] + (right[5] - left[5]) * t;

            if (invZ <= 0 || !Double.isFinite(invZ)) {
                continue;
            }

            // Perspective-correct texture coordinates.
            double u = (left[3] + (right[3] - left[3]) * t) / invZ;
            double v = (left[4] + (right[4] - left[4]) * t) / invZ;

            // Perspective-correct world position.
            double worldX = (left[6] + (right[6] - left[6]) * t) / invZ;
            double worldY = (left[7] + (right[7] - left[7]) * t) / invZ;
            double worldZ = (left[8] + (right[8] - left[8]) * t) / invZ;

            // Perspective-correct interpolated normal.
            double nx = (left[9] + (right[9] - left[9]) * t) / invZ;
            double ny = (left[10] + (right[10] - left[10]) * t) / invZ;
            double nz = (left[11] + (right[11] - left[11]) * t) / invZ;

            double normalLength = Math.sqrt(nx * nx + ny * ny + nz * nz);

            if (normalLength > 1e-10) {
                nx /= normalLength;
                ny /= normalLength;
                nz /= normalLength;
            } else {
                nx = 0;
                ny = 1;
                nz = 0;
            }

            double intensity = rendererPanel.lightManager.calculateIntensity(
                    worldX, worldY, worldZ, nx, ny, nz,
                    rendererPanel.world.lights
            );

            Color texel = texture.sampleRepeat(u, v);

            Color litColor = rendererPanel.lightManager.applyLighting(
                    texel,
                    intensity
            );

            putPixel(x, y, 1.0 / invZ, litColor);
        }
    }




    // =========================================================
    // CLIP LINE
    // =========================================================

    public Vector3[] clipLine(
            Vector3 a,
            Vector3 b,
            double plane,
            boolean keepGreater) {

        boolean aInside =
                keepGreater
                        ? a.z >= plane
                        : a.z <= plane;

        boolean bInside =
                keepGreater
                        ? b.z >= plane
                        : b.z <= plane;

        if (!aInside &&
                !bInside) {

            return null;
        }

        if (aInside &&
                bInside) {

            return new Vector3[]{
                    a,
                    b
            };
        }

        double denominator =
                b.z - a.z;

        if (Math.abs(denominator) < 1e-12) {
            return null;
        }

        double t =
                (plane - a.z) /
                        denominator;

        Vector3 intersection =
                new Vector3(
                        a.x +
                                t * (b.x - a.x),

                        a.y +
                                t * (b.y - a.y),

                        plane
                );

        if (!aInside) {

            return new Vector3[]{
                    intersection,
                    b
            };

        }

        return new Vector3[]{
                a,
                intersection
        };
    }


    // =========================================================
    // CLIP POLYGON
    // =========================================================


    // =========================================================
    // FRUSTUM CLIPPING
    // =========================================================


    // =========================================================
    // FRUSTUM PLANE
    // =========================================================


    // =========================================================
    // FRUSTUM VALUE
    // =========================================================

    private double frustumValue(
            Vector3 point,
            int plane,
            double aspectRatio,
            double focalLength) {

        return switch (plane) {

            // LEFT
            case 0 -> point.x +
                    point.z *
                            aspectRatio /
                            focalLength;

            // RIGHT
            case 1 -> point.z *
                    aspectRatio /
                    focalLength -
                    point.x;

            // BOTTOM
            case 2 -> point.y +
                    point.z /
                            focalLength;

            // TOP
            case 3 -> point.z /
                    focalLength -
                    point.y;
            default -> 0;
        };
    }


    // =========================================================
    // FRUSTUM INTERSECTION
    // =========================================================


    // =========================================================
    // Z-PLANE INTERSECTION
    // =========================================================


    // =========================================================
    // WORLD → CAMERA
    // =========================================================

    public Vector3 worldToCamera(
            Vector3 worldPoint,
            Camera camera) {

        double radYaw =
                Math.toRadians(
                        camera.rotation.y
                );

        double radPitch =
                Math.toRadians(
                        camera.rotation.x
                );

        double cosYaw =
                Math.cos(radYaw);

        double sinYaw =
                Math.sin(radYaw);

        double cosPitch =
                Math.cos(radPitch);

        double sinPitch =
                Math.sin(radPitch);

        double dx =
                worldPoint.x -
                        camera.coordinates.x;

        double dy =
                worldPoint.y -
                        camera.coordinates.y;

        double dz =
                worldPoint.z -
                        camera.coordinates.z;

        double x1 =
                dx * cosYaw -
                        dz * sinYaw;

        double z1 =
                dx * sinYaw +
                        dz * cosYaw;

        double y2 =
                dy * cosPitch -
                        z1 * sinPitch;

        double z2 =
                dy * sinPitch +
                        z1 * cosPitch;

        return new Vector3(
                x1,
                y2,
                z2
        );
    }

    // =========================================================
    // PERSPECTIVE PROJECTION
    // =========================================================

    public Vector2 perspectiveProjection(
            Vector3 cameraPoint) {

        if (cameraPoint == null) {
            return null;
        }

        if (cameraPoint.z <
                rendererPanel.renderer.nearPlane) {

            return null;
        }

        if (cameraPoint.z >
                rendererPanel.renderer.farPlane) {

            return null;
        }

        double aspectRatio =
                (double)
                        rendererPanel.screenWidth /
                        rendererPanel.screenHeight;

        double ndcX =
                rendererPanel.renderer.focalLength
                        * cameraPoint.x
                        /
                        (
                                cameraPoint.z *
                                        aspectRatio
                        );

        double ndcY =
                rendererPanel.renderer.focalLength
                        * cameraPoint.y
                        /
                        cameraPoint.z;

        return new Vector2(
                ndcX,
                ndcY
        );
    }


    // =========================================================
    // NDC → SCREEN X
    // =========================================================

    private double ndcToScreenX(
            double ndcX) {

        return (ndcX + 1.0)
                * 0.5
                * (rendererPanel.screenWidth - 1);
    }


    // =========================================================
    // NDC → SCREEN Y
    // =========================================================

    private double ndcToScreenY(
            double ndcY) {

        return (1.0 - ndcY)
                * 0.5
                * (rendererPanel.screenHeight - 1);
    }


    // =========================================================
    // PIXELATED COORDINATES
    // =========================================================

    public Pixel pixelatedCoordinates(
            Vector2 ndc) {

        return new Pixel(
                (int) Math.round(
                        ndcToScreenX(ndc.x)
                ),

                (int) Math.round(
                        ndcToScreenY(ndc.y)
                )
        );
    }
}
