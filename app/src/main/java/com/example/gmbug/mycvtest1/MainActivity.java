package com.example.gmbug.mycvtest1;

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

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    //---------------------- variables ----------------------------
    private static final Scalar RECT_COLOR = new Scalar(0, 255, 0, 255);
    private static final Scalar RECT_COLOR_FINGER = new Scalar(255,0,255,0);

    private String TAG = "MyCvTest1";

    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier cascadeClassifier_arm; //recognize arm to show the timeline
    private CascadeClassifier cascadeClassifier_finger; //recognize finger to get input signal

    private Mat rgbaImg;
    private Mat grayImg;
    private int absoluteObjectSize = 0;

    private Rect currentArm;
    private Mat timeline;

    private Point screenCenter;
    private int frameCounter;

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
                        cascadeClassifier_arm = generalizationInitializeCascadeClassifier(R.raw.test2_arm_cascade,"test2_arm_cascade.xml");
                        Log.i(TAG, "BaseLoaderCallback: cascadeClassifier_arm initialize success.");
                    }catch(Exception e){
                        Log.e(TAG, "BaseLoaderCallback: cascadeClassifier_arm initialize fail.");
                    }

                    //initialize cascadeClassifier_finger
                    try{
                        cascadeClassifier_finger = generalizationInitializeCascadeClassifier(R.raw.test4_finger_cascade,"test4_finger_cascade.xml");
                        Log.i(TAG, "BaseLoaderCallback: cascadeClassifier_finger initialize success.");
                    }catch(Exception e){
                        Log.e(TAG, "BaseLoaderCallback: cascadeClassifier_finger initialize fail.");
                    }

                    mOpenCvCameraView.enableView();

                    screenCenter = new Point(mOpenCvCameraView.getWidth()/2,mOpenCvCameraView.getHeight()/2);
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
        getTimelineImage();
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

        //region test resize the image and add on frame -> success!
        /*...
        Mat resizeImg = new Mat();
        Size sz = new Size(700,100);
        Imgproc.resize(timeline,resizeImg,sz);

        Rect roi = new Rect(10,10,resizeImg.cols(),resizeImg.rows());
        Core.addWeighted(rgbaImg.submat(roi), 0.8,resizeImg,0.5,1,rgbaImg.submat(roi));
        */
        //endregion

        //region test add image on frame success!
        /*
        Bitmap bmapimg = BitmapFactory.decodeResource(getResources(), R.drawable.likeicon);
        Mat matimg =new Mat();
        Utils.bitmapToMat(bmapimg,matimg);
        Rect roi = new Rect(10,10,matimg.cols(),matimg.rows());
        //p.s. copyTo or addWeighted choose one to use!
        //matimg.copyTo(rgbaImg.submat(roi));
        Core.addWeighted(rgbaImg.submat(roi), 0.8,matimg,0.2,1,rgbaImg.submat(roi));
        */
        //endregion

        //region test putText in frame
        /*
        Mat n = inputFrame.rgba().colRange(10,500).rowRange(10,300).setTo(new Scalar(0,0,0));
        Imgproc.putText(n, " frame!!", new Point(50, 50), Core.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(255, 0, 255));
        n.copyTo(rgbaImg.submat(10,300,10,500));
        Rect roi = new Rect(10,10,n.cols(),n.rows());
        Core.addWeighted(rgbaImg.submat(roi), 0.8,n,0.2,1,rgbaImg.submat(roi));
        */
        //endregion

        //region draw the screenCenter cross
        //draw a "+" on frame
        Point p1= new Point(screenCenter.x-5,screenCenter.y);
        Point p2= new Point(screenCenter.x+5,screenCenter.y);
        Imgproc.line(rgbaImg,p1,p2,RECT_COLOR,3);
        Point p3= new Point(screenCenter.x,screenCenter.y-5);
        Point p4= new Point(screenCenter.x,screenCenter.y+5);
        Imgproc.line(rgbaImg,p3,p4,RECT_COLOR,3);
        //endregion

        Log.i(TAG, "[onCameraFrame]: start to detect arm and show timeline image!");
        //region start to detect arm and show timeline image
        //using classifier to detect arms
        MatOfRect arms = detectArms();

        //choose one fit arm
        Rect fitArm = chooseFitArm(arms);

        //draw a rectangle around fitArm & show the timeline image on fitArm
        try{

            if(fitArm!=null) {
                //draw a rectangle
                //Imgproc.rectangle(rgbaImg, fitArm.tl(), fitArm.br(), RECT_COLOR, 3);

                //show timeline image
                //set roi range & add image on frame
                //p.s. addWeighted function: output = src1*alpha + src2*beta + gamma;

                //resize the timeline img to resizeImg
                // p.s. you should write the code to select the size to resize ---
                Mat resizeImg = new Mat();
                Size sz = new Size(700,100);
                Imgproc.resize(timeline,resizeImg,sz);

                //set the coordinate to show timeline image
                int x0 = (int)fitArm.tl().x;
                int y0 = (int)fitArm.tl().y;
                int width = resizeImg.cols();
                int height = resizeImg.rows();
                int x = x0 + (int)( (fitArm.br().x - fitArm.tl().x - width)/2 );
                int y = y0 + (int)( (fitArm.br().y - fitArm.tl().y - height)/2 );

                //set the para of addWeighted function
                double alpha = 0.8;
                double beta = 0.5;
                double gamma = 1;

                Rect roi = new Rect(x,y,width,height);
                Core.addWeighted(rgbaImg.submat(roi), alpha, resizeImg, beta, gamma, rgbaImg.submat(roi));

                Log.i(TAG, "[onCameraFrame]: draw a rectangle around arm & show timeline success!");
            }

        }catch(Exception e){
            Log.e(TAG, "[onCameraFrame]: draw a rectangle around arm & show timeline ERROR!");
        }
        //endregion

        Log.i(TAG, "[onCameraFrame]: start to detect finger and draw a rectangle!");
        //region start to detect finger and draw a rectangle!
        //using classifier to detect fingers
        MatOfRect fingers = detectFingers();

        //choose one fit arm
        Rect fitFinger = chooseFitArm(fingers);

        //draw a rectangle around fitArm & show the timeline image on fitArm
        try{
            if(fitFinger!=null)
            {
                //draw a rectangle on frame
                Imgproc.rectangle(rgbaImg, fitFinger.tl(), fitFinger.br(),RECT_COLOR_FINGER,3);

                Log.i(TAG, "[onCameraFrame]: draw a rectangle around finger success!");
            }
        }catch (Exception e){
            Log.e(TAG, "[onCameraFrame]: draw a rectangle around finger ERROR!");
        }

        //endregion

        //region If there are any objects found, draw a rectangle around it and add image on it
        /**
        try{

            Rect[] objectsArray = objects.toArray();

            for (Rect oneobject : objectsArray) {
                //draw a rectangle on frame
                Imgproc.rectangle(rgbaImg, oneobject.tl(), oneobject.br(),RECT_COLOR,3);

                //set roi range & add image on frame
                //p.s. addWeighted function: output = src1*alpha + src2*beta + gamma;

                //resize the timeline img to resizeImg
                // p.s. you should write the code to select the size to resize ---
                Mat resizeImg = new Mat();
                Size sz = new Size(700,100);
                Imgproc.resize(timeline,resizeImg,sz);

                //set the coordinate to show timeline image
                int x0 = (int)oneobject.tl().x;
                int y0 = (int)oneobject.tl().y;
                int width = resizeImg.cols();
                int height = resizeImg.rows();
                int x = x0 + (int)( (oneobject.br().x - oneobject.tl().x - width)/2 );
                int y = y0 + (int)( (oneobject.br().y - oneobject.tl().y - height)/2 );

                //set the para of addWeighted function
                double alpha = 0.8;
                double beta = 0.5;
                double gamma = 1;

                Rect roi = new Rect(x,y,width,height);
                Core.addWeighted(rgbaImg.submat(roi), alpha, resizeImg, beta, gamma, rgbaImg.submat(roi));
            }
        }catch (Exception e){
            Log.i(TAG,"draw the rectangle on frame Error!");
        }
         */
         //endregion

        frameCounter++; //frameCounter = frameCounter + 1;
        Log.e(TAG, "[onCameraFrame]: frameCounter="+frameCounter+".");

        return rgbaImg;
    }


//---------------------- functions ----------------------------

    /** initialize Detect Arm Cascade Classifier */
    //region
    /**
    private void initializeCascadeClassifier()
    {
        try{
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.test2_arm_cascade);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "test2_arm_cascade.xml");
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
            cascadeClassifier_arm = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if(cascadeClassifier_arm.empty()){
                Log.i(TAG,"Failed to load cascade classifier");
                cascadeClassifier_arm = null;
            }else{
                Log.i(TAG,"Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }

        }catch (Exception e) {
            Log.e(TAG,"initializeCascadeClassifier function: Error msg "+e.getMessage());
        }

    }*/
    //endregion

    /** Generalization Initialize Cascade Classifier */
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

    /** set the size of detection face */
    private void setAbsoluteObjectSize(int height)
    {
        // The faces will be a 20% of the height of the screen
        absoluteObjectSize = (int)(height *0.2);
    }

    /** using classifier detect arms then return MatOfRect */
    private  MatOfRect detectArms()
    {
        MatOfRect arms = new MatOfRect();

        try{
            if(cascadeClassifier_arm != null)
            {
                cascadeClassifier_arm.detectMultiScale(grayImg,arms,1.1,2,2, new Size(absoluteObjectSize,absoluteObjectSize),new Size());
            }

            Log.i(TAG, "[detectArms]: using classifier to detect arms success!");
        }catch(Exception e){
            Log.e(TAG, "[detectArms]: using classifier to detect arms ERROR!");
        }

        return arms;
    }

    /** using classifier detect finger then return MatOfRect */
    private MatOfRect detectFingers()
    {
        MatOfRect fingers = new MatOfRect();

        try{
            if(cascadeClassifier_finger != null)
            {
                cascadeClassifier_finger.detectMultiScale(grayImg,fingers,1.1,2,2, new Size(absoluteObjectSize,absoluteObjectSize),new Size());
            }
        }catch(Exception e){
            Log.i(TAG, "[detectFingers]: using classifier to detect face ERROR!");
        }

        return fingers;
    }

    /** get timeline image resources */
    private void getTimelineImage()
    {
        try{
            //timeline = Imgcodecs.imread(getResources().getDrawable(R.drawable.timeline).toString()); //not good ,it maybe make error!

            timeline = new Mat(); //initialize timeline
            Bitmap bmapimg = BitmapFactory.decodeResource(getResources(), R.drawable.timeline2);
            Utils.bitmapToMat(bmapimg,timeline);

        }catch(Exception e){
            Log.e(TAG, e.getMessage());
        }


    }

       private Rect chooseFitArm(MatOfRect arms)
    {
        Rect fitArm = null;
        Rect[] armsArray = null;

        //get Rect[] armsArray
        try{
            armsArray = arms.toArray();

            Log.i(TAG,"[chooseFitArm]: Get Rect[] armsArray success!!");
        }catch(Exception e){
            Log.e(TAG,"[chooseFitArm]: Get Rect[] armsArray ERROR!");
        }

        //choose fitArm from armsArray
        try{
            if(armsArray!=null){
                for(Rect oneArm : armsArray)
                {
                    if(fitArm==null){
                        fitArm = oneArm;
                    }else {
                        fitArm = compareTwoRect(fitArm,oneArm);
                    }
                }
            }

            Log.i(TAG, "[chooseFitArm]: Choose fitArm from armsArray success!");
        }catch(Exception e){
            Log.e(TAG, "[chooseFitArm]: Choose fitArm from armsArray ERROR!");
        }

        return fitArm;
    }

   private Rect chooseFitFinger(MatOfRect fingers)
   {
       Rect fitFinger = null;
       Rect[] fingersArray = null;

       //get Rect[] fingersArray
       try{
           fingersArray = fingers.toArray();

           Log.i(TAG,"[chooseFitFinger]: Get Rect[] fingersArray success!!");
       }catch(Exception e){
           Log.e(TAG,"[chooseFitFinger]: Get Rect[] fingersArray ERROR!");
       }

       //choose fitArm from armsArray
       try{
           if(fingersArray!=null){
               for(Rect oneArm : fingersArray)
               {
                   if(fitFinger==null){
                       fitFinger = oneArm;
                   }else {
                       fitFinger = compareTwoRect(fitFinger,oneArm);
                   }
               }
           }

           Log.i(TAG, "[chooseFitFinger]: Choose fitFinger from fingersArray success!");
       }catch(Exception e){
           Log.e(TAG, "[chooseFitFinger]: Choose fitFinger from fingersArray ERROR!");
       }

       return fitFinger;
   }

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



} //end class
