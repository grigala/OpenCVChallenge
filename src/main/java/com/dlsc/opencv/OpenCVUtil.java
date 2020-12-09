package com.dlsc.opencv;


import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OpenCVUtil {

    public static Image process(Image image) {
        try {
            BufferedImage alphaLessImage = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_RGB);
            BufferedImage bImage = SwingFXUtils.fromFXImage(image, alphaLessImage);
            Path filePath = Files.createTempFile("clip", ".jpeg");
            ImageIO.write(bImage, "jpeg", filePath.toFile());
            return new Image(process(filePath.toFile().getAbsolutePath()).toUri().toURL().toExternalForm());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static Path process(String originalFile) throws IOException {
        Mat src = Imgcodecs.imread(originalFile);
        Mat result = doBackgroundRemoval(src);

        Path resultPath = Files.createTempFile("clip-processed", ".jpeg");

        // Write files
        Imgcodecs.imwrite(originalFile, src);
        Imgcodecs.imwrite(resultPath.toFile().getAbsolutePath(), result);

        return resultPath;
    }

    private static Mat doBackgroundRemoval(Mat frame) {
        // init
        Mat hsvImg = new Mat();
        List<Mat> hsvPlanes = new ArrayList<>();
        Mat thresholdImg = new Mat();

        // threshold the image with the average hue value
        hsvImg.create(frame.size(), CvType.CV_8U);
        Imgproc.cvtColor(frame, hsvImg, Imgproc.COLOR_BGR2HSV);
        Core.split(hsvImg, hsvPlanes);

        // get the average hue value of the image
        double threshValue = getHistAverage(hsvImg, hsvPlanes.get(0));

        Imgproc.threshold(hsvPlanes.get(0), thresholdImg, threshValue, 255.0, Imgproc.THRESH_BINARY);

        Imgproc.blur(thresholdImg, thresholdImg, new Size(5, 5));

        // dilate to fill gaps, erode to smooth edges
        Imgproc.dilate(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), 1);
        Imgproc.erode(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), 3);

        Imgproc.threshold(thresholdImg, thresholdImg, threshValue, 179.0, Imgproc.THRESH_BINARY);

        // create the new image
        Mat foreground = new Mat(frame.size(), CvType.CV_8UC4, new Scalar(255, 255, 255, 255));
        frame.copyTo(foreground, thresholdImg);

        return foreground;
    }

    /**
     * Get the average hue value of the image starting from its Hue channel
     * histogram
     *
     * @param hsvImg    the current frame in HSV
     * @param hueValues the Hue component of the current frame
     * @return the average Hue value
     */
    private static double getHistAverage(Mat hsvImg, Mat hueValues) {
        // init
        double average = 0.0;
        Mat hist_hue = new Mat();
        // 0-180: range of Hue values
        MatOfInt histSize = new MatOfInt(180);
        List<Mat> hue = new ArrayList<>();
        hue.add(hueValues);

        // compute the histogram
        Imgproc.calcHist(hue, new MatOfInt(0), new Mat(), hist_hue, histSize, new MatOfFloat(0, 179));

        // get the average Hue value of the image
        // (sum(bin(h)*h))/(image-height*image-width)
        // -----------------
        // equivalent to get the hue of each pixel in the image, add them, and
        // divide for the image size (height and width)
        for (int h = 0; h < 180; h++) {
            // for each bin, get its value and multiply it for the corresponding
            // hue
            average += (hist_hue.get(h, 0)[0] * h);
        }

        // return the average hue of the image
        return average / hsvImg.size().height / hsvImg.size().width;
    }
}