package ecnu.cs14.btlock.view;

import android.widget.ListView;

public abstract class AbstractSettingActivity extends InteractiveActivity{
    public abstract ListView getSettingList();
    public abstract int chooseUserToDelete(boolean[] choicesAvailable);
}
