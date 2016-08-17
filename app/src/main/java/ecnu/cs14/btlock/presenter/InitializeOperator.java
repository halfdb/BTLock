package ecnu.cs14.btlock.presenter;

import ecnu.cs14.btlock.model.*;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

public class InitializeOperator {
    public InitializeOperator(Context context, BluetoothDevice device){
        BTLockManager.connectLock(device, context);
    }
}
