package com.example.myapplication2;

public enum VCPStatus {
    Idle(1),
    Connected(2),
    Disconnected(3),
    Detached(4),
    Permission(5),
    STATE_CANCEL_PERMISSION(9);
    private int value;
    VCPStatus(int value){this.value= value;}

    public void setValue(int value) {
        this.value = value;
    }

    public int getValue(){return this.value;}
}
