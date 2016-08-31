package ecnu.cs14.btlock.presenter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import ecnu.cs14.btlock.model.BTLockManager;

class BTLock implements Comparable{
    static final String TAG = BTLock.class.getSimpleName();

    private static BTLock sCurrentLock;

    private static Runnable disconnectionCallback = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "disconnectionCallback");
            sCurrentLock = null;
            BTLockManager.deregisterDisconnectionCallback(this);
        }
    };

    private static void updateHasLock() {
        if(!BTLockManager.hasLock()) {
            sCurrentLock = null;
        }
    }

    public static boolean hasLock() {
//        updateHasLock();
        return (sCurrentLock != null);
    }

    public synchronized static BTLock getCurrentLock() {
//        updateHasLock();
        return sCurrentLock;
    }

    private BluetoothDevice mDevice;
    private int mRssi;

    public BTLock(BluetoothDevice device, int rssi) {
        mDevice = device;
        mRssi = rssi;
    }

    public String getName() {
        return mDevice.getName();
    }

    public String getAddress() {
        return mDevice.getAddress();
    }

    public BluetoothGattCharacteristic getMainChar() {
        return BTLockManager.getMainCharacteristic();
    }

    public int getRssi(){
        return mRssi;
    }

    /**
     * Compares this object to the specified object to determine their relative
     * order.
     *
     * @param another the object to compare to this instance.
     * @return a negative integer if this instance is less than {@code another};
     * a positive integer if this instance is greater than
     * {@code another}; 0 if this instance has the same order as
     * {@code another}.
     * @throws ClassCastException if {@code another} cannot be converted into something
     *                            comparable to {@code this} instance.
     */
    @Override
    public int compareTo(@NonNull Object another) {
        if (another instanceof BTLock) {
            BTLock a = (BTLock) another;
            return this.mRssi - a.mRssi;
        }
        throw new ClassCastException("The Object should be an instance of BTLock.");
    }

    /**
     * Returns a string containing a concise, human-readable description of this
     * object. Subclasses are encouraged to override this method and provide an
     * implementation that takes into account the object's type and data. The
     * default implementation is equivalent to the following expression:
     * <pre>
     *   getClass().getName() + '@' + Integer.toHexString(hashCode())</pre>
     * <p>See <a href="{@docRoot}reference/java/lang/Object.html#writing_toString">Writing a useful
     * {@code toString} method</a>
     * if you intend implementing your own {@code toString} method.
     *
     * @return a printable representation of this object.
     */
    @Override
    public String toString() {
        String ret = mDevice.getName();
        if (ret == null) {
            ret = "Unknown";
        }
        return ret;
    }

    public void connect(Context context) {
        BTLockManager.registerDisconnectionCallback(disconnectionCallback);
        synchronized (BTLock.class) {
            BTLockManager.connectLock(mDevice, context);
            sCurrentLock = this;
        }
    }

    public static void disconnect() {
        if (hasLock()) {
            sCurrentLock = null;
            BTLockManager.disconnectLock();
        }
    }
}
