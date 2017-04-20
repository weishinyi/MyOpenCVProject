package com.example.gmbug.mycvtest2;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.example.gmbug.mycvtest2.entity.GlobalVariable;
import com.example.gmbug.mycvtest2.entity.util;

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
    private CameraBridgeViewBase mOpenCvCameraView_ky;
    private CascadeClassifier cascadeClassifier_palm; //recognize palm to show the keyboard
    private int absoluteObjectSize = 0;

    private Mat rgbaImg;
    private Mat grayImg;
    private Mat bgImg;

    //counters
    private int frameCounter = 0;
    private int nonPalmCounter = 0;

    //flags
    //private List<Boolean> palmFlags = new ArrayList<Boolean>();
    Boolean palmFlag = false;

    //endregion

    //region ---------------------- basic functions ----------------------------

    private BaseLoaderCallback myBaseLoaderCallback_ky = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);

            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(util.TAG, "[ky] OpenCV loaded successfully!");

                    //initialize CascadeClassifier_palm
                    try{
                        cascadeClassifier_palm = generalizationInitializeCascadeClassifier(R.raw.hand_cascade,"hand_cascade.xml");
                        Log.i(util.TAG, "[ky]BaseLoaderCallback: cascadeClassifier_object initialize success.");
                    }catch(Exception e){
                        Log.e(util.TAG, "[ky]BaseLoaderCallback: cascadeClassifier_object initialize fail.");
                    }

                    mOpenCvCameraView_ky.enableView();

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
        Log.i(util.TAG, "[ky] onCreate!");
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
        Log.i(util.TAG, "[ky] onResume!");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, myBaseLoaderCallback_ky);
    }

    @Override
    protected void onPause() {
        Log.i(util.TAG, "[ky] onPause!");
        super.onPause();
        if(mOpenCvCameraView_ky!=null)
            mOpenCvCameraView_ky.disableView();
    }

    @Override
    protected void onDestroy() {
        Log.i(util.TAG, "[ky] onDestroy!");
        super.onDestroy();
        if(mOpenCvCameraView_ky!=null)
            mOpenCvCameraView_ky.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(util.TAG, "[ky] onCameraViewStarted!");

        //set the size of detection object
        setAbsoluteObjectSize(height);
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(util.TAG, "[ky] onCameraViewStopped!");
    }

    //endregion

    //---------------------- main function ----------------------------
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.i(util.TAG, "[ky]onCameraFrame!");

        GlobalVariable globalVariable = ((GlobalVariable)getApplicationContext());

        rgbaImg = inputFrame.rgba();
        grayImg = inputFrame.gray();

        //region ------ step0: draw tl/ky/hw btn ------
        try{
            Imgproc.rectangle(rgbaImg, globalVariable.getBtntlRect().tl(), globalVariable.getBtntlRect().br(), util.RECT_COLOR_BLUE, globalVariable.getBtnThickness());
            Imgproc.rectangle(rgbaImg, globalVariable.getBtnkyRect().tl(), globalVariable.getBtnkyRect().br(), util.RECT_COLOR_RED, globalVariable.getBtnThickness());
            Imgproc.rectangle(rgbaImg, globalVariable.getBtnhwRect().tl(), globalVariable.getBtnhwRect().br(), util.RECT_COLOR_BLUE, globalVariable.getBtnThickness());

            putTextAtCenter(rgbaImg, globalVariable.getBtntlRect(), "TL", util.RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, globalVariable.getBtnThickness());
            putTextAtCenter(rgbaImg, globalVariable.getBtnkyRect(), "KY", util.RECT_COLOR_RED, Core.FONT_HERSHEY_DUPLEX, 2.0f, globalVariable.getBtnThickness());
            putTextAtCenter(rgbaImg, globalVariable.getBtnhwRect(), "HW", util.RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, globalVariable.getBtnThickness());

        }catch (Exception e){
            Log.e(util.TAG,"[ky] step0: draw tl/ky/hw btn. " + e.getMessage());
        }
        //endregion

        //region ------ step1: palm detection ------
        MatOfRect palms = detectObjects();
        Rect[] palmsArray = palms.toArray();
        /*for (int i = 0; i <palmsArray.length; i++) {
            Imgproc.rectangle(rgbaImg, palmsArray[i].tl(), palmsArray[i].br(), util.RECT_COLOR_RED, 3);
        }*/

        //set palmFlags & nonPalmCounter
        if(palmsArray.length>0){
            palmFlag = true;
            nonPalmCounter = 0;
        }else{
            palmFlag = false;
            nonPalmCounter++;
        }

        //set bgImg
        if(palmFlag && bgImg==null)
        {
            bgImg = inputFrame.rgba().clone();
        }
        if(nonPalmCounter>10 && bgImg!=null)
        {
            bgImg = null;
        }
        if(bgImg != null)
        {
            Imgproc.circle(rgbaImg, globalVariable.getScreenCenter(),2, util.RECT_COLOR_BLUE,-1);
        }
        /*if(palmFlag && bgImg==null){
            bgImg = inputFrame.rgba().clone();
            Imgproc.putText(rgbaImg,"get gbImg!!", globalVariable.getScreenCenter(), Core.FONT_HERSHEY_SIMPLEX, 2.6f, util.RECT_COLOR_RED,3);
        }
        if(!palmFlag && bgImg!=null){
            bgImg = null;
            Imgproc.putText(rgbaImg,"clear gbImg!!", globalVariable.getScreenCenter(), Core.FONT_HERSHEY_SIMPLEX, 2.6f, util.RECT_COLOR_GREEN,3);
        }*/
        //endregion

        //region ------ step2: create the keyboard & step3: locate the trigger point ------
        if(palmFlag)
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

                Log.i(util.TAG, "[ky-onCameraFrame]: show keyboardImg success!");
                //endregion

                //region part2: local the trigger point
                //...
                //endregion
            }
        }
        //endregion

        //region ------ step4: fingertip detection ------
        Rect fingertipRect = fingertipDetect(rgbaImg);
        if(fingertipRect != null)
        {
            Imgproc.rectangle(rgbaImg, fingertipRect.tl(), fingertipRect.br(), util.RECT_COLOR_PURPLE, 5);
        }
        //endregion

        //region ------ step5: touch detection ------
        if(fingertipRect != null)
        {
            //region --- touch btn ---
            Point fingertipCenter = new Point(fingertipRect.tl().x+(fingertipRect.width/2) , fingertipRect.tl().y+(fingertipRect.height/2));
            if(globalVariable.getBtntlRect().contains(fingertipCenter)){
                Intent intent = new Intent();
                intent.setClass(KeyboardActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }else if(globalVariable.getBtnhwRect().contains(fingertipCenter)){
                Intent intent = new Intent();
                intent.setClass(KeyboardActivity.this, HandwriteActivity.class);
                startActivity(intent);
                finish();
            }
            //endregion

        }
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
                Log.i(util.TAG,"[Initialize Cascade Classifier]: Failed to load cascade classifier");
                cascadeClassifier = null;
            }else{
                Log.i(util.TAG,"[Initialize Cascade Classifier]: Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }

        }catch (Exception e) {
            Log.e(util.TAG, e.getMessage());
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

            //Log.i(util.TAG, "[ky-detectObjects]: using classifier to detect palm success!");
        }catch(Exception e){
            Log.e(util.TAG, "[ky-detectObjects]: using classifier to detect palm ERROR!");
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

            Log.i(util.TAG, "[ky-getDisplayImage]: get display image success!");
        }catch(Exception e){
            Log.e(util.TAG, "[ky-getDisplayImage]: get displayImage ERROR! "+e.getMessage());
        }

        //resize the displayImage
        try{
            if(!displayImage.empty())
            {
                Imgproc.resize(displayImage,resizeDisplayImage,size);
                Log.i(util.TAG, "[ky-getDisplayImage]: resize success!");
            }
        }catch (Exception e){
            Log.e(util.TAG, "[ky-getDisplayImage]: resize ERROR! "+e.getMessage());
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
            Log.e(util.TAG, "[ky-putTextAtCenter]: put text ERROR! "+e.getMessage());
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
