package com.example.gmbug.mycvtest2;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {


    //region ---------------------- variables ----------------------------
    private String TAG = "MyCvTest2";

    private CameraBridgeViewBase mOpenCvCameraView;
    //private CascadeClassifier cascadeClassifier_palm; //recognize palm to show the keyboard
    //private int absoluteObjectSize = 0;

    //color Scalar (r,g,b)
    private static final Scalar RECT_COLOR_GREEN = new Scalar(0, 255, 0, 255); //green color scalar
    private static final Scalar RECT_COLOR_PURPLE = new Scalar(255, 0, 255, 0); //purple color scalar
    private static final Scalar RECT_COLOR_RED = new Scalar(255, 0, 0, 0); // red color scalar
    private static final Scalar RECT_COLOR_BLUE = new Scalar(0, 0 ,255, 0); // blue color scalar


    //Mat
    private Mat rgbaImg;
    private Mat grayImg;
    //private Mat bgImg = null;

    //private Rect fitArm = null;

    //screen
    private Point screenCenter;
    private int screenWidth;
    private int screenHeight;

    //button
    private byte modeCode = 0;
    private int btnWidth = 100;
    private int btnHeight = 100;
    private int btnPadding = 10;
    private int btnThickness = 2;
    private Rect btntlRect;
    private Rect btnkyRect;
    private Rect btnhwRect;

    //counters
    private int frameCounter = 0;
    private int preFrameCounter = -1;
    private int prePreFrameCounter = -2;

    //flags
    //ex: List<Boolean> list = ArrayList<Boolean> ();
    private List<Boolean> armFlagls = new ArrayList<Boolean>();
    //private List<Boolean> palmFlags = new ArrayList<Boolean>();

    //skin detection  H,S,V range
    /** maybe use
     *  double scaleSatLower = 0.28;
        double scaleSatUpper = 0.68;

        double scaleSatLower = 0.18; // maybe better
        double scaleSatLower = 0.08; // maybe too much
        double scaleSatUpper = 0.78;
     * */
    private Scalar skinLower = new Scalar(0, 0.18*255, 0);
    private Scalar skinUpper = new Scalar(25, 0.68*255, 255);

    //fingertip color detection H,S,V range
    private Scalar fingertipLower = new Scalar(240, 50, 178);
    private Scalar fingertipUpper = new Scalar(255, 80, 255);

    //trigger point array
    //Point[] kyTriggerPointArray = null;
    Point[] tlTriggerPointArray = null;

    //endregion

    //region ---------------------- basic functions ----------------------------

    //BaseLoaderCallback
    private BaseLoaderCallback myLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully!");

                    //region initialize CascadeClassifier_palm (註解了)
                    /*
                    try{
                        cascadeClassifier_palm = generalizationInitializeCascadeClassifier(R.raw.hand_cascade,"hand_cascade.xml");
                        Log.i(TAG, "BaseLoaderCallback: cascadeClassifier_object initialize success.");
                    }catch(Exception e){
                        Log.e(TAG, "BaseLoaderCallback: cascadeClassifier_object initialize fail.");
                    }*/
                    //endregion

                    mOpenCvCameraView.enableView();

                    //get screenCenter
                    screenWidth = mOpenCvCameraView.getWidth();
                    screenHeight = mOpenCvCameraView.getHeight();
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
        Log.i(TAG, "onCreate!");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.cameraView);
        if(mOpenCvCameraView!=null)
        {
            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(MainActivity.this);
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume!");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, myLoaderCallback);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause!");
        super.onPause();
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "onDestroy!");
        super.onDestroy();
        if(mOpenCvCameraView!=null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "onCameraViewStarted!");

        //set the size of detection object
        //setAbsoluteObjectSize(height);
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped!");
    }

    //endregion

    //---------------------- main function ----------------------------

    /**onCameraFrame function
     * note1: you can process the input frame in this function!
     * note2: Do not save or use CvCameraViewFrame object out of onCameraFrame callback.
     *        This object does not have its own state and its behavior out of callback is unpredictable!
     * */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.i(TAG, "onCameraFrame!");

        rgbaImg = inputFrame.rgba();
        grayImg = inputFrame.gray();
        Mat rgbaImg2 = inputFrame.rgba().clone();

        //region ------ step0: draw tl/ky/hw btn ------
        try{
            Imgproc.rectangle(rgbaImg, btntlRect.tl(), btntlRect.br(), (modeCode==0)?RECT_COLOR_RED:RECT_COLOR_BLUE, btnThickness);
            putTextAtCenter(rgbaImg, btntlRect, "TL", (modeCode == 0) ? RECT_COLOR_RED : RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, btnThickness);

            Imgproc.rectangle(rgbaImg, btnkyRect.tl(), btnkyRect.br(), (modeCode == 1) ? RECT_COLOR_RED : RECT_COLOR_BLUE, btnThickness);
            putTextAtCenter(rgbaImg, btnkyRect, "KY", (modeCode == 1) ? RECT_COLOR_RED : RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, btnThickness);

            Imgproc.rectangle(rgbaImg, btnhwRect.tl(), btnhwRect.br(), (modeCode == 2) ? RECT_COLOR_RED : RECT_COLOR_BLUE, btnThickness);
            putTextAtCenter(rgbaImg, btnhwRect, "HW", (modeCode == 2) ? RECT_COLOR_RED : RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, btnThickness);

        }catch (Exception e){
            Log.e(TAG,"step0: draw tl/ky/hw btn. " + e.getMessage());
        }

        //endregion

        //region ------ step1: arm and palm detection ------

        //region --- part1 palm detection (註解了)
        Boolean currentPalmFlag = false;
        /*
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
        */
        //endregion

        //region --- part2 arm detection
        //region 未封裝的arm detection流程(已註解)
        /*
        // skin color detection
        Mat skinImg = skinColorDetect(rgbaImg);

        //edge detection (find the Largest Area Contour that maybe Arm)
        ArrayList<MatOfPoint> largestContour = findLargestAreaContour(skinImg);

        if(!largestContour.isEmpty())
        {
            //Imgproc.drawContours(rgbaImg, largestContour, 0, RECT_COLOR_GREEN, 5); //draw Contour
            Rect rectBound = Imgproc.boundingRect(largestContour.get(0)); // Get bounding rect of contour
            //Imgproc.rectangle(rgbaImg, new Point(rectBound.x, rectBound.y), new Point(rectBound.x + rectBound.width, rectBound.y + rectBound.height), RECT_COLOR_GREEN, 3);

            // conditions 1:Aspect ratio ( h > 0.5*screenHeight &&  w > 0.75*screenWidth && w/h > 1.5)

            if(rectBound.height > 0.5*screenHeight && rectBound.width > 0.75*screenWidth && rectBound.width/ rectBound.height > 1.5)
            {
                Imgproc.rectangle(rgbaImg, new Point(rectBound.x, rectBound.y), new Point(rectBound.x + rectBound.width, rectBound.y + rectBound.height), RECT_COLOR_GREEN, 3);
            }
        }
        */
        //endregion
        Boolean currentArmFlag = false;
        Rect armRect = null;
        if(!currentPalmFlag) {
            armRect = armDetect(rgbaImg);
            if (armRect != null) {
                Imgproc.rectangle(rgbaImg, new Point(armRect.x, armRect.y), new Point(armRect.x + armRect.width, armRect.y + armRect.height), RECT_COLOR_GREEN, 3);
                currentArmFlag = true;
            }
            //set armFlags
            if (armFlagls.size() < 3) {
                armFlagls.add(currentArmFlag);
            } else {
                armFlagls.remove(0);
                armFlagls.add(currentArmFlag);
            }
        }
        //endregion

        //endregion

        //region ------ step2: create the keyboard and timeline & step3: locate the trigger point ------

        //region --- part1 keyboard (註解了)
        /*
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

                Log.i(TAG, "[onCameraFrame]: show keyboardImg success!");
                //endregion

                //region part2: local the trigger point
                //...
                //endregion
            }
        }
        */
        //endregion

        //region --- part2 timeline
        if(currentArmFlag)
        {
            //get timeline image
            Mat timelineImg =  getDisplayImage(R.drawable.timeline2,700,100);
            if(!timelineImg.empty())
            {
                //region part1: show timeline
                //set the coordinate to show timelineImg
                Point armC = new Point(armRect.tl().x+(armRect.width/2) , armRect.tl().y+(armRect.height/2));
                int x = (int) armC.x-350;
                int y = (int) armC.y-50;

                //set the para of addWeighted function
                //p.s. addWeighted function: output = src1*alpha + src2*beta + gamma;
                double alpha = 0.8;
                double beta = 1;
                double gamma = 1;

                //show timelineImg
                Rect timelineImg_roi = new Rect(x, y, timelineImg.width(), timelineImg.height());
                Core.addWeighted(rgbaImg.submat(timelineImg_roi), alpha, timelineImg, beta, gamma, rgbaImg.submat(timelineImg_roi));

                Log.i(TAG, "[onCameraFrame]: show timelineImg success!");
                //endregion

                //region part2: local the trigger point
                //66,45 162,45  350,50  600,50
                int[] datePointX = {66,162,350,600};
                int[] datePointY = {45,45,50,50};
                tlTriggerPointArray = new Point[4];
                for(int i=0; i<datePointX.length; i++)
                {
                    tlTriggerPointArray[i] = new Point(x+datePointX[i],y+datePointY[i]);
                }

                //test tlTriggerPoint
                for(int i=0; i<tlTriggerPointArray.length;i++)
                {
                    //draw a "+" on frame
                    Point p = tlTriggerPointArray[i];
                    Imgproc.circle(rgbaImg, p, 3, RECT_COLOR_RED,-1);
                }
                //endregion
            }
        }

        //endregion

        //endregion

        //region ------ step4: fingertip detection ------
        Rect fingertipRect = null;
        fingertipRect = fingertipDetect(rgbaImg2);
        if(fingertipRect!=null)
        {
            Imgproc.rectangle(rgbaImg, fingertipRect.tl(), fingertipRect.br(), RECT_COLOR_PURPLE, 5);
        }

       //Mat dst = new Mat(inputFrame.rgba().size(), CvType.CV_8UC1);
       //if(bgImg!=null){
       //     Core.absdiff(bgImg, inputFrame.rgba().clone(), dst);
       // }

        //endregion

        //region ------ step5: touch detection ------

        //if palm, arm, fingertip exist then do touch detection
        if(fingertipRect!=null)
        {
            //region --- touch btn ---
            Point fingertipCenter = new Point(fingertipRect.tl().x+(fingertipRect.width/2) , fingertipRect.tl().y+(fingertipRect.height/2));
            byte btnTouchFlag = -1;
            if(btntlRect.contains(fingertipCenter)){
                btnTouchFlag = 0;
            }else if(btnkyRect.contains(fingertipCenter)){
                btnTouchFlag = 1;
            }else if(btnhwRect.contains(fingertipCenter)){
                btnTouchFlag = 2;
            }

            if(btnTouchFlag!=-1){
                Intent intent = new Intent();
                switch (btnTouchFlag){
                    case 0:
                        break;
                    case 1:
                        intent.setClass(MainActivity.this, KeyboardActivity.class);
                        startActivity(intent);
                        finish();
                        break;
                    case 2:
                        intent.setClass(MainActivity.this, HandwriteActivity.class);
                        startActivity(intent);
                        finish();
                        break;
                    default:
                        break;
                }
            }
            //endregion

            //region --- palm is exist ---
            /*
            if(currentPalmFlag)
            {
             //if touch the keyboard trigger point then show result
            }*/
            //endregion

            //region --- arm is exist ---
            if(currentArmFlag)
            {
                //if the tlTriggerPoint in the fingertipRect, and which tlTriggerPoint
                int tlTriggerFlag = -1;
                for(int i=0; i<tlTriggerPointArray.length; i++)
                {
                    if(fingertipRect.contains(tlTriggerPointArray[i]))
                        tlTriggerFlag= i;
                }

                //show result image
                Mat displayImg;
                switch (tlTriggerFlag)
                {
                    case 0:
                        displayImg = getDisplayImage(R.drawable.funghi1,200,200);
                        if(!displayImg.empty())
                        {
                            //set the coordinate to show displayimg
                            int x = (int)tlTriggerPointArray[0].x;
                            int y = (int)tlTriggerPointArray[0].y/2;

                            //set the para of addWeighted function
                            //p.s. addWeighted function: output = src1*alpha + src2*beta + gamma;
                            double alpha = 0.8;
                            double beta = 1;
                            double gamma = 1;

                            //show displayimg
                            Rect displayimg_roi = new Rect(x, y, displayImg.width(), displayImg.height());
                            Core.addWeighted(rgbaImg.submat(displayimg_roi), alpha, displayImg, beta, gamma, rgbaImg.submat(displayimg_roi));
                            Boolean reflag = saveImage(rgbaImg);

                            Log.i(TAG, "[onCameraFrame]: show displayimg success!" + "save a photo: "+ reflag.toString() );
                        }
                        break;
                    case 1:
                        displayImg = getDisplayImage(R.drawable.funghi2,200,200);
                        if(!displayImg.empty())
                        {
                            //set the coordinate to show displayimg
                            int x = (int)tlTriggerPointArray[1].x;
                            int y = (int)tlTriggerPointArray[1].y/2;

                            //set the para of addWeighted function
                            //p.s. addWeighted function: output = src1*alpha + src2*beta + gamma;
                            double alpha = 0.8;
                            double beta = 1;
                            double gamma = 1;

                            //show displayimg
                            Rect displayimg_roi = new Rect(x, y, displayImg.width(), displayImg.height());
                            Core.addWeighted(rgbaImg.submat(displayimg_roi), alpha, displayImg, beta, gamma, rgbaImg.submat(displayimg_roi));
                            Boolean reflag = saveImage(rgbaImg);

                            Log.i(TAG, "[onCameraFrame]: show displayimg success!" + "save a photo: " + reflag.toString());
                        }
                        break;
                    case 2:
                        displayImg = getDisplayImage(R.drawable.funghi3,200,200);
                        if(!displayImg.empty())
                        {
                            //set the coordinate to show displayimg
                            int x = (int)tlTriggerPointArray[2].x;
                            int y = (int)tlTriggerPointArray[2].y/2;

                            //set the para of addWeighted function
                            //p.s. addWeighted function: output = src1*alpha + src2*beta + gamma;
                            double alpha = 0.8;
                            double beta = 1;
                            double gamma = 1;

                            //show displayimg
                            Rect displayimg_roi = new Rect(x, y, displayImg.width(), displayImg.height());
                            Core.addWeighted(rgbaImg.submat(displayimg_roi), alpha, displayImg, beta, gamma, rgbaImg.submat(displayimg_roi));
                            Boolean reflag = saveImage(rgbaImg);

                            Log.i(TAG, "[onCameraFrame]: show displayimg success!" + "save a photo: " + reflag.toString());
                        }
                        break;
                    case 3:
                        displayImg = getDisplayImage(R.drawable.funghi4,200,200);
                        if(!displayImg.empty())
                        {
                            //set the coordinate to show displayimg
                            int x = (int)tlTriggerPointArray[3].x;
                            int y = (int)tlTriggerPointArray[3].y/2;

                            //set the para of addWeighted function
                            //p.s. addWeighted function: output = src1*alpha + src2*beta + gamma;
                            double alpha = 0.8;
                            double beta = 1;
                            double gamma = 1;

                            //show displayimg
                            Rect displayimg_roi = new Rect(x, y, displayImg.width(), displayImg.height());
                            Core.addWeighted(rgbaImg.submat(displayimg_roi), alpha, displayImg, beta, gamma, rgbaImg.submat(displayimg_roi));
                            Boolean reflag = saveImage(rgbaImg);
                            Log.i(TAG, "[onCameraFrame]: show displayimg success!" + "save a photo: " + reflag.toString());
                        }
                        break;
                }
            }
            //endregion
        }

        //endregion

        //region ------ step6: touch verify ------

        //endregion

        //region ------ step7: output the result ------

        //endregion

        frameCounter++; //frameCounter = frameCounter + 1;
        preFrameCounter++;
        prePreFrameCounter++;
        Log.e(TAG, "[onCameraFrame]: frameCounter="+frameCounter+".");

        return rgbaImg;
    }

//region ---------------------- functions ----------------------------

    //region --- CascadeClassifier about (註解了) ---
    // Generalization Initialize Cascade Classifier
    /*
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
    }*/

    // set the size of detection object
    /*
    private void setAbsoluteObjectSize(int height)
    {
        // The faces will be a 20% of the height of the screen
        absoluteObjectSize = (int)(height *0.2);
    }*/

    // using classifier detect objects then return MatOfRect
    /*
    private  MatOfRect detectObjects()
    {
        MatOfRect objects = new MatOfRect();

        try{
            if(cascadeClassifier_palm != null)
            {
                cascadeClassifier_palm.detectMultiScale(grayImg,objects,1.1,2,2, new Size(absoluteObjectSize,absoluteObjectSize),new Size());
            }

            //Log.i(TAG, "[detectObjects]: using classifier to detect palm success!");
        }catch(Exception e){
            Log.e(TAG, "[detectObjects]: using classifier to detect palm ERROR!");
        }

        return objects;
    }*/

    //endregion

    //region --- choose Fit Object (註解了) ---
    /*
    private Rect chooseFitObject(MatOfRect objects)
    {
        Rect fitObject = null;
        Rect[] objectsArray = null;

        //get Rect[] objectsArray
        try{
            objectsArray = objects.toArray();

            Log.i(TAG,"[chooseFitObject]: Get Rect[] objectsArray success!!");
        }catch(Exception e){
            Log.e(TAG,"[chooseFitObject]: Get Rect[] objectsArray ERROR!");
        }

        //choose fitObject from objectsArray
        try{
            if(objectsArray!=null){
                for(Rect oneArm : objectsArray)
                {
                    if(fitObject==null){
                        fitObject = oneArm;
                    }else {
                        fitObject = compareTwoRect(fitObject,oneArm);
                    }
                }
            }

            Log.i(TAG, "[chooseFitObject]: Choose fitObject from objectsArray success!");
        }catch(Exception e){
            Log.e(TAG, "[chooseFitObject]: Choose fitObject from objectsArray ERROR!");
        }

        return fitObject;
    }*/

    /*
    private Rect compareTwoRect(Rect r1,Rect r2)
    {
        Point center1=null,
              center2=null;
        double distance1=0,
               distance2=0;

        //get center of r1 & r2
        try{
            center1 = new Point( r1.tl().x+(r1.width/2),  r1.tl().y+(r1.height/2));
            center2 = new Point( r2.tl().x+(r2.width/2),  r2.tl().y+(r2.height/2));

            Log.i(TAG,"[compareTwoRect]: Get Point center1,center2 success!");
        }catch(Exception e){
            Log.e(TAG,"[compareTwoRect]: Get Point center1,center2 ERROR!");
        }

        //compute the distance between center1/center2 and screenCenter
        try{
            double deltaX1 = Math.abs(center1.x-screenCenter.x);
            double deltaY1 = Math.abs(center1.y-screenCenter.y);
            double deltaX2 = Math.abs(center2.x-screenCenter.x);
            double deltaY2 = Math.abs(center2.y - screenCenter.y);
            distance1 = Math.sqrt(Math.pow(deltaX1, 2) + Math.pow(deltaY1, 2));
            distance2 = Math.sqrt(Math.pow(deltaX2,2)+Math.pow(deltaY2,2));

            Log.i(TAG,"[compareTwoRect]: Compute the distance success!");
        }catch(Exception e) {
            Log.e(TAG, "[compareTwoRect]: Compute the distance ERROR!");
        }

        //choose return
        if(distance1<distance2 ){
            return r2;
        }else {
            return r1;
        }
    }*/
    //endregion

    //region --- arm detection ---
    private Rect armDetect(Mat inFrame)
    {
        Rect arm = null;

        //skin detect
        Mat imgSkin = skinColorDetect(inFrame);

        //find the contour of largest area
        ArrayList<MatOfPoint> largestContour = findLargestAreaContour(imgSkin);

        // conditions 1:Aspect ratio
        if(!largestContour.isEmpty())
        {
            Rect rectBound = Imgproc.boundingRect(largestContour.get(0)); // Get bounding rect of contour

            // conditions 1:Aspect ratio ( h > 0.5*screenHeight &&  w > 0.75*screenWidth && w/h > 1.5)
            if(rectBound.height > 0.5*screenHeight && rectBound.width > 0.75*screenWidth && rectBound.width/ rectBound.height > 1.5)
            {
                arm = rectBound;
            }
        }

        return arm;
    }

    //region 找出arm輪廓質心(沒用到,已註解)
    /*
    private Point fineArmCentroid(Mat inFrame)
    {
        Point Centroid = null;

        //get arm contour
        ArrayList<MatOfPoint> contours = findArmContour(inFrame);

        //find arm centroid
        if(contours!= null)
        {
            MatOfPoint contour = contours.get(0);
            Moments moments = Imgproc.moments(contour, false);
            Centroid = new Point(moments.m10 / moments.m00, moments.m01 / moments.m00);
        }

        return Centroid;
    }*/
    //endregion

    //region find Arm Contour(with conditions filter) (註解了)
    /*
    private ArrayList<MatOfPoint> findArmContour(Mat inFrame)
    {
        ArrayList<MatOfPoint> contour = null;

        //skin detect
        Mat imgSkin = skinColorDetect(inFrame);

        //find the contour of largest area
        ArrayList<MatOfPoint> largestContour = findLargestAreaContour(imgSkin);

        // conditions 1:Aspect ratio
        if(!largestContour.isEmpty())
        {
            Rect rectBound = Imgproc.boundingRect(largestContour.get(0)); // Get bounding rect of contour

            // conditions 1:Aspect ratio ( h > 0.5*screenHight &&  w > 0.75*screenWidth && w/h > 1.5)
            if(rectBound.height > 0.5*screenHeight && rectBound.width > 0.75*screenWidth && rectBound.width/ rectBound.height > 1.5)
            {
                contour = largestContour;
            }
        }

        return contour;
    }*/
    //endregion

    // skin color detection
    private Mat skinColorDetect(Mat rgbImage)
    {
        Mat resultImg = new Mat();

        try {
            //skin detect
            Mat hvsImg = new Mat();
            Imgproc.cvtColor(rgbImage, hvsImg, Imgproc.COLOR_RGB2HSV_FULL);
            Core.inRange(hvsImg, skinLower, skinUpper, resultImg);

            //Perform and decrease noise
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
            Imgproc.dilate(resultImg, resultImg, kernel); //膨脹
            Imgproc.erode(resultImg, resultImg, kernel); //侵蝕
            //Imgproc.GaussianBlur(resultImg, resultImg, new Size(5, 5), 0); //高斯模糊
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }

        return  resultImg;
    }

    // find the largest area Contour
    private ArrayList<MatOfPoint> findLargestAreaContour(Mat skinImage)
    {
        ArrayList<MatOfPoint> largestAreaContour = new ArrayList<MatOfPoint>();

        try {
            ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();

            //find all contours
            Imgproc.findContours(skinImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

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

            largestAreaContour.add(contours.get(maxValIdx));

        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }

        return largestAreaContour;
    }

    //endregion

    //region --- get display image ---
    // get the image that you want to show on the screen.
    private Mat getDisplayImage(int drawableId,double resize_x, double resize_y)
    {
        Mat displayImage = new Mat();
        Mat resizeDisplayImage = new Mat();
        Size size = new Size(resize_x,resize_y);

        //get displayImage
        try{
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), drawableId);
            Utils.bitmapToMat(bmp,displayImage);

            Log.i(TAG, "[getDisplayImage]: get display image success!");
        }catch(Exception e){
            Log.e(TAG, "[getDisplayImage]: get displayImage ERROR! "+e.getMessage());
        }

        //resize the displayImage
        try{
            if(!displayImage.empty())
            {
                Imgproc.resize(displayImage,resizeDisplayImage,size);
                Log.i(TAG, "[getDisplayImage]: resize success!");
            }
        }catch (Exception e){
            Log.e(TAG, "[getDisplayImage]: resize ERROR! "+e.getMessage());
        }

        return resizeDisplayImage;
    }
    //endregion

    //region --- fingertip detection ---
    private Rect fingertipDetect(Mat inFrame)
    {
        Rect fingertip = null;

        ArrayList<MatOfPoint> contour = findFingertipContour(inFrame);

        if(contour!=null)
        {
            fingertip = Imgproc.boundingRect(contour.get(0)); // Get bounding rect of contour
        }

        return fingertip;
    }

    private ArrayList<MatOfPoint> findFingertipContour(Mat inFrame)
    {
        ArrayList<MatOfPoint> contour = null;

        //fingertip color detect
        Mat imgFingertip = fingertipColorDetect(inFrame);

        //find the contour of largest area
        ArrayList<MatOfPoint> largestContour = findLargestAreaContour(imgFingertip);

        // conditions 1:Aspect ratio
        if(!largestContour.isEmpty())
        {
            Rect rectBound = Imgproc.boundingRect(largestContour.get(0)); // Get bounding rect of contour

            // conditions 1:Aspect ratio (h,w) range in (100,100) ~ (30,30)
            if(rectBound.height>30 && rectBound.width>30  && rectBound.height<100 && rectBound.width<100)
            {
                contour = largestContour;
            }
        }

        return contour;
    }

    // fingertip color detection
    private Mat fingertipColorDetect(Mat rgbImage)
    {
        Mat resultImg = new Mat();

        try {
            Mat hvsImg = new Mat();
            Imgproc.cvtColor(rgbImage, hvsImg, Imgproc.COLOR_RGB2HSV_FULL);
            Core.inRange(hvsImg, fingertipLower, fingertipUpper, resultImg);

            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
            Imgproc.dilate(resultImg, resultImg, kernel); //膨脹
            //Imgproc.erode(resultImg, resultImg, kernel); //侵蝕
            //Imgproc.GaussianBlur(resultImg, resultImg, new Size(5, 5), 0); //高斯模糊

        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }

        return  resultImg;
    }

    //endregion

    //region--- Take a picture ---
    private Boolean saveImage(Mat img)
    {
        Boolean flag = false;
        Bitmap bitmapImage = null;

        //Mat to bitmap
        try {
            bitmapImage = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bitmapImage);
        }catch (CvException e){
            Log.e(TAG, e.getMessage());
        }

        //save the bitmap image
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root+"/myCvPhotos");
        if(!myDir.exists())
        {
            myDir.mkdir();
        }
        String fname = "photo.jpg";
        File file = new File(myDir,fname);
        if(file.exists())
        {
            file.delete();
        }

        FileOutputStream out = null;
        try{
            out = new FileOutputStream(file);
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

            flag = true;
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }finally {
            try{
                out.close();
            }catch (Exception e){
                Log.e(TAG, e.getMessage());
            }
        }

        return flag;
    }

    //region get Extermal Storage Public Dir (沒用到,已註解)
    /*
    private File getExtermalStoragePublicDir(String albumName) {
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if(file.mkdir()){
            File f = new File(file, albumName);
            if(f.mkdir()){
                return f;
            }
        }
        return new File(file, albumName);
    }*/
    //endregion

    //endregion

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
            Log.e(TAG, "[putTextAtCenter]: put text ERROR! "+e.getMessage());
        }
    }
    //endregion

//endregion

} //end class
