package com.dlsc.opencv;


import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public class BackgroundRemovalUtils {

    private static final String MODEL = "src/main/resources/model/yolov3.weights";
    private static final String CONFIG = "src/main/resources/model/yolov3.cfg";
    private static final String[] CLASSES = {"person"};

    public static Image removeBackgroundNN(String originalFile) {
        Mat src = Imgcodecs.imread(originalFile, Imgcodecs.IMREAD_COLOR);
        double h = src.size().height;
        double w = src.size().width;

        // Image size should be multiplier of 32, following if will crop if it isn't.
        if (h % 32 != 0.0 || w % 32 != 0.0) {
            h = h - (h % 32);
            w = w - (w % 32);
        }
        Mat properlySizedImage = new Mat();
        Size s = new Size(w, h);
        Imgproc.resize(src, properlySizedImage, s, 0, 0, Imgproc.INTER_AREA);

        Net net = Dnn.readNet(MODEL, CONFIG);
        double scale = 1.0 / 255;
        Scalar mean = new Scalar(0, 0, 0);
        Mat blob = Dnn.blobFromImage(properlySizedImage, scale, s, mean, true, false);

        net.setInput(blob);

        List<String> outputLayers = getOutputLayers(net);
        Mat output = net.forward(); // List<Mat>
        double width = properlySizedImage.size().width;
        double height = properlySizedImage.size().height;
        List<String> class_ids = new ArrayList<>();
        double[] confidences;

        // TODO

        return mat2Image(src);
    }

    /**
     * Removes background given a rectangle that user defines
     *
     * @param originalFile target image
     * @param x            x coordinate of the pixel(top left corner)
     * @param y            y coordinate of the pixel
     * @param width        width of the rectangle
     * @param height       height of the rectangle
     * @return Image
     */
    public static Image removeBackgroundManual(String originalFile,
                                               int x,
                                               int y,
                                               int width,
                                               int height) {
        Rect rect = new Rect(x, y, width, height);

        Mat maskMatrix = new Mat();
        Mat bgModel = new Mat(); //Mat.zeros(1, 65, CvType.CV_64F);
        Mat fgModel = new Mat(); //Mat.zeros(1, 65, CvType.CV_64F);
        Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(3));
        Mat originalImage = Imgcodecs.imread(originalFile, Imgcodecs.IMREAD_COLOR);

        Imgproc.grabCut(originalImage, maskMatrix, rect, bgModel, fgModel, 8, Imgproc.GC_INIT_WITH_RECT);
        Core.compare(maskMatrix, source, maskMatrix, Core.CMP_EQ);
        Mat foreground = new Mat(originalImage.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
        originalImage.copyTo(foreground, maskMatrix);

        // TODO Transform white background to transparent
        return mat2Image(foreground);
    }

    /**
     * Overloaded method that directly receives Rect object.
     *
     * @param originalFile target image
     * @param rect         Rect(x, y, width, height)
     * @return Image
     */
    public static Image removeBackgroundManualRect(String originalFile, Rect rect) {
        Mat maskMatrix = new Mat();
        Mat bgModel = new Mat(); //Mat.zeros(1, 65, CvType.CV_64F);
        Mat fgModel = new Mat(); //Mat.zeros(1, 65, CvType.CV_64F);
        Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(3));
        Mat originalImage = Imgcodecs.imread(originalFile, Imgcodecs.IMREAD_COLOR);

        Imgproc.grabCut(originalImage, maskMatrix, rect, bgModel, fgModel, 8, Imgproc.GC_INIT_WITH_RECT);
        Core.compare(maskMatrix, source, maskMatrix, Core.CMP_EQ);
        Mat foreground = new Mat(originalImage.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
        originalImage.copyTo(foreground, maskMatrix);

        return mat2Image(foreground);
    }

    public static void letPythonDoIt(String originalFile, String pythonFile) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("python3", resolvePythonScriptPath(pythonFile));
        processBuilder.redirectErrorStream(true);

        Process ps = processBuilder.start();
        List<String> results = readProcessOutput(ps.getInputStream());
        results.forEach(System.out::println);
    }

    private static List<String> getOutputLayers(Net net) {
        List<String> layerNames = net.getLayerNames();
        List<String> outputLayers = new ArrayList<>();
        for (int i : net.getUnconnectedOutLayers().toArray()) {
            outputLayers.add(layerNames.get(i - 1));
        }
        return outputLayers;
    }


    private static void drawBoundingBox(Mat img,
                                        String id,
                                        double confidence,
                                        double x,
                                        double y,
                                        double x_plus_w,
                                        double y_plus_h) {
        String label = Arrays.stream(CLASSES)
                .takeWhile(s -> s.equals(id))
                .findFirst()
                .orElseThrow(RuntimeException::new);

        Scalar c1 = new Scalar(0.0, 0.0, 255.0);
        Scalar c2 = new Scalar(0.0, 255.0, 255.0);

        Point p1 = new Point(x, y);
        Point p2 = new Point(x_plus_w, y_plus_h);
        Imgproc.rectangle(img, p1, p2, c1, 2);
        Imgproc.putText(img, label, new Point(x - 10, y - 10), Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, c2, 2);
    }

    private static Image mat2Image(Mat frame) {
        try {
            return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
        } catch (Exception e) {
            System.err.println("Cannot convert the Mat object: " + e);
            return null;
        }
    }

    private static BufferedImage matToBufferedImage(Mat original) {
        BufferedImage image = null;
        int width = original.width(), height = original.height(), channels = original.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        original.get(0, 0, sourcePixels);

        if (original.channels() > 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    private static List<String> readProcessOutput(InputStream inputStream) throws IOException {
        try (BufferedReader output = new BufferedReader(new InputStreamReader(inputStream))) {
            return output.lines().collect(Collectors.toList());
        }
    }

    private static String resolvePythonScriptPath(String filename) {
        File file = new File("src/main/resources/" + filename);
        return file.getAbsolutePath();
    }
}

