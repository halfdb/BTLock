package ecnu.cs14.btlock.presenter;

import android.util.Log;
import ecnu.cs14.btlock.R;
import ecnu.cs14.btlock.model.*;
import ecnu.cs14.btlock.view.AbstractMainActivity;

import java.util.HashSet;

public class MainOperator {
    private static final String TAG = MainOperator.class.getSimpleName();

    private AbstractMainActivity mActivity;

    public MainOperator(AbstractMainActivity activity) {
        mActivity = activity;
        if (BTLock.hasLock()) {
            BTLockManager.registerDisconnectionCallback(new Runnable() {
                @Override
                public void run() {
                    synchronized (MainOperator.this) {
                        mIsReady = false;
                    }
                    mActivity.disableUnlock();
                    mActivity.toast(mActivity.getString(R.string.lock_disconnected));
                    BTLockManager.deregisterDisconnectionCallback(this);
                }
            });
            new Thread(new Runnable() {
                @Override
                public void run() {
                    prepareForUnlock();
                }
            }).start();
            mActivity.enableUnlock();
        } else {
            mActivity.disableUnlock();
            mActivity.toast(mActivity.getString(R.string.no_lock_available));
            mActivity.startListActivity();
        }
    }

    public synchronized void updateUnlockState() {
        if (mIsReady) {
            if(!mIsWaiting) {
                mActivity.enableUnlock();
            } else {
                mActivity.waitUnlocking();
            }
        } else {
            mActivity.disableUnlock();
        }
    }

    private Data mUnlockCommand;
    private Data mExpectedResponse;

    private volatile boolean mIsReady = false;
    private volatile boolean mIsWaiting = false;

    public synchronized void prepareForUnlock() {
        BTLock lock = BTLock.getCurrentLock();
        if (lock == null) {
            return;
        }

        // find stored uids
        AccountStorage as = new AccountStorage(mActivity, lock.getAddress());
        HashSet<Byte> uids = as.getStoredUids();
        if (uids.size() == 0) {
            mActivity.startInitializingActivity();
            return;
        }

        // find the best account
        byte uidCandidate = -128;
        for (byte uid: uids) {
            uidCandidate = uid;
            if (CommandCode.uid.getGuestNum(uid) == 0) {
                break;
            }
        }
        Account account = as.getStoredAccount(uidCandidate);
        if (account == null) {
            return;
        }

        // build the command to unlock the BTLock
        byte commandHead = CommandCode.unlock.getCmdUnlock(account.getUid());
        mUnlockCommand = Data.extendPassword(account.getPassword());
        if (mUnlockCommand == null) {
            Log.e(TAG, "prepareForUnlock: Failed to prepare unlock command.");
            return;
        }
        mUnlockCommand.set(0, commandHead);
        mExpectedResponse = new Data();
        mExpectedResponse.set(0, (byte)(CommandCode.unlock.CMD_ACK | account.getUid()));
        for (int i = 1; i < Data.SIZE; i++) {
            mExpectedResponse.set(i, (byte) 0);
        }

        mIsReady = true;
    }

    public boolean unlock() {
        synchronized (this) {
            if (!mIsReady) {
                return false;
            }
            mIsWaiting = true;
        }
        mActivity.waitUnlocking();

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

        if (!new Task(mUnlockCommand, mExpectedResponse, cb).execute(true)) {
            mIsWaiting = false;
            mActivity.enableUnlock();
            mActivity.toast(mActivity.getString(R.string.unlock_failure));
            return false;
        } else {
            mIsWaiting = false;
            mActivity.enableUnlock();
            return true;
        }
    }
}
