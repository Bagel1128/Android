package com.example.myapplication2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.taidoc.pclinklibrary.connection.TD4143Connection;

import static com.taidoc.pclinklibrary.connection.TD4143Connection.STATE_CONNECTED;

public class SecondFragment extends Fragment {
    public static VCP myVCP;
    boolean threadActive;
    private View mleak;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mleak = inflater.inflate(R.layout.fragment_second, container, false);
        return mleak;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_second).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });

        // start the work thread
        MyUtil.setDisplay(getActivity(), (TextView) getActivity().findViewById(R.id.textview_second));
        myVCP = MyUtil.getConnection();
        threadActive = true;

        MyVCPRun myRun = new MyVCPRun();
        new Thread(myRun).start();

    }

    public String convertToType2(int type) {
        String typeStript = "";
        switch (type) {
            case 0x00:
                typeStript = "Gluecose";
                break;
            case 0x36:
                typeStript = "Hematocrit";
                break;
            case 0x37:
                typeStript = "Ketone";
                break;
            case 0x39:
                typeStript = "Cholesterol";
                break;
            case 0xFF:
                typeStript = "No Strip";
                break;
            default:
                typeStript = "UNKNOWN";
                break;
        }
        return typeStript;
    }

    class MyVCPRun implements Runnable {
        @Override
        public void run() {
            byte[] txCmd;
            int[] rxCmd;
            boolean isError = false;

            // todo: expiration date by the same 0x2c command
            // get code number
            while (myVCP.usbStatus == VCPStatus.Connected && threadActive) {
                try {
                    txCmd = new byte[]{0x51, 0x2c, 0x00, 0x00, 0x00, 0x00, (byte) 0xa3, 0x20};
                    rxCmd = myVCP.SendandReceive(txCmd);
                    MyUtil.displayMessage("");

                    if (rxCmd != null && rxCmd[1] == 0x2C) {
                        int codeNumber = (rxCmd[3] * 256) + rxCmd[2];
                        MyUtil.displayMessage("code: " + codeNumber);
                        MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\nStrip Type: " + convertToType2(rxCmd[5]));
                        break;
                    }
                } catch (Exception e) {
                    MyUtil.displayMessage("get code number exception... 2");
                    break;
                }
            }

            while (myVCP.usbStatus == VCPStatus.Connected && threadActive) {
                try {
                    txCmd = new byte[]{0x51, 0x2f, 0x00, 0x00, 0x1, 0x00, (byte) 0xa3, 0x24};
                    rxCmd = myVCP.SendandReceive(txCmd);
                    //MyUtil.displayMessage(myVCP.convertToHexString(rxCmd));
                    if (rxCmd != null && rxCmd[1] == 0x2f) {
                        int autoQC = (rxCmd[5]) * 256 + rxCmd[4];
                        MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\nautoQC: " + autoQC);
                        MyUtil.setIsAutoQC(autoQC == 0xA55A);
                        break;
                    }
                } catch (Exception e) {
                    MyUtil.displayMessage("get autoQC exception... 2");
                    break;
                }
            }


            // get temperature
            while (myVCP.usbStatus == VCPStatus.Connected && threadActive) {
                try {
                    txCmd = new byte[]{0x51, 0x44, 0x00, 0x00, 0x00, 0x00, (byte) 0xa3, 0x38};
                    rxCmd = myVCP.SendandReceive(txCmd);
                    //MyUtil.displayMessage(myVCP.convertToHexString(rxCmd));

                    if (rxCmd != null && rxCmd[1] == 0x44 && rxCmd[6] == 0xa5) {
                        float tempReading = 0;
                        tempReading = (rxCmd[3]) * 256 + rxCmd[2];
                        MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\ntemp: " + tempReading / 10);
                        break;
                    }
                } catch (Exception e) {
                    MyUtil.displayMessage("get temp reading exception... 2");
                    break;
                }
            }

            // start measurement
            while (myVCP.usbStatus == VCPStatus.Connected && threadActive) {
                try {

                    MyUtil.displayMessage(MyUtil.getDisplayMessage() + "\n\nwaiting for blood...");
                    txCmd = new byte[]{0x51, 0x42, 0x00, 0x00, 0x00, 0x00, (byte) 0xa3, 0x36};
                    rxCmd = null;
                    boolean state = true;
                    long startReceiveInMillis = System.currentTimeMillis();
                    while (state) {
                        long endReceiveInMillis = System.currentTimeMillis();
                        rxCmd = myVCP.SendandReceive(txCmd);
                        if (rxCmd != null && rxCmd[6] == 0xa5) {
                            MyUtil.displayMessage(MyUtil.getDisplayMessage() + ".");//(MyUtil.getDisplayMessage() + myVCP.convertToHexString(rxCmd));
                            state = false;
                        }
                    }
                    if (rxCmd != null && rxCmd[1] == 0x42) {

                        switch (rxCmd[2]) {
                            case 0x2c:
                                MyUtil.displayMessage("user cancel measuring...");
                                isError = true;
                                break;

                            case 0x2d:
                                MyUtil.displayMessage("Influent sample is not enough...");
                                isError = true;
                                break;

                            case 0x00:
                            case 0x1a:
                                MyUtil.displayMessage("processing...");
                                NavHostFragment.findNavController(SecondFragment.this)
                                        .navigate(R.id.action_SecondFragment_to_ThirdFragment);
                                return;

                            default:
                                //MyUtil.displayMessage(myVCP.convertToHexString(rxCmd));
                                isError = true;
                        }
                    } else {
                        isError = true;
                    }

                } catch (Exception e) {
                    MyUtil.displayMessage("processing...");
                    NavHostFragment.findNavController(SecondFragment.this)
                            .navigate(R.id.action_SecondFragment_to_ThirdFragment);
                    return;

                }
                if (isError)
                    break;

            }

            while (isError) {
                try {

                    // check error code
                    txCmd = new byte[]{0x51, 0x2e, 0x00, 0x00, 0x00, 0x00, (byte) 0xa3, 0x22};
                    rxCmd = myVCP.SendandReceive(txCmd);
                    // MyUtil.displayMessage(myVCP.convertToHexString(rxCmd));
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
                    MyUtil.displayMessage("get error code exception... 2");
                    break;
                }
            }

            // wait for strip unplugged
            while (myVCP.usbStatus == VCPStatus.Connected && threadActive) {
                try {
                    txCmd = new byte[]{0x51, 0x21, 0x00, 0x00, 0x00, 0x00, (byte) 0xa3, 0x15};
                    rxCmd = myVCP.SendandReceive(txCmd);
                    //MyUtil.displayMessage(myVCP.convertToHexString(rxCmd));
                    if (rxCmd != null && rxCmd[1] == 0x21) {
                        if ((rxCmd[4] & 0x40) == 0) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    MyUtil.displayMessage("get status exception... 2");
                    break;
                }
            }

            NavHostFragment.findNavController(SecondFragment.this)
                    .navigate(R.id.action_SecondFragment_to_FirstFragment);
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
