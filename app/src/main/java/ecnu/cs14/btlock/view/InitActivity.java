package ecnu.cs14.btlock.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import ecnu.cs14.btlock.R;
import ecnu.cs14.btlock.presenter.InitializeOperator;

public class InitActivity extends AbstractInitActivity {

    private static final String TAG = InitActivity.class.getSimpleName();

    @Override
    protected synchronized void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        startInitThread();
    }

    @Override
    protected void onStart() {
        super.onStart();
        failureCount = 0;
    }

    private int failureCount = 0;
    @Override
    public void initializeFinished(InitializeOperator.InitResult result) {
        switch (result) {
            case COMPLETED:
                break;
            case ALREADY_INITIALIZED:
                finish();
                break;
            case NO_VERIFICATION_PROVIDED:
            case FAILURE:
                if (failureCount++ < 5) {
                    startInitThread();
                } else {
                    stopWaiting();
                    toastError();
                    finish();
                }
                break;
            case CONNECTION_LOST:
                finish();
                break;
        }
    }

    @Override
    public void finish() {
        Log.d(TAG, "finish: start");
        super.finish();
        Log.d(TAG, "finish: finish");
    }

    private void startInitThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (InitActivity.this) {
                    new InitializeOperator(InitActivity.this).initialize();
                }
            }
        }).start();
    }

    @Override
    public String inquireVerification() {
        return inputBox("", getString(R.string.please_input_verification));
    }

    @Override
    public String inquireNickname() {
        return inputBox(getString(R.string.default_nickname), getString(R.string.make_a_nickname));
    }
}
