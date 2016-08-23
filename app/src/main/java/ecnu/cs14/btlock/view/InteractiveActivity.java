package ecnu.cs14.btlock.view;

import android.app.Activity;
import android.widget.Toast;

abstract class InteractiveActivity extends Activity{

    public final void messageBox(String msg, String title) {
        // TODO
    }

    public final boolean yesNoBox(String msg, String title) {
        // TODO
        return false;
    }

    public final String inputBox(String msg, String title) {
        // TODO
        return "";
    }

    public final void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
