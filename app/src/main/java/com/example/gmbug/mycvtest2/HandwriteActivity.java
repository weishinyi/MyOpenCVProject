package com.example.gmbug.mycvtest2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.example.gmbug.mycvtest2.entity.GlobalVariable;
import com.example.gmbug.mycvtest2.entity.util;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.ArrayList;
import java.util.List;

public class HandwriteActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    //region ---------------------- variables ----------------------------
    private CameraBridgeViewBase mOpenCvCameraView_hw;

    private Mat rgbaImg;
    private Mat grayImg;

    //handwrite area
    private Rect handwriteArea;
    private int handwriteAreaWidth = 300;
    private int handwriteAreaThickness = 3;

    //counters
    private int frameCounter = 0;
    private int nonCounter = 0;

    //trace list
    private List<Point> traceList = new ArrayList<Point>();

    //endregion

    //region ---------------------- basic functions ----------------------------

    private BaseLoaderCallback myBaseLoaderCallback_hw = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case  LoaderCallbackInterface.SUCCESS:
                    Log.i(util.TAG, "[hw] OpenCV loaded successfully!");

                    mOpenCvCameraView_hw.enableView();

                    //set handwriteArea Rect
                    GlobalVariable globalValue = ((GlobalVariable)getApplicationContext());
                    handwriteArea = new Rect((int)(globalValue.getScreenCenter().x-(handwriteAreaWidth/2)), (int)(globalValue.getScreenCenter().y-(handwriteAreaWidth/2)), handwriteAreaWidth, handwriteAreaWidth);

                    //initialize counter
                    frameCounter = 0;
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(util.TAG, "[hw] onCreate!");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_handwrite);

        mOpenCvCameraView_hw = (CameraBridgeViewBase)findViewById(R.id.cameraView_hw);
        if(mOpenCvCameraView_hw!=null)
        {
            mOpenCvCameraView_hw.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView_hw.setCvCameraViewListener(HandwriteActivity.this);
        }
    }

    @Override
    protected void onResume() {
        Log.i(util.TAG, "[hw] onResume!");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, myBaseLoaderCallback_hw);
    }

    @Override
    protected void onPause() {
        Log.i(util.TAG, "[hw] onPause!");
        super.onPause();
        if(mOpenCvCameraView_hw!=null)
            mOpenCvCameraView_hw.disableView();
    }

    @Override
    protected void onDestroy() {
        Log.i(util.TAG, "[hw] onDestroy!");
        super.onDestroy();
        if(mOpenCvCameraView_hw!=null)
            mOpenCvCameraView_hw.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(util.TAG, "[hw] onCameraViewStarted!");
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(util.TAG, "[hw] onCameraViewStopped!");
    }

    //endregion

    //---------------------- main function ----------------------------
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.i(util.TAG, "[hw]onCameraFrame!");

        GlobalVariable globalVariable = ((GlobalVariable)getApplicationContext());

        rgbaImg = inputFrame.rgba();
        grayImg = inputFrame.gray();

        //region ------ step0: draw tl/ky/hw btn ------
        try{
            Imgproc.rectangle(rgbaImg, globalVariable.getBtntlRect().tl(), globalVariable.getBtntlRect().br(), util.RECT_COLOR_BLUE, globalVariable.getBtnThickness());
            Imgproc.rectangle(rgbaImg, globalVariable.getBtnkyRect().tl(), globalVariable.getBtnkyRect().br(), util.RECT_COLOR_BLUE, globalVariable.getBtnThickness());
            Imgproc.rectangle(rgbaImg, globalVariable.getBtnhwRect().tl(), globalVariable.getBtnhwRect().br(), util.RECT_COLOR_RED, globalVariable.getBtnThickness());

            putTextAtCenter(rgbaImg, globalVariable.getBtntlRect(), "TL", util.RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, globalVariable.getBtnThickness());
            putTextAtCenter(rgbaImg, globalVariable.getBtnkyRect(), "KY", util.RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, globalVariable.getBtnThickness());
            putTextAtCenter(rgbaImg, globalVariable.getBtnhwRect(), "HW", util.RECT_COLOR_RED, Core.FONT_HERSHEY_DUPLEX, 2.0f, globalVariable.getBtnThickness());

        }catch (Exception e){
            Log.e(util.TAG,"[hw] step0: draw tl/ky/hw btn. " + e.getMessage());
        }

        //endregion

        //region ------ step1: draw the handwrite Area ------
        try{
            Imgproc.rectangle(rgbaImg, handwriteArea.tl(), handwriteArea.br(), util.RECT_COLOR_GREEN, handwriteAreaThickness);
        }catch (Exception e){
            Log.e(util.TAG, "[hw] step1: draw the handwrite Area. " + e.getMessage());
        }

        //endregion

        //region ------ step2: fingertip detection ------
        Rect fingertipRect = fingertipDetect(rgbaImg);
        Point center = null;
        if(fingertipRect != null)
        {
            //show the rectangle surrounding fingertip
            Imgproc.rectangle(rgbaImg, fingertipRect.tl(), fingertipRect.br(), util.RECT_COLOR_PURPLE, 5);

            //show the center of fingertip
            center = new Point(fingertipRect.tl().x+(fingertipRect.width/2) , fingertipRect.tl().y+(fingertipRect.height/2));
            Imgproc.circle(rgbaImg, center,2, util.RECT_COLOR_BLUE,-1);
            //String str = "(" + center.x + "," + center.y + ")";
            //Imgproc.putText(rgbaImg, str, center, Core.FONT_HERSHEY_SIMPLEX, 2.6f, RECT_COLOR_BLUE, 3);
        }

        //endregion

        //region ------ step3: touch detection ------
        if(fingertipRect != null)
        {
            Point fingertipCenter = new Point(fingertipRect.tl().x+(fingertipRect.width/2) , fingertipRect.tl().y+(fingertipRect.height/2));
            if(globalVariable.getBtntlRect().contains(fingertipCenter)){
                Intent intent = new Intent();
                intent.setClass(HandwriteActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }else if(globalVariable.getBtnkyRect().contains(fingertipCenter)){
                Intent intent = new Intent();
                intent.setClass(HandwriteActivity.this, KeyboardActivity.class);
                startActivity(intent);
                finish();
            }
        }
        //endregion

        //region --- step4: trace the fingertip ---
        if(fingertipRect != null)
        {
            if(handwriteArea.contains(center))
            {
                traceList.add(center);
            }
        } else {
            nonCounter++;
            if(nonCounter==10)
            {
                nonCounter = 0;
                traceList = new ArrayList<Point>();
            }
        }

        //show the trajectory
        if(traceList.size()>1)
        {
            for(int i=1; i<traceList.size(); i++)
            {
                Imgproc.line(rgbaImg, traceList.get(i), traceList.get(i-1), util.RECT_COLOR_BLUE, 3);
            }
        }
        //endregion


        return rgbaImg;
    }

    //region ---------------------- functions ----------------------------

    //region--- put text at center ---
    private void putTextAtCenter(Mat img, Rect roi,String text, Scalar color, int fontFace, double fontScale, int thickness)
    {
        try{
            // Calculates the width and height of a text string
            Size textSize = Imgproc.getTextSize(text, fontFace, fontScale,thickness, null);

            // Calculates the center of roi
            Point center = new Point(roi.tl().x+(roi.width/2), roi.tl().y+(roi.height/2));

            // Calculates the position to put text (Bottom-left corner of the text string)
            Point org = new Point(center.x-(textSize.width/2), center.y+(textSize.height/2));

            //put text on the roi of img
            Imgproc.putText(img, text, org, fontFace, fontScale, color, thickness);

        }catch (Exception e){
            Log.e(util.TAG, "[hw-putTextAtCenter]: put text ERROR! "+e.getMessage());
        }
    }
    //endregion

    //region --- fingertip detection ---
    private Rect fingertipDetect(Mat inFrame)
    {
        Rect fingertip = null;

        try{
            //fingertip color detect
            Mat imgFingertip = fingertipColorDetect(inFrame);

            //find all contours
            ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(imgFingertip, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

            //find the largest contour
            MatOfPoint largestContour = findLargestAreaContour(contours);

            //conditions
            if(largestContour != null)
            {
                Rect rectBound = Imgproc.boundingRect(largestContour); // Get bounding rect of contour
                // conditions 1:Aspect ratio (h,w) range in (100,100) ~ (10,10)
                if(rectBound.height>10 && rectBound.width>10  && rectBound.height<100 && rectBound.width<100)
                {
                    fingertip = new Rect(rectBound.tl(), rectBound.br());
                }
            }

        }catch (Exception e){
            Log.e(util.TAG,"[hw-fingertipDetect]: "+e.getMessage());
        }

        return fingertip;
    }

    private Mat fingertipColorDetect(Mat rgbImage)
    {
        Mat resultImg = new Mat();

        try {
            Mat hvsImg = new Mat();
            Imgproc.cvtColor(rgbImage, hvsImg, Imgproc.COLOR_RGB2HSV_FULL);
            Core.inRange(hvsImg, util.fingertipLower, util.fingertipUpper, resultImg);

            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
            Imgproc.dilate(resultImg, resultImg, kernel); //膨脹

        }catch (Exception e){
            Log.e(util.TAG, e.getMessage());
        }

        return  resultImg;
    }

    // find the largest area Contour
    private MatOfPoint findLargestAreaContour(ArrayList<MatOfPoint> contours)
    {
        MatOfPoint largestAreaContour = null;

        try {
            //extract the largest area contour
            double maxVal = 0;
            int maxValIdx = 0;
            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
                double contourArea = Imgproc.contourArea(contours.get(contourIdx));
                if (maxVal < contourArea) {
                    maxVal = contourArea;
                    maxValIdx = contourIdx;
                }
            }
            largestAreaContour = new MatOfPoint(contours.get(maxValIdx));

        }catch (Exception e){
            Log.e(util.TAG, e.getMessage());
        }

        return largestAreaContour;
    }

    //endregion

    //endregion
}
