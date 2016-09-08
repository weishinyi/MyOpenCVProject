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

    private static final Scalar RECT_COLOR = new Scalar(0, 255, 0, 255);

    private String TAG = "OpenCV";

    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier cascadeClassifier;

    private Mat rgbaImg;
    private Mat grayImg;
    private int absoluteObjectSize = 0;

    private Mat timeline;

    //BaseLoaderCallback
    private BaseLoaderCallback myLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG,"OpenCV loaded successfully!");

                    initializeCascadeClassifier();
                    mOpenCvCameraView.enableView();
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

        //set the size of detection face
        setAbsoluteObjectSize(height);
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped!");
    }

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


        //region test putText in frame
        /*
        Mat n = inputFrame.rgba().colRange(10,500).rowRange(10,300).setTo(new Scalar(0,0,0));
        Imgproc.putText(n, " frame!!", new Point(50, 50), Core.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(255, 0, 255));
        n.copyTo(rgbaImg.submat(10,300,10,500));
        Rect roi = new Rect(10,10,n.cols(),n.rows());
        Core.addWeighted(rgbaImg.submat(roi), 0.8,n,0.2,1,rgbaImg.submat(roi));
        */
        //endregion

        //region using classifier to detect object
        /*
        MatOfRect objects = new MatOfRect();
        try{
            if(cascadeClassifier != null)
            {
                cascadeClassifier.detectMultiScale(grayImg, objects,1.1,2,2, new Size(absoluteObjectSize,absoluteObjectSize),new Size());
            }
        }catch(Exception e){
            Log.i(TAG,"using classifier to detect face ERROR!");
        }*/
        //endregion

        //region If there are any objects found, draw a rectangle around it and add image on it
        /*
        try{

            Rect[] objectsArray = objects.toArray();

            for (Rect oneobject : objectsArray) {
                //draw a rectangle on frame
                Imgproc.rectangle(rgbaImg, oneobject.tl(), oneobject.br(),RECT_COLOR,3);

                //set roi range & add image on frame
                //p.s. addWeighted function: output = src1*alpha + src2*beta + gamma;
                getTimelineImage();
                int x = (int)oneobject.tl().x;
                int y = (int)oneobject.tl().y;;
                int width = (int)timeline.size().width;
                int height = (int)timeline.size().height;
                double alpha = 0.5;
                double beta = 1.0;
                double gamma = 0;

                Rect ROI = new Rect(x,y,width,height);
                Core.addWeighted(rgbaImg.submat(ROI), alpha, timeline, beta, gamma, rgbaImg.submat(ROI));

            }


        }catch (Exception e){
            Log.i(TAG,"draw the rectangle on frame Error!");
        }*/
        //endregion

        return rgbaImg;
    }

    /** initialize Cascade Classifier */
    private void initializeCascadeClassifier()
    {
        try{
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.test_arm_cascade);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "test_arm_cascade.xml");
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
                Log.i(TAG,"Failed to load cascade classifier");
                cascadeClassifier = null;
            }else{
                Log.i(TAG,"Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }

        }catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    /** set the size of detection face */
    private void setAbsoluteObjectSize(int height)
    {
        // The faces will be a 20% of the height of the screen
        absoluteObjectSize = (int)(height *0.2);
    }

    /** get timeline image */
    private void getTimelineImage()
    {
        try{
            timeline = Imgcodecs.imread(getResources().getDrawable(R.drawable.timeline).toString());
        }catch(Exception e){
            Log.e(TAG,e.getMessage());
        }


    }

} //end class
