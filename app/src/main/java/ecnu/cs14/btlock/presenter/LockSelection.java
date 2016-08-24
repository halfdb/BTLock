package ecnu.cs14.btlock.presenter;

import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import ecnu.cs14.btlock.R;
import ecnu.cs14.btlock.model.*;
import ecnu.cs14.btlock.view.AbstractListActivity;

import java.util.ArrayList;

public class LockSelection implements AdapterView.OnItemClickListener {
    public LockSelection(AbstractListActivity activity){
        mActivity = activity;
        mAdapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_list_item_1, mDevices);
    }

    private AbstractListActivity mActivity;
    private ArrayList<BTLock> mDevices = new ArrayList<>();
    private ArrayAdapter<BTLock> mAdapter;

    /**
     * Start a discovery for locks.
     * @return True unless some error happened.
     */
    public boolean startDiscovery(){
        // start bt
        if (!DeviceManager.isBtAvailable()) {
            if(mActivity.yesNoBox(mActivity.getString(R.string.enable_bt_or_not), "")) {
                if(!DeviceManager.enableBt()){
                    return false;
                }
            } else {
                return false;
            }
        }

        // register a scan callback
        DeviceManager.ScanCallback cb = new DeviceManager.ScanCallback(){
            @Override
            public void onFound(BluetoothDevice device, int rssi) {
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
                // initialize the array adapter
                mDevices.clear();
                mActivity.getListView().setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
                mActivity.getListView().setOnItemClickListener(LockSelection.this);
            }

            @Override
            public void onFinish() {
                DeviceManager.deregisterScanCallback(this);
            }
        };
        DeviceManager.registerScanCallback(cb);

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

    private void connectLock(BTLock lock) {
        synchronized (LockSelection.class) {
            lock.connect(mActivity);
        }
        mActivity.callFinish();
    }
}
