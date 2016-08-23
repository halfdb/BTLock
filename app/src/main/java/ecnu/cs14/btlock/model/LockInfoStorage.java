package ecnu.cs14.btlock.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

public class LockInfoStorage {
    private static LockInfoStorage sInstance;

    public static LockInfoStorage getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LockInfoStorage(context);
        }
        return sInstance;
    }

    private final static String NAME = "ecnu.cs14.btlock.lis";

    private SharedPreferences mSetting;

    private LockInfoStorage(Context context) {
        mSetting = context.getSharedPreferences(NAME, Context.MODE_APPEND);
    }

    private final static String ADDRESS_PREFIX = "address_";
    private final static String NICKNAME_PREFIX = "nickname_";

    private static String processedAddress(String address) {
        return ADDRESS_PREFIX + address;
    }
    private static String processedNickname(String nickname) {
        return NICKNAME_PREFIX + nickname;
    }

    public void addLockInfo(String address, String nickname) {
        String a =  processedAddress(address);
        String n =  processedNickname(nickname);
        mSetting.edit()
                .putString(a, n)
                .putString(n, a)
                .apply();
    }

    @Nullable
    public String getNickname(String address) {
        String ret = mSetting.getString(processedAddress(address), null);
        if (ret == null) {
            return null;
        }
        return ret.replaceFirst(NICKNAME_PREFIX, "");
    }
    @Nullable
    public String getAddress(String nickname) {
        String ret = mSetting.getString(processedNickname(nickname), null);
        if (ret == null) {
            return null;
        }
        return ret.replaceFirst(ADDRESS_PREFIX, "");
    }
}
