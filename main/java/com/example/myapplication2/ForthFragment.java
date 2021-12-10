package com.example.myapplication2;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.taidoc.pclinklibrary.connection.TD4143Connection;
import com.taidoc.pclinklibrary.constant.PCLinkLibraryEnum;
import com.taidoc.pclinklibrary.exceptions.MeterCmdWrongException;

import static com.taidoc.pclinklibrary.connection.TD4143Connection.STATE_CONNECTED;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ForthFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ForthFragment extends Fragment {


    public static VCP myVCP;
    boolean threadActive;
    boolean isQc = true;
    boolean isFactory = true;
    boolean isHct = true;
    boolean isKT = false;
    private View mleak;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ForthFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BlankFragment2.
     */
    // TODO: Rename and change types and number of parameters
    public ForthFragment newInstance(String param1, String param2) {
        ForthFragment fragment = new ForthFragment();
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
        mleak = inflater.inflate(R.layout.fragment_forth, container, false);
        return mleak;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // set up button action
        view.findViewById(R.id.button_test2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                NavHostFragment.findNavController(ForthFragment.this)
                        .navigate(R.id.action_ForthFragment_to_FirstFragment);
            }
        });

        // start the work thread
        MyUtil.setDisplay(getActivity(), (TextView) getActivity().findViewById(R.id.textview_forth));
        myVCP = MyUtil.getConnection();
        threadActive = true;

        ForthFragment.MyRun myRun = new ForthFragment.MyRun();
        new Thread(myRun).start();

    }


    /**
     * see document 0x2c cmd ,definition of strip
     *
     * @param type
     * @return
     */
    public static PCLinkLibraryEnum.BloodGlucoseType convertToType2(int type) {
        PCLinkLibraryEnum.BloodGlucoseType typeStript;
        switch (type) {
            case 0:
                typeStript = PCLinkLibraryEnum.BloodGlucoseType.General;
                Log.e("TD4143", "General");
                break;
            case 6:
                typeStript = PCLinkLibraryEnum.BloodGlucoseType.HEMATOCRIT;
                Log.e("TD4143", "HEMATOCRIT");
                break;
            case 7:
                typeStript = PCLinkLibraryEnum.BloodGlucoseType.KETONE;
                Log.e("TD4143", "KETONE");
                break;
            case 8:
                typeStript = PCLinkLibraryEnum.BloodGlucoseType.UA;
                Log.e("TD4143", "UA");
                break;
            case 9:
                typeStript = PCLinkLibraryEnum.BloodGlucoseType.CHOL;
                Log.e("TD4143", "CHOL");
                break;
            case 11:
                typeStript = PCLinkLibraryEnum.BloodGlucoseType.HB;
                Log.e("TD4143", "HB");
                break;
            case 12:
                typeStript = PCLinkLibraryEnum.BloodGlucoseType.LACTATE;
                Log.e("TD4143", "LACTATE");
                break;
            case 13:
                typeStript = PCLinkLibraryEnum.BloodGlucoseType.TG;
                Log.e("TD4143", "TG");
                break;
            default:
                typeStript = PCLinkLibraryEnum.BloodGlucoseType.UNKNOWN;
                break;
        }

        return typeStript;
    }

    public ResultWithType getGlucoseResult(byte[] rxCmd) {

        if (rxCmd != null && rxCmd[1] == 0x26) {
            int glucoseReading = rxCmd[3] * 256 + rxCmd[2];

            ///////////////////////////////test start
            //******多參數 參考  MeterCmdServiceMeasure.parseRx25CmdAndRx26CmdToBloodGlucoseRec;
            int ambientValue = rxCmd[4];
            int codeNo = rxCmd[5] & 0x3f;
            // type 1 -> General(0), AC(1), PC(2), QC(3)
            PCLinkLibraryEnum.BloodGlucoseType typeMeasuring = PCLinkLibraryEnum.BloodGlucoseType.General;
            PCLinkLibraryEnum.BloodGlucoseType typeStript = PCLinkLibraryEnum.BloodGlucoseType.General;

            int responseType = (rxCmd[5] >> 6);
            switch (responseType) {
                case 0:
                default:
                    typeMeasuring = PCLinkLibraryEnum.BloodGlucoseType.General;
                    Log.e("TD4143", "General");
                    break;

                case 1:
                    typeMeasuring = PCLinkLibraryEnum.BloodGlucoseType.AC;
                    Log.e("TD4143", "AC");
                    break;
                case 2:
                    typeMeasuring = PCLinkLibraryEnum.BloodGlucoseType.PC;
                    Log.e("TD4143", "PC");
                    break;
                case 3:
                    typeMeasuring = PCLinkLibraryEnum.BloodGlucoseType.QC;
                    Log.e("TD4143", "QC");
                    break;

            }

            Log.e("TD4143", String.format("26 rxCmd[5]:%d", rxCmd[5]));
            typeStript = convertToType2((rxCmd[5] & 0x3c) >> 2);


            ResultWithType resultWithType = new ResultWithType();
            resultWithType.setResponseType(typeStript);
            resultWithType.setResponseValue(glucoseReading);
            resultWithType.setmTypeOfMeasureing(typeMeasuring);
            resultWithType.setmTypeOfTestStript(typeStript);
            Log.e("TD4143", " return resultWithType");
            return resultWithType;
        }

        MyUtil.displayMessage(String.format("%s content incorrect.", myVCP.convertToHexString(rxCmd)));
        return null;
    }

    private void showTestRsult(ResultWithType resultWithType) {
        if (resultWithType == null)
            return;
        String testStriptTypeName = "";
        PCLinkLibraryEnum.BloodGlucoseType typeMeasuring = resultWithType.getmTypeOfMeasureing();

        if (PCLinkLibraryEnum.BloodGlucoseType.General == resultWithType.getmTypeOfTestStript()) {
            testStriptTypeName = "glucose";
        } else if (PCLinkLibraryEnum.BloodGlucoseType.HEMATOCRIT == resultWithType.getmTypeOfTestStript()) {
            testStriptTypeName = "hct";
        } else if (PCLinkLibraryEnum.BloodGlucoseType.KETONE == resultWithType.getmTypeOfTestStript()) {
            testStriptTypeName = "Ketone";
            isKT = true;
        } else if (PCLinkLibraryEnum.BloodGlucoseType.UA == resultWithType.getmTypeOfTestStript()) {
            testStriptTypeName = "UA";
        } else if (PCLinkLibraryEnum.BloodGlucoseType.CHOL == resultWithType.getmTypeOfTestStript()) {
            testStriptTypeName = "Chol";
        } else if (PCLinkLibraryEnum.BloodGlucoseType.LACTATE == resultWithType.getmTypeOfTestStript()) {
            testStriptTypeName = "Lactate";
        } else if (PCLinkLibraryEnum.BloodGlucoseType.TG == resultWithType.getmTypeOfTestStript()) {
            testStriptTypeName = "TG";
        }

        double tempValue = resultWithType.getResponseValue();
        if (isKT) {
            tempValue = MyUtil.convertMeterKetoneMgdlToMmol(tempValue);
        }

        if (typeMeasuring == PCLinkLibraryEnum.BloodGlucoseType.QC ? true : false) {
            MyUtil.displayMessage(String.format("%s\n\n%s(QC): %.1f", MyUtil.getDisplayMessage(), testStriptTypeName, tempValue));
        } else {
            MyUtil.displayMessage(String.format("%s\n\n%s: %.1f", MyUtil.getDisplayMessage(), testStriptTypeName, tempValue));
        }
    }

   class MyRun implements Runnable {


        @Override
        public void run() {
            byte[] txCmd;
            int[] rxCmd;

            // get glucose reading
            isKT = false;
            if (true) {

                while (threadActive) {

                    try {

                        txCmd = new byte[]{0x51, 0x26, 0x00, 0x00, 0x00, 0x00, (byte) 0xa3, 0x1a};
                        rxCmd = myVCP.SendandReceive(txCmd);

                        if (rxCmd != null && rxCmd[1] == 0x26) {

                            int glucoseReading = (rxCmd[3]) * 256 + rxCmd[2];
                            //MyUtil.displayMessage(myVCP.convertToHexString(rxCmd));
                            MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\n\nglucose: " + glucoseReading);
                            //showTestRsult(getGlucoseResult(rxCmd));
                            break;
                        }

                    } catch (Exception e) {

                        // waiting for the response
                        MyUtil.displayMessage("get glucose reading exception... 4");
                        break;
                    }
                }
            }

            // get hct reading
            if (isHct) {

                while (threadActive) {

                    try {

                        txCmd = new byte[]{0x51, 0x26, 0x02, 0x00, 0x00, 0x00, (byte) 0xa3, 0x1c};
                        rxCmd = myVCP.SendandReceive(txCmd);
                        if (rxCmd != null && rxCmd[1] == 0x26) {

                            //MyUtil.displayCommand(rxCmd);

                            int hctReading = (rxCmd[3]) * 256 + rxCmd[2];
                            MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\nhct: " + hctReading);

                            break;
                        }

                    } catch (Exception e) {

                        MyUtil.displayMessage("get hct reading exception... 4");
                        break;
                    }
                }
            }

            // get qc reading
            if (isQc && !MyUtil.isIsAutoQC()) {
                while (threadActive) {
                    try {

                        txCmd = new byte[]{0x51, 0x26, 0x01, 0x00, 0x00, 0x00, (byte) 0xa3, 0x1b};
                        rxCmd = myVCP.SendandReceive(txCmd);
                        if (rxCmd != null && rxCmd[1] == 0x26) {

                            //MyUtil.displayCommand(rxCmd);

                            int qcReading = (rxCmd[3]) * 256 + rxCmd[2];
                            double tempValue = qcReading;
                            if (isKT) {
                                tempValue = MyUtil.convertMeterKetoneMgdlToMmol(tempValue);
                            }
                            MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\nqc: " + String.valueOf(tempValue));

                            break;
                        }

                    } catch (Exception e) {

                        MyUtil.displayMessage("get qc reading exception... 4");
                        break;
                    }
                }
            }

            // get rcode
            if (isFactory) {

                while (threadActive) {

                    try {

                        txCmd = new byte[]{0x51, 0x26, 0x03, 0x00, 0x00, 0x00, (byte) 0xa3, 0x1d};
                        rxCmd = myVCP.SendandReceive(txCmd);
                        if (rxCmd != null && rxCmd[1] == 0x26) {

                            //MyUtil.displayCommand(rxCmd);

                            int hctReading = (rxCmd[3]) * 256 + rxCmd[2];
                            MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\nrcode: " + hctReading);

                            break;
                        }

                    } catch (Exception e) {

                        MyUtil.displayMessage("get qc reading exception... 4");
                        break;
                    }
                }
            }

            // get ac reading
            if (isFactory) {

                while ( threadActive) {

                    try {

                        txCmd = new byte[]{0x51, 0x26, 0x04, 0x00, 0x00, 0x00, (byte) 0xa3, 0x1e};
                        rxCmd = myVCP.SendandReceive(txCmd);
                        if (rxCmd != null && rxCmd[1] == 0x26) {

                            //MyUtil.displayCommand(rxCmd);

                            float acReading = (rxCmd[3]) * 256 + rxCmd[2];
                            MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\nac: " + acReading / 10);

                            break;
                        }

                    } catch (Exception e) {

                        MyUtil.displayMessage("get qc reading exception... 4");
                        break;
                    }
                }
            }

            // get dc reading
            if (isFactory) {

                while (threadActive) {

                    try {

                        txCmd = new byte[]{0x51, 0x26, 0x05, 0x00, 0x00, 0x00, (byte) 0xa3, 0x1f};
                        rxCmd = myVCP.SendandReceive(txCmd);
                        if (rxCmd != null && rxCmd[1] == 0x26) {

                            //MyUtil.displayCommand(rxCmd);

                            float dcReading = (rxCmd[3]) * 256 + rxCmd[2];
                            MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\ndc: " + dcReading / 100);

                            break;
                        }

                    } catch (Exception e) {

                        MyUtil.displayMessage("get qc reading exception... 4");
                        break;
                    }
                }
            }

            // wait for strip unplugged
            while (threadActive) {
                try {
                    MyUtil.displayMessage(MyUtil.getDisplayMessage());
                    txCmd = new byte[]{0x51, 0x21, 0x00, 0x00, 0x00, 0x00, (byte) 0xa3, 0x15};
                    int[] rxCmd2 = null;
                    rxCmd2 = myVCP.SendandReceive(txCmd);
                    //rxCmd = myVCP.SendandReceive(txCmd);
                    if (rxCmd2 != null && rxCmd2[1] == 0x21) {

                        if ((rxCmd2[4] & 0x40) == 0) {
                            //MyUtil.displayMessage("");
                            break;
                        }
                    }

                } catch (Exception e) {

                    MyUtil.displayMessage("get status exception... 4");
                    break;
                }
            }

            NavHostFragment.findNavController(ForthFragment.this)
                    .navigate(R.id.action_ForthFragment_to_FirstFragment);
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
        if (myVCP.usbStatus != VCPStatus.Connected) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_FirstFragment_to_connectFragment);
        }
    }
}
