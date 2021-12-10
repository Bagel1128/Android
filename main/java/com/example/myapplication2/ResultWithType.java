package  com.example.myapplication2;

import com.taidoc.pclinklibrary.constant.PCLinkLibraryEnum;

public class ResultWithType {

    private PCLinkLibraryEnum.BloodGlucoseType responseType;
    private int responseValue;
    private int responseHCTValue;
    public ResultWithType() {
    }

    public PCLinkLibraryEnum.BloodGlucoseType getResponseType() {
        return responseType;
    }

    public void setResponseType(PCLinkLibraryEnum.BloodGlucoseType responseType) {
        this.responseType = responseType;
    }

    public void setResponseValue(int responseValue) {
        this.responseValue = responseValue;
    }

    public int getResponseValue() {
        return responseValue;
    }

    public int getResponseHCTValue() {
        return responseHCTValue;
    }

    public void setResponseHCTValue(int responseHCTValue) {
        this.responseHCTValue = responseHCTValue;
    }

    private PCLinkLibraryEnum.BloodGlucoseType mTypeOfMeasureing;//0:GLU 3:QC
    private PCLinkLibraryEnum.BloodGlucoseType mTypeOfTestStript;//GLU,KT,CHOL,TG,UA,etc.

    public PCLinkLibraryEnum.BloodGlucoseType getmTypeOfMeasureing() {
        return mTypeOfMeasureing;
    }

    public void setmTypeOfMeasureing(PCLinkLibraryEnum.BloodGlucoseType mTypeOfMeasureing) {
        this.mTypeOfMeasureing = mTypeOfMeasureing;
    }

    public PCLinkLibraryEnum.BloodGlucoseType getmTypeOfTestStript() {
        return mTypeOfTestStript;
    }

    public void setmTypeOfTestStript(PCLinkLibraryEnum.BloodGlucoseType mTypeOfTestStript) {
        this.mTypeOfTestStript = mTypeOfTestStript;
    }
}
