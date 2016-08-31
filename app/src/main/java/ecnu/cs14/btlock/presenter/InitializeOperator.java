package ecnu.cs14.btlock.presenter;

import android.util.Log;
import ecnu.cs14.btlock.model.*;
import ecnu.cs14.btlock.view.AbstractInitActivity;

public class InitializeOperator implements Task.Handler {
    private static final String TAG = InitializeOperator.class.getSimpleName();

    private AbstractInitActivity mActivity;
    private volatile BTLock mLock;
    private volatile AccountStorage mAS;

    private volatile Data mCommand;
    private volatile Data mMask;
    private volatile Data mExpectedRes;

    private volatile boolean mConnLost = false;


    public enum InitResult {
        COMPLETED,
        ALREADY_INITIALIZED,
        FAILURE,
        NO_VERIFICATION_PROVIDED,
        CONNECTION_LOST
    }

    public InitializeOperator(AbstractInitActivity activity){
        mActivity = activity;
        mLock = BTLock.getCurrentLock();
        mAS = new AccountStorage(activity, mLock.getAddress());

        mCommand = new Data();
        mCommand.set(0, CommandCode.account.CMD_ADD);
        mMask = new Data();
        mMask.set(0, CommandCode.PREFIX_MASK_5BIT);
        mExpectedRes = new Data();
        mExpectedRes.set(0, CommandCode.account.CMD_ADD_ACK);

        BTLockManager.registerDisconnectionCallback(new Runnable() {
            @Override
            public void run() {
                synchronized (InitializeOperator.this) {
                    mLock = null;
                    mAS = null;
                    mCommand = null;
                    mExpectedRes = null;
                    mMask = null;
                    mConnLost = true;
                }
                BTLockManager.deregisterDisconnectionCallback(this);
            }
        });
    }

    private boolean fillVerification(Data data){
        Data command = new Data();
        command.set(0, CommandCode.account.CMD_ASK_VERI);
        Data expected = new Data();
        expected.set(0, CommandCode.account.CMD_ASK_VERI_ACK);
        if(!new Task(command, expected, null).execute(false)) {
            return false;
        }
        mActivity.inquireVerification();
        String input = mActivity.getInputText();
        for (int i = 1; i < 7; i++) {
            data.set(i, (byte)input.charAt(i-1));
        }
        return true;
    }

    public InitResult initialize() {
        InitResult result;
        try {
            if (mAS.getStoredUids().size() != 0) {
                result = InitResult.ALREADY_INITIALIZED;
            } else if (!fillVerification(mCommand)) {
                result = InitResult.NO_VERIFICATION_PROVIDED;
            } else if(new Task(mCommand, mExpectedRes, mMask, this).execute(true)) {
                setNickname(mActivity.inquireNickname());
                result = InitResult.COMPLETED;
            } else {
                result = InitResult.FAILURE;
            }
        } catch (NullPointerException e) {
            synchronized (this) {
                if (mConnLost) {
                    Log.e(TAG, "initialize: Connection lost.");
                    result = InitResult.CONNECTION_LOST;
                } else {
                    throw e;
                }
            }
        }
        mActivity.initializeFinished(result);
        return result;
    }

    private User userFromResponse(Data response) {
        byte uidMask = (byte) ~mMask.get(0);
        byte uid = CommandCode.uid.getUidOf((byte) (response.get(0) & uidMask), 0);
        Password password = Password.extractFromData(response);
        return new User(uid, password);
    }

    @Override
    public void onReceive(Data response) {
        final User user = userFromResponse(response);
        mAS.addAccount(user);
        new Thread(new Runnable() {
            @Override
            public void run() {
                new AccountOperator(mAS).fillGuests(user);
                mActivity.finish();
            }
        }).start();
    }

    @Override
    public void onUnexpected(Data response) {
        if (response == null) {
            Log.e(TAG, "Failed to fetch the response after sending.");
        } else {
            Log.e(TAG, "Unexpected response: " + response.toString());
        }
    }

    public void setNickname(String nickname) {
        new LockInfoOperator(mActivity).setNickname(mLock, nickname);
    }
}
