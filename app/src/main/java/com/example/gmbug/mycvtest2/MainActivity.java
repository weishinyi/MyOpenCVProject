package com.example.gmbug.mycvtest2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    //---------------------- variables ----------------------------
    private String TAG = "MyCvTest2";

    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier cascadeClassifier_object; //recognize palm to show the keyboard

    private static final Scalar RECT_COLOR = new Scalar(0, 255, 0, 255); //green color scalar
    private static final Scalar RECT_COLOR_FINGER = new Scalar(255, 0, 255, 0); //purple color scalar

    private Mat rgbaImg;
    private Mat grayImg;
    private int absoluteObjectSize = 0;

    private Rect fitArm = null;

    private Point screenCenter;
    private int screenWidth;
    private int screenHight;
    private int frameCounter = 0;
    private int preFrameCounter = -1;
    private int prePreFrameCounter = -2;

    //ex: boolean[] array = new boolean[size];
    private boolean[] armFlagArr = new boolean[3];
    private boolean[] palmFlagArr = new boolean[3];

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


    //---------------------- basic functions ----------------------------

    //BaseLoaderCallback
    private BaseLoaderCallback myLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully!");

                    //initialize CascadeClassifier_arm
                    try{
                        cascadeClassifier_object = generalizationInitializeCascadeClassifier(R.raw.test5_finger_cascade,"test5_finger_cascade.xml");
                        Log.i(TAG, "BaseLoaderCallback: cascadeClassifier_object initialize success.");
                    }catch(Exception e){
                        Log.e(TAG, "BaseLoaderCallback: cascadeClassifier_object initialize fail.");
                    }

                    mOpenCvCameraView.enableView();

                    screenWidth = mOpenCvCameraView.getWidth();
                    screenHight = mOpenCvCameraView.getHeight();
                    screenCenter = new Point(screenWidth/2, screenHight/2);
                    frameCounter = 0; //initialize frame counter
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

        //initialize armFlagArr & palmFlagArr
        Arrays.fill(armFlagArr, false);
        Arrays.fill(palmFlagArr, false);
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
        setAbsoluteObjectSize(height);

        //get timeline image resources
        //timeline = getDisplayImage(R.drawable.timeline2,700,100);
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped!");
    }

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

        //region ------ step1: arm and palm detection ------
        //region --- part1 palm detection

        //endregion

        //region --- part2 arm detection
        // skin color detection
        Mat skinImg = skinColorDetect(rgbaImg);

        //edge detection (find the Largest Area Contour that maybe Arm)
        ArrayList<MatOfPoint> largestContour = findLargestAreaContour(skinImg);

        if(!largestContour.isEmpty()){
            //Imgproc.drawContours(rgbaImg, largestContour, 0, RECT_COLOR, 5); //draw Contour
            Rect rectBound = Imgproc.boundingRect(largestContour.get(0)); // Get bounding rect of contour
            //Imgproc.rectangle(rgbaImg, new Point(rectBound.x, rectBound.y), new Point(rectBound.x + rectBound.width, rectBound.y + rectBound.height), RECT_COLOR, 3);

            // conditions 1:Aspect ratio ( h > 0.5*screenHight &&  w > 0.75*screenWidth)

            if(rectBound.height > 0.5*screenHight && rectBound.width > 0.75*screenWidth){
                Imgproc.rectangle(rgbaImg, new Point(rectBound.x, rectBound.y), new Point(rectBound.x + rectBound.width, rectBound.y + rectBound.height), RECT_COLOR, 3);
            }

            // conditions 2:Accounting for the proportion of the screen



        }

        //endregion



        //endregion

        //region ------ step2: create the timeline and keyboard ------

        //endregion

        //region ------ step3: locate the trigger point ------

        //endregion

        //region ------ step4: fingertip detection ------

        //endregion

        //region ------ step5: touch detection ------

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


//---------------------- functions ----------------------------

    /** Generalization Initialize Cascade Classifier */
    //region CascadeClassifier generalizationInitializeCascadeClassifier(int xmlfileId, String xmlfileName)
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
    //endregion

    /** set the size of detection face */
    //region setAbsoluteObjectSize(int height)
    private void setAbsoluteObjectSize(int height)
    {
        // The faces will be a 20% of the height of the screen
        absoluteObjectSize = (int)(height *0.2);
    }
    //endregion

    /** using classifier detect arms then return MatOfRect */
    //region MatOfRect detectObjects()
    private  MatOfRect detectObjects()
    {
        MatOfRect objects = new MatOfRect();

        try{
            if(cascadeClassifier_object != null)
            {
                cascadeClassifier_object.detectMultiScale(grayImg,objects,1.1,2,2, new Size(absoluteObjectSize,absoluteObjectSize),new Size());
            }

            Log.i(TAG, "[detectArms]: using classifier to detect arms success!");
        }catch(Exception e){
            Log.e(TAG, "[detectArms]: using classifier to detect arms ERROR!");
        }

        return objects;
    }
    //endregion

    //region Rect chooseFitObject(MatOfRect objects)
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
    }
    //endregion

    //region Rect compareTwoRect(Rect r1,Rect r2)
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
    }
    //endregion


    //region arm detection
    private void armDetect(Mat inFrame)
    {
        //skin detect
        //find the contour of largest area
        // conditions 1:Aspect ratio
        // conditions 2:Accounting for the proportion of the screen
    }

    /** skin color detection */
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

    /** find the largest area Contour */
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

} //end class
