package com.example.myapplication2;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.taidoc.pclinklibrary.connection.TD4143Connection;

import static com.taidoc.pclinklibrary.connection.TD4143Connection.STATE_CONNECTED;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ThirdFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ThirdFragment extends Fragment {


    public static VCP myVCP;
    boolean threadActive;
    private View mleak;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ThirdFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BlankFragment.
     */
    // TODO: Rename and change types and number of parameters
    public ThirdFragment newInstance(String param1, String param2) {
        ThirdFragment fragment = new ThirdFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mleak=inflater.inflate(R.layout.fragment_third, container, false);
        return mleak;


    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // set up button action
        view.findViewById(R.id.button_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // start the work thread
        MyUtil.setDisplay(getActivity(), (TextView)getActivity().findViewById(R.id.textview_third));
        myVCP= MyUtil.getConnection();
        threadActive = true;

        MyRun myRun = new MyRun();
        new Thread(myRun).start();

    }

    class MyRun implements Runnable {

        @Override
        public void run() {

            byte[] txCmd ;
            int[] rxCmd;
            boolean isError = false;
            while (myVCP.usbStatus== VCPStatus.Connected && threadActive)
            {


                try {

                    // check status code
                    txCmd =new byte[]{0x51, 0x21, 0x00, 0x00, 0x00, 0x00, (byte)0xa3, 0x15};
                    rxCmd = myVCP.SendandReceive(txCmd);
                    if (rxCmd != null && rxCmd[1] == 0x21) {

                        //MyUtil.displayCommand(rxCmd);

                        switch (rxCmd[4]) {

                            case 0x02:
                            case 0x42:
                                MyUtil.displayMessage("measurement done...");

                                NavHostFragment.findNavController(ThirdFragment.this)
                                        .navigate(R.id.action_ThirdFragment_to_ForthFragment);
                                return;

                            case 0x03:
                            case 0x04:
                            case 0x44:
                                MyUtil.displayMessage("measurement error...");
                                isError = true;
                                break;

                            default:
                                // debug
                                //MyUtil.displayCommand(rxCmd);
                                //Thread.sleep(5000);
                                isError = true;
                        }

                    } else {

                        isError = true;
                    }

                } catch (Exception e) {

                    MyUtil.displayMessage("get status exception... 3");
                    //isError = true;
                }

                if (isError) {

                    try {

                        // check error code
                        txCmd =new byte[] {0x51, 0x2e, 0x00, 0x00, 0x00, 0x00, (byte)0xa3, 0x22};
                        rxCmd = myVCP.SendandReceive(txCmd);

                        if (rxCmd != null && rxCmd[1] == 0x2e) {
                            if (rxCmd[2] != 0x1E) {
                                MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\n\nerror code: " + rxCmd[2]);

                            } else {//0x1E =  Rcode Out of range
                                MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\n\nRcode Out of range: " + MyUtil.checkRcodeOutofRangeValue(myVCP));
                            }
                            Thread.sleep(2000);
                            break;
                        }

                    } catch (Exception e) {

                        MyUtil.displayMessage("get error code exception... 3");
                        break;
                    }
                }
            }

            NavHostFragment.findNavController(ThirdFragment.this)
                    .navigate(R.id.action_ThirdFragment_to_FirstFragment);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        threadActive = false;
        mleak=null;
        myVCP = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (myVCP.usbStatus!= VCPStatus.Connected) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_FirstFragment_to_connectFragment);
        }
    }
}
