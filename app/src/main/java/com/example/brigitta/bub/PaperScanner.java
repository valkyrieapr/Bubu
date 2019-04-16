package com.example.brigitta.bub;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.constraint.solver.widgets.Rectangle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import junit.framework.Test;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PaperScanner extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    private boolean runScanner = false, paperFound = false;
    CameraBridgeViewBase cameraBridgeViewBase;
    private Button btnStart;
    Mat mRgba, mGray, mGaussianBlur, mCanny, hierarchy;
    String studentID;
    List<MatOfPoint> contours;
    List<Point> curves;
    MatOfPoint2f maxCurve, approxCurve, src;
    double maxArea;

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "PAPERSCANNER >>>> OpenCV loaded successfully.");
                    cameraBridgeViewBase.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_paper_scanner);

        btnStart = (Button) findViewById(R.id.btn_str);
        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.show_camera_activity_java_surface_view);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        //cameraBridgeViewBase.setMaxFrameSize(800, 600);
        cameraBridgeViewBase.setMaxFrameSize(1280, 720);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        Intent studentIntent = getIntent();
        studentID = studentIntent.getExtras().getString(Configuration.STD_ID);

        System.out.println("PAPERSCANNER >>>> Student ID:" + studentID);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (maxArea == 0) {
                        System.out.println("PAPERSCANNER >>>> Answer sheet is not detected.");
                    } else {
                        runScanner = true;
                    }
                } catch (Exception e) {
                    System.out.println("PAPERSCANNER >>>> Error " + e.getMessage());
                }
            }
        });
    }

    // This method is invoked when target activity return result data back.
    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                studentIDback = dataIntent.getExtras().getString(Configuration.STD_ID);
                System.out.println("PAPERSCANNER >>>> STRINGGG: " + studentID);
            }
        }
    }*/

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null) ;
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase != null) ;
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "PAPERSCANNER >>>> Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "PAPERSCANNER >>>> OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        hierarchy = new Mat();
        mGray = new Mat();
        mGaussianBlur = new Mat();
        mCanny = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        hierarchy.release();
        mGray.release();
        mGaussianBlur.release();
        mCanny.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mRgba = inputFrame.rgba();
        //Imgproc.putText(mRgba, studentID, new Point(10, 30),Core.FONT_HERSHEY_PLAIN,1, new Scalar(0, 255, 0), 2);

        contours = new ArrayList<MatOfPoint>();

        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(mGray, mGaussianBlur, new Size(5, 5), 0);
        Imgproc.Canny(mGaussianBlur, mCanny, 75, 200);
        Imgproc.findContours(mCanny, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        maxArea = 0;
        int maxAreaIdx = 0;
        approxCurve = new MatOfPoint2f();
        maxCurve = new MatOfPoint2f();

        for (MatOfPoint contour : contours) {
            double contourArea = Imgproc.contourArea(contour);

            //Page area must be bigger than maxArea
            if (contourArea >= maxArea) {
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                double contourSize = Imgproc.arcLength(contour2f, true);
                Imgproc.approxPolyDP(contour2f, approxCurve, contourSize * 0.02, true);

                //Page has 4 corners and it is convex
                if (approxCurve.total() == 4) {
                    double maxCosine = 0;
                    curves = approxCurve.toList();

                    for (int j = 2; j < 5; j++) {
                        double cosine = Math.abs(angle(curves.get(j % 4), curves.get(j - 2), curves.get(j - 1)));
                        maxCosine = Math.max(maxCosine, cosine);
                    }

                    if (maxCosine < 0.3) {
                        maxCurve = approxCurve;
                        maxArea = contourArea;
                        maxAreaIdx = contours.indexOf(contour);
                        Imgproc.drawContours(mRgba, contours, maxAreaIdx, new Scalar(8, 255, 0), 2); //will draw the largest square/rectangle
                    }
                }
            }
        }

        if (runScanner) {
            Point[] sortedPoints = new Point[4];
            Moments moment = Imgproc.moments(maxCurve);
            int x = (int) (moment.get_m10() / moment.get_m00());
            int y = (int) (moment.get_m01() / moment.get_m00());

            double[] data;
            int count = 0;
            for (int i = 0; i < maxCurve.rows(); i++) {
                data = maxCurve.get(i, 0);
                double datax = data[0];
                double datay = data[1];
                if (datax < x && datay > y) { //top-left
                    sortedPoints[0] = new Point(datax, datay);
                    count++;
                } else if (datax > x && datay > y) { //top-right
                    sortedPoints[1] = new Point(datax, datay);
                    count++;
                } else if (datax > x && datay < y) { //bottom-right
                    sortedPoints[2] = new Point(datax, datay);
                    count++;
                } else if (datax < x && datay < y) { //bottom-left
                    sortedPoints[3] = new Point(datax, datay);
                    count++;
                }
            }

            src = new MatOfPoint2f(sortedPoints[0], sortedPoints[1], sortedPoints[2], sortedPoints[3]);

            contours.clear();
            hierarchy.release();
            mGray.release();
            mGaussianBlur.release();
            mCanny.release();

            Mat AnswerSheet = paperWarp(mRgba, src);
            long AnswerSheetCopy = AnswerSheet.getNativeObjAddr();

            Intent intent = new Intent(getApplicationContext(), AnswerScanner.class);
            intent.putExtra("AnswerSheet", AnswerSheetCopy);
            intent.putExtra(Configuration.STD_ID, studentID);
            startActivity(intent);
        }
        System.gc();
        return mRgba;
    }

    public static Mat paperWarp(Mat mRgba, Mat src) {
        int resultWidth = 840;
        int resultHeight = 1188;

        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC4);

        List<Point> dest = new ArrayList<Point>();
        dest.add(new Point(0, 0));
        dest.add(new Point(0, resultHeight));
        dest.add(new Point(resultWidth, resultHeight));
        dest.add(new Point(resultWidth, 0));
        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(src, endM);

        Imgproc.warpPerspective(mRgba, outputMat, perspectiveTransform, new Size(resultWidth, resultHeight), Imgproc.INTER_CUBIC);

        return outputMat;
    }

    //helper method
    private static double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    @Override
    public void onBackPressed() {
        Intent backIntent = new Intent(PaperScanner.this, MainActivity.class);
        backIntent.putExtra(Configuration.STD_ID, studentID);
        startActivity(backIntent);
        finish();
    }
}