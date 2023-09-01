package exporters;

import core.Object3d;

import java.io.IOException;

public interface Exporter {
    void export(Object3d object, String outputPath) throws IOException;
}

