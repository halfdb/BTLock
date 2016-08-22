package ecnu.cs14.btlock.model;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.Nullable;
import android.util.Log;

public class Task {
    private static final String TAG = Task.class.getSimpleName();

    private BluetoothGattCharacteristic mChar;
    private Data mDataToSend;
    private Data mMasks;
    private Data mExpectedResponse;
    private Handler mHandler;

    public interface Handler{
        void onReceive(Data response);
        void onUnexpected(Data response);
    }

    public Task(BluetoothGattCharacteristic characteristic, Data dataToSend, Data expectedResponse, @Nullable Handler callback){
        mChar = characteristic;
        mDataToSend = dataToSend;
        mMasks = new Data();
        for (int i = 0; i < Data.SIZE; i++) {
            mMasks.set(i, (byte)-1);
        }
        mExpectedResponse = expectedResponse;
        mHandler = callback;
    }

    public Task(BluetoothGattCharacteristic characteristic, Data dataToSend, Data expectedResponse, Data masks, @Nullable Handler callback) {
        mChar = characteristic;
        mDataToSend = dataToSend;
        mMasks = masks;
        mExpectedResponse = expectedResponse;
        mHandler = callback;
    }

    public boolean execute(boolean useCallback){
        Log.i(TAG, "The following Data is going to be sent: " + mDataToSend);

        if(mHandler == null) {
            useCallback = false;
        }

        Data response = BTLockManager.writeCharacteristicAndReadResponse(mDataToSend, mChar);
        if (response == null) {
            if(useCallback) {
                mHandler.onUnexpected(null);
            }
            return false;
        }
        Data maskedResponse = new Data();
        for (int i = 0; i < Data.SIZE; i++) {
            byte b = response.get(i);
            b = (byte) (b & mMasks.get(i));
            maskedResponse.set(i, b);
        }
        if(useCallback) {
            if (!maskedResponse.equals(mExpectedResponse)) {
                mHandler.onUnexpected(response);
            } else {
                mHandler.onReceive(response);
            }
        }
        return maskedResponse.equals(mExpectedResponse);
    }
}
