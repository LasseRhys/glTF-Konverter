package exporters;

import core.Object3d;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.List;

public class ObjExporter implements Exporter {

    @Override
    public void export(Object3d object, String objFilePath) throws IOException {

        String outputDirectoryPath = objFilePath.replace(".obj", ""); // Removing the extension for further use
        String mtlFilePath = outputDirectoryPath + ".mtl";

        // Write .obj file
        BufferedWriter objWriter = new BufferedWriter(new FileWriter(objFilePath));
            objWriter.write("mtllib " + new File(mtlFilePath).getName() + "\n");
            objWriter.write("usemtl material0\n");

            // Write vertices
            for (Object3d.Vertex vertex : object.getVertices()) {
                objWriter.write("v " + vertex.x + " " + vertex.y + " " + vertex.z + "\n");
            }

            // Write texture coordinates
            for (Object3d.TexCoord texCoord : object.getTexCoords()) {
                objWriter.write("vt " + texCoord.u + " " + texCoord.v + "\n");
            }

            // Write faces
            for (Object3d.Face face : object.getFaces()) {
                // Assuming each face is a triangle with texture coordinates.
                objWriter.write(String.format("f %d/%d %d/%d %d/%d\n",
                        face.indices[0] + 1, face.indices[0] + 1,
                        face.indices[1] + 1, face.indices[1] + 1,
                        face.indices[2] + 1, face.indices[2] + 1));
            }

        // Write .mtl file
        BufferedWriter mtlWriter = new BufferedWriter(new FileWriter(mtlFilePath));
        List<Object3d.Material> materials = object.getMaterials();
        for (int i = 0; i < materials.size(); i++) {

            Object3d.Material material = materials.get(i);
            mtlWriter.write("newmtl material" + i);
            mtlWriter.newLine();

            // Check for a diffuse texture
            if (material.baseColorTexture != null) {
                String texturePath = outputDirectoryPath + "_base" + i + ".png";
                Files.write(Paths.get(texturePath), material.baseColorTexture);
                mtlWriter.write("map_Kd " + texturePath);
                mtlWriter.newLine();
            }
            if (material.metallicRoughness != null) {
                String texturePath = outputDirectoryPath + "_roughness" + i + ".png";
                Files.write(Paths.get(texturePath), material.metallicRoughness);
                mtlWriter.write("map_Pr " + texturePath);
                mtlWriter.newLine();
            }
            if (material.normalTexture != null) {
                String texturePath = outputDirectoryPath + "_normal" + i + ".png";
                Files.write(Paths.get(texturePath), material.normalTexture);
                mtlWriter.write("map_Bump " + texturePath);
                mtlWriter.newLine();
            }
            if (material.occlusionTexture != null) {
                String texturePath = outputDirectoryPath + "_occlusion" + i + ".png";
                Files.write(Paths.get(texturePath), material.occlusionTexture);
                mtlWriter.write("map_Ao " + texturePath);
                mtlWriter.newLine();
            }
            if (material.emissiveTexture != null) {
                String texturePath = outputDirectoryPath + "_emissive" + i + ".png";
                Files.write(Paths.get(texturePath), material.emissiveTexture);
                mtlWriter.write("map_Ke " + texturePath);
                mtlWriter.newLine();
            }
        }

        mtlWriter.close();
        objWriter.close();
    }
}
