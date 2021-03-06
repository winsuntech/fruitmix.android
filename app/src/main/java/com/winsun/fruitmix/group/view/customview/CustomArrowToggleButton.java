package com.winsun.fruitmix.group.view.customview;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;

import com.winsun.fruitmix.R;

/**
 * Created by Administrator on 2017/7/24.
 */

public class CustomArrowToggleButton extends AppCompatImageButton {

    private boolean arrowDown = true;

    private PingToggleListener pingToggleListener;

    public CustomArrowToggleButton(Context context) {
        super(context);
        init(context);
    }

    public CustomArrowToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CustomArrowToggleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

        setBackground(ContextCompat.getDrawable(context, R.color.white));
        switchImageSource();

    }

    public void onclick() {

        if (arrowDown) {

            collapsePing();


        } else {

            expandPing();

        }

    }

    public boolean isExpandPing(){
        return arrowDown;
    }

    private void expandPing() {
        arrowDown = true;
        switchImageSource();

        if (pingToggleListener != null)
            pingToggleListener.onPingToggleArrowToDown();
    }

    public void collapsePing() {
        arrowDown = false;
        switchImageSource();

        if (pingToggleListener != null)
            pingToggleListener.onPingToggleArrowToUp();
    }


    private void switchImageSource() {
        if (arrowDown)
            setImageResource(R.drawable.ic_arrow_drop_down_black_24dp);
        else
            setImageResource(R.drawable.ic_arrow_drop_up_black_24dp);
    }

    public void setPingToggleListener(PingToggleListener pingToggleListener) {
        this.pingToggleListener = pingToggleListener;
    }

    public interface PingToggleListener {

        void onPingToggleArrowToDown();

        void onPingToggleArrowToUp();

    }

}
