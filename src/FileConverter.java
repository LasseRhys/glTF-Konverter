import core.Object3d;
import exporters.Exporter;
import parsers.Parser;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileConverter implements ActionListener {

    private static final String[] SUPPORTED_FILE_EXTENSIONS = {"gltf", "glb"};
    private static final String[] SUPPORTED_OUTPUT_FORMATS = {"obj", "dae", "stl" };

    private final JFrame frame;
    private final JFileChooser fileChooser;
    private final JCheckBox keepOriginalCheckBox;
    private final JComboBox<String> outputFormatComboBox;

    private final ParserFactory parsers = new ParserFactory();

    private final ExporterFactory exporters = new ExporterFactory();

    public FileConverter() {
        frame = new JFrame("glTF Konverter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("GLTF Files", SUPPORTED_FILE_EXTENSIONS);
        fileChooser.setFileFilter(filter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        JButton selectButton = new JButton("Datei auswählen");
        selectButton.addActionListener(this);

        keepOriginalCheckBox = new JCheckBox("Ursprungsdatei behalten", true);

        outputFormatComboBox = new JComboBox<>(SUPPORTED_OUTPUT_FORMATS);

        JPanel optionsPanel = new JPanel();
        optionsPanel.add(selectButton);
        optionsPanel.add(keepOriginalCheckBox);
        optionsPanel.add(outputFormatComboBox);

        // Panel for drag and drop functionality
        JPanel dropPanel = new JPanel();
        dropPanel.setBorder(BorderFactory.createTitledBorder("Datei hier reinziehen"));
        dropPanel.setTransferHandler(new FileDropHandler());
        dropPanel.setLayout(new BorderLayout());

        frame.add(optionsPanel, BorderLayout.NORTH);
        frame.add(dropPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Datei auswählen")) {
            int result = fileChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    if (selectedFile.isDirectory()) {
                        convertFolder(selectedFile.getAbsolutePath());
                    } else {
                        convertFile(selectedFile.getAbsolutePath());
                    }
                } catch (IOException exception) {
                    System.out.println(exception.getMessage());
                }
            }
        }
    }

    private class FileDropHandler extends TransferHandler {

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support))
                return false;

            Transferable transferable = support.getTransferable();
            try {
                java.util.List<File> fileList = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                for (File file : fileList) {
                    if (file.isDirectory()) {
                        convertFolder(file.getAbsolutePath());
                    } else {
                        convertFile(file.getAbsolutePath());
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public void convertFolder(String folderPath) throws IOException {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Invalid folder path: " + folderPath);
            return;
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    convertFile(file.getAbsolutePath());
                }
            }
        }
    }

    public void convertFile(String filePath) throws IOException {
        String fileExtension = getFileExtension(filePath);

        if (fileExtension == null)
            throw new InvalidObjectException("file extension not found.");

        String outputFormat = (String) outputFormatComboBox.getSelectedItem();

        if (outputFormat == null)
            throw new InvalidObjectException("output format was null.");



        Parser parser = parsers.getParser(fileExtension);
        Exporter exporter = exporters.getExporter(outputFormat);

        String outputFilePath = filePath.replace(fileExtension, outputFormat);
        outputFilePath = insertSubfolderBeforeFile(outputFilePath, "output");

        try {
            Object3d object = parser.parse(filePath);
            exporter.export(object, outputFilePath);
            System.out.println("Converted file: " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Delete the original file if required
        if (!keepOriginalCheckBox.isSelected()) {
            File originalFile = new File(filePath);
            originalFile.delete();
        }
    }

    public static String insertSubfolderBeforeFile(String originalPath, String subfolderName) {
        Path path = Paths.get(originalPath);
        Path parent = path.getParent();
        Path subfolder = parent.resolve(subfolderName);
        Path newPath = subfolder.resolve(path.getFileName());

        try {
            if (!Files.exists(subfolder)) {
                Files.createDirectories(subfolder);
                System.out.println("Directory created: " + subfolder);
            } else {
                System.out.println("Directory already exists: " + subfolder);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return newPath.toString();
    }

    private static String getFileExtension(String filePath) {
        int dotIndex = filePath.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < filePath.length() - 1) {
            return filePath.substring(dotIndex + 1).toLowerCase();
        }
        return null;
    }

    public static void main(String[] args) {
        new FileConverter();
    }
}
