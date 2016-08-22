package ecnu.cs14.btlock.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashSet;

public class BTLockManager {
    private static final String TAG = BTLockManager.class.getSimpleName();

    private BTLockManager() { }

    private static BTLock sLock = null;

    public static boolean hasLock(){
        return (sLock != null);
    }

    private static final BTLock.GeneralCallback sStateCallback = new BTLock.GeneralCallback() {
        @Override
        public void callback(int newState, BluetoothGattCharacteristic characteristic) {
            switch (newState) {
                case BluetoothProfile.STATE_DISCONNECTED:
                    disconnectLock();
                    break;
            }
        }
    };

    public static void connectLock(BluetoothDevice device, Context context){
        if (hasLock()) {
            disconnectLock();
        }

        sLock = new BTLock(device);
        sLock.registerCallback(BTLock.CB_CONNECTION_STATE_CHANGE, sStateCallback);

        final Object lock = new Object();

        synchronized (lock) {
            BTLock.GeneralCallback cb = new BTLock.GeneralCallback() {
                @Override
                public void callback(int status, BluetoothGattCharacteristic characteristic) {
                    synchronized (lock) {
                        switch (status) {
                            case BTLock.IL_FALSE:
                                Log.w(TAG, "Not a BTLock. Disconnecting...");
                                disconnectLock();
                            case BTLock.IL_TRUE:
                                Log.i(TAG, "The address of the device is " + sLock.getAddress());
                                break;
                        }
                        lock.notify();
                    }
                }
            };
            sLock.registerCallback(BTLock.CB_IS_LOCK, cb);
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
            switch (sLock.isLock()) {
                case BTLock.IL_UNKNOWN:
                    try {
                        Log.i(TAG, "Whether a lock unknown. Waiting...");
                        lock.wait(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case BTLock.IL_FALSE:
                    Log.w(TAG, "Not a BTLock. Disconnecting...");
                    disconnectLock();
                case BTLock.IL_TRUE:
                    Log.i(TAG, "The address of the device is " + sLock.getAddress());
                    break;
            }
            sLock.deregisterCallback(BTLock.CB_IS_LOCK, cb);
        }
    }

    public static BluetoothGattCharacteristic getMainCharacteristic() {
        return sLock.getmMainCharacteristic();
    }

    private static HashSet<Runnable> callbacks = new HashSet<>();

    public static void registerDisconnectionCallback(Runnable callback) {
        callbacks.add(callback);
    }

    public static void deregisterDisconnectionCallback(Runnable callback) {
        callbacks.remove(callback);
    }

    public static void disconnectLock(){
        BTLock lock = sLock;
        sLock = null;
        if (lock == null){
            return;
        }
        lock.close();
        for (Runnable callback: callbacks) {
            callback.run();
        }
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
        characteristic.setValue(data.byteArray());
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
