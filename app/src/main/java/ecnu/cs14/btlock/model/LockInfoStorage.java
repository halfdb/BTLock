package ecnu.cs14.btlock.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        if (getAddress(nickname) != null) {
            int i = 2;
            while (getAddress(nickname + Integer.toString(i)) != null) {
                i++;
            }
            n = n + Integer.toString(i);
        }
        Set<String> addresses = getAllAddress();
        if (addresses == null) {
            addresses = new HashSet<>();
        }
        addresses.add(address);
        Set<String> nicknames = getAllNickname();
        if (nicknames == null) {
            nicknames = new HashSet<>();
        }
        nicknames.add(nickname);
        mSetting.edit()
                .putStringSet(ALL_ADDRESS_SETTING, addresses)
                .putStringSet(ALL_NICKNAME_SETTING, nicknames)
                .putString(a, n)
                .putString(n, a)
                .apply();
    }

    public void removeDeviceByAddress(String address) {
        SharedPreferences.Editor editor = mSetting.edit();

        String a = processedAddress(address);
        String n = mSetting.getString(a, null);
        if (n == null) {
            return;
        } else {
            editor.remove(a).remove(n);
        }

        HashSet<String> addresses = getAllAddress();
        if (addresses != null) {
            addresses.remove(address);
        } else {
            addresses = new HashSet<>();
        }
        HashSet<String> nicknames =  getAllNickname();
        if (nicknames == null) {
            nicknames = new HashSet<>();
        } else {
            nicknames.remove(getNickname(address));
        }
        editor.putStringSet(ALL_ADDRESS_SETTING, addresses).putStringSet(ALL_NICKNAME_SETTING, nicknames)
                .apply();
    }

    public void removeDeviceByNickname(String nickname){
        removeDeviceByAddress(getAddress(nickname));
    }

    private static final String ALL_ADDRESS_SETTING = "all_address";
    private static final String ALL_NICKNAME_SETTING = "all_nickname";

    @Nullable
    public HashSet<String> getAllAddress() {
        Set<String> s = mSetting.getStringSet(ALL_ADDRESS_SETTING, null);
        if (s == null) {
            return null;
        }
        return new HashSet<>(s);
    }

    @Nullable
    public HashSet<String> getAllNickname() {
        Set<String> s = mSetting.getStringSet(ALL_NICKNAME_SETTING, null);
        if (s == null) {
            return null;
        }
        return new HashSet<>(s);
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
