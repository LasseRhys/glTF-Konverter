import exporters.DaeExporter;
import exporters.Exporter;
import exporters.ObjExporter;
import exporters.StlExporter;

import java.util.UnknownFormatConversionException;

public class ExporterFactory {
    public Exporter getExporter(String fileExtension) {
        return switch (fileExtension) {
            case "obj" -> new ObjExporter();
            case "stl" -> new StlExporter();
            case "dae" -> new DaeExporter();
            default -> throw new UnknownFormatConversionException("Dateityp nicht unterstützt");
        };
    }
}
