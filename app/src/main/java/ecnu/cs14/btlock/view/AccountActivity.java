package ecnu.cs14.btlock.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ListView;
import ecnu.cs14.btlock.R;
import ecnu.cs14.btlock.presenter.AccountOperator;

import java.util.ArrayList;

public class AccountActivity extends AbstractAccountActivity {

    final ListView list = (ListView) findViewById(R.id.optionList);
    AccountOperator presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter = new AccountOperator(this);
    }

    int deleteChoice;
    @Override
    public void chooseUserToDelete(final boolean[] choicesAvailable) {
        ArrayList<CharSequence> strings = new ArrayList<>();
        final ArrayList<Integer> choices = new ArrayList<>();
        for (int i = 0; i < choicesAvailable.length; i++) {
            if (choicesAvailable[i]) {
                strings.add(Integer.toString(i+1));
                choices.add(i+1);
            }
        }

        deleteChoice = -1;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.choose_user_to_delete));
        builder.setSingleChoiceItems((CharSequence[]) strings.toArray(), -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteChoice = which;
            }
        });
        builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (deleteChoice != -1) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            startWaiting();
                            presenter.deleteUser(choices.get(deleteChoice));
                            stopWaiting();
                        }
                    }).start();
                }
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { }
        });
        builder.setCancelable(true);
        builder.show();
    }

    @Override
    public ListView getOptionList() {
        return list;
    }
}
