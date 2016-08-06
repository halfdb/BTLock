package ecnu.cs14.btlock.model;

import android.bluetooth.BluetoothGattCharacteristic;

public abstract class Task {
    private BluetoothGattCharacteristic mChar;
    private Data mDataToSend;
    private Data mMasks;
    private Data mExpectedResponse;
    private Handler mHandler;

    public interface Handler{
        void onReceive(Data response);
        void onUnexpected(Data response);
    }

    public Task(BluetoothGattCharacteristic characteristic, Data dataToSend, Data expectedResponse, Handler callback){
        mChar = characteristic;
        mDataToSend = dataToSend;
        mMasks = new Data();
        for (int i = 0; i < Data.SIZE; i++) {
            mMasks.set(i, (byte)-127);
        }
        mExpectedResponse = expectedResponse;
        mHandler = callback;
    }

    public Task(BluetoothGattCharacteristic characteristic, Data dataToSend, Data expectedResponse, Data masks, Handler callback) {
        mChar = characteristic;
        mDataToSend = dataToSend;
        mMasks = masks;
        mExpectedResponse = expectedResponse;
        mHandler = callback;
    }

    public void execute(){
        Data response = BTLockManager.writeCharacteristicAndReadResponse(mDataToSend, mChar);
        if (response == null) {
            mHandler.onUnexpected(null);
            return;
        }
        Data maskedResponse = new Data();
        for (int i = 0; i < Data.SIZE; i++) {
            byte b = response.get(i);
            b = (byte) (b & mMasks.get(i));
            maskedResponse.set(i, b);
        }
        if (!maskedResponse.equals(mExpectedResponse)) {
            mHandler.onUnexpected(response);
        } else {
            mHandler.onReceive(response);
        }
    }
}
