package ecnu.cs14.btlock.view;

import android.app.Activity;
import android.widget.Toast;
import ecnu.cs14.btlock.R;

abstract class InteractiveActivity extends Activity{

    public void messageBox(String msg, String title) {
        // TODO
    }

    public boolean yesNoBox(String msg, String title) {
        // TODO
        return false;
    }

    public String inputBox(String msg, String title) {
        // TODO
        return "";
    }

    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    public void toastError() {
        toast(getString(R.string.error));
    }

    public void startWaiting() {

    }

    public void stopWaiting() {

    }
}
