package ecnu.cs14.btlock.view;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import ecnu.cs14.btlock.R;

abstract class InteractiveActivity extends AppCompatActivity{

    private static final String TAG = InteractiveActivity.class.getSimpleName();

    public void messageBox(final String msg, final String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(InteractiveActivity.this)
                        .setTitle(title)
                        .setMessage(msg)
                        .setPositiveButton(android.R.string.ok, null)
                        .setCancelable(true)
                        .show();
            }
        });

    }

    private String inputText;
    public synchronized String getInputText() {
        return inputText;
    }
    public synchronized String inputBox(String defaultText, String title) {
        inputText = null;
        final Object lock = new Object();
        final EditText editText = new EditText(this);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setView(editText);
        editText.setText(defaultText);
        editText.selectAll();
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                synchronized (lock) {
                    inputText = editText.getText().toString();
                    lock.notify();
                }
            }
        });
        builder.setCancelable(false);

        synchronized (lock) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.show();
                }
            });
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return inputText;
    }

    private boolean yesOrNo;
    public boolean getYesOrNo() {
        return yesOrNo;
    }
    public synchronized boolean yesNoBox(final String msg, final String title) {
        final Object lock = new Object();
        synchronized (lock) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: started");
                    new AlertDialog.Builder(InteractiveActivity.this)
                            .setMessage(msg)
                            .setTitle(title)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    yesOrNo = true;
                                    synchronized (lock) {
                                        lock.notify();
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    yesOrNo = false;
                                    synchronized (lock) {
                                        lock.notify();
                                    }
                                }
                            })
                            .setCancelable(false)
                            .show();
                }
            });
            try {
                Log.d(TAG, "yesNoBox: waiting");
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return yesOrNo;
    }

    public void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(InteractiveActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void toastError() {
        toast(getString(R.string.error));
    }

    private ProgressDialog waitingDialog;
    public void startWaiting() {
        waitingDialog = new ProgressDialog(this);
        waitingDialog.setTitle(R.string.executing);
        waitingDialog.setMessage(getString(R.string.please_wait));
        waitingDialog.setCancelable(false);
        waitingDialog.setIndeterminate(true);
        waitingDialog.show();
    }

    public void stopWaiting() {
        waitingDialog.dismiss();
    }
}
