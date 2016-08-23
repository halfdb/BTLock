package ecnu.cs14.btlock.presenter;

import android.content.Context;
import ecnu.cs14.btlock.model.LockInfoStorage;

public class LockInfoOperator {
    private LockInfoStorage mLIS;

    public LockInfoOperator(LockInfoStorage lis) {
        mLIS = lis;
    }

    public LockInfoOperator(Context context) {
        mLIS = LockInfoStorage.getInstance(context);
    }

    public void setNickname(String address, String nickname) {
        mLIS.addLockInfo(address, nickname);
    }

    public void setNickname(BTLock lock, String nickname) {
        setNickname(lock.getAddress(), nickname);
    }
}
