package exporters;

import core.Object3d;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class StlExporter implements Exporter {

    @Override
    public void export(Object3d object, String outputPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("solid exported_object\n");

            for (Object3d.Face face : object.getFaces()) {
                // Calculate normal for the triangle
                // Note: This is a simple normal calculation and might not be suitable for all meshes.
                Object3d.Vertex v1 = object.getVertices().get(face.indices[0]);
                Object3d.Vertex v2 = object.getVertices().get(face.indices[1]);
                Object3d.Vertex v3 = object.getVertices().get(face.indices[2]);

                Object3d.Vertex normal = calculateNormal(v1, v2, v3);

                writer.write(String.format("  facet normal %.6f %.6f %.6f\n", normal.x, normal.y, normal.z));
                writer.write("    outer loop\n");
                writer.write(String.format("      vertex %.6f %.6f %.6f\n", v1.x, v1.y, v1.z));
                writer.write(String.format("      vertex %.6f %.6f %.6f\n", v2.x, v2.y, v2.z));
                writer.write(String.format("      vertex %.6f %.6f %.6f\n", v3.x, v3.y, v3.z));
                writer.write("    endloop\n");
                writer.write("  endfacet\n");
            }

            writer.write("endsolid exported_object\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Object3d.Vertex calculateNormal(Object3d.Vertex v1, Object3d.Vertex v2, Object3d.Vertex v3) {
        // Calculate two vectors from the triangle
        Object3d.Vertex vector1 = new Object3d.Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
        Object3d.Vertex vector2 = new Object3d.Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);

        // Calculate the cross product of the two vectors
        double normalX = vector1.y * vector2.z - vector1.z * vector2.y;
        double normalY = vector1.z * vector2.x - vector1.x * vector2.z;
        double normalZ = vector1.x * vector2.y - vector1.y * vector2.x;

        // Normalize the result
        double length = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        normalX /= length;
        normalY /= length;
        normalZ /= length;

        return new Object3d.Vertex(normalX, normalY, normalZ);
    }
}
