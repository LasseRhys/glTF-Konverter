package core;

import java.util.ArrayList;
import java.util.List;

public class Object3d {

        // Inner classes to represent different data types
    public static class Vertex {
        public double x, y, z;

        public Vertex(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class TexCoord {
        public double u, v;

        public TexCoord(double u, double v) {
            this.u = u;
            this.v = v;
        }
    }

    public static class Face {
        public int[] indices;

        public Face(int... indices) {
            this.indices = indices;
        }
    }

    // Lists to store the data
    private List<Vertex> vertices = new ArrayList<>();
    private List<TexCoord> texCoords = new ArrayList<>();
    private List<Face> faces = new ArrayList<>();

    // Methods to add data
    public void addVertex(double x, double y, double z) {
        vertices.add(new Vertex(x, y, z));
    }

    public void addTexCoord(double u, double v) {
        texCoords.add(new TexCoord(u, v));
    }

    public void addFace(int... indices) {
        faces.add(new Face(indices));
    }

    // Getter methods if you need to access data
    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<TexCoord> getTexCoords() {
        return texCoords;
    }

    public List<Face> getFaces() {
        return faces;
    }

    private List<Material> materials = new ArrayList<>();

    public void addMaterial(Material material) {
        materials.add(material);
    }

    public List<Material> getMaterials() {
        return materials;
    }

    public static class Material {

        public byte[] baseColorTexture;

        public byte[] metallicRoughness;

        public byte[] normalTexture;

        public byte[] occlusionTexture;

        public byte[] emissiveTexture;
    }
}
