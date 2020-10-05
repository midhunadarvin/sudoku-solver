package com.reactlibrary;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import com.google.gson.GsonBuilder;
import com.reactlibrary.imageproc.BlobExtract;
import com.reactlibrary.imageproc.ConnectedComponentLabel;
import com.reactlibrary.ocr.TessOCR;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ImageManipulation {
    private Bitmap mBitmap;
    private Mat clean;
    private BlobExtract mBlobExtract;
    private TessOCR mOCR;
    private boolean error = false;

    public ImageManipulation(Context context, Bitmap bitmap) {
        mBitmap = bitmap;
        mBlobExtract = new BlobExtract();
        mOCR = new TessOCR(context);
    }

    /**
     * performs all the required image processing to find sudoku grid numbers
     */
    public int[][] getSudokuGridNums() {
        clean = ImageManipUtils.bitmapToMat(mBitmap);
        Mat result = extractSudokuGrid(clean);
        if (error) {
            return null;
        }

        // start new
        ConnectedComponentLabel ccl = new ConnectedComponentLabel();
        byte[][] cleanByteArray = ccl.getByteArrayForOCR(clean);

        mOCR.initOCR();
        String ans = mOCR.doOCR(cleanByteArray);

        ImageManipUtils.dilateMat(result, 4);
        ImageManipUtils.binaryThreshold(result);

        List<Rect> boundingRects = mBlobExtract.getBoundingRects(result);
        Queue<Mat> listmats = mBlobExtract.findCleanNumbers(result,
                boundingRects);
        Mat rectMat = mBlobExtract.drawRectsToMat(result, boundingRects);

        boolean[][] containNums = findNumTiles(rectMat, boundingRects);
        int containCount = 0;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (containNums[i][j]) {
                    containCount++;
                }
            }
        }
        Log.d("assert count", "containCount: " + containCount + ", listMats: "
                + listmats.size());
        if (containCount != listmats.size()) {
            error = true;
            return null;
        }

        int[][] grid = storeNumsToGrid(containNums, listmats);
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                Log.d("Grid", i + "," + j + ": " + grid[i][j] + "");
            }
        }
        return grid;
    }

    public Mat extractSudokuGrid(Mat mat) {
        Mat resultImage = new Mat();
        resultImage = ImageManipUtils.convertToGrayScale(mat);
        resultImage = ImageManipUtils.applyGausianBlur(resultImage);
        resultImage = ImageManipUtils.adaptiveThreshold(resultImage);
        resultImage = ImageManipUtils.applyBitwiseNot(resultImage);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(resultImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat warpedImage = new Mat();
        int biggestPolygonIndex = getBiggestPolygonIndex(contours);
        if (biggestPolygonIndex != 0) {
            final MatOfPoint biggest = contours.get(biggestPolygonIndex);
            List<Point> corners = getCornersFromPoints(biggest.toList());

            warpedImage = ImageManipUtils.fixPerspective(corners.get(0), corners.get(1), corners.get(2), corners.get(3), resultImage);
        }
        return warpedImage;
    }

    /**
     * returns smaller mat based on bounds
     *
     * @param mat
     *            source mat
     * @param bounds
     *            array: [0]=left, [1]=right, [2]=top, [3]=bottom
     * @return smaller subMat according to bounds
     */
    public static Mat subMat(Mat mat, int[] bounds) {
        int left = bounds[0];
        int right = bounds[1];
        int top = bounds[2];
        int bot = bounds[3];

        return mat.submat(top, bot, left, right);
    }

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

    public static int getBiggestPolygonIndex(List<MatOfPoint> contours) {
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

    private static List<Point> getCornersFromPoints(final List<Point> points) {
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

    /**
     * finds which tile contains a number and which doesn't
     *
     * @param m
     *            source mat image
     * @param rects
     *            List of Rects indicating where the numbers are located
     * @return grid array indicating which tiles are empty; true == contains
     *         number, false == empty
     */
    private boolean[][] findNumTiles(Mat m, List<Rect> rects) {
        byte[][] arrayMat = addRectsToMat(m, rects);
        boolean[][] numTileArray = new boolean[9][9];

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                numTileArray[i][j] = containsNumberTile(arrayMat, j, i);
            }
        }
        return numTileArray;
    }

    /**
     * determines if array holding mat contains a number
     *
     * @param matarray
     *            array containing pixel info for mat
     * @param xBound
     *            from 0 to 8 the number of the tile
     * @param yBound
     *            from 0 to 8 the number of the tile
     * @return true if empty, false otherwise
     */
    private boolean containsNumberTile(byte[][] matarray, int xBound, int yBound) {
        int area = matarray.length * matarray[0].length;
        int totalWhite = 0;
        int xStart = xBound * matarray[0].length / 9;
        int xEnd = xStart + matarray[0].length / 9 - 5;
        int yStart = yBound * matarray.length / 9;
        int yEnd = yStart + matarray.length / 9 - 5;

        for (int y = yStart; y < yEnd; y++) {
            for (int x = xStart; x < xEnd; x++) {
                if (matarray[y][x] == 1) {
                    totalWhite++;
                }
            }
        }
        if (totalWhite > 0 * area) {
            return true;
        } else {
            return false;
        }
    }

    private byte[][] addRectsToMat(Mat m, List<Rect> nums) {
        byte[][] matArray = new byte[m.rows()][m.cols()];

        for (Rect r : nums) {
            for (int y = r.y; y < r.y + r.height - 1; y++) {
                for (int x = r.x; x < r.x + r.width - 1; x++) {
                    // set to 1 (white)
                    matArray[y][x] = 1;
                }
            }
        }
        return matArray;
    }

    /**
     * uses OCR to find the number in tile and stores results in 2D array
     *
     * @param tileContainNum
     *            grid array indicating which tiles contains numbers
     * @param nums
     *            queue of Mats containing each individual number
     * @return grid array representing sudoku puzzle (empty == 0)
     */
    public int[][] storeNumsToGrid(boolean[][] tileContainNum, Queue<Mat> nums) {
        int count = 0;
        int[][] grid = new int[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                Log.d("nums queue count", nums.size() + "");
                if (tileContainNum[i][j]) {
                    grid[i][j] = getOCRNum(nums.remove(), count);
                    count++;
                }
            }

        }
        if (!mOCR.isEnded()) {
            mOCR.endTessOCR();
        }
        return grid;
    }

    /**
     * uses tessOCR to recognize the digit in the Mat
     *
     * @param num
     *            Mat containing image of the digit
     * @param count
     *            used for debugging/logging purposes
     * @return recognized integer
     */
    private int getOCRNum(Mat num, int count) {
        if (!mOCR.isInit()) {
            mOCR.initOCR();
        }
        Bitmap b = ImageManipUtils.convertMatToBitmap(num);
        //FileSaver.storeImage(b, count + "");
        int ans = Integer.parseInt(mOCR.doOCR(b));
        if (ans > 9) {
            ans = trimNum(ans);
        }
        Log.d("num", count + ": " + ans);
        return ans;
    }

    /**
     * safety method that trims integer to single digit
     *
     * @param n
     * @return
     */
    private int trimNum(int n) {
        while (n > 9) {
            n = n / 10;
        }
        return n;
    }
}

class SudokuGridResponse {
    private final int[][] grid;
    private final Mat image;

    public SudokuGridResponse(int[][] grid, Mat result) {
        this.grid = grid;
        this.image = result;
    }

    public int[][] getGrid() {
        return grid;
    }

    public Mat getResultImage() {
        return image;
    }

    public String toJsonString() {
        GsonBuilder builder = new GsonBuilder();
        return builder.create().toJson(this);
    }
}
