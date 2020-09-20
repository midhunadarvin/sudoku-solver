package com.reactlibrary;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageManipulation {

    /**
     * finds corners of the sudoku grid in the Mat image using openCV HoughLines
     * points of intersection
     *
     * @param mat
     *            source image
     * @return List of Points representing coordinates of the four corners
     */
    public static List<Point> findCorners(Mat mat) {
        Mat lines = new Mat();
        List<double[]> horizontalLines = new ArrayList<double[]>();
        List<double[]> verticalLines = new ArrayList<double[]>();

        Imgproc.HoughLinesP(mat, lines, 1, Math.PI / 180, 150);

        for (int i = 0; i < lines.cols(); i++) {
            double[] line = lines.get(0, i);
            double x1 = line[0];
            double y1 = line[1];
            double x2 = line[2];
            double y2 = line[3];
            if (Math.abs(y2 - y1) < Math.abs(x2 - x1)) {
                horizontalLines.add(line);
            } else if (Math.abs(x2 - x1) < Math.abs(y2 - y1)) {
                verticalLines.add(line);
            }
        }

        // find the lines furthest from centre which will be the bounds for the
        // grid
        double[] topLine = horizontalLines.get(0);
        double[] bottomLine = horizontalLines.get(0);
        double[] leftLine = verticalLines.get(0);
        double[] rightLine = verticalLines.get(0);

        double xMin = 1000;
        double xMax = 0;
        double yMin = 1000;
        double yMax = 0;

        for (int i = 0; i < horizontalLines.size(); i++) {
            if (horizontalLines.get(i)[1] < yMin
                    || horizontalLines.get(i)[3] < yMin) {
                topLine = horizontalLines.get(i);
                yMin = horizontalLines.get(i)[1];
            } else if (horizontalLines.get(i)[1] > yMax
                    || horizontalLines.get(i)[3] > yMax) {
                bottomLine = horizontalLines.get(i);
                yMax = horizontalLines.get(i)[1];
            }
        }

        for (int i = 0; i < verticalLines.size(); i++) {
            if (verticalLines.get(i)[0] < xMin
                    || verticalLines.get(i)[2] < xMin) {
                leftLine = verticalLines.get(i);
                xMin = verticalLines.get(i)[0];
            } else if (verticalLines.get(i)[0] > xMax
                    || verticalLines.get(i)[2] > xMax) {
                rightLine = verticalLines.get(i);
                xMax = verticalLines.get(i)[0];
            }
        }

        // obtain four corners of sudoku grid
        Point topLeft = ImageManipUtils.findCorner(topLine, leftLine);
        Point topRight = ImageManipUtils.findCorner(topLine, rightLine);
        Point bottomLeft = ImageManipUtils.findCorner(bottomLine, leftLine);
        Point bottomRight = ImageManipUtils.findCorner(bottomLine, rightLine);

        List<Point> corners = new ArrayList<Point>(4);
        corners.add(topLeft);
        corners.add(topRight);
        corners.add(bottomLeft);
        corners.add(bottomRight);

        return corners;
    }
}
