package ecnu.cs14.btlock.model;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.HashSet;

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
    }
    private static HashSet<ScanCallback> sCallbacks = new HashSet<>();

    public static void registerScanCallback(ScanCallback callback){
        sCallbacks.add(callback);
    }
    public static void deregisterScanCallback(ScanCallback callback){
        sCallbacks.remove(callback);
    }

    private static BroadcastReceiver sReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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
            }
        }
    };

    private static IntentFilter sFilter = new IntentFilter();
    static {
        sFilter.addAction(BluetoothDevice.ACTION_FOUND);
        sFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        sFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    }

    public static boolean startDiscovery(Context context) {
        context.registerReceiver(sReceiver, sFilter);
        return sAdapter.startDiscovery();
    }
    public static boolean cancelDiscovery(Context context) {
        boolean ret = sAdapter.cancelDiscovery();
        context.unregisterReceiver(sReceiver);
        return ret;
    }
}
