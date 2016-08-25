package ecnu.cs14.btlock.view;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import ecnu.cs14.btlock.R;

abstract class InteractiveActivity extends AppCompatActivity{

    public void messageBox(String msg, String title) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true)
                .show();
    }

    boolean yesOrNo;
    public boolean yesNoBox(String msg, String title) {
        final Object lock = new Object();
        synchronized (lock) {
            new AlertDialog.Builder(this)
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
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return yesOrNo;
    }

    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    public void toastError() {
        toast(getString(R.string.error));
    }

    ProgressDialog waitingDialog;
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
