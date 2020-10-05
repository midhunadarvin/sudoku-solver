package com.reactlibrary;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageManipUtils {

    public static final int NOTSQUARE = 25;
    /**
     * returns undistorted version of Mat using transformation from OpenCV
     * library
     *
     * @param upLeft
     *            top left corner coordinates
     * @param upRight
     *            top right corner coordinates
     * @param downLeft
     *            bottom left corner coordinates
     * @param downRight
     *            bottom right corner coordinates
     * @param source
     *            source Mat
     * @return
     */
    public static Mat fixPerspective(Point upLeft, Point upRight,
                                     Point downLeft, Point downRight, Mat source) {
        List<Point> src = new ArrayList<Point>();
        List<Point> dest = new ArrayList<Point>();
        Mat result = new Mat(source.size(), source.type());

        // add the four corners to List
        src.add(upLeft);
        src.add(upRight);
        src.add(downLeft);
        src.add(downRight);

        Point topLeft = new Point(0, 0);
        Point topRight = new Point(source.cols(), 0);
        Point bottomLeft = new Point(0, source.rows());
        Point bottomRight = new Point(source.cols(), source.rows());

        // add destination corners to List (adjusted for rotation)
        dest.add(topLeft);
        dest.add(topRight);
        dest.add(bottomLeft);
        dest.add(bottomRight);

        // convert List to Mat
        Mat srcM = Converters.vector_Point2f_to_Mat(src);
        Mat destM = Converters.vector_Point2f_to_Mat(dest);

        // apply perspective transform using 3x3 matrix
        Mat perspectiveTrans = new Mat(3, 3, CvType.CV_32FC1);
        perspectiveTrans = Imgproc.getPerspectiveTransform(srcM, destM);
        Imgproc.warpPerspective(source, result, perspectiveTrans, result.size());

        return result;
    }

    /**
     * returns point of intersection between two lines
     *
     * @param l1
     *            array containing x1, y1, x2, y2
     * @param l2
     *            array containing x1, y1, x2, y2
     * @return Point of intersection between two lines
     */
    public static Point findCorner(double[] l1, double[] l2) {
        double x1 = l1[0];
        double y1 = l1[1];
        double x2 = l1[2];
        double y2 = l1[3];
        double x3 = l2[0];
        double y3 = l2[1];
        double x4 = l2[2];
        double y4 = l2[3];

        double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        double x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2)
                * (x3 * y4 - y3 * x4))
                / d;
        double y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2)
                * (x3 * y4 - y3 * x4))
                / d;

        Point p = new Point(x, y);
        return p;
    }

    /**
     * trims the bitmap to contain only the sudoku grid
     *
     * @param bmp
     *            source bitmap image
     * @return int array containing bounds-- [0]=left, [1]=right, [2]=top,
     *         [3]=bot
     */
    public static int[] findGridBounds(Mat mat) {
        int[] bounds = new int[4];
        // find the four general edges of the sudoku grid; 5 pixel buffer region
        // in case any part of the grid gets cut off
        // Bitmap bmp = matToBitmap(mat);
        int left = findBorders(1, mat) - 5;
        int right = findBorders(2, mat) + 5;
        int top = findBorders(3, mat) - 5;
        int bot = findBorders(4, mat) + 5;

        bounds[0] = left;
        bounds[1] = right;
        bounds[2] = top;
        bounds[3] = bot;

        return bounds;
    }

    /**
     * find the borders of the sudoku grid; the check for white line begins 1/3
     * away from the centre of the image
     *
     * @param side
     *            1=left, 2=right, 3=top, 4=bottom
     * @param bmp
     *            source bitmap
     * @return the x or y coordinate of the border
     */
    private static int findBorders(int side, Mat mat) {
        switch (side) {
            // left
            case 1:
                for (int i = mat.cols() / 3; i > 0; i--) {
                    if (isBorderHeight(i, mat))
                        return i;
                }
                break;
            // right
            case 2:
                for (int i = 2 * mat.cols() / 3; i < mat.cols(); i++) {
                    if (isBorderHeight(i, mat))
                        return i;
                }
                break;
            // top
            case 3:
                for (int i = mat.rows() / 3; i > 0; i--) {
                    if (isBorderWidth(i, mat))
                        return i;
                }
                break;
            // bottom
            case 4:
                for (int i = 2 * mat.rows() / 3; i < mat.rows(); i++) {
                    if (isBorderWidth(i, mat))
                        return i;
                }
                break;
        }

        // returns negative border if not found
        return -6;
    }

    /**
     * checks if horizontal line(width) is outside the sudoku grid
     *
     * @param height
     *            y coordinate
     * @param bmp
     *            source bitmap
     * @return true if line is outside of sudoku grid, false otherwise
     */
    private static boolean isBorderWidth(int height, Mat mat) {
        for (int i = 2 * mat.cols() / 5; i < 3 * mat.cols() / 5; i++) {
            // if pixel is black
            if ((int) mat.get(height, i)[0] == 255) {
                return false;
            }
        }
        return true;
    }

    /**
     * checks if vertical line(height) is outside the sudoku grid
     *
     * @param width
     *            x coordinate
     * @param bmp
     *            bitmap containing image
     * @return true if line is outside of sudoku grid, false otherwise
     */
    private static boolean isBorderHeight(int width, Mat mat) {
        for (int i = 2 * mat.rows() / 5; i < 3 * mat.rows() / 5; i++) {
            // if pixel is black
            if ((int) mat.get(i, width)[0] == 255) {
                return false;
            }
        }
        return true;
    }

    public static boolean notSquare(int[] bounds) {
        int left = bounds[0];
        int right = bounds[1];
        int top = bounds[2];
        int bot = bounds[3];

        if (Math.abs(right - left - (bot - top)) > NOTSQUARE) {
            return true;
        }
        return false;
    }

    public static Bitmap convertMatToBitmap(Mat tmp) {
        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmp, bmp);

        } catch (CvException e){
            Log.d("Exception",e.getMessage());
        }
        return bmp;
    }

    public static String convertToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();

        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        return encoded;
    }

    public static Mat convertToGrayScale(Mat src) {
        Mat resultImage = new Mat();
        Imgproc.cvtColor(src, resultImage, Imgproc.COLOR_BGR2GRAY);
        return resultImage;
    }

    public static Mat applyGausianBlur(Mat src) {
        Mat resultImage = new Mat();
        Imgproc.blur(src, resultImage, new Size(3,3));
        return resultImage;
    }

    public static Mat adaptiveThreshold(Mat src) {
        Mat resultImage = new Mat();
        Imgproc.adaptiveThreshold(src, resultImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
        return resultImage;
    }

    public static Mat applyBitwiseNot(Mat src) {
        Mat resultImage = new Mat();
        Core.bitwise_not(src, resultImage);
        return resultImage;
    }

    public static Mat dilateMat(Mat src, int size) {
        Mat resultImage = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(size, size));
        Imgproc.dilate(src, resultImage, kernel);
        return resultImage;
    }

    public static Mat binaryThreshold(Mat mat) {
        Mat resultImage = new Mat();
        Imgproc.threshold(mat, resultImage, 128, 255, Imgproc.THRESH_BINARY);
        return resultImage;
    }

    public static Mat bitmapToMat(Bitmap bmp) {
        Mat mat = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bmp, mat);
        return mat;
    }
}
