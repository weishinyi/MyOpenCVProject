package com.example.gmbug.mycvtest2.entity;

import android.app.Application;

import org.opencv.core.Point;
import org.opencv.core.Rect;

/**
 * Created by GMBug on 2017/4/18.
 */
public class GlobalVariable extends Application {

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

    //region --- setter ---
    public void setScreenCenter() {
        this.screenCenter = new Point(this.screenWidth/2, this.screenHeight/2);
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    public void setBtnWidth(int btnWidth) {
        this.btnWidth = btnWidth;
    }

    public void setBtnHeight(int btnHeight) {
        this.btnHeight = btnHeight;
    }

    public void setBtnPadding(int btnPadding) {
        this.btnPadding = btnPadding;
    }

    public void setBtnThickness(int btnThickness) {
        this.btnThickness = btnThickness;
    }

    public void setBtntlRect() {
        int x0 = this.screenWidth - 3 * this.btnWidth - this.btnPadding;
        this.btntlRect = new Rect(x0, this.btnPadding, this.btnWidth, this.btnHeight);
    }

    public void setBtnkyRect() {
        int x0 = this.screenWidth - 3 * this.btnWidth - this.btnPadding;
        this.btnkyRect = new Rect(x0+this.btnWidth, this.btnPadding, this.btnWidth, this.btnHeight);;
    }

    public void setBtnhwRect() {
        int x0 = this.screenWidth - 3 * this.btnWidth - this.btnPadding;
        this.btnhwRect = new Rect(x0+2*this.btnWidth, this.btnPadding, this.btnWidth, this.btnHeight);
    }
    //endregion

    //region --- getter ---

    public Point getScreenCenter() {
        return screenCenter;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getBtnWidth() {
        return btnWidth;
    }

    public int getBtnHeight() {
        return btnHeight;
    }

    public int getBtnPadding() {
        return btnPadding;
    }

    public int getBtnThickness() {
        return btnThickness;
    }

    public Rect getBtntlRect() {
        return btntlRect;
    }

    public Rect getBtnkyRect() {
        return btnkyRect;
    }

    public Rect getBtnhwRect() {
        return btnhwRect;
    }
    //endregion
}
