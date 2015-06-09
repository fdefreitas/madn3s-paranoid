package org.madn3s.controller.utils;

import android.os.SystemClock;
import android.widget.Chronometer;
import android.widget.Toast;

import org.madn3s.controller.MainActivity;
import org.madn3s.controller.R;

/**
 * Created by fernando on 07/06/15.
 */
public class Chron {

    private MainActivity mActivity;
    private Chronometer elapsedChronometer;

    public Chron(MainActivity mActivity) {
        this.mActivity = mActivity;

        elapsedChronometer = (Chronometer) mActivity.findViewById(R.id.elapsed_chronometer);
        resetChron();
    }

    public void showElapsedTime(String msg) {
        long elapsedMillis = SystemClock.elapsedRealtime() - elapsedChronometer.getBase();
        Toast.makeText(mActivity, (msg == null ? "" : msg) + " : " + elapsedMillis,
                Toast.LENGTH_SHORT).show();
    }

    public void startChron() {
        elapsedChronometer.start();
    }

    public void stopChron(String message) {
        elapsedChronometer.stop();
        if(message != null){
            showElapsedTime(message);
        }
    }

    public void resetChron() {
        elapsedChronometer.setBase(SystemClock.elapsedRealtime());
        showElapsedTime("resetChron");
    }

    public void restartChron(){
        resetChron();
        startChron();
    }
}
