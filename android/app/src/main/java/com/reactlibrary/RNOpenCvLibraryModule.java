package com.reactlibrary;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.gson.GsonBuilder;

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

            ImageManipulation imgManip = new ImageManipulation(reactContext, image);
            Mat resultImage = new Mat();
            resultImage = ImageManipUtils.bitmapToMat(image);
            resultImage = ImageManipUtils.convertToGrayScale(resultImage);
            resultImage = ImageManipUtils.applyGausianBlur(resultImage);
            resultImage = ImageManipUtils.adaptiveThreshold(resultImage);
            resultImage = ImageManipUtils.applyBitwiseNot(resultImage);

            int[][] grid = imgManip.getSudokuGridNums();
            Bitmap bitmapImage = ImageManipUtils.convertMatToBitmap(resultImage);
            String imageBase64 = ImageManipUtils.convertToBase64(bitmapImage);
            ImageScanResponse result = new ImageScanResponse(grid, resultImage, imageBase64);
            successCallback.invoke(result.toJsonString());
//            Mat resultImage = response.getResultImage();
//            if (!resultImage.empty()) {
//                /* Give Base64 Image Back */
//                Bitmap bitmapImage = ImageManipUtils.convertMatToBitmap(resultImage);
//                String imageBase64 = ImageManipUtils.convertToBase64(bitmapImage);
//                ImageScanResponse result = new ImageScanResponse(response.getGrid(), response.getResultImage(), imageBase64);
//                successCallback.invoke(result.toJsonString());
//            } else {
//                errorCallback.invoke("Unsupported Image");
//            }

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    private void setGreenFrame(List<MatOfPoint> contours, int biggestPolygonIndex, Mat originalImage) {
        Scalar color = new Scalar(0, 255, 0, 255);
        Imgproc.drawContours(originalImage, contours, biggestPolygonIndex, color, 3);
    }
}

final class ImageScanResponse extends SudokuGridResponse {
    private String imageBase64;
    public ImageScanResponse(int[][] grid, Mat result, String imageBase64) {
        super(grid, result);
        this.imageBase64 = imageBase64;
    }
}