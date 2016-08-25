package ecnu.cs14.btlock.presenter;

import android.util.Log;
import ecnu.cs14.btlock.R;
import ecnu.cs14.btlock.model.*;
import ecnu.cs14.btlock.view.AbstractMainActivity;

public class MainOperator {
    private static final String TAG = MainOperator.class.getSimpleName();

    private AbstractMainActivity mActivity;

    public MainOperator(AbstractMainActivity activity) {
        mActivity = activity;
        if (BTLock.hasLock()) {
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
    private Task.Handler mCallback;

    private volatile boolean mIsReady = false;
    private volatile boolean mIsWaiting = false;

    public synchronized void prepareForUnlock() {
        BTLock lock = BTLock.getCurrentLock();
        if (lock == null) {
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (MainOperator.this) {
                    mIsReady = false;
                }
                mActivity.disableUnlock();
                mActivity.toast(mActivity.getString(R.string.lock_disconnected));
                BTLockManager.deregisterDisconnectionCallback(this);
            }
        };
        BTLockManager.registerDisconnectionCallback(runnable);

        // find the best account
        final AccountStorage as = new AccountStorage(mActivity, lock.getAddress());
        final Account account = AccountOperator.findBestAccount(as);
        if (account == null) {
            BTLockManager.deregisterDisconnectionCallback(runnable);
            mActivity.startInitializingActivity();
            return;
        }
        final boolean isGuest = account instanceof Guest;

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

        // build the callback
        mCallback = new Task.Handler() {
            @Override
            public void onReceive(Data response) {
                if (isGuest) {
                    as.removeStoredAccount(account.getUid());
                }
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

        mIsReady = true;

        // now able to unlock, start to fill guests
        if (!isGuest) {
            new AccountOperator(as).fillGuests((User) account);
        }
    }

    public boolean unlock() {
        synchronized (this) {
            if (!mIsReady) {
                return false;
            }
            mIsWaiting = true;
        }
        mActivity.waitUnlocking();

        if (!new Task(mUnlockCommand, mExpectedResponse, mCallback).execute(true)) {
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
