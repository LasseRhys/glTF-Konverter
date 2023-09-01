package exporters;

import core.Object3d;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DaeExporter implements Exporter {

    @Override
    public void export(Object3d object, String outputPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            Path baseOutputPath = Paths.get(outputPath).getParent();
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writer.write("<COLLADA xmlns=\"http://www.collada.org/2005/11/COLLADASchema\" version=\"1.4.1\">\n");

            writer.write("  <asset>\n");
            writer.write("    <up_axis>Y_UP</up_axis>\n");
            writer.write("  </asset>\n");


// Library Images (for textures)
            writer.write("  <library_images>\n");
            for (int i = 0; i < object.getMaterials().size(); i++) {
                Object3d.Material material = object.getMaterials().get(i);

                if (material.baseColorTexture != null) {
                    String textureFileName = "baseColorTexture_" + i + ".png"; // Assuming PNG format
                    Path texturePath = baseOutputPath.resolve(textureFileName);
                    Files.write(texturePath, material.baseColorTexture);

                    writer.write("    <image id=\"baseColorTexture-" + i + "-image\">\n");
                    writer.write("      <init_from>" + textureFileName + "</init_from>\n");
                    writer.write("    </image>\n");
                }

                if (material.metallicRoughness != null) {
                    String textureFileName = "metallicRoughness_" + i + ".png"; // Assuming PNG format
                    Path texturePath = baseOutputPath.resolve(textureFileName);
                    Files.write(texturePath, material.metallicRoughness);

                    writer.write("    <image id=\"metallicRoughness-" + i + "-image\">\n");
                    writer.write("      <init_from>" + textureFileName + "</init_from>\n");
                    writer.write("    </image>\n");
                }

                if (material.normalTexture != null) {
                    String textureFileName = "normalTexture_" + i + ".png"; // Assuming PNG format
                    Path texturePath = baseOutputPath.resolve(textureFileName);
                    Files.write(texturePath, material.normalTexture);

                    writer.write("    <image id=\"normalTexture-" + i + "-image\">\n");
                    writer.write("      <init_from>" + textureFileName + "</init_from>\n");
                    writer.write("    </image>\n");
                }

                if (material.occlusionTexture != null) {
                    String textureFileName = "occlusionTexture_" + i + ".png"; // Assuming PNG format
                    Path texturePath = baseOutputPath.resolve(textureFileName);
                    Files.write(texturePath, material.occlusionTexture);

                    writer.write("    <image id=\"occlusionTexture-" + i + "-image\">\n");
                    writer.write("      <init_from>" + textureFileName + "</init_from>\n");
                    writer.write("    </image>\n");
                }

                if (material.emissiveTexture != null) {
                    String textureFileName = "emissiveTexture_" + i + ".png"; // Assuming PNG format
                    Path texturePath = baseOutputPath.resolve(textureFileName);
                    Files.write(texturePath, material.emissiveTexture);

                    writer.write("    <image id=\"emissiveTexture-" + i + "-image\">\n");
                    writer.write("      <init_from>" + textureFileName + "</init_from>\n");
                    writer.write("    </image>\n");
                }
            }
            writer.write("  </library_images>\n");

            // Library Effects (for materials)
            writer.write("  <library_effects>\n");
            for (int i = 0; i < object.getMaterials().size(); i++) {
                writer.write("    <effect id=\"material-" + i + "-effect\">\n");
                writer.write("      <profile_COMMON>\n");
                writer.write("        <newparam sid=\"material-" + i + "-surface\">\n");
                writer.write("          <surface type=\"2D\">\n");
                writer.write("            <init_from>material-" + i + "-image</init_from>\n");
                writer.write("          </surface>\n");
                writer.write("        </newparam>\n");
                writer.write("        <newparam sid=\"material-" + i + "-sampler\">\n");
                writer.write("          <sampler2D>\n");
                writer.write("            <source>material-" + i + "-surface</source>\n");
                writer.write("          </sampler2D>\n");
                writer.write("        </newparam>\n");
                writer.write("        <technique sid=\"COMMON\">\n");
                writer.write("          <phong>\n");
                writer.write("            <diffuse>\n");
                writer.write("              <texture texture=\"material-" + i + "-sampler\" texcoord=\"UV\"/>\n");
                writer.write("            </diffuse>\n");
                writer.write("          </phong>\n");
                writer.write("        </technique>\n");
                writer.write("      </profile_COMMON>\n");
                writer.write("    </effect>\n");
            }
            writer.write("  </library_effects>\n");

            // Library Materials (linking effects)
            writer.write("  <library_materials>\n");
            for (int i = 0; i < object.getMaterials().size(); i++) {
                writer.write("    <material id=\"material-" + i + "\" name=\"material-" + i + "\">\n");
                writer.write("      <instance_effect url=\"#material-" + i + "-effect\"/>\n");
                writer.write("    </material>\n");
            }
            writer.write("  </library_materials>\n");

            writer.write("  <library_geometries>\n");
            writer.write("    <geometry id=\"object-mesh\">\n");
            writer.write("      <mesh>\n");

            writer.write("        <source id=\"object-positions\">\n");
            writer.write("          <float_array id=\"object-positions-array\" count=\"" + (object.getVertices().size() * 3) + "\">");
            for (Object3d.Vertex vertex : object.getVertices()) {
                writer.write(String.format("%.6f %.6f %.6f ", vertex.x, vertex.y, vertex.z));
            }
            writer.write("</float_array>\n");
            writer.write("          <technique_common>\n");
            writer.write("            <accessor source=\"#object-positions-array\" count=\"" + object.getVertices().size() + "\" stride=\"3\">\n");
            writer.write("              <param name=\"X\" type=\"float\"/>\n");
            writer.write("              <param name=\"Y\" type=\"float\"/>\n");
            writer.write("              <param name=\"Z\" type=\"float\"/>\n");
            writer.write("            </accessor>\n");
            writer.write("          </technique_common>\n");
            writer.write("        </source>\n");

            writer.write("        <vertices id=\"object-vertices\">\n");
            writer.write("          <input semantic=\"POSITION\" source=\"#object-positions\"/>\n");
            writer.write("        </vertices>\n");

            writer.write("        <triangles count=\"" + object.getFaces().size() + "\">\n");
            writer.write("          <input semantic=\"VERTEX\" source=\"#object-vertices\" offset=\"0\"/>\n");
            writer.write("          <p>");
            for (Object3d.Face face : object.getFaces()) {
                for (int index : face.indices) {
                    writer.write(index + " ");
                }
            }
            writer.write("</p>\n");
            writer.write("        </triangles>\n");

            writer.write("      </mesh>\n");
            writer.write("    </geometry>\n");
            writer.write("  </library_geometries>\n");

            // Library Visual Scenes (to link geometry and materials)
            writer.write("  <library_visual_scenes>\n");
            writer.write("    <visual_scene id=\"scene\">\n");
            writer.write("      <node name=\"object\">\n");
            writer.write("        <instance_geometry url=\"#object-mesh\">\n");
            writer.write("          <bind_material>\n");
            writer.write("            <technique_common>\n");
            for (int i = 0; i < object.getMaterials().size(); i++) {
                writer.write("              <instance_material symbol=\"material-" + i + "\" target=\"#material-" + i + "\"/>\n");
            }
            writer.write("            </technique_common>\n");
            writer.write("          </bind_material>\n");
            writer.write("        </instance_geometry>\n");
            writer.write("      </node>\n");
            writer.write("    </visual_scene>\n");
            writer.write("  </library_visual_scenes>\n");

            writer.write("  <scene>\n");
            writer.write("    <instance_visual_scene url=\"#scene\"/>\n");
            writer.write("  </scene>\n");

            writer.write("</COLLADA>\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
