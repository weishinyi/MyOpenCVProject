package com.example.gmbug.mycvtest2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class KeyboardActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    //region ---------------------- variables ----------------------------
    private String TAG = "MyCvTest2";

    private CameraBridgeViewBase mOpenCvCameraView_ky;
    private CascadeClassifier cascadeClassifier_palm; //recognize palm to show the keyboard
    private int absoluteObjectSize = 0;

    Mat rgbaImg;
    Mat grayImg;
    Mat bgImg;

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
    private byte modeCode = 1;
    private int btnWidth = 100;
    private int btnHeight = 100;
    private int btnPadding = 10;
    private int btnThickness = 2;
    private Rect btntlRect;
    private Rect btnkyRect;
    private Rect btnhwRect;

    //fingertip color detection H,S,V range
    private Scalar fingertipLower = new Scalar(240, 50, 178);
    private Scalar fingertipUpper = new Scalar(255, 80, 255);

    //counters
    private int frameCounter = 0;

    //flags
    private List<Boolean> palmFlags = new ArrayList<Boolean>();

    //endregion

    //region ---------------------- basic functions ----------------------------

    private BaseLoaderCallback myBaseLoaderCallback_ky = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);

            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "[ky] OpenCV loaded successfully!");

                    //initialize CascadeClassifier_palm
                    try{
                        cascadeClassifier_palm = generalizationInitializeCascadeClassifier(R.raw.hand_cascade,"hand_cascade.xml");
                        Log.i(TAG, "[ky]BaseLoaderCallback: cascadeClassifier_object initialize success.");
                    }catch(Exception e){
                        Log.e(TAG, "[ky]BaseLoaderCallback: cascadeClassifier_object initialize fail.");
                    }

                    mOpenCvCameraView_ky.enableView();

                    //get screenCenter
                    screenWidth = mOpenCvCameraView_ky.getWidth();
                    screenHeight = mOpenCvCameraView_ky.getHeight();
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
        Log.i(TAG, "[ky] onCreate!");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_keyboard);

        mOpenCvCameraView_ky = (CameraBridgeViewBase)findViewById(R.id.cameraView_ky);
        if(mOpenCvCameraView_ky!=null)
        {
            mOpenCvCameraView_ky.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView_ky.setCvCameraViewListener(KeyboardActivity.this);
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "[ky] onResume!");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, myBaseLoaderCallback_ky);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "[ky] onPause!");
        super.onPause();
        if(mOpenCvCameraView_ky!=null)
            mOpenCvCameraView_ky.disableView();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "[ky] onDestroy!");
        super.onDestroy();
        if(mOpenCvCameraView_ky!=null)
            mOpenCvCameraView_ky.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "[ky] onCameraViewStarted!");

        //set the size of detection object
        setAbsoluteObjectSize(height);
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "[ky] onCameraViewStopped!");
    }

    //endregion

    //---------------------- main function ----------------------------
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.i(TAG, "[ky]onCameraFrame!");

        rgbaImg = inputFrame.rgba();
        grayImg = inputFrame.gray();

        //region ------ step0: draw tl/ky/hw btn ------
        try{
            Imgproc.rectangle(rgbaImg, btntlRect.tl(), btntlRect.br(), RECT_COLOR_BLUE, btnThickness);
            Imgproc.rectangle(rgbaImg, btnkyRect.tl(), btnkyRect.br(), RECT_COLOR_RED, btnThickness);
            Imgproc.rectangle(rgbaImg, btnhwRect.tl(), btnhwRect.br(), RECT_COLOR_BLUE, btnThickness);

            putTextAtCenter(rgbaImg, btntlRect, "TL", RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, btnThickness);
            putTextAtCenter(rgbaImg, btnkyRect, "KY", RECT_COLOR_RED, Core.FONT_HERSHEY_DUPLEX, 2.0f, btnThickness);
            putTextAtCenter(rgbaImg, btnhwRect, "HW", RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, btnThickness);

        }catch (Exception e){
            Log.e(TAG,"[ky] step0: draw tl/ky/hw btn. " + e.getMessage());
        }
        //endregion

        //region ------ step1: palm detection ------
        Boolean currentPalmFlag = false;
        MatOfRect palms = detectObjects();
        Rect[] palmsArray = palms.toArray();
        for (int i = 0; i <palmsArray.length; i++) {
            Imgproc.rectangle(rgbaImg, palmsArray[i].tl(), palmsArray[i].br(), RECT_COLOR_RED, 3);
        }

        //set palmFlags
        if(palmsArray.length>0){
            currentPalmFlag = true;
        }
        if(palmFlags.size() < 3)
        {
            palmFlags.add(currentPalmFlag);
        }else{
            palmFlags.remove(0);
            palmFlags.add(currentPalmFlag);
        }

        //set bgImg
        if(currentPalmFlag && bgImg==null){
            bgImg = inputFrame.rgba().clone();
            Imgproc.putText(rgbaImg,"get gbImg!!", screenCenter, Core.FONT_HERSHEY_SIMPLEX, 2.6f, RECT_COLOR_RED,3);
        }
        if(!currentPalmFlag && bgImg!=null){
            bgImg = null;
            Imgproc.putText(rgbaImg,"clear gbImg!!", screenCenter, Core.FONT_HERSHEY_SIMPLEX, 2.6f, RECT_COLOR_GREEN,3);
        }
        //endregion

        //region ------ step2: create the keyboard & step3: locate the trigger point ------
        //region --- part1 keyboard

        if(currentPalmFlag)
        {
            //get keyboard image
            Mat keyboardImg =  getDisplayImage(R.drawable.numberkeypad2,200,300);
            if(!keyboardImg.empty())
            {
                //region part1: show keyboardImg
                //set the coordinate to show keyboardImg
                Rect palmRect = new Rect(palmsArray[0].tl(),palmsArray[0].br());
                Point palmC = new Point(palmRect.x+(palmRect.width/2), palmRect.y+(palmRect.height/2));
                int x = (int) palmC.x-100;
                int y = (int) palmC.y-150;

                //set the para of addWeighted function
                //p.s. addWeighted function: output = src1*alpha + src2*beta + gamma;
                double alpha = 0.8;
                double beta = 1;
                double gamma = 1;

                //show keyboardImg
                Rect keyboardImg_roi = new Rect(x, y, keyboardImg.width(), keyboardImg.height());
                Core.addWeighted(rgbaImg.submat(keyboardImg_roi), alpha, keyboardImg, beta, gamma, rgbaImg.submat(keyboardImg_roi));

                Log.i(TAG, "[ky-onCameraFrame]: show keyboardImg success!");
                //endregion

                //region part2: local the trigger point
                //...
                //endregion
            }
        }

        //endregion

        //endregion

        //region ------ step4: fingertip detection ------
        //endregion

        //region ------ step5: touch detection ------
        //endregion

        return rgbaImg;
    }


    //region ---------------------- functions ----------------------------

    //region --- CascadeClassifier about ---

    private CascadeClassifier generalizationInitializeCascadeClassifier(int xmlfileId, String xmlfileName)
    {
        CascadeClassifier cascadeClassifier = null;

        try{
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(xmlfileId);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, xmlfileName);
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while((bytesRead=is.read(buffer)) != -1)
            {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            //Load the CascadeClassifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if(cascadeClassifier.empty()){
                Log.i(TAG,"[Initialize Cascade Classifier]: Failed to load cascade classifier");
                cascadeClassifier = null;
            }else{
                Log.i(TAG,"[Initialize Cascade Classifier]: Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }

        }catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return cascadeClassifier;
    }

    private void setAbsoluteObjectSize(int height)
    {
        // The faces will be a 20% of the height of the screen
        absoluteObjectSize = (int)(height *0.2);
    }

    private  MatOfRect detectObjects()
    {
        MatOfRect objects = new MatOfRect();

        try{
            if(cascadeClassifier_palm != null)
            {
                cascadeClassifier_palm.detectMultiScale(grayImg,objects,1.1,2,2, new Size(absoluteObjectSize,absoluteObjectSize),new Size());
            }

            //Log.i(TAG, "[ky-detectObjects]: using classifier to detect palm success!");
        }catch(Exception e){
            Log.e(TAG, "[ky-detectObjects]: using classifier to detect palm ERROR!");
        }

        return objects;
    }

    //endregion

    //region --- get display image ---
    /** get the image that you want to show on the screen. */
    private Mat getDisplayImage(int drawableId,double resize_x, double resize_y)
    {
        Mat displayImage = new Mat();
        Mat resizeDisplayImage = new Mat();
        Size size = new Size(resize_x,resize_y);

        //get displayImage
        try{
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), drawableId);
            Utils.bitmapToMat(bmp, displayImage);

            Log.i(TAG, "[ky-getDisplayImage]: get display image success!");
        }catch(Exception e){
            Log.e(TAG, "[ky-getDisplayImage]: get displayImage ERROR! "+e.getMessage());
        }

        //resize the displayImage
        try{
            if(!displayImage.empty())
            {
                Imgproc.resize(displayImage,resizeDisplayImage,size);
                Log.i(TAG, "[ky-getDisplayImage]: resize success!");
            }
        }catch (Exception e){
            Log.e(TAG, "[ky-getDisplayImage]: resize ERROR! "+e.getMessage());
        }

        return resizeDisplayImage;
    }
    //endregion

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
            Log.e(TAG, "[ky-putTextAtCenter]: put text ERROR! "+e.getMessage());
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
                // conditions 1:Aspect ratio (h,w) range in (100,100) ~ (30,30)
                if(rectBound.height>30 && rectBound.width>30  && rectBound.height<100 && rectBound.width<100)
                {
                    fingertip = new Rect(rectBound.tl(), rectBound.br());
                }
            }

        }catch (Exception e){
            Log.e(TAG,"[hw-fingertipDetect]: "+e.getMessage());
        }

        return fingertip;
    }

    private Mat fingertipColorDetect(Mat rgbImage)
    {
        Mat resultImg = new Mat();

        try {
            Mat hvsImg = new Mat();
            Imgproc.cvtColor(rgbImage, hvsImg, Imgproc.COLOR_RGB2HSV_FULL);
            Core.inRange(hvsImg, fingertipLower, fingertipUpper, resultImg);

            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
            Imgproc.dilate(resultImg, resultImg, kernel); //膨脹

        }catch (Exception e){
            Log.e(TAG, e.getMessage());
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
            Log.e(TAG, e.getMessage());
        }

        return largestAreaContour;
    }

    //endregion

    //endregion

}
