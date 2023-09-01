package parsers;

import core.Object3d;

import java.io.IOException;

public interface Parser {
    Object3d parse(String filePath) throws IOException;
}
