package ecnu.cs14.btlock.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ProgressBar;
import ecnu.cs14.btlock.R;
import ecnu.cs14.btlock.presenter.InitializeOperator;

public class InitActivity extends AbstractInitActivity {

    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        startInitThread();
    }

    @Override
    public void initializeFinished(InitializeOperator.InitResult result) {
        switch (result) {
            case COMPLETED:
            case ALREADY_INITIALIZED:
                finish();
                break;
            case NO_VERIFICATION_PROVIDED:
            case FAILURE:
                startInitThread();
                break;
            case CONNECTION_LOST:
                finish();
                break;
        }
    }

    private void startInitThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                new InitializeOperator(InitActivity.this).initialize();
            }
        }).start();
    }

    String nickname;
    @Override
    public String inquireNickname() {
        final Object lock = new Object();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.make_a_nickname));
        final EditText editText = new EditText(this);
        editText.setText(R.string.default_nickname);
        editText.selectAll();
        builder.setView(editText);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                synchronized (lock) {
                    nickname = editText.getText().toString();
                    lock.notify();
                }
            }
        });
        builder.setCancelable(false);

        synchronized (lock) {
            builder.show();
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return nickname;
    }
}
