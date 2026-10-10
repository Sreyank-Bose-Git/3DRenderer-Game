package main;

import datatypes.Pixel;
import datatypes.Vector2;
import datatypes.Vector3;
import objects.Line;
import objects.Triangle;
import objects.Vertex;
import renderer.Camera;
import visuals.Texture;

import java.awt.*;
import java.util.ArrayList;

public class Scripts {

    RendererPanel rendererPanel;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Scripts(RendererPanel rendererPanel) {
        this.rendererPanel = rendererPanel;
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

        Vertex vertex1 =
                rendererPanel.world.vertices[line.index1];

        Vertex vertex2 =
                rendererPanel.world.vertices[line.index2];

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

        // -----------------------------------------------------
        // Frustum rejection
        // -----------------------------------------------------

        if (!lineInsideFrustum(cV1, cV2)) {
            return;
        }

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

    private boolean lineInsideFrustum(
            Vector3 a,
            Vector3 b) {

        double aspectRatio =
                (double)
                        rendererPanel.screenWidth /
                        rendererPanel.screenHeight;

        double focalLength =
                rendererPanel.renderer.focalLength;

        double leftA =
                -a.z * aspectRatio / focalLength;

        double leftB =
                -b.z * aspectRatio / focalLength;

        double rightA =
                a.z * aspectRatio / focalLength;

        double rightB =
                b.z * aspectRatio / focalLength;

        double bottomA =
                -a.z / focalLength;

        double bottomB =
                -b.z / focalLength;

        double topA =
                a.z / focalLength;

        double topB =
                b.z / focalLength;

        if (a.x < leftA &&
                b.x < leftB) {

            return false;
        }

        if (a.x > rightA &&
                b.x > rightB) {

            return false;
        }

        if (a.y < bottomA &&
                b.y < bottomB) {

            return false;
        }

        return !(a.y > topA) ||
                !(b.y > topB);
    }


    // =========================================================
    // DRAW TRIANGLE
    // =========================================================

    public void drawTriangle(
            Triangle triangle,
            Camera camera) {

        if (!triangle.opaque) {
            return;
        }

        Vertex vertex1 = rendererPanel.world.vertices[triangle.index1];
        Vertex vertex2 = rendererPanel.world.vertices[triangle.index2];
        Vertex vertex3 = rendererPanel.world.vertices[triangle.index3];

        if (vertex1 == null || vertex2 == null || vertex3 == null) {
            return;
        }

        Vector3 cV1 = worldToCamera(vertex1.coordinates, camera);
        Vector3 cV2 = worldToCamera(vertex2.coordinates, camera);
        Vector3 cV3 = worldToCamera(vertex3.coordinates, camera);

        Texture texture = triangle.texture;

        // This renderer draws triangles from textures, not a solid triangle color.
        // Keep the existing color-based rasterizer available for other callers,
        // but do not use it as a substitute when a triangle has no texture.
        if (texture == null) {
            return;
        }

        // Each row stores camera-space x,y,z and its corner UV: [x,y,z,u,v].
        double[][] polygon = {
                {cV1.x, cV1.y, cV1.z, triangle.u1, triangle.v1},
                {cV2.x, cV2.y, cV2.z, triangle.u2, triangle.v2},
                {cV3.x, cV3.y, cV3.z, triangle.u3, triangle.v3}
        };

        polygon = clipTexturedZ(polygon, rendererPanel.renderer.nearPlane, true);
        if (polygon == null || polygon.length < 3) return;
        polygon = clipTexturedZ(polygon, rendererPanel.renderer.farPlane, false);
        if (polygon == null || polygon.length < 3) return;
        polygon = clipTexturedFrustum(polygon);
        if (polygon == null || polygon.length < 3) return;

        // Fan triangulation keeps the UVs generated by every clipping intersection.
        for (int i = 1; i < polygon.length - 1; i++) {
            rasterizeTexturedTriangle(polygon[0], polygon[i], polygon[i + 1], texture);
        }
    }

    // =========================================================
    // RASTERIZE TRIANGLE
    // =========================================================

    private void rasterizeTriangle(
            Vector3 cV1,
            Vector3 cV2,
            Vector3 cV3,
            Color color) {

        Vector2 ndc1 =
                perspectiveProjection(cV1);

        Vector2 ndc2 =
                perspectiveProjection(cV2);

        Vector2 ndc3 =
                perspectiveProjection(cV3);

        if (ndc1 == null ||
                ndc2 == null ||
                ndc3 == null) {

            return;
        }

        RasterVertex a =
                new RasterVertex(
                        ndc1,
                        cV1.z,
                        rendererPanel.screenWidth,
                        rendererPanel.screenHeight
                );

        RasterVertex b =
                new RasterVertex(
                        ndc2,
                        cV2.z,
                        rendererPanel.screenWidth,
                        rendererPanel.screenHeight
                );

        RasterVertex c =
                new RasterVertex(
                        ndc3,
                        cV3.z,
                        rendererPanel.screenWidth,
                        rendererPanel.screenHeight
                );

        // -----------------------------------------------------
        // Sort vertices by Y
        // -----------------------------------------------------

        if (a.y > b.y) {

            RasterVertex temp = a;
            a = b;
            b = temp;
        }

        if (a.y > c.y) {

            RasterVertex temp = a;
            a = c;
            c = temp;
        }

        if (b.y > c.y) {

            RasterVertex temp = b;
            b = c;
            c = temp;
        }

        // -----------------------------------------------------
        // Degenerate triangle
        // -----------------------------------------------------

        if (Math.abs(c.y - a.y) < 1e-9) {
            return;
        }

        // -----------------------------------------------------
        // Scanline range
        // -----------------------------------------------------

        int startY =
                Math.max(
                        0,
                        (int) Math.ceil(a.y - 0.5)
                );

        int endY =
                Math.min(
                        rendererPanel.screenHeight - 1,
                        (int) Math.floor(c.y - 0.5)
                );

        // -----------------------------------------------------
        // Scan every pixel row
        // -----------------------------------------------------

        for (int y = startY;
             y <= endY;
             y++) {

            double sampleY =
                    y + 0.5;

            ArrayList<EdgePoint> intersections =
                    new ArrayList<>(2);

            addEdgeIntersection(
                    a,
                    b,
                    sampleY,
                    intersections
            );

            addEdgeIntersection(
                    b,
                    c,
                    sampleY,
                    intersections
            );

            addEdgeIntersection(
                    c,
                    a,
                    sampleY,
                    intersections
            );

            if (intersections.size() < 2) {
                continue;
            }

            EdgePoint left =
                    intersections.get(0);

            EdgePoint right =
                    intersections.get(1);

            if (left.x > right.x) {

                EdgePoint temp = left;
                left = right;
                right = temp;
            }

            drawScanline(
                    y,
                    left.x,
                    left.z,
                    right.x,
                    right.z,
                    color
            );
        }
    }


    // =========================================================
    // ADD EDGE INTERSECTION
    // =========================================================

    private void addEdgeIntersection(
            RasterVertex a,
            RasterVertex b,
            double y,
            ArrayList<EdgePoint> intersections) {

        if (Math.abs(a.y - b.y) < 1e-9) {
            return;
        }

        boolean intersects =
                (a.y <= y && y < b.y) ||
                        (b.y <= y && y < a.y);

        if (!intersects) {
            return;
        }

        double t =
                (y - a.y) /
                        (b.y - a.y);

        double x =
                a.x +
                        (b.x - a.x) * t;

        double z =
                a.z +
                        (b.z - a.z) * t;

        intersections.add(
                new EdgePoint(x, z)
        );
    }


    // =========================================================
    // DRAW SCANLINE
    // =========================================================

    private void drawScanline(
            int y,
            double x1,
            double z1,
            double x2,
            double z2,
            Color color) {

        if (y < 0 ||
                y >= rendererPanel.screenHeight) {

            return;
        }

        if (x1 > x2) {

            double temp = x1;
            x1 = x2;
            x2 = temp;

            temp = z1;
            z1 = z2;
            z2 = temp;
        }

        int startX =
                (int) Math.ceil(x1 - 0.5);

        int endX =
                (int) Math.floor(x2 - 0.5);

        startX =
                Math.max(
                        0,
                        startX
                );

        endX =
                Math.min(
                        rendererPanel.screenWidth - 1,
                        endX
                );

        if (startX > endX) {
            return;
        }

        double dx =
                x2 - x1;

        if (Math.abs(dx) < 1e-12) {

            putPixel(
                    startX,
                    y,
                    Math.min(z1, z2),
                    color
            );

            return;
        }

        double zPerX =
                (z2 - z1) / dx;

        double z =
                z1 +
                        (startX + 0.5 - x1)
                                * zPerX;

        for (int x = startX;
             x <= endX;
             x++) {

            putPixel(
                    x,
                    y,
                    z,
                    color
            );

            z += zPerX;
        }
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

    private double[] interpolateTextured(double[] a, double[] b, double t, double forcedZ) {
        double[] result = new double[5];
        for (int i = 0; i < 5; i++) result[i] = a[i] + (b[i] - a[i]) * t;
        if (!Double.isNaN(forcedZ)) result[2] = forcedZ;
        return result;
    }

    // Perspective-correct scanline rasterization. Screen points store [x,y,z,u,v].
    private void rasterizeTexturedTriangle(double[] p0, double[] p1, double[] p2, Texture texture) {
        Vector2 n0 = perspectiveProjection(new Vector3(p0[0], p0[1], p0[2]));
        Vector2 n1 = perspectiveProjection(new Vector3(p1[0], p1[1], p1[2]));
        Vector2 n2 = perspectiveProjection(new Vector3(p2[0], p2[1], p2[2]));
        if (n0 == null || n1 == null || n2 == null) return;

        double[][] a = screenTexturedPoint(n0, p0);
        double[][] b = screenTexturedPoint(n1, p1);
        double[][] c = screenTexturedPoint(n2, p2);
        // point layout: [x,y,z,uOverZ,vOverZ,oneOverZ]
        if (a[0][1] > b[0][1]) { double[][] t = a; a = b; b = t; }
        if (a[0][1] > c[0][1]) { double[][] t = a; a = c; c = t; }
        if (b[0][1] > c[0][1]) { double[][] t = b; b = c; c = t; }
        double[] va = a[0], vb = b[0], vc = c[0];
        if (Math.abs(vc[1] - va[1]) < 1e-9) return;
        int startY = Math.max(0, (int)Math.ceil(va[1] - 0.5));
        int endY = Math.min(rendererPanel.screenHeight - 1, (int)Math.floor(vc[1] - 0.5));
        for (int y = startY; y <= endY; y++) {
            double sy = y + 0.5;
            ArrayList<double[]> hits = new ArrayList<>(2);
            addTexturedEdgeHit(va, vb, sy, hits);
            addTexturedEdgeHit(vb, vc, sy, hits);
            addTexturedEdgeHit(vc, va, sy, hits);
            if (hits.size() < 2) continue;
            double[] left = hits.get(0), right = hits.get(1);
            if (left[0] > right[0]) { double[] t = left; left = right; right = t; }
            drawTexturedScanline(y, left, right, texture);
        }
    }

    private double[][] screenTexturedPoint(Vector2 ndc, double[] cameraPoint) {
        double z = cameraPoint[2];
        double invZ = 1.0 / z;
        return new double[][] {{
                (ndc.x + 1.0) * 0.5 * (rendererPanel.screenWidth - 1),
                (1.0 - ndc.y) * 0.5 * (rendererPanel.screenHeight - 1),
                z, cameraPoint[3] * invZ, cameraPoint[4] * invZ, invZ
        }};
    }

    private void addTexturedEdgeHit(double[] a, double[] b, double y, ArrayList<double[]> hits) {
        if (Math.abs(a[1] - b[1]) < 1e-9) return;
        boolean intersects = (a[1] <= y && y < b[1]) || (b[1] <= y && y < a[1]);
        if (!intersects) return;
        double t = (y - a[1]) / (b[1] - a[1]);
        double[] hit = new double[6];
        for (int i = 0; i < 6; i++) hit[i] = a[i] + (b[i] - a[i]) * t;
        hits.add(hit);
    }

    private void drawTexturedScanline(int y, double[] left, double[] right, Texture texture) {
        if (y < 0 || y >= rendererPanel.screenHeight) return;
        if (left[0] > right[0]) { double[] t = left; left = right; right = t; }
        int startX = Math.max(0, (int)Math.ceil(left[0] - 0.5));
        int endX = Math.min(rendererPanel.screenWidth - 1, (int)Math.floor(right[0] - 0.5));
        if (startX > endX) return;
        double dx = right[0] - left[0];
        double step = Math.abs(dx) < 1e-12 ? 0 : 1.0 / dx;
        double t = Math.abs(dx) < 1e-12 ? 0 : (startX + 0.5 - left[0]) * step;
        for (int x = startX; x <= endX; x++, t += step) {
            double invZ = left[5] + (right[5] - left[5]) * t;
            if (invZ <= 0 || !Double.isFinite(invZ)) continue;
            double u = (left[3] + (right[3] - left[3]) * t) / invZ;
            double v = (left[4] + (right[4] - left[4]) * t) / invZ;
            Color texel = texture.sampleRepeat(u, v);
            putPixel(x, y, 1.0 / invZ, texel);
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

    public Vector3[] clipPolygon(
            Vector3[] input,
            double plane,
            boolean keepGreater) {

        if (input == null ||
                input.length == 0) {

            return null;
        }

        ArrayList<Vector3> output =
                new ArrayList<>();

        for (int i = 0;
             i < input.length;
             i++) {

            Vector3 current =
                    input[i];

            Vector3 previous =
                    input[
                            (i + input.length - 1)
                                    % input.length
                            ];

            boolean currentInside =
                    keepGreater
                            ? current.z >= plane
                            : current.z <= plane;

            boolean previousInside =
                    keepGreater
                            ? previous.z >= plane
                            : previous.z <= plane;

            if (!previousInside &&
                    currentInside) {

                output.add(
                        intersectionWithZPlane(
                                previous,
                                current,
                                plane
                        )
                );

                output.add(current);

            } else if (previousInside &&
                    currentInside) {

                output.add(current);

            } else if (previousInside) {

                output.add(
                        intersectionWithZPlane(
                                previous,
                                current,
                                plane
                        )
                );
            }
        }

        if (output.size() < 3) {
            return null;
        }

        return output.toArray(
                new Vector3[0]
        );
    }


    // =========================================================
    // FRUSTUM CLIPPING
    // =========================================================

    private Vector3[] clipFrustum(
            Vector3[] polygon) {

        double aspectRatio =
                (double)
                        rendererPanel.screenWidth /
                        rendererPanel.screenHeight;

        double focalLength =
                rendererPanel.renderer.focalLength;

        // LEFT
        polygon =
                clipFrustumPlane(
                        polygon,
                        0,
                        aspectRatio,
                        focalLength
                );

        if (polygon == null) {
            return null;
        }

        // RIGHT
        polygon =
                clipFrustumPlane(
                        polygon,
                        1,
                        aspectRatio,
                        focalLength
                );

        if (polygon == null) {
            return null;
        }

        // BOTTOM
        polygon =
                clipFrustumPlane(
                        polygon,
                        2,
                        aspectRatio,
                        focalLength
                );

        if (polygon == null) {
            return null;
        }

        // TOP
        polygon =
                clipFrustumPlane(
                        polygon,
                        3,
                        aspectRatio,
                        focalLength
                );

        return polygon;
    }


    // =========================================================
    // FRUSTUM PLANE
    // =========================================================

    private Vector3[] clipFrustumPlane(
            Vector3[] polygon,
            int plane,
            double aspectRatio,
            double focalLength) {

        ArrayList<Vector3> output =
                new ArrayList<>();

        for (int i = 0;
             i < polygon.length;
             i++) {

            Vector3 current =
                    polygon[i];

            Vector3 previous =
                    polygon[
                            (i + polygon.length - 1)
                                    % polygon.length
                            ];

            double currentValue =
                    frustumValue(
                            current,
                            plane,
                            aspectRatio,
                            focalLength
                    );

            double previousValue =
                    frustumValue(
                            previous,
                            plane,
                            aspectRatio,
                            focalLength
                    );

            boolean currentInside =
                    currentValue >= 0;

            boolean previousInside =
                    previousValue >= 0;

            if (!previousInside &&
                    currentInside) {

                output.add(
                        frustumIntersection(
                                previous,
                                current,
                                previousValue,
                                currentValue
                        )
                );

                output.add(current);

            } else if (previousInside &&
                    currentInside) {

                output.add(current);

            } else if (previousInside) {

                output.add(
                        frustumIntersection(
                                previous,
                                current,
                                previousValue,
                                currentValue
                        )
                );
            }
        }

        if (output.size() < 3) {
            return null;
        }

        return output.toArray(
                new Vector3[0]
        );
    }


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

    private Vector3 frustumIntersection(
            Vector3 previous,
            Vector3 current,
            double previousValue,
            double currentValue) {

        double t =
                previousValue /
                        (previousValue -
                                currentValue);

        return new Vector3(

                previous.x +
                        t *
                                (current.x -
                                        previous.x),

                previous.y +
                        t *
                                (current.y -
                                        previous.y),

                previous.z +
                        t *
                                (current.z -
                                        previous.z)
        );
    }


    // =========================================================
    // Z-PLANE INTERSECTION
    // =========================================================

    private Vector3 intersectionWithZPlane(
            Vector3 a,
            Vector3 b,
            double plane) {

        double denominator =
                b.z - a.z;

        if (Math.abs(denominator) < 1e-12) {

            return new Vector3(
                    a.x,
                    a.y,
                    plane
            );
        }

        double t =
                (plane - a.z) /
                        denominator;

        return new Vector3(

                a.x +
                        t * (b.x - a.x),

                a.y +
                        t * (b.y - a.y),

                plane
        );
    }


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
    // CAMERA → WORLD
    // =========================================================

    public Vector3 cameraToWorld(
            Vector3 cameraPoint,
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

        double dy =
                cameraPoint.y * cosPitch +
                        cameraPoint.z * sinPitch;

        double z1 =
                -cameraPoint.y * sinPitch +
                        cameraPoint.z * cosPitch;

        double dx =
                cameraPoint.x * cosYaw +
                        z1 * sinYaw;

        double dz =
                -cameraPoint.x * sinYaw +
                        z1 * cosYaw;

        return new Vector3(

                dx + camera.coordinates.x,

                dy + camera.coordinates.y,

                dz + camera.coordinates.z
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


    // =========================================================
    // INTERNAL RASTER VERTEX
    // =========================================================

    private static class RasterVertex {

        double x;
        double y;
        double z;

        RasterVertex(
                Vector2 ndc,
                double z,
                int screenWidth,
                int screenHeight) {

            this.x =
                    (ndc.x + 1.0)
                            * 0.5
                            * (screenWidth - 1);

            this.y =
                    (1.0 - ndc.y)
                            * 0.5
                            * (screenHeight - 1);

            this.z = z;
        }
    }


    // =========================================================
    // INTERNAL EDGE POINT
    // =========================================================

    private static class EdgePoint {

        double x;
        double z;

        EdgePoint(
                double x,
                double z) {

            this.x = x;
            this.z = z;
        }
    }
}