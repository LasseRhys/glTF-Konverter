import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import exporters.Exporter;
import exporters.ObjExporter;
import exporters.StlExporter;
import parsers.GlbParser;
import parsers.GltfParser;
import parsers.Parser;

import java.util.UnknownFormatConversionException;

public class ParserFactory {
    public Parser getParser(String fileExtension) {
        return switch (fileExtension) {
            case "glb" -> new GlbParser();
            case "gltf" -> new GltfParser();
            default -> throw new UnknownFormatConversionException("Dateityp nicht unterstütz");
        };
    }
}
