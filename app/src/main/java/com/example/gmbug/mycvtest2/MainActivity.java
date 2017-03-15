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

    //color Scalar (r,g,b)
    private static final Scalar RECT_COLOR_GREEN = new Scalar(0, 255, 0, 255); //green color scalar
    private static final Scalar RECT_COLOR_PURPLE = new Scalar(255, 0, 255, 0); //purple color scalar
    private static final Scalar RECT_COLOR_RED = new Scalar(255, 0, 0, 0); // red color scalar
    private static final Scalar RECT_COLOR_BLUE = new Scalar(0, 0 ,255, 0); // blue color scalar

    //Mat
    private Mat rgbaImg;

    //screen
    private Point screenCenter;
    private int screenWidth;
    private int screenHeight;

    //button
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
                    Log.i(TAG, "[tl] OpenCV loaded successfully!");

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
        Log.i(TAG, "[tl] onCreate!");
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
        Log.i(TAG, "[tl] onResume!");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, myLoaderCallback);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "[tl] onPause!");
        super.onPause();
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "[tl] onDestroy!");
        super.onDestroy();
        if(mOpenCvCameraView!=null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "[tl] onCameraViewStarted!");
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "[tl] onCameraViewStopped!");
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
        Log.i(TAG, "[tl] onCameraFrame!");

        rgbaImg = inputFrame.rgba();
        Mat rgbaImg2 = inputFrame.rgba().clone();

        //region ------ step0: draw tl/ky/hw btn ------
        try{
            Imgproc.rectangle(rgbaImg, btntlRect.tl(), btntlRect.br(), RECT_COLOR_RED, btnThickness);
            Imgproc.rectangle(rgbaImg, btnkyRect.tl(), btnkyRect.br(), RECT_COLOR_BLUE, btnThickness);
            Imgproc.rectangle(rgbaImg, btnhwRect.tl(), btnhwRect.br(), RECT_COLOR_BLUE, btnThickness);

            putTextAtCenter(rgbaImg, btntlRect, "TL", RECT_COLOR_RED, Core.FONT_HERSHEY_DUPLEX, 2.0f, btnThickness);
            putTextAtCenter(rgbaImg, btnkyRect, "KY", RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, btnThickness);
            putTextAtCenter(rgbaImg, btnhwRect, "HW", RECT_COLOR_BLUE, Core.FONT_HERSHEY_DUPLEX, 2.0f, btnThickness);

        }catch (Exception e){
            Log.e(TAG,"[tl] step0: draw tl/ky/hw btn. " + e.getMessage());
        }

        //endregion

        //region ------ step1: arm detection ------
        Boolean currentArmFlag = false;
        Rect armRect = null;
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
        //endregion

        //region ------ step2: create the keyboard and timeline & step3: locate the trigger point ------
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

                Log.i(TAG, "[tl-onCameraFrame]: show timelineImg success!");
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

        //if fingertip exist then do touch detection
        if(fingertipRect!=null)
        {
            //region --- touch btn ---
            Point fingertipCenter = new Point(fingertipRect.tl().x+(fingertipRect.width/2) , fingertipRect.tl().y+(fingertipRect.height/2));
            if(btnkyRect.contains(fingertipCenter)){
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, KeyboardActivity.class);
                startActivity(intent);
                finish();
            }else if(btnhwRect.contains(fingertipCenter)){
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, HandwriteActivity.class);
                startActivity(intent);
                finish();
            }
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

                            Log.i(TAG, "[tl-onCameraFrame]: show displayimg success!" + "save a photo: "+ reflag.toString() );
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

                            Log.i(TAG, "[tl-onCameraFrame]: show displayimg success!" + "save a photo: " + reflag.toString());
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

                            Log.i(TAG, "[tl-onCameraFrame]: show displayimg success!" + "save a photo: " + reflag.toString());
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
                            Log.i(TAG, "[tl-onCameraFrame]: show displayimg success!" + "save a photo: " + reflag.toString());
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
        Log.e(TAG, "[tl-onCameraFrame]: frameCounter="+frameCounter+".");

        return rgbaImg;
    }

//region ---------------------- functions ----------------------------

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
            // Calculates the width and height of a text string
            Size textSize = Imgproc.getTextSize(text, fontFace, fontScale,thickness, null);

            // Calculates the center of roi
            Point center = new Point(roi.tl().x+(roi.width/2), roi.tl().y+(roi.height/2));

            // Calculates the position to put text (Bottom-left corner of the text string)
            Point org = new Point(center.x-(textSize.width/2), center.y+(textSize.height/2));

            //put text on the roi of img
            Imgproc.putText(img, text, org, fontFace, fontScale, color, thickness);

        }catch (Exception e){
            Log.e(TAG, "[tl-putTextAtCenter]: put text ERROR! "+e.getMessage());
        }
    }
    //endregion

//endregion

} //end class
