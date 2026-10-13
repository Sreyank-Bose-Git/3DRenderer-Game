
package exec.visuals;

import types.visuals.Light;
import types.datatypes.Vector3;

import java.awt.Color;

public class LightManager {

    public double ambientStrength = 0.2;

    public Vector3 calculateNormal(Vector3 a, Vector3 b, Vector3 c) {
        double abX = b.x - a.x;
        double abY = b.y - a.y;
        double abZ = b.z - a.z;

        double acX = c.x - a.x;
        double acY = c.y - a.y;
        double acZ = c.z - a.z;

        double nx = abY * acZ - abZ * acY;
        double ny = abZ * acX - abX * acZ;
        double nz = abX * acY - abY * acX;

        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);

        if (length < 1e-10) {
            return new Vector3(0, 1, 0);
        }

        return new Vector3(nx / length, ny / length, nz / length);
    }

    public double calculateIntensity(
            Vector3 position,
            Vector3 normal,
            Light[] lights) {

        double intensity = ambientStrength;

        for (Light light : lights) {
            if (light == null || light.lightStrength <= 0) {
                continue;
            }

            double lx, ly, lz;

            if ("directional".equalsIgnoreCase(light.lightType)) {
                // rotation represents the direction the light shines.
                // Reverse it to get the direction toward the light.
                double rx = light.rotation.x;
                double ry = light.rotation.y;
                double rz = light.rotation.z;

                double length = Math.sqrt(rx * rx + ry * ry + rz * rz);
                if (length < 1e-10) {
                    continue;
                }

                lx = -rx / length;
                ly = -ry / length;
                lz = -rz / length;

            } else if ("point".equalsIgnoreCase(light.lightType)) {
                lx = light.coordinates.x - position.x;
                ly = light.coordinates.y - position.y;
                lz = light.coordinates.z - position.z;

                double distance = Math.sqrt(lx * lx + ly * ly + lz * lz);
                if (distance < 1e-10) {
                    continue;
                }

                lx /= distance;
                ly /= distance;
                lz /= distance;

            } else {
                continue;
            }

            double diffuse = Math.max(
                    0.0,
                    normal.x * lx +
                            normal.y * ly +
                            normal.z * lz
            );

            intensity += light.lightStrength * diffuse;
        }

        return Math.clamp(intensity, 0.0, 1.0);
    }

    public Color applyLighting(Color base, double intensity) {
        return new Color(
                (int) (base.getRed() * intensity),
                (int) (base.getGreen() * intensity),
                (int) (base.getBlue() * intensity),
                base.getAlpha()
        );
    }
}
