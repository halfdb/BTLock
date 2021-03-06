package ecnu.cs14.btlock.model;

import android.bluetooth.*;
import android.content.Context;
import android.util.Log;

import java.util.*;

class BTLock extends BluetoothGattCallback {

    final private static String TAG = BTLock.class.getSimpleName();

    private BTLock() { }

    final public static long SERVICE_UUID_MSD = 0x11DB00000000L;
    final public static long MAIN_CHAR_UUID_MSD = 0x11DC00000000L;
    final public static long MASK_UUID_MSD = 0xFFFF00000000L;

    public static abstract class BTLockCallback{
        public abstract void onGattConnect(BluetoothGatt gatt);
    }

    private BluetoothDevice mDevice;
    private BluetoothGatt mGatt;
    private BluetoothGattService mService = null;
    private BluetoothGattCharacteristic mMainCharacteristic = null;

    public BTLock(BluetoothDevice device){
        mDevice = device;
    }

    public BluetoothGatt connectGatt(Context context) throws Exception{
        mGatt = mDevice.connectGatt(context, false, this);
        if(null == mGatt) {
            throw new Exception("Failed to connect the gatt.");
        }
        if (mGatt.getServices().size() != 0) {
            onServicesDiscovered(mGatt, 0);
        }
        return mGatt;
    }

    public String getAddress(){
        return mDevice.getAddress();
    }

    public String getName() {
        return mDevice.getName();
    }

    public BluetoothGattCharacteristic getmMainCharacteristic() { return mMainCharacteristic; }

    public void close(){
        Log.d(TAG, "close");
        BluetoothGatt gatt = mGatt;
        mGatt = null;
        if (gatt != null) {
            gatt.disconnect();
        }
        mService = null;
        mDevice = null;
        mMainCharacteristic = null;
        for (HashSet s : callbackLists) {
            s.clear();
        }
    }

    public BluetoothGattService getService() throws Exception {
        if(null == mService){
            throw new Exception("Service not set yet.");
        }
        return mService;
    }

    public static final int IL_UNKNOWN = -1;
    public static final int IL_FALSE = 0;
    public static final int IL_TRUE = 1;
    private int mIsLock = IL_UNKNOWN;
    public int isLock(){
        return mIsLock;
    }

    public static abstract class GeneralCallback{
        public int result = BluetoothGatt.GATT_SUCCESS - 1;
        public abstract void callback(int status, BluetoothGattCharacteristic characteristic);
    }
    public static final int CB_CONNECTION_STATE_CHANGE = 0;
    public static final int CB_CHAR_READ = 1;
    public static final int CB_CHAR_WRITE = 2;
    public static final int CB_CHAR_CHANGE = 3;
    public static final int CB_WRITE_COMPLETE = 4;
    public static final int CB_IS_LOCK = 5;
    private static final int CB_COUNT = 6;
    private static HashSet<?>[] callbackLists = {
            new HashSet<GeneralCallback>(),
            new HashSet<GeneralCallback>(),
            new HashSet<GeneralCallback>(),
            new HashSet<GeneralCallback>(),
            new HashSet<GeneralCallback>(),
            new HashSet<GeneralCallback>()
    };

    public void registerCallback(int index, GeneralCallback callback){
        if(index<0 || index>=CB_COUNT){
            Log.e(TAG, "Callback index out of range.");
            return;
        }
        ((HashSet<GeneralCallback>)callbackLists[index]).add(callback);
    }

    public void deregisterCallback(int index, GeneralCallback callback){
        if(index<0 || index>=CB_COUNT){
            Log.e(TAG, "Callback index out of range.");
            return;
        }
        callbackLists[index].remove(callback);
    }


    /**
     * Callback indicating when GATT client has connected/disconnected to/from a remote
     * GATT server.
     *
     * @param gatt GATT client
     * @param status Status of the connect or disconnect operation.
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     * @param newState Returns the new connection state. Can be one of
     *                  {@link BluetoothProfile#STATE_DISCONNECTED} or
     *                  {@link BluetoothProfile#STATE_CONNECTED}
     */
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                        int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if(newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices();
        }
        for(Object cb : callbackLists[CB_CONNECTION_STATE_CHANGE]){
            ((GeneralCallback)cb).callback(newState, null);
        }
    }

    /**
     * Callback invoked when the list of remote services, characteristics and descriptors
     * for the remote device have been updated, ie new services have been discovered.
     *
     * @param gatt   GATT client invoked {@link BluetoothGatt#discoverServices}
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the remote device
     */
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (mService != null) {
            return;
        }
        List<BluetoothGattService> list = mGatt.getServices();
        UUID uuid = null;
        for(BluetoothGattService service: list){
            uuid = service.getUuid();
            if((uuid.getMostSignificantBits() & MASK_UUID_MSD) == SERVICE_UUID_MSD){
                break;
            } else {
                uuid = null;
            }
        }
        if(null != uuid){
            mService = mGatt.getService(uuid);

            for (BluetoothGattCharacteristic c: mService.getCharacteristics()) {
                if((c.getUuid().getMostSignificantBits() & MASK_UUID_MSD) == MAIN_CHAR_UUID_MSD) {
                    mMainCharacteristic = c;
                    mGatt.setCharacteristicNotification(c, true);
                    break;
                }
            }

            if(null != mMainCharacteristic){
                mIsLock = IL_TRUE;
            } else {
                mIsLock = IL_FALSE;
            }
        } else {
            mIsLock = IL_FALSE;
        }
        for(Object cb : callbackLists[CB_IS_LOCK]){
            ((GeneralCallback)cb).callback(mIsLock, null);
        }
    }

    /**
     * Callback reporting the result of a characteristic read operation.
     *
     * @param gatt           GATT client invoked {@link BluetoothGatt#readCharacteristic}
     * @param characteristic Characteristic that was read from the associated
     *                       remote device.
     * @param status         {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        for(Object cb : callbackLists[CB_CHAR_READ]){
            ((GeneralCallback)cb).callback(status, characteristic);
        }
    }

    /**
     * Callback indicating the result of a characteristic write operation.
     * <p/>
     * <p>If this callback is invoked while a reliable write transaction is
     * in progress, the value of the characteristic represents the value
     * reported by the remote device. An application should compare this
     * value to the desired value to be written. If the values don't match,
     * the application must abort the reliable write transaction.
     *
     * @param gatt           GATT client invoked {@link BluetoothGatt#writeCharacteristic}
     * @param characteristic Characteristic that was written to the associated
     *                       remote device.
     * @param status         The result of the write operation
     *                       {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        for(Object cb : callbackLists[CB_CHAR_WRITE]){
            ((GeneralCallback)cb).callback(status, characteristic);
        }
    }

    /**
     * Callback triggered as a result of a remote characteristic notification.
     *
     * @param gatt           GATT client the characteristic is associated with
     * @param characteristic Characteristic that has been updated as a result
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        for(Object cb : callbackLists[CB_CHAR_CHANGE]){
            ((GeneralCallback)cb).callback(0, characteristic);
        }
    }

    /**
     * Callback invoked when a reliable write transaction has been completed.
     *
     * @param gatt   GATT client invoked {@link BluetoothGatt#executeReliableWrite}
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the reliable write
     */
    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
        for(Object cb : callbackLists[CB_WRITE_COMPLETE]){
            ((GeneralCallback)cb).callback(status, null);
        }
    }

    public synchronized boolean writeCharacteristic(BluetoothGattCharacteristic characteristic){
        GeneralCallback cb = new GeneralCallback(){
            @Override
            public void callback(int status, BluetoothGattCharacteristic characteristic) {
                Log.d(TAG, "writeChar: callback: write complete");
                result = status;
                synchronized (BTLock.this) {
                    BTLock.this.notify();
                }
            }
        };
//        registerCallback(CB_WRITE_COMPLETE, cb);
        boolean ret = mGatt.writeCharacteristic(characteristic);
//        if(ret){
//            Log.d(TAG, "writeCharacteristic: trying to execute reliable write");
//            ret = mGatt.beginReliableWrite();
//            if (ret) {
//                try {
//                    wait(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                    ret = false;
//                }
//                if (ret) {
//                    if (cb.result != BluetoothGatt.GATT_SUCCESS) {
//                        ret = false;
//                    }
//                }
//            }
//        }
//        deregisterCallback(CB_WRITE_COMPLETE, cb);
        return ret;
    }

    public byte[] readCharacteristic(final BluetoothGattCharacteristic characteristic) throws Exception {
        final Object lock = new Object();
        GeneralCallback cb = new GeneralCallback() {
            @Override
            public void callback(int status, BluetoothGattCharacteristic c) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    characteristic.setValue(c.getValue());
                }
                result = status;
                synchronized (lock) {
                    lock.notify();
                }
            }
        };
        registerCallback(CB_CHAR_READ, cb);
        synchronized (lock){
            if(mGatt.readCharacteristic(characteristic)) {
                try {
                    lock.wait(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        deregisterCallback(CB_CHAR_READ, cb);
        if (cb.result != BluetoothGatt.GATT_SUCCESS) {
            throw new Exception("Failed to read the characteristic.");
        }
        return characteristic.getValue();
    }
}
