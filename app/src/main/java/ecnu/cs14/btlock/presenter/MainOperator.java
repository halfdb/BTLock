package ecnu.cs14.btlock.presenter;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import ecnu.cs14.btlock.R;
import ecnu.cs14.btlock.model.*;
import ecnu.cs14.btlock.view.AbstractMainActivity;

import java.util.HashSet;

public class MainOperator {
    private static final String TAG = MainOperator.class.getSimpleName();

    private AbstractMainActivity mActivity;

    private Runnable mDisconnectionCallback;

    public MainOperator(AbstractMainActivity activity) {
        mActivity = activity;
        if (BTLock.hasLock()) {
            mDisconnectionCallback = new Runnable() {
                @Override
                public void run() {
                    mIsReady = false;
                    mActivity.disableUnlock();
                    mActivity.toast(mActivity.getString(R.string.lock_disconnected));
                }
            };
            BTLockManager.registerDisconnectionCallback(mDisconnectionCallback);
            prepareForUnlock();
            mActivity.enableUnlock();
        } else {
            mActivity.disableUnlock();
            mActivity.toast(mActivity.getString(R.string.no_lock_available));
            mActivity.startInitializingActivity();
        }
    }

    private BluetoothGattCharacteristic mMainChar;
    private Data mUnlockCommand;
    private Data mExpectedResponse;

    private boolean mIsReady = false;

    public void prepareForUnlock() {
        BTLock lock = BTLock.getCurrentLock();
        if (lock == null) {
            return;
        }

        // prepare main characteristic
        mMainChar = lock.getMainChar();

        // find stored uids
        AccountStorage as = new AccountStorage(mActivity, lock.getAddress());
        HashSet<Byte> uids = as.getStoredUids();
        if (uids.size() == 0) {
            return;
        }

        // find the best account
        byte uidCandidate = -128;
        for (byte uid: uids) {
            if (uid > uidCandidate) {
                uidCandidate = uid;
            } else if (CommandCode.uid.getGuestNum(uid) == 0) {
                uidCandidate = uid;
                break;
            }
        }
        Account account = as.getStoredAccount(uidCandidate);

        // build the command to unlock the BTLock
        byte commandHead = CommandCode.unlock.getCmdUnlock(account.getUid());
        mUnlockCommand = Data.extendPassword(account.getPassword());
        mUnlockCommand.set(0, commandHead);
        mExpectedResponse = new Data();
        mExpectedResponse.set(0, (byte)(CommandCode.unlock.CMD_ACK | commandHead));
        for (int i = 1; i < Data.SIZE; i++) {
            mExpectedResponse.set(i, (byte) 0);
        }

        mIsReady = true;
    }

    public boolean unlock() {
        if (!mIsReady) {
            return false;
        }

        Task.Handler cb = new Task.Handler() {
            @Override
            public void onReceive(Data response) {

            }

            @Override
            public void onUnexpected(Data response) {
                if (response == null) {
                    Log.e(TAG, "Failed to fetch the response after sending.");
                } else {
                    Log.e(TAG, "Unexpected response: " + response.toString());
                }
            }
        };

        if (!new Task(mMainChar, mUnlockCommand, mExpectedResponse, cb).execute(true)) {
            mActivity.toast(mActivity.getString(R.string.unlock_failure));
            return false;
        } else {
            return true;
        }
    }
}
