package ecnu.cs14.btlock.model;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.*;

public class DeviceManager {
    private static String TAG = DeviceManager.class.getSimpleName();

    private DeviceManager() { }

    private static BluetoothAdapter sAdapter = BluetoothAdapter.getDefaultAdapter();

    public static boolean isBtAvailable(){
        if(sAdapter == null){
            Log.e(TAG, "Bluetooth adapter not set.");
            return false;
        }
        return sAdapter.isEnabled();
    }
    public static boolean enableBt(){
        return sAdapter!=null && sAdapter.enable();
    }

    public interface ScanCallback{
        void onFound(BluetoothDevice device, int rssi);

        void onStart();

        void onFinish();

        void onError();
    }
    private static HashSet<ScanCallback> sCallbacks = new HashSet<>();

    public static void registerScanCallback(ScanCallback callback){
        Log.d(TAG, "registerScanCallback: A callback added");
        sCallbacks.add(callback);
    }
    public static void deregisterScanCallback(ScanCallback callback){
        sCallbacks.remove(callback);
    }

    private static BroadcastReceiver sReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: called");
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                for(ScanCallback callback: sCallbacks){
                    callback.onFound(device, rssi);
                }
            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                for(ScanCallback callback: sCallbacks){
                    callback.onStart();
                }
            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                for(ScanCallback callback: sCallbacks){
                    callback.onFinish();
                }
                context.unregisterReceiver(this);
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        if(sScanFlag) {
                            startLeScan();
                        }
                        break;
                }
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                synchronized (bondLock) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (waitingDevice == null) {
                        return;
                    }
                    if (waitingBond && device.getAddress().equals(waitingDevice.getAddress())) {
                        int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                        switch (state) {
                            case BluetoothDevice.BOND_BONDED:
                                Log.d(TAG, "onReceive: notify");
                                bondLock.notify();
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
    };

    private static IntentFilter sFilter = new IntentFilter();
    static {
//        sFilter.addAction(BluetoothDevice.ACTION_FOUND);
        sFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        sFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        sFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        sFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    }

    private static android.bluetooth.le.ScanCallback sLeCallback = new android.bluetooth.le.ScanCallback() {
        /**
         * Callback when a BLE advertisement has been found.
         *
         * @param callbackType Determines how this callback was triggered. Could be one of
         *            {@link ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
         *            {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
         *            {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
         * @param result A Bluetooth LE scan result.
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            int rssi = result.getRssi();
            for (ScanCallback cb :
                    sCallbacks) {
                cb.onFound(device, rssi);
            }
        }

        /**
         * Callback when batch results are delivered.
         *
         * @param results List of scan results that are previously scanned.
         */
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result :
                    results) {
                onScanResult(0, result);
            }
        }

        /**
         * Callback when scan could not be started.
         *
         * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
         */
        @Override
        public void onScanFailed(int errorCode) {
            if (errorCode == SCAN_FAILED_ALREADY_STARTED) {
                return;
            }
            Log.e(TAG, "onScanFailed: error code:" + errorCode);
            for (ScanCallback cb :
                    sCallbacks) {
                cb.onError();
            }
        }
    };

    private static void startLeScan() {
        Log.d(TAG, "startLeScan");
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(
                        new ParcelUuid(new UUID(BTLock.SERVICE_UUID_MSD, 0)),
                        new ParcelUuid(new UUID(BTLock.MASK_UUID_MSD, 0))
                )
                .build();
        ScanSettings settings = new ScanSettings.Builder().build();
        ArrayList<ScanFilter> a = new ArrayList<>();
        a.add(filter);
//        sAdapter.getBluetoothLeScanner().startScan(a, settings, sLeCallback);
        sAdapter.getBluetoothLeScanner().startScan(sLeCallback);
//        sAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
//            @Override
//            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//                for (ScanCallback cb :
//                        sCallbacks) {
//                    cb.onFound(device, rssi);
//                }
//            }
//        });
    }

    static boolean sScanFlag;
    public static boolean startDiscovery(Context context) {
        context.registerReceiver(sReceiver, sFilter);
        sScanFlag = sAdapter.getState() != BluetoothAdapter.STATE_ON;
        if (!sScanFlag) {
            startLeScan();
        }
        return true;
    }
    public static boolean cancelDiscovery(Context context) {
        try {
            context.unregisterReceiver(sReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "cancelDiscovery: IllegalArgument", e);
        }
        try {
            sAdapter.getBluetoothLeScanner().stopScan(sLeCallback);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static final Object bondLock = new Object();
    private static boolean waitingBond = false;
    private static BluetoothDevice waitingDevice;
    public static boolean createBond(BluetoothDevice device) {
        Log.d(TAG, "createBond: start");
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDED:
                return true;
            case BluetoothDevice.BOND_NONE:
                synchronized (bondLock) {
                    waitingDevice = device;
                    waitingBond = true;
                    device.createBond();
                    try {
                        bondLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case BluetoothDevice.BOND_BONDING:
                synchronized (bondLock) {
                    waitingDevice = device;
                    waitingBond = true;
                    try {
                        bondLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
        return (device.getBondState() == BluetoothDevice.BOND_BONDED);
    }
}
