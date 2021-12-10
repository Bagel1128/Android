package com.example.myapplication2;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.os.Handler;

import com.taidoc.pclinklibrary.connection.TD4143Connection;



public class MyUtil {
    public static VCP vcpConn;
    public static Activity activity;
    public static Context mcontext;
    public static TextView textView;
    private static boolean isAutoQC = false;


    public static void setConnection(VCP _usbConn) {
        vcpConn = _usbConn;
    }

    public static VCP getConnection() {
        return vcpConn;
    }

    public static void setDisplay(Activity _activity, TextView _textView) {
        activity = _activity;
        textView = _textView;
    }


    public static void displayMessage(final String message) {

        if (activity == null || textView == null)
            return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(message);
            }
        });
        MyUtil.delay(1000);
    }

    public static String getDisplayMessage() {
        return textView.getText().toString();
    }

    public void displayCommand(int[] cmd) {

        String rxCmdString = "[";

        for (int i = 0; i < 8; i++) {
            rxCmdString += Integer.toHexString(cmd[i]);
            rxCmdString += ", ";
        }

        rxCmdString += "]";
        displayMessage(rxCmdString);

    }

    public static void delay(int mSecond) {

        try {
            Thread.sleep(mSecond);
        } catch (InterruptedException ie) {

        }
    }

    public static boolean isIsAutoQC() {
        return isAutoQC;
    }

    public static void setIsAutoQC(boolean isAutoQC) {
        MyUtil.isAutoQC = isAutoQC;
    }

    public static String convertToHexString(int[] cmd) {
        StringBuffer resultStr = new StringBuffer("[");

        for (int i = 0; i < cmd.length; ++i) {
            if (i != cmd.length - 1) {
                resultStr.append(Integer.toHexString(cmd[i]) + ", ");
            } else {
                resultStr.append(Integer.toHexString(cmd[i]));
            }
        }

        resultStr.append("]");
        return resultStr.toString();
    }

    public static String getCurrentVersionName(Activity activity) {
        PackageManager packageManager = activity.getPackageManager();
        String packageName = activity.getPackageName();
        try {
            PackageInfo info = packageManager.getPackageInfo(packageName, 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        } /* end of if */
    }
    public static int checkRcodeOutofRangeValue(VCP myConn) throws InterruptedException {
        // check error code
        byte[] txCmd ={0x51, 0x2e, 0x0, 0x0, 0x1, 0x0, (byte)0xa3, 0x23};
        int[] rxCmd = myConn.SendandReceive(txCmd);

        if (rxCmd != null && rxCmd[1] == 0x2e) {

            if (rxCmd[2] == 0x1E) {
                return ((rxCmd[5] * 256) + rxCmd[4]);
            }
        }
        return 0;
    }
    public static double convertMeterKetoneMgdlToMmol(double mgdlValue) {
        // mg/dL = mmol/L * 30/
        double mmoL = mgdlValue / 30;
        mmoL = mmoL * 10 ;
        mmoL = Math.floor(mmoL);
        mmoL = mmoL / 10 ;
        return mmoL;
    }
}
