package ecnu.cs14.btlock.model;

import android.bluetooth.*;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class BTLock extends BluetoothGattCallback {

    final private static String TAG = BTLock.class.getClass().getSimpleName();

    final public static long SERVICE_UUID_MSD=0x11DB00000000L;
//    final public static UUID SERVICE_UUID = new UUID(SERVICE_UUID_MSD, 0);
//    final public static ParcelUuid SERVICE_PARCEL_UUID = new ParcelUuid(SERVICE_UUID);
    final public static long MASK_UUID_MSD=0xFFFF00000000L;
//    final public static UUID MASK_UUID = new UUID(MASK_UUID_MSD, 0);
//    final public static ParcelUuid MASK_PARCEL_UUID = new ParcelUuid(MASK_UUID);

    public abstract class BTLockCallback{
        public abstract void onGattConnect();
    }

    private BluetoothDevice mDevice;
    private BluetoothGatt mGatt;
    private BluetoothGattService mService = null;

    public BTLock(BluetoothDevice device){
        mDevice = device;
    }

    public BluetoothGatt connectGatt(Context context, BTLockCallback callback) throws Exception{
        mGatt = mDevice.connectGatt(context, false, this);
        if(null != mGatt){
            callback.onGattConnect();
        } else {
            throw new Exception("Failed to connect the gatt.");
        }
        return mGatt;
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable){
        boolean ret = mGatt.setCharacteristicNotification(characteristic, enable);
        if(!ret){
            Log.w(TAG, "Characteristic notification set failed.");
        }
        return ret;
    }

    public BluetoothGattService getService() throws Exception {
        if(null == mService){
            throw new Exception("Service not set yet.");
        }
        return mService;
    }

    public abstract class GeneralCallback{
        public abstract void callback(int status, BluetoothGattCharacteristic characteristic);
    }
    public static final int CB_CONNECTION_STATE_CHANGE = 0;
    public static final int CB_CHAR_READ = 1;
    public static final int CB_CHAR_WRITE = 2;
    public static final int CB_CHAR_CHANGE = 3;
    public static final int CB_WRITE_COMPLETE = 4;
    private static final int CB_COUNT = 5;
    private ArrayList<?>[] callbackLists = {
            new ArrayList<GeneralCallback>(),
            new ArrayList<GeneralCallback>(),
            new ArrayList<GeneralCallback>(),
            new ArrayList<GeneralCallback>(),
            new ArrayList<GeneralCallback>()
    };

    public void registerCallback(int index, GeneralCallback callback){
        if(index<0 || index>=CB_COUNT){
            Log.e(TAG, "Callback index out of range.");
            return;
        }
        ((ArrayList<GeneralCallback>)callbackLists[index]).add(callback);
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
        for(Object cb : callbackLists[CB_CONNECTION_STATE_CHANGE]){
            ((GeneralCallback)cb).callback(status, null);
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
        super.onServicesDiscovered(gatt, status);
        List<BluetoothGattService> list = mGatt.getServices();
        UUID uuid = null;
        for(BluetoothGattService service: list){
            uuid = service.getUuid();
            if((uuid.getMostSignificantBits() & MASK_UUID_MSD) == SERVICE_UUID_MSD){
                break;
            }
        }
        if(null != uuid){
            mService = mGatt.getService(uuid);
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
}
