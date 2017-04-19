package com.example.gmbug.mycvtest2.entity;

import org.opencv.core.Scalar;

/**
 * Created by GMBug on 2017/4/18.
 */
public class util {

    public final static String TAG = "MyCvTest2";

    //color
    public static final Scalar RECT_COLOR_GREEN = new Scalar(0, 255, 0, 255); //green
    public static final Scalar RECT_COLOR_PURPLE = new Scalar(255, 0, 255, 0); //purple
    public static final Scalar RECT_COLOR_RED = new Scalar(255, 0, 0, 0); // red
    public static final Scalar RECT_COLOR_BLUE = new Scalar(0, 0 ,255, 0); // blue

    //skin color
    /** maybe use
     *  double scaleSatLower = 0.28;
     double scaleSatUpper = 0.68;

     double scaleSatLower = 0.18; // maybe better
     double scaleSatLower = 0.08; // maybe too much
     double scaleSatUpper = 0.78;
     * */
    public static final Scalar skinLower = new Scalar(0, 0.18*255, 0);
    public static final Scalar skinUpper = new Scalar(25, 0.68*255, 255);

    //fingertip color detection H,S,V range
    public static final Scalar fingertipLower = new Scalar(240, 50, 178);
    public static final Scalar fingertipUpper = new Scalar(255, 80, 255);

}
