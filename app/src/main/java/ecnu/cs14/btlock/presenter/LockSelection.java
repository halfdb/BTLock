package ecnu.cs14.btlock.presenter;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import ecnu.cs14.btlock.R;
import ecnu.cs14.btlock.model.*;
import ecnu.cs14.btlock.view.AbstractListActivity;

import java.util.ArrayList;

public class LockSelection implements AdapterView.OnItemClickListener {
    static final String TAG = LockSelection.class.getSimpleName();

    public LockSelection(AbstractListActivity activity){
        mActivity = activity;
        mAdapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_list_item_1, mDevices);

        // initialize ListView
        mActivity.getListView().setAdapter(mAdapter);
        mActivity.getListView().setOnItemClickListener(LockSelection.this);
    }

    private AbstractListActivity mActivity;
    private ArrayList<BTLock> mDevices = new ArrayList<>();
    private ArrayAdapter<BTLock> mAdapter;
    private DeviceManager.ScanCallback mCallback;

    /**
     * Start a discovery for locks.
     * @return True unless some error happened.
     */
    public boolean startDiscovery(){
        // start bt
        if (!DeviceManager.isBtAvailable()) {
            final Object lock = new Object();
            synchronized (lock) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.yesNoBox(mActivity.getString(R.string.enable_bt_or_not), "");
                        synchronized (lock){
                            Log.d(TAG, "run: notify");
                            lock.notify();
                        }
                    }
                }).start();
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(mActivity.getYesOrNo()) {
                if(!DeviceManager.enableBt()){
                    return false;
                }
            } else {
                return false;
            }
        }

        final LockInfoOperator lio = new LockInfoOperator(mActivity);

        // register a scan callback
        mCallback = new DeviceManager.ScanCallback(){
            boolean connecting = false;

            @Override
            public void onFound(BluetoothDevice device, int rssi) {
                Log.d(TAG, "onFound");

                if(!connecting) {
                    if (lio.isLockLogged(device.getAddress())) {
                        final BTLock l = new BTLock(device, rssi);
                        connectLock(l);
                        connecting = true;
                        mActivity.toast(mActivity.getString(R.string.auto_connect_to) + lio.getNickname(l));
                        return;
                    }
                }

                for (BTLock l : mDevices) {
                    String address = l.getAddress();
                    if (address.equals(device.getAddress())) {
                        return;
                    }
                }

                BTLock l = new BTLock(device, rssi);
                int i = 0;
                for (; i < mDevices.size(); i++) {
                    if (mDevices.get(i).compareTo(l) < 0) {
                        break;
                    }
                }
                mDevices.add(i, l);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onStart() {
                Log.d(TAG, "onStart");
                // initialize the array adapter
                mDevices.clear();
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "onFinish");
                DeviceManager.deregisterScanCallback(this);
            }

            @Override
            public void onError() {
                DeviceManager.deregisterScanCallback(this);
                mActivity.finish();
            }
        };
        DeviceManager.registerScanCallback(mCallback);

        // start discovery and return
        return DeviceManager.startDiscovery(mActivity);
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        connectLock((BTLock) parent.getItemAtPosition(position));
    }

    private void connectLock(final BTLock lock) {
        mActivity.startWaiting();
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.connect(mActivity);
                mActivity.stopWaiting();
                mActivity.finish();
            }
        }).start();
    }

    public void finish(){
        DeviceManager.deregisterScanCallback(mCallback);
        DeviceManager.cancelDiscovery(mActivity);
    }
}
