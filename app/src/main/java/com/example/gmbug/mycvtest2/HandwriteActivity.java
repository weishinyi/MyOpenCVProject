package com.example.gmbug.mycvtest2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class HandwriteActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    //region ---------------------- variables ----------------------------
    private String TAG = "MyCvTest2";

    private CameraBridgeViewBase mOpenCvCameraView_hw;

    Mat rgbaImg;
    Mat grayImg;

    //color Scalar (r,g,b)
    private static final Scalar RECT_COLOR_GREEN = new Scalar(0, 255, 0, 255); //green color scalar
    private static final Scalar RECT_COLOR_PURPLE = new Scalar(255, 0, 255, 0); //purple color scalar
    private static final Scalar RECT_COLOR_RED = new Scalar(255, 0, 0, 0); // red color scalar
    private static final Scalar RECT_COLOR_BLUE = new Scalar(0, 0 ,255, 0); // blue color scalar

    //screen
    private Point screenCenter;
    private int screenWidth;
    private int screenHeight;

    //button
    private byte modeCode = 2;
    private int btnWidth = 100;
    private int btnHeight = 100;
    private int btnPadding = 10;
    private int btnThickness = 2;
    private Rect btntlRect;
    private Rect btnkyRect;
    private Rect btnhwRect;

    //counters
    private int frameCounter = 0;

    //endregion

    //region ---------------------- basic functions ----------------------------

    private BaseLoaderCallback myBaseLoaderCallback_hw = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case  LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "[hw] OpenCV loaded successfully!");

                    mOpenCvCameraView_hw.enableView();

                    //get screenCenter
                    screenWidth = mOpenCvCameraView_hw.getWidth();
                    screenHeight = mOpenCvCameraView_hw.getHeight();
                    screenCenter = new Point(screenWidth/2, screenHeight/2);

                    //set btns Rect & center
                    int x0 = screenWidth-3*btnWidth-btnPadding;
                    btntlRect = new Rect(x0, btnPadding, btnWidth, btnHeight);
                    btnkyRect = new Rect(x0+btnWidth, btnPadding, btnWidth, btnHeight);
                    btnhwRect = new Rect(x0+2*btnWidth, btnPadding, btnWidth, btnHeight);


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
        Log.i(TAG, "[hw] onCreate!");
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
        Log.i(TAG, "[hw] onResume!");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, myBaseLoaderCallback_hw);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "[hw] onPause!");
        super.onPause();
        if(mOpenCvCameraView_hw!=null)
            mOpenCvCameraView_hw.disableView();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "[hw] onDestroy!");
        super.onDestroy();
        if(mOpenCvCameraView_hw!=null)
            mOpenCvCameraView_hw.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "[hw] onCameraViewStarted!");
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "[hw] onCameraViewStopped!");
    }

    //endregion

    //---------------------- main function ----------------------------
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Log.i(TAG, "[hw]onCameraFrame!");

        rgbaImg = inputFrame.rgba();
        grayImg = inputFrame.gray();

        //region ------ step0: draw tl/ky/hw btn ------
        try{
            Imgproc.rectangle(rgbaImg, btntlRect.tl(), btntlRect.br(), (modeCode == 0) ? RECT_COLOR_RED : RECT_COLOR_BLUE, btnThickness);
            putTextAtCenter(rgbaImg, btntlRect, "TL", (modeCode == 0) ? RECT_COLOR_RED : RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, btnThickness);

            Imgproc.rectangle(rgbaImg, btnkyRect.tl(), btnkyRect.br(), (modeCode == 1) ? RECT_COLOR_RED : RECT_COLOR_BLUE, btnThickness);
            putTextAtCenter(rgbaImg, btnkyRect, "KY", (modeCode == 1) ? RECT_COLOR_RED : RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, btnThickness);

            Imgproc.rectangle(rgbaImg, btnhwRect.tl(), btnhwRect.br(), (modeCode == 2) ? RECT_COLOR_RED : RECT_COLOR_BLUE, btnThickness);
            putTextAtCenter(rgbaImg, btnhwRect, "HW", (modeCode == 2) ? RECT_COLOR_RED : RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, btnThickness);

        }catch (Exception e){
            Log.e(TAG,"[hw] step0: draw tl/ky/hw btn. " + e.getMessage());
        }


        //endregion

        //region ------ step1: palm detection ------

        //endregion

        //region ------ step2: fingertip detection ------
        //endregion

        //region ------ step3: touch detection ------
        //endregion

        return rgbaImg;
    }

    //region ---------------------- functions ----------------------------

    //region--- put text at center ---
    private void putTextAtCenter(Mat img, Rect roi,String text, Scalar color, int fontFace, double fontScale, int thickness)
    {
        try{
            int[] baseline = new int[1];
            baseline[0] = 0;

            // Calculates the width and height of a text string
            Size textSize = Imgproc.getTextSize(text, fontFace, fontScale,thickness, baseline);

            // Calculates the center of roi
            Point center = new Point(roi.tl().x+(roi.width/2), roi.tl().y+(roi.height/2));

            // Calculates the position to put text (Bottom-left corner of the text string)
            Point org = new Point(center.x-(textSize.width/2), center.y+(textSize.height/2));

            //put text on the roi of img
            Imgproc.putText(img, text, org, fontFace, fontScale, color, thickness);

        }catch (Exception e){
            Log.e(TAG, "[hw-putTextAtCenter]: put text ERROR! "+e.getMessage());
        }
    }
    //endregion

    //endregion
}
