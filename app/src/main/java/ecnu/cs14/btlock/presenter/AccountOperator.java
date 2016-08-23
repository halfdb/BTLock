package ecnu.cs14.btlock.presenter;

import android.util.Log;
import ecnu.cs14.btlock.model.*;

import java.util.HashSet;

public class AccountOperator {
    private static final String TAG = AccountOperator.class.getSimpleName();

    private AccountStorage mAS;

    public AccountOperator(AccountStorage as) {
        mAS = as;
    }

    public boolean inquireNewPwd(byte uid) {
        byte userUid = (byte) (uid >> 2);
        userUid = (byte) (userUid << 2);
        Account user = mAS.getStoredAccount(userUid);
        if (user == null) {
            return false;
        }
        Password pwd = user.getPassword();

        Data command = Data.extendPassword(pwd);
        if (command == null) {
            return false;
        }
        byte commandHead = CommandCode.account.getCmdNewPwd(uid);
        command.set(0, commandHead);
        Data expectedRes = new Data();
        expectedRes.set(0, (byte) (CommandCode.account.CMD_NEW_PWD_ACK | uid));
        Data mask = new Data();
        mask.set(0, (byte)-1);

        return new Task(command, expectedRes, mask, new Task.Handler() {
            @Override
            public void onReceive(Data response) {
                Password password = Password.extractFromData(response);
                byte uid = (byte) (response.get(0) & ~CommandCode.PREFIX_MASK_3BIT);
                mAS.addAccount(AccountFactory.newInstance(uid, password));
            }

            @Override
            public void onUnexpected(Data response) {
                if (response == null) {
                    Log.e(TAG, "Failed to fetch the response after sending.");
                } else {
                    Log.e(TAG, "Unexpected response: " + response.toString());
                }
            }
        }).execute(true);
    }

    public boolean[] fillGuests(User user) {
        HashSet<Byte> uids = mAS.getStoredUids();
        byte uid0 = user.getUid();
        boolean[] ret = new boolean[]{false, false, false, false};
        if (!uids.contains(uid0)) {
            return ret;
        }
        ret[0] = true;
        for (int i = 1; i < 4; i++) {
            byte uid = (byte)(uid0 + i);
            if (uids.contains(uid)) {
                ret[i] = true;
            } else {
                Log.v(TAG, "Start to inquire new password for uid " + Byte.toString(uid));
                ret[i] = inquireNewPwd(uid);
            }
        }
        return ret;
    }

}
