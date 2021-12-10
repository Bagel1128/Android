package com.example.myapplication2;

import static com.taidoc.pclinklibrary.connection.AndroidBluetoothConnection.BT_CONNECT_TIMEOUT;
import static com.taidoc.pclinklibrary.connection.TD4143Connection.STATE_CONNECTED;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ConnectFragment extends Fragment {

    private String TAG = "ConnectFragment";
    public static VCP myVCP;
    public boolean threadActive;
    private View mleak;

    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mleak = inflater.inflate(R.layout.fragment_connect, container, false);
        return mleak;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        myVCP = new VCP(getContext());
        myVCP.Connect();
        myVCP.setRetryTimes(100);
        myVCP.setRxTimeoutInMillis(1000);

        MyUtil.setConnection(myVCP);
        MyUtil.setDisplay(getActivity(), (TextView) getActivity().findViewById(R.id.textview_connect));
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
            byte mFlag;
            Log.i(TAG, " Run...");

            while (threadActive) {

                if (myVCP.usbStatus == VCPStatus.Idle) {
                    MyUtil.displayMessage("Please connect meter...");
                    continue;
                }
                if (myVCP.usbStatus != VCPStatus.Connected) {

                    long sTime = System.currentTimeMillis();
                    //MyUtil.displayMessage("Connecting...");

                    try {
                        myVCP.Connect();
                    } catch (Exception e) {
                        MyUtil.displayMessage("connect exception...");
                    }


                    while (myVCP.usbStatus != VCPStatus.Connected && threadActive) {

                        long cTime = System.currentTimeMillis();

                        if (cTime - sTime > BT_CONNECT_TIMEOUT) {
                            //MyUtil.displayMessage("connect timeout...");
                            break;
                        }
                    }
                }
                while (myVCP.usbStatus == VCPStatus.Connected && threadActive) {
                    NavHostFragment.findNavController(ConnectFragment.this)
                            .navigate(R.id.action_connectFragment_to_FirstFragment);
                    return;


                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        threadActive = false;
        mleak = null;
        myVCP = null;

    }
}


