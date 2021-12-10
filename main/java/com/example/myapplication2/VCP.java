package com.example.myapplication2;

import com.exar.android.usbdriver.XRDriver;
import com.taidoc.pclinklibrary.exceptions.MeterCmdWrongException;
import com.taidoc.pclinklibrary.connection.TD4143Connection;
import com.taidoc.pclinklibrary.meter.service.MeterCmdService;
import com.taidoc.pclinklibrary.usb_cdc.CDCConstants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Message;
import android.widget.TabHost;
import android.widget.Toast;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class VCP {
    static XRDriver mSerial;
    private static final String ACTION_USB_PERMISSION = "com.exarusb.android.exarusb.USB_PERMISSION";
    private static String TAG = "VCP";
    public VCPStatus usbStatus = VCPStatus.Idle;
    private int default_baudrate = 19200;
    private static final int BUFF_SIZE = 4 * 1024;
    static final int HANDLE_READ = 1;

    private static int serialportsel = 0;
    private int baudrate = default_baudrate;
    private int dataBits = 8;
    private int stopBits = 1;
    private int parityBits = 0;
    private int flowControl = 0;
    private boolean flagLoopBack = false;
    private static boolean readFlag;
    private Activity activity;
    private static Context context;
    private boolean bGetPermission = true;
    private static int RxTimeoutInMillis = 500;
    private int RetryTimes = 1;

    private StringBuilder datasb = new StringBuilder();
    private static ArrayList<Byte> RxList = new ArrayList();

    static Thread receiveThread, contsendThread;

    public VCP(Context context) {
        this.context = context;
        usbStatus = VCPStatus.Idle;
    }

    public void setRetryTimes(int retryTimes) {
        this.RxTimeoutInMillis = retryTimes;
    }

    public void setRxTimeoutInMillis(int rxTimeoutInMillis) {
        this.RxTimeoutInMillis = rxTimeoutInMillis;
    }

    public static int[] SendandReceive(byte[] tx) throws InterruptedException {
        boolean bResult = false;
        RxList.clear();
        bResult = ContSend(tx);

        if (bResult) {
            return Receive(8);
        } else
            return null;
    }

    ;

    public void Connect() {
        mSerial = new XRDriver((UsbManager) context.getSystemService(Context.USB_SERVICE));

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
        mSerial.setPermissionIntent(mPermissionIntent);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbReceiver, filter);
    }

    private void autoConnect() {
        mSerial.setParameters(serialportsel, baudrate, dataBits, stopBits, parityBits,
                flowControl, flagLoopBack);
        readFlag = true;
        usbStatus = VCPStatus.Connected;
        StartReceive();
    }

    ;

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device.getVendorId() == 1250) {//1250
                    if (bGetPermission) {
                        if (mSerial.begin(serialportsel)) {
                            Log.i(TAG, "serial Begin ok");
                            autoConnect();
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                bGetPermission = false;
                readFlag = false;
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    usbStatus = VCPStatus.Detached;
                }
            } else if (action.equals(ACTION_USB_PERMISSION)) {
                usbStatus = VCPStatus.Permission;
                bGetPermission = true;
                if (mSerial.begin(serialportsel)) {
                    autoConnect();
                }
            } else {
                Log.i(TAG, "VCP onReceive other" + action);
            }
        }
    };

    public static int[] Receive(int rxCmdLength) throws InterruptedException {
        boolean bWait = true;
        long startReceiveInMillis = System.currentTimeMillis();
        int[] mResult = null;
        while (bWait) {
            long endReceiveInMillis = System.currentTimeMillis();
            if (RxList.size() < rxCmdLength) {
                if (endReceiveInMillis - startReceiveInMillis > (long) RxTimeoutInMillis) {
                    bWait = false;
                    Log.i(TAG, "Receive::Timeout");
                }
            } else {
                if (RxList.size() >= 8) {
                    bWait = false;
                    mResult = new int[RxList.size()];
                    for (int i = 0; i < RxList.size(); i++) {
                        mResult[i] = RxList.get(i).byteValue() & 255;
                    }
                    Log.i(TAG, "Receive::" + convertToHexIntString((mResult)));
                }
            }
        }
        return mResult;
    }

    ;

    public static void StartReceive() {
        receiveThread = new Thread(new Runnable() {

            @Override
            public void run() {
                byte[] rbuf = new byte[BUFF_SIZE];
                while (readFlag) {
                    try {
                        int len = mSerial.read(rbuf, serialportsel);

                        if (len > 0) {
                            for (int i = 0; i < len; i++)
                                RxList.add(rbuf[i]);
                            byte[] tmpArr = Arrays.copyOf(rbuf, len);
                            String rspStr = convertToHexString(tmpArr);
                        }

                    } catch (NullPointerException e1) {
                        e1.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        receiveThread.start();
    }


    /**
     * This method starts another thread to continuous send the data to the UART.
     **/
    public static boolean ContSend(byte[] txByteCmd) {
        boolean bResult = false;
        int iWrite;
        if (txByteCmd == null || txByteCmd.length <= 0) {
            return bResult;
        }
        try {
            iWrite = mSerial.write_ext(txByteCmd, serialportsel);
            if (iWrite == txByteCmd.length)
                bResult = true;
            Log.i(TAG, "ContSend::" + convertToHexString(txByteCmd));
            Thread.sleep(20);
            return bResult;
        } catch (InterruptedException e) {
            Log.i(TAG, "InterruptedException");
            e.printStackTrace();
        } finally {
            return bResult;
        }

    }

    ;

    public void disconnect() {
        mSerial.end();
        readFlag = false;
        RxList.clear();
    }

    ;

    public static String convertToHexString(byte[] cmd) {
        StringBuffer resultStr = new StringBuffer("");

        for (int i = 0; i < cmd.length; ++i) {
            if (i != cmd.length - 1) {
                resultStr.append(String.format("%02x ", cmd[i]));
            } else {
                resultStr.append(String.format("%02x", cmd[i]));
            }
        }

        resultStr.append("");
        return resultStr.toString();
    }

    ;

    public static String convertToHexIntString(int[] cmd) {
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


}
