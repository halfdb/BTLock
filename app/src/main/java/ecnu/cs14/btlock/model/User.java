package ecnu.cs14.btlock.model;

import android.util.Log;

public class User extends Account {
    private static final String TAG = User.class.getSimpleName();

    public User(byte uid, Password password) {
        super(uid, password);
    }

    public static User fromAccount(Account account) {
        if (CommandCode.uid.getGuestNum(account.getUid())!=0) {
            return null;
        } else {
            return new User(account.getUid(), account.getPassword());
        }
    }

    private Guest[] mGuests = new Guest[3];

    public void addGuest(Guest guest) {
        byte uid = guest.getUid();
        int g_u = CommandCode.uid.getUserNum(uid);
        int g_g = CommandCode.uid.getGuestNum(uid);
        int u = CommandCode.uid.getUserNum(getUid());
        if (g_u != u || g_g == 0) {
            Log.e(TAG, "Not a proper guest.");
            return;
        }
        mGuests[g_g - 1] = guest;
    }

    public Guest[] getGuests() {
        return mGuests;
    }
}
