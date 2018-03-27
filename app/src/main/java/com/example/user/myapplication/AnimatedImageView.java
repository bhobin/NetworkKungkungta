package com.example.user.myapplication;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class AnimatedImageView extends android.support.v7.widget.AppCompatImageView {
    private AnimationDrawable mAnim;
    private boolean mAttached;

    public AnimatedImageView(Context context){
        super(context);
    }

    public AnimatedImageView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public AnimatedImageView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    private void updateAnim(){
        Drawable drawable = getDrawable();
        if(mAttached && mAnim != null)
        {
            mAnim.stop();
        }
        if(drawable instanceof AnimationDrawable)
        {
            mAnim = (AnimationDrawable)drawable;
            if (mAttached)
            {
                mAnim.start();
            }
        }
        else
        {
            mAnim = null;
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable)
    {
        super.setImageDrawable(drawable);
        updateAnim();
    }

    @Override
    public void setImageResource(int resid)
    {
        super.setImageDrawable(null);
        super.setImageResource(resid);
        updateAnim();
    }

    @Override
    public void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        if (mAnim != null)
        {
            mAnim.start();
        }
        mAttached = true;
    }

    @Override
    public void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        if (mAnim != null)
        {
            mAnim.stop();
        }
        mAttached = false;
    }

    @Override
    protected void finalize() throws Throwable
    {
        clearAnimation();
        super.finalize();
    }

}