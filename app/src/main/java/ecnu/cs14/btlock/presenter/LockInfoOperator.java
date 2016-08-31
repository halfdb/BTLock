package ecnu.cs14.btlock.presenter;

import android.content.Context;
import ecnu.cs14.btlock.model.LockInfoStorage;

import java.util.HashSet;

public class LockInfoOperator {
    private LockInfoStorage mLIS;

    public LockInfoOperator(LockInfoStorage lis) {
        mLIS = lis;
    }

    public LockInfoOperator(Context context) {
        mLIS = LockInfoStorage.getInstance(context);
    }

    public boolean isLockLogged(BTLock lock) {
        return isLockLogged(lock.getAddress());
    }

    public boolean isLockLogged(String address) {
        return mLIS.getNickname(address) != null;
    }

    public String getAddress(String nickname) {
        return mLIS.getAddress(nickname);
    }

    public String getNickname(String address) {
        return mLIS.getNickname(address);
    }

    public String getNickname(BTLock lock) {
        return getNickname(lock.getAddress());
    }

    public void setNickname(String address, String nickname) {
        mLIS.addLockInfo(address, nickname);
    }

    public void setNickname(BTLock lock, String nickname) {
        setNickname(lock.getAddress(), nickname);
    }

    public HashSet<String> getAllNickname() {
        HashSet<String> ret = mLIS.getAllNickname();
        if (ret == null) {
            ret = new HashSet<>();
        }
        return ret;
    }

    public HashSet<String> getAllAddress() {
        HashSet<String> ret = mLIS.getAllAddress();
        if (ret == null) {
            ret = new HashSet<>();
        }
        return ret;
    }
}
