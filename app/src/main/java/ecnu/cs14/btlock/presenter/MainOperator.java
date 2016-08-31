package ecnu.cs14.btlock.presenter;

import android.content.*;
import android.util.Log;
import ecnu.cs14.btlock.R;
import ecnu.cs14.btlock.model.*;
import ecnu.cs14.btlock.view.AbstractMainActivity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MainOperator {
    private static final String TAG = MainOperator.class.getSimpleName();

    private AbstractMainActivity mActivity;

    public MainOperator(AbstractMainActivity activity) {
        mActivity = activity;
        if (BTLock.hasLock()) {
            mActivity.enableUnlock();
        } else {
            mActivity.disableUnlock();
            mActivity.toast(mActivity.getString(R.string.no_lock_connected));
//            mActivity.startListActivity();
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
        mIsReady = false;
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
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.disableUnlock();
                        mActivity.toast(mActivity.getString(R.string.lock_disconnected));
                    }
                });
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
            String address = BTLock.getCurrentLock().getAddress();
            final AccountOperator ao = new AccountOperator(new AccountStorage(mActivity, address));
            final Account account = ao.findBestAccount();

            mIsWaiting = false;
            mActivity.enableUnlock();

            if (account == null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        forgetCurrentLock();
                    }
                });
            } else if (account instanceof User) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ao.fillGuests((User) account);
                    }
                }).start();
            }

            return true;
        }
    }

    public void disconnect() {
        mIsReady = false;
        BTLock.disconnect();
    }

    public void share() {
        LockInfoOperator lio = new LockInfoOperator(mActivity);
        Set<String> nicknameSet = lio.getAllNickname();
        Log.d(TAG, "share: all nicknames: " + nicknameSet.toString());
        if (nicknameSet.size() == 0) {
            mActivity.toast(mActivity.getString(R.string.no_lock_stored));
            return;
        }

        final String[] nicknames = new String[nicknameSet.size()];
        int i = 0;
        for (String s: nicknameSet) {
            nicknames[i++] = s;
        }

        int choice = mActivity.chooseShare(nicknames);

        if (choice < 0 || choice >= nicknames.length) {
            return;
        }

        String address = lio.getAddress(nicknames[choice]);
        AccountStorage as = new AccountStorage(mActivity, address);
        HashSet<Byte> uids = as.getStoredUids();
        byte uid = -1;
        for (Byte t: uids) {
            if (CommandCode.uid.getGuestNum(t) != 0) {
                uid = t;
                break;
            }
        }
        if (uid == -1) {
            mActivity.toastError();
            return;
        }
        Account account = as.getStoredAccount(uid);
        if (account == null) {
            mActivity.toastError();
            return;
        }
        as.removeStoredAccount(uid);
        String share = mActivity.getString(R.string.share_hint) + ShareCode.generate(address, account);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, share);
        mActivity.startActivity(intent);
    }

    public void forgetCurrentLock() {
        BTLock lock = BTLock.getCurrentLock();
        if (lock == null) {
            return;
        }
        AccountStorage as = new AccountStorage(mActivity, lock.getAddress());
        as.removeAllStoredAccount();

        LockInfoStorage lis = LockInfoStorage.getInstance(mActivity);
        lis.removeDeviceByAddress(lock.getAddress());

        mActivity.disableUnlock();
        disconnect();
        mActivity.toast(mActivity.getString(R.string.delete_remote_account_advice));
    }

    public void checkClip() {
        // get text from clipboard
        ClipboardManager clipboardManager = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (!clipboardManager.hasPrimaryClip()) {
            return;
        }
        ClipDescription description = clipboardManager.getPrimaryClipDescription();
        if (!"text/plain".equals(description.getMimeType(0))){
            return;
        }
        ClipData data = clipboardManager.getPrimaryClip();
        CharSequence text = data.getItemAt(0).coerceToText(mActivity);

        // try parse
        String address;
        Byte uid;
        Password pwd;
        try {
            HashMap<String, Object> map = ShareCode.parse(text.toString());
            address = (String) map.get(ShareCode.KEY_ADDRESS);
            uid = (Byte) map.get(ShareCode.KEY_UID);
            pwd = (Password) map.get(ShareCode.KEY_PASSWORD);
        } catch (Exception e) {
            Log.d(TAG, "checkClip: likely not a useful clip");
            return;
        }

        // add the key
        LockInfoStorage lis = LockInfoStorage.getInstance(mActivity);
        lis.addLockInfo(address, "BTLock");
        AccountStorage as = new AccountStorage(mActivity, address);
        as.addAccount(AccountFactory.newInstance(uid, pwd));

        mActivity.toast(mActivity.getString(R.string.temp_key_added));

        // clear the clipboard
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""));
    }
}
