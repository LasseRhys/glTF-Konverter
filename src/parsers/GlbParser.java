package parsers;

import core.Object3d;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GlbParser implements Parser {
    @Override
    public Object3d parse(String filePath) {

        Object3d result = new Object3d();

        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(filePath))) {
            byte[] magic = new byte[4];
            dataInputStream.readFully(magic);

            if (!new String(magic).equals("glTF")) {
                System.out.println("Ungültiges Datei: " + filePath);
                return null;
            }

            dataInputStream.skipBytes(8);

            // Read the JSON chunk length as an unsigned int (4 bytes)
            int jsonChunkLengthByte1 = dataInputStream.readUnsignedByte();
            int jsonChunkLengthByte2 = dataInputStream.readUnsignedByte();
            int jsonChunkLengthByte3 = dataInputStream.readUnsignedByte();
            int jsonChunkLengthByte4 = dataInputStream.readUnsignedByte();

            long jsonChunkLength = ((long) jsonChunkLengthByte4 << 24) |
                    ((long) jsonChunkLengthByte3 << 16) |
                    ((long) jsonChunkLengthByte2 << 8) |
                    jsonChunkLengthByte1;

            System.out.println("JSON Chunk Length: " + jsonChunkLength);

            int jsonChunkType = dataInputStream.readInt();
            System.out.println("JSON Chunk Type: " + jsonChunkType);

            byte[] jsonChunkBytes = new byte[(int) jsonChunkLength];
            dataInputStream.readFully(jsonChunkBytes);
            String jsonString = new String(jsonChunkBytes);

            // Read the binary chunk
            byte[] binChunkHeaderBuffer = new byte[8];
            dataInputStream.readFully(binChunkHeaderBuffer);
            ByteBuffer buffer = ByteBuffer.wrap(binChunkHeaderBuffer);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            int binChunkLength = buffer.getInt();
            int binChunkType = buffer.getInt();

            byte[] binChunkBuffer = new byte[binChunkLength];
            dataInputStream.readFully(binChunkBuffer);

            // Save the extracted GLTF file
            String gltfFilePath = filePath.replace(".glb", ".gltf");
            try (FileWriter fileWriter = new FileWriter(gltfFilePath);
                 BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                bufferedWriter.write(jsonString);
            }

            JSONObject gltfJson = readGLTFFile(gltfFilePath);
            if (gltfJson != null) {

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
                byte[] positionsData = Arrays.copyOfRange(binChunkBuffer,
                        bufferViewPositions.getInt("byteOffset"),
                        bufferViewPositions.getInt("byteOffset") + bufferViewPositions.getInt("byteLength"));
                ByteBuffer positionsBuffer = ByteBuffer.wrap(positionsData);
                positionsBuffer.order(ByteOrder.LITTLE_ENDIAN);

                int numVertices = positionsAccessor.getInt("count");
                for (int i = 0; i < numVertices; i++) {
                    double x = positionsBuffer.getFloat();
                    double y = positionsBuffer.getFloat();
                    double z = positionsBuffer.getFloat();
                    result.addVertex(x, y, z);
                }

                JSONObject attributes = gltfJson.getJSONArray("meshes")
                        .getJSONObject(0)
                        .getJSONArray("primitives")
                        .getJSONObject(0)
                        .getJSONObject("attributes");

                if (attributes.has("TEXCOORD_0")) {
                    // Extract texture coordinates
                    int texCoordAccessorIndex = attributes
                            .getInt("TEXCOORD_0");

                    JSONObject texCoordAccessor = accessors.getJSONObject(texCoordAccessorIndex);
                    JSONObject bufferViewTexCoord = gltfJson.getJSONArray("bufferViews")
                            .getJSONObject(texCoordAccessor.getInt("bufferView"));
                    JSONObject bufferTexCoord = gltfJson.getJSONArray("buffers")
                            .getJSONObject(bufferViewTexCoord.getInt("buffer"));
                    byte[] texCoordData = Arrays.copyOfRange(binChunkBuffer,
                            bufferViewTexCoord.getInt("byteOffset"),
                            bufferViewTexCoord.getInt("byteOffset") + bufferViewTexCoord.getInt("byteLength"));
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
                        int bufferViewIndex = image.getInt("bufferView");
                        JSONObject bufferViewImage = gltfJson.getJSONArray("bufferViews").getJSONObject(bufferViewIndex);

                        int byteOffset = bufferViewImage.has("byteOffset") ? bufferViewImage.getInt("byteOffset") : 0;
                        byte[] imageData = Arrays.copyOfRange(binChunkBuffer,
                                byteOffset,
                                byteOffset + bufferViewImage.getInt("byteLength"));

                        imageBytes.add(imageData);
                    }
                }

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

                        if (material.has("normalTexture")) {
                            int textureIndex = material.getJSONObject("normalTexture").getInt("index");
                            resultMaterial.normalTexture = imageBytes.get(textureIndex);
                        }

                        if (material.has("occlusionTexture")) {
                            int textureIndex = material.getJSONObject("occlusionTexture").getInt("index");
                            resultMaterial.occlusionTexture = imageBytes.get(textureIndex);
                        }

                        if (material.has("emissiveTexture")) {
                            int textureIndex = material.getJSONObject("emissiveTexture").getInt("index");
                            resultMaterial.emissiveTexture = imageBytes.get(textureIndex);
                        }
                    }

                    // Extract indices and write faces with texture coordinates and material reference
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
                    byte[] indicesData = Arrays.copyOfRange(binChunkBuffer,
                            bufferViewIndices.getInt("byteOffset"),
                            bufferViewIndices.getInt("byteOffset") + bufferViewIndices.getInt("byteLength"));
                    ByteBuffer indicesBufferByte = ByteBuffer.wrap(indicesData);
                    indicesBufferByte.order(ByteOrder.LITTLE_ENDIAN);

                    int numIndices = indicesAccessor.getInt("count");
                    for (int i = 0; i < numIndices; i += 3) {
                        int index1 = toUnsignedInt(indicesBufferByte.getShort());
                        int index2 = toUnsignedInt(indicesBufferByte.getShort());
                        int index3 = toUnsignedInt(indicesBufferByte.getShort());

                        result.addFace(index1, index2, index3);
                    }
                }

                return result;
            }
        } catch (EOFException e) {
            System.out.println("Unexpected end of file encountered: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int toUnsignedInt(short value) {
        return value & 0xFFFF;
    }

    private static JSONObject readGLTFFile(String filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        String jsonString = new String(bytes);
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}