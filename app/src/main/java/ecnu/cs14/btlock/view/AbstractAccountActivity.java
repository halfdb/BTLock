package ecnu.cs14.btlock.view;

import android.widget.ListView;

public abstract class AbstractAccountActivity extends InteractiveActivity{
    public abstract ListView getOptionList();
    public abstract void chooseUserToDelete(boolean[] choicesAvailable);
}
