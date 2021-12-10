package com.example.myapplication2;

import static com.taidoc.pclinklibrary.connection.AndroidBluetoothConnection.BT_CONNECT_TIMEOUT;
import static com.taidoc.pclinklibrary.connection.TD4143Connection.STATE_CONNECTED;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import androidx.navigation.fragment.NavHostFragment;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FirstFragment extends Fragment {
    private String TAG = "FirstFragment";
    //public static MyHandler myHandler;
    //public static TD4143Connection myConn;
    public static VCP myVCP;
    public boolean threadActive;
private View mleak;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mleak=inflater.inflate(R.layout.fragment_first, container, false);
        return mleak;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        MyUtil.setDisplay(getActivity(), (TextView) getActivity().findViewById(R.id.textview_first));
        myVCP= MyUtil.getConnection();
        threadActive = true;

        MyVCPRun myRun = new MyVCPRun();
        new Thread(myRun).start();
    }

    class MyVCPRun implements Runnable {

        @Override
        public void run() {
            byte[] txCmd;
            int[] rxCmd;
            boolean isError = false;
            boolean isCodeCard = false;
            boolean isCodeCardFactory = false;
            int mFlag;

            Log.i(TAG, " Run...");

            while (threadActive) {

               if (myVCP.usbStatus == VCPStatus.Connected) {
                   Log.i(TAG, "VCP connected");
                   //MyUtil.displayMessage("Meter Connected.");
                   try {
                       //sync time
                       //clear status
                      // MyUtil.displayMessage("Meter Connected.");
                       isError = false;
                       {
                           txCmd = new byte[]{0x51, 0x31, 0x00, 0x00, 0x00, 0x00, (byte) 0xa3, 0x25};
                           rxCmd = myVCP.SendandReceive(txCmd);
                       }
                       //MyUtil.displayMessage(myVCP.convertToHexString(rxCmd));
                   } catch (Exception e) {
                       MyUtil.displayMessage("First fragment exception.");
                       Log.i(TAG, "cmd 0x33/31");
                   }
               }
               else
               {
                   myVCP.disconnect();
                   NavHostFragment.findNavController(FirstFragment.this)
                           .navigate(R.id.action_FirstFragment_to_connectFragment);
                   return;
               }


                while (myVCP.usbStatus == VCPStatus.Connected && threadActive) {
                    try {

                        //check status
                        txCmd = new byte[]{0x51, 0x21, 0x00, 0x00, 0x00, 0x00, (byte) 0xa3, 0x15};
                        rxCmd = myVCP.SendandReceive(txCmd);

                        if (rxCmd != null && rxCmd[1] == 0x21) {
                            //MyUtil.displayMessage(MyUtil.convertToHexString(rxCmd));
                            //mFlag = rxCmd[4];
                            switch (rxCmd[4]) {
                                case 0x01:
                                case 0x03:
                                    MyUtil.displayMessage("please inserting strip...");
                                    break;

                                case 0x04:
                                    MyUtil.displayMessage("measurement error...");
                                    isError = true;
                                    break;

                                case 0x44:
                                    MyUtil.displayMessage("strip error...");
                                    isError = true;
                                    break;

                                case 0x41:
                                    MyUtil.displayMessage("strip inserted...");
                                    // navigate to second fragment
                                    NavHostFragment.findNavController(FirstFragment.this)
                                            .navigate(R.id.action_FirstFragment_to_SecondFragment);
                                    return;

                                case 0x02:
                                case 0x42:
                                    MyUtil.displayMessage("measurement already done...");
                                    break;

                                case 0x81:
                                    MyUtil.displayMessage("code card inserted...");
                                    isCodeCard = true;
                                    break;

                                case 0x91:
                                    MyUtil.displayMessage("code card inserted...(Factory test)");
                                    isCodeCardFactory = true;
                                    break;

                                case 0x84:
                                    MyUtil.displayMessage("other error...");
                                    isError = true;
                                    break;

                                default:
                                    break;
                            }
                        }
                    } catch (Exception e) {

                        //MyUtil.displayMessage("get status exception... 1");
                    }
                    if (isError) {
                        try {
                            // check error code
                            txCmd = new byte[]{0x51, 0x2e, 0x00, 0x00, 0x00, 0x00, (byte) 0xa3, (byte) 0x22};
                            rxCmd = myVCP.SendandReceive(txCmd);
                            if (rxCmd != null && rxCmd[1] == 0x2e) {
                                if (rxCmd[2] != 0x1E) {
                                    MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\n\nError code: " + rxCmd[2]);
                                } else {
                                    //0x1E =  Rcode Out of range
                                    MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\n\nRcode Out of range: " + MyUtil.checkRcodeOutofRangeValue(myVCP));
                                }
                                Thread.sleep(2000);
                                break;
                            }

                        } catch (Exception e) {
                            MyUtil.displayMessage("get error code exception... 1");
                        }
                    }

                    if (isCodeCard) {
                        try {
                            // check code
                            txCmd = new byte[]{0x51, 0x2c, 0x00, 0x00, 0x00, 0x00, (byte) 0xa3, (byte) 0x20};
                            rxCmd = myVCP.SendandReceive(txCmd);
                            if (rxCmd != null && rxCmd[1] == 0x2c) {
                                int code = (rxCmd[3]) * 256 + rxCmd[2];
                                MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\n\nCode: " + code);
                                Thread.sleep(2000);
                            }
                        } catch (Exception e) {
                            MyUtil.displayMessage("code card exception...");
                        }
                    }

                    if (isCodeCardFactory) {
                        try {
                            txCmd = new byte[]{0x51, 0x22, (byte) 0xc2, 0x00, 0x00, 0x00, (byte) 0xa3, (byte) 0xD8};
                            rxCmd = myVCP.SendandReceive(txCmd);

                            if (rxCmd != null && rxCmd[1] == 0x22) {
                                int CID = (rxCmd[5]) * 256 + rxCmd[4];
                                MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\n\nCID: " + CID);
                                Thread.sleep(2000);
                            }
                        } catch (Exception e) {
                            MyUtil.displayMessage("code card exception...(Factory test)");
                        }
                    }


                }


            }
        }
    }
    @Override
    public void onStart() {
        super.onStart();

        if (myVCP.usbStatus!= VCPStatus.Connected) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_FirstFragment_to_connectFragment);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        threadActive = false;
        mleak=null;
        myVCP = null;
    }
}



