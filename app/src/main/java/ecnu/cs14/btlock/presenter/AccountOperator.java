package ecnu.cs14.btlock.presenter;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import ecnu.cs14.btlock.R;
import ecnu.cs14.btlock.model.*;
import ecnu.cs14.btlock.view.AbstractSettingActivity;

import java.util.HashSet;

public class AccountOperator implements AdapterView.OnItemClickListener {
    private static final String TAG = AccountOperator.class.getSimpleName();

    private AccountStorage mAS;
    private AbstractSettingActivity mActivity;

    public AccountOperator(AccountStorage as) {
        mAS = as;
    }

    public AccountOperator(AbstractSettingActivity activity) {
        BTLock lock = BTLock.getCurrentLock();
        if (lock == null) {
            mActivity.toastError();
            return;
        }
        mActivity = activity;
        mAS = new AccountStorage(mActivity, lock.getAddress());

         String[] options = new String[] {
                activity.getString(R.string.account_setting_new_pwd),
                activity.getString(R.string.account_setting_delete_user)
        };

        ListView view = activity.getSettingList();
        view.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, options));
        view.setOnItemClickListener(this);
        view.setEnabled(hasPermission());
    }


    public static Account findBestAccount(AccountStorage as) {
        // find stored uids
        HashSet<Byte> uids = as.getStoredUids();
        if (uids.size() == 0) {
            return null;
        }

        // find the best account
        byte uidCandidate = -128;
        for (byte uid: uids) {
            uidCandidate = uid;
            if (CommandCode.uid.getGuestNum(uid) == 0) {
                break;
            }
        }
        return as.getStoredAccount(uidCandidate);
    }

    public Account findBestAccount() {
        return findBestAccount(mAS);
    }

    public boolean hasPermission() {
        return findBestAccount() instanceof User;
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
                Account account = findBestAccount();
                if (account == null) {
                    Log.e(TAG, "onItemClick: Could not find a user account");
                    return;
                }
                inquireNewPwd(account.getUid());
                break;
            case 1:
                decideAndDelete();
                break;
        }
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

    public boolean deleteUser(final int uToDelete) {
        Account operatorA = findBestAccount();
        User operator;
        if (operatorA instanceof User) {
            operator = (User) operatorA;
        } else {
            return false;
        }

        int u = CommandCode.uid.getUserNum(operator.getUid());
        byte commandHead = CommandCode.account.getCmdDel(u, uToDelete);
        Data command = Data.extendPassword(operator.getPassword());
        if (command == null) {
            return false;
        }
        command.set(0, commandHead);
        Data expectedRes = new Data();
        expectedRes.set(0, CommandCode.account.CMD_DEL_ACK);

        return new Task(command, expectedRes, new Task.Handler() {
            @Override
            public void onReceive(Data response) {
                for (byte uid :
                        CommandCode.uid.getAllAccounts(uToDelete)) {
                    mAS.removeStoredAccount(uid);
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
        }).execute(true);
    }

    public void decideAndDelete() {
        Account account = findBestAccount();
        User user;
        if (account instanceof User) {
            user = (User) account;
        } else {
            Log.w(TAG, "decideAndDelete: Not a user");
            return;
        }

        byte commandHead = CommandCode.account.getCmdInquiry(CommandCode.uid.getUserNum(user.getUid()));
        Data command = Data.extendPassword(user.getPassword());
        if (command == null) {
            return;
        }
        command.set(0, commandHead);
        Data expectedRes = new Data();
        expectedRes.set(0, CommandCode.account.CMD_INQUIRY_ACK);
        Data mask = new Data();
        mask.set(0, (byte)-1);
        for (int i = 2; i < Data.SIZE; i++) {
            mask.set(i, (byte)-1);
        }

        new Task(command, expectedRes, mask, new Task.Handler() {
            @Override
            public void onReceive(Data response) {
                byte b = response.get(1);
                boolean[] status = new boolean[5];
                for (int i = 1; i < 6; i++) {
                    status[i-1] = ((b & (1<<i)) != 0);
                }
                deleteUser(mActivity.chooseUserToDelete(status));
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
}
