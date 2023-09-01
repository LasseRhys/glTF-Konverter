package parsers;

import core.Object3d;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GltfParser implements Parser {

    @Override
    public Object3d parse(String filePath) {
        Object3d result = new Object3d();
        Path basePath = Paths.get(filePath).getParent();

        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
            JSONObject gltfJson = new JSONObject(jsonString);

// Extract vertex positions
            int positionsAccessorIndex = gltfJson.getJSONArray("meshes")
                    .getJSONObject(0)
                    .getJSONArray("primitives")
                    .getJSONObject(0)
                    .getJSONObject("attributes")
                    .getInt("POSITION");

            JSONArray accessors = gltfJson.getJSONArray("accessors");
            JSONObject positionsAccessor = accessors.getJSONObject(positionsAccessorIndex);
            JSONObject bufferViewPositions = gltfJson.getJSONArray("bufferViews")
                    .getJSONObject(positionsAccessor.getInt("bufferView"));
            JSONObject bufferPositions = gltfJson.getJSONArray("buffers")
                    .getJSONObject(bufferViewPositions.getInt("buffer"));

            byte[] positionsData = Files.readAllBytes(basePath.resolve(bufferPositions.getString("uri")));
            ByteBuffer positionsBuffer = ByteBuffer.wrap(positionsData);
            positionsBuffer.order(ByteOrder.LITTLE_ENDIAN);

// Consider the byte offset from the buffer view
            int byteOffset = bufferViewPositions.optInt("byteOffset", 0);
            positionsBuffer.position(byteOffset);

            int numVertices = positionsAccessor.getInt("count");
            for (int i = 0; i < numVertices; i++) {
                double x = positionsBuffer.getFloat();
                double y = positionsBuffer.getFloat();
                double z = positionsBuffer.getFloat();
                result.addVertex(x, y, z);
            }

            // Extract texture coordinates
            if (gltfJson.getJSONArray("meshes").getJSONObject(0).getJSONArray("primitives").getJSONObject(0).getJSONObject("attributes").has("TEXCOORD_0")) {
                int texCoordAccessorIndex = gltfJson.getJSONArray("meshes")
                        .getJSONObject(0)
                        .getJSONArray("primitives")
                        .getJSONObject(0)
                        .getJSONObject("attributes")
                        .getInt("TEXCOORD_0");

                JSONObject texCoordAccessor = accessors.getJSONObject(texCoordAccessorIndex);
                JSONObject bufferViewTexCoord = gltfJson.getJSONArray("bufferViews")
                        .getJSONObject(texCoordAccessor.getInt("bufferView"));
                JSONObject bufferTexCoord = gltfJson.getJSONArray("buffers")
                        .getJSONObject(bufferViewTexCoord.getInt("buffer"));

                byte[] texCoordData = Files.readAllBytes(basePath.resolve(bufferTexCoord.getString("uri")));
                ByteBuffer texCoordBuffer = ByteBuffer.wrap(texCoordData);
                texCoordBuffer.order(ByteOrder.LITTLE_ENDIAN);

                int numTexCoords = texCoordAccessor.getInt("count");
                for (int j = 0; j < numTexCoords; j++) {
                    double u = texCoordBuffer.getFloat();
                    double v = 1.0 - texCoordBuffer.getFloat(); // Flip the v coordinate
                    result.addTexCoord(u, v);
                }
            }

            List<byte[]> imageBytes = new ArrayList<>();

            // Extract images and save them to an array of file paths
            if (gltfJson.has("images")) {
                JSONArray images = gltfJson.getJSONArray("images");
                for (int i = 0; i < images.length(); i++) {
                    JSONObject image = images.getJSONObject(i);
                    byte[] imageData = Files.readAllBytes(basePath.resolve(image.getString("uri")));
                    imageBytes.add(imageData);
                }
            }

            // Extract materials and textures
            if (gltfJson.has("materials")) {
                JSONArray materials = gltfJson.getJSONArray("materials");
                for (int i = 0; i < materials.length(); i++) {
                    JSONObject material = materials.getJSONObject(i);

                    var resultMaterial = new Object3d.Material();
                    result.addMaterial(resultMaterial);

                    // Check for a diffuse texture
                    if (material.has("pbrMetallicRoughness") && material.getJSONObject("pbrMetallicRoughness").has("baseColorTexture")) {
                        int textureIndex = material.getJSONObject("pbrMetallicRoughness").getJSONObject("baseColorTexture").getInt("index");
                        resultMaterial.baseColorTexture = imageBytes.get(textureIndex);
                    }

                    // Check for metallic/roughness texture
                    if (material.has("pbrMetallicRoughness") && material.getJSONObject("pbrMetallicRoughness").has("metallicRoughnessTexture")) {
                        int textureIndex = material.getJSONObject("pbrMetallicRoughness").getJSONObject("metallicRoughnessTexture").getInt("index");
                        resultMaterial.metallicRoughness = imageBytes.get(textureIndex); // Roughness map
                    }

                    // Check for normal texture
                    if (material.has("normalTexture")) {
                        int textureIndex = material.getJSONObject("normalTexture").getInt("index");
                        resultMaterial.normalTexture = imageBytes.get(textureIndex);
                    }

                    // Check for occlusion texture
                    if (material.has("occlusionTexture")) {
                        int textureIndex = material.getJSONObject("occlusionTexture").getInt("index");
                        resultMaterial.occlusionTexture = imageBytes.get(textureIndex);
                    }

                    // Check for emissive texture
                    if (material.has("emissiveTexture")) {
                        int textureIndex = material.getJSONObject("emissiveTexture").getInt("index");
                        resultMaterial.emissiveTexture = imageBytes.get(textureIndex);
                    }
                }
            }

            // Extract indices and write faces with texture coordinates and material reference
            if (gltfJson.getJSONArray("meshes").getJSONObject(0).getJSONArray("primitives").getJSONObject(0).has("indices")) {
                int indicesAccessorIndex = gltfJson.getJSONArray("meshes")
                        .getJSONObject(0)
                        .getJSONArray("primitives")
                        .getJSONObject(0)
                        .getInt("indices");
                JSONObject indicesAccessor = accessors.getJSONObject(indicesAccessorIndex);
                JSONObject bufferViewIndices = gltfJson.getJSONArray("bufferViews")
                        .getJSONObject(indicesAccessor.getInt("bufferView"));
                JSONObject bufferIndices = gltfJson.getJSONArray("buffers")
                        .getJSONObject(bufferViewIndices.getInt("buffer"));
                byte[] indicesData = Files.readAllBytes(basePath.resolve(bufferIndices.getString("uri")));
                ByteBuffer indicesBufferByte = ByteBuffer.wrap(indicesData);
                indicesBufferByte.order(ByteOrder.LITTLE_ENDIAN);

                int componentType = indicesAccessor.getInt("componentType");
                int numIndices = indicesAccessor.getInt("count");

                // Consider the byte offset from the buffer view
                byteOffset = bufferViewIndices.optInt("byteOffset", 0);
                indicesBufferByte.position(byteOffset);

                for (int i = 0; i < numIndices; i += 3) {
                    int index1 = readIndex(indicesBufferByte, componentType);
                    int index2 = readIndex(indicesBufferByte, componentType);
                    int index3 = readIndex(indicesBufferByte, componentType);

                    result.addFace(index1, index2, index3);
                }
            }

            return result;
        } catch (EOFException e) {
            System.out.println("Unexpected end of file encountered: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int readIndex(ByteBuffer byteBuffer, int componentType) {
        switch (componentType) {
            case 5120: // BYTE
                return byteBuffer.get();
            case 5121: // UNSIGNED_BYTE
                return byteBuffer.get() & 0xFF;
            case 5122: // SHORT
                return byteBuffer.getShort();
            case 5123: // UNSIGNED_SHORT
                return byteBuffer.getShort() & 0xFFFF;
            case 5125: // UNSIGNED_INT
                return byteBuffer.getInt();
            default:
                throw new IllegalArgumentException("Unsupported component type for indices: " + componentType);
        }
    }

    private static int toUnsignedInt(short value) {
        return value & 0xFFFF;
    }
}
