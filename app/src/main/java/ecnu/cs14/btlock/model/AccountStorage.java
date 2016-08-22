package ecnu.cs14.btlock.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

public class AccountStorage {
    private final static String NAME_PREFIX = "ecnu.cs14.btlock.as.";

    private SharedPreferences mSetting;

    public AccountStorage(Context context, String address) {
        String settingName = NAME_PREFIX + address;
        mSetting = context.getSharedPreferences(settingName, Context.MODE_APPEND);
    }

    private final static String KEY_UIDS = "uids";

    public HashSet<Byte> getStoredUids() {
        Set<String> uids = mSetting.getStringSet(KEY_UIDS, null);
        HashSet<Byte> ret = new HashSet<>();
        if (uids == null) {
            return ret;
        }
        for (String s: uids) {
            ret.add(Byte.valueOf(s));
        }
        return ret;
    }

    private final static String KEY_UID_PREFIX = "uid_";

    private static String keyForAccount(byte uid) {
        return KEY_UID_PREFIX + Byte.toString(uid);
    }

    @Nullable
    public Account getStoredAccount(byte uid) {
        String storedString = mSetting.getString(keyForAccount(uid), null);
        if (storedString == null) {
            return null;
        }
        Password password = new Password(storedString);
        return new Account(uid, password);
    }

    public void removeStoredAccount(byte uid) {
        Set<String> uids = mSetting.getStringSet(KEY_UIDS, null);
        if (uids == null) {
            return;
        }
        HashSet<String> set = new HashSet<>(uids);
        for (String s: set) {
            if (s.equals(Byte.toString(uid))) {
                set.remove(s);
                break;
            }
        }
        mSetting.edit().putStringSet(KEY_UIDS, set).apply();

        String key = keyForAccount(uid);
        if(mSetting.contains(key)){
            mSetting.edit().remove(key).apply();
        }
    }

    public void addAccount(Account account) {
        byte uid = account.getUid();
        String key = keyForAccount(uid);

        Set<String> oldUids = mSetting.getStringSet(KEY_UIDS, new HashSet<String>());
        HashSet<String> newUids = new HashSet<>(oldUids);
        newUids.add(Byte.toString(uid));

        mSetting.edit()
                .putStringSet(KEY_UIDS, newUids)
                .putString(key, account.getPassword().toString())
                .apply();
    }
}
