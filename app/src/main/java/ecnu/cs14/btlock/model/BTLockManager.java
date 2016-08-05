package ecnu.cs14.btlock.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

public class BTLockManager {
    private static final String TAG = BTLockManager.class.getSimpleName();
    private static BTLockManager instance = new BTLockManager();

    public static BTLockManager getInstance() {
        return instance;
    }

    private BTLockManager() { }

    private static BTLock sLock = null;

    private static boolean hasLock(){
        return (sLock != null);
    }

    public static void connectLock(BluetoothDevice device, Context context){
        if (hasLock()) {
            disconnectLock();
        }
        sLock = new BTLock(device);
        try {
            sLock.connectGatt(context, new BTLock.BTLockCallback() {
                @Override
                public void onGattConnect(BluetoothGatt gatt) {
                    gatt.discoverServices();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        switch (sLock.isLock()){
            case BTLock.IL_UNKNOWN:
                sLock.registerCallback(BTLock.CB_IS_LOCK, new BTLock.GeneralCallback(){
                    @Override
                    public void callback(int status, BluetoothGattCharacteristic characteristic) {
                        switch (status){
                            case BTLock.IL_FALSE:
                                Log.w(TAG, "Not a BTLock. Disconnecting...");
                                disconnectLock();
                                break;
                            case BTLock.IL_TRUE:
                                break;
                        }
                    }
                });
                break;
            case BTLock.IL_FALSE:
                Log.w(TAG, "Not a BTLock. Disconnecting...");
                disconnectLock();
                break;
            case BTLock.IL_TRUE:
                break;
        }
    }

    public static void disconnectLock(){
        if (!hasLock()){
            return;
        }
        sLock.close();
        sLock = null;
    }

    /**
     * Write the data to the characteristic in the remote device with
     * a reliable method. Time-consuming. It is recommended to run
     * this method in a new thread.
     * @param data              The data to be sent.
     * @param characteristic    The characteristic to which the data
     *                          will be written.
     * @return Whether succeeded.
     */
    public static synchronized boolean writeCharacteristic(Data data, BluetoothGattCharacteristic characteristic) {
        if(!hasLock())
        {
            return false;
        }
        byte[] dataArray = new byte[Data.SIZE];
        for (int i = 0; i < Data.SIZE; i++) {
            dataArray[i] = data.get(i);
        }
        characteristic.setValue(dataArray);
        return sLock.writeCharacteristic(characteristic);
    }

    /**
     * Write the data and fetch the response from the device.
     * Time-consuming.
     * @param data              The data to be sent.
     * @param characteristic    The characteristic to be watched.
     * @return The response. {@code null} if error.
     */
    @Nullable
    public static Data writeCharacteristicAndReadResponse(final Data data, final BluetoothGattCharacteristic characteristic) {
        final Object lock = new Object();
        BTLock.GeneralCallback cb = new BTLock.GeneralCallback() {
            @Override
            public synchronized void callback(int status, BluetoothGattCharacteristic c) {
                if (c.getUuid() != characteristic.getUuid())
                {
                    return;
                }
                Data newData = null;
                try {
                    newData = new Data(c.getValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (data.equals(newData)) {
                    return;
                }
                synchronized (lock) {
                    characteristic.setValue(c.getValue());
                    lock.notify();
                }
            }
        };
        Data ret;
        sLock.registerCallback(BTLock.CB_CHAR_CHANGE, cb);
        sLock.setCharacteristicNotification(characteristic, true);
        try {
            synchronized (lock) {
                if (!writeCharacteristic(data, characteristic)) {
                    throw new Exception("Failed to write.");
                }
                lock.wait(5000);
            }
            ret = new Data(characteristic.getValue());
        } catch (Exception e) {
            Log.e(TAG, "Unable to write or read the response. Message: " + e.getMessage());
            ret = null;
        }
        sLock.setCharacteristicNotification(characteristic, false);
        sLock.deregisterCallback(BTLock.CB_CHAR_CHANGE, cb);
        return ret;
    }

    /**
     * Read the updated characteristic from the device. Time-consuming.
     * @param characteristic The characteristic to be read.
     * @return The data in the characteristic. {@code null} if error.
     */
    @Nullable
    public static synchronized Data readUpdatedCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(!hasLock())
        {
            return null;
        }
        Data ret = null;
        try {
            ret = new Data(sLock.readCharacteristic(characteristic));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        return ret;
    }
}
