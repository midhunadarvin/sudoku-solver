package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.opencv.android.Utils;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import android.graphics.Color;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

public class RNOpenCvLibraryModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private static int GAP;
    public final static Scalar WHITE = new Scalar(255);
    public final static Scalar BLACK = new Scalar(0);
    private int threshold = 100;
    private boolean error = false;
    private Mat clean;

    public RNOpenCvLibraryModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNOpenCvLibrary";
    }

    @ReactMethod
    public void checkForBlurryImage(String imageAsBase64, Callback errorCallback, Callback successCallback) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = true;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            byte[] decodedString = Base64.decode(imageAsBase64, Base64.DEFAULT);
            Bitmap image = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            int l = CvType.CV_8UC1;
            Mat matImage = new Mat();
            Utils.bitmapToMat(image, matImage);
            Mat matImageGrey = new Mat();
            Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_BGR2GRAY);

            Bitmap destImage;
            destImage = Bitmap.createBitmap(image);
            Mat dst2 = new Mat();
            Utils.bitmapToMat(destImage, dst2);
            Mat laplacianImage = new Mat();
            dst2.convertTo(laplacianImage, l);
            Imgproc.Laplacian(matImageGrey, laplacianImage, CvType.CV_8U);
            Mat laplacianImage8bit = new Mat();
            laplacianImage.convertTo(laplacianImage8bit, l);

            Bitmap bmp = Bitmap.createBitmap(laplacianImage8bit.cols(), laplacianImage8bit.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(laplacianImage8bit, bmp);
            int[] pixels = new int[bmp.getHeight() * bmp.getWidth()];
            bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

            int maxLap = -16777216; // 16m
            for (int pixel : pixels) {
                if (pixel > maxLap)
                    maxLap = pixel;
            }
            int soglia = -8118750;
            if (maxLap <= soglia) {
                System.out.println("is blur image");
            }

            successCallback.invoke(maxLap <= soglia);
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void scanSudoku(String imageAsBase64, Callback errorCallback, Callback successCallback) {
        try {
            /* Get the Image */
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = true;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            byte[] decodedString = Base64.decode(imageAsBase64, Base64.DEFAULT);
            Bitmap image = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);


            Mat originalImage = new Mat();
            Utils.bitmapToMat(image, originalImage);

            Mat resultImage = new Mat();
            resultImage = ImageManipUtils.convertToGrayScale(originalImage);
            resultImage = ImageManipUtils.applyGausianBlur(resultImage);
            resultImage = ImageManipUtils.applyAdaptiveThreshold(resultImage);
            resultImage = ImageManipUtils.applyBitwiseNot(resultImage);

//            Mat edges = new Mat(originalImage.size(), originalImage.type());
//            Imgproc.Canny(originalImage, edges, 50, 200);

            // trim external noise to localize the sudoku puzzle and stores in bmp
            // then m2
//            int[] bounds = ImageManipUtils.findGridBounds(edges);
//            error = ImageManipUtils.notSquare(bounds);
//
//            /* Find corners */
//            edges = ImageManipUtils.subMat(edges, bounds);
//            clean = ImageManipUtils.subMat(clean, bounds);
//
//            List<Point> corners = ImageManipulation.findCorners(edges);
//            Point topLeft = corners.get(0);
//            Point topRight = corners.get(1);
//            Point bottomLeft = corners.get(2);
//            Point bottomRight = corners.get(3);

//            edges = ImageManipUtils.fixPerspective(topLeft, topRight, bottomLeft,
//                    bottomRight, edges);
//            clean = ImageManipUtils.fixPerspective(topLeft, topRight, bottomLeft,
//                    bottomRight, clean);


            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(resultImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            Mat warpedImage = new Mat();
            int biggestPolygonIndex = getBiggestPolygonIndex(contours);
            if (biggestPolygonIndex != 0) {
                final MatOfPoint biggest = contours.get(biggestPolygonIndex);
                List<Point> corners = getCornersFromPoints(biggest.toList());
//                for (Point corner : corners) {
//                    Imgproc.drawMarker(resultImage, corner, new Scalar(0, 191, 255, 255), 0, 20, 3);
//                }

                warpedImage = ImageManipUtils.fixPerspective(corners.get(0), corners.get(1), corners.get(2), corners.get(3), originalImage);
                setGreenFrame(contours, biggestPolygonIndex, originalImage);
            }

            /* Give Base64 Image Back */
            Bitmap result = ImageManipUtils.convertMatToBitmap(warpedImage);
            String resultBase64 = ImageManipUtils.convertToBase64(result);
            successCallback.invoke(resultBase64);
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    private void setGreenFrame(List<MatOfPoint> contours, int biggestPolygonIndex, Mat originalImage) {
        Scalar color = new Scalar(0, 255, 0, 255);
        Imgproc.drawContours(originalImage, contours, biggestPolygonIndex, color, 3);
    }

    private List<Point> getCornersFromPoints(final List<Point> points) {
        double minX = 0;
        double minY = 0;
        double maxX = 0;
        double maxY = 0;

        for (Point point : points) {
            double x = point.x;
            double y = point.y;

            if (minX == 0 || x < minX) {
                minX = x;
            }
            if (minY == 0 || y < minY) {
                minY = y;
            }
            if (maxX == 0 || x > maxX) {
                maxX = x;
            }
            if (maxY == 0 || y > maxY) {
                maxY = y;
            }
        }

        List<Point> corners = new ArrayList<>(4);

        corners.add(new Point(minX, minY)); // upLeft
        corners.add(new Point(maxX, minY)); // upRight
        corners.add(new Point(minX, maxY)); // downLeft
        corners.add(new Point(maxX, maxY)); // downRight

        return corners;
    }

    public int getBiggestPolygonIndex(List<MatOfPoint> contours) {
        int maxIndex = 0;
        double maxArea = 0;
        for (int i = 0; i < contours.size(); i++) {
            if (Imgproc.contourArea(contours.get(i)) > maxArea) {
                maxArea = Imgproc.contourArea(contours.get(i));
                maxIndex = i;
            }
        }
        return maxIndex;
    }
}