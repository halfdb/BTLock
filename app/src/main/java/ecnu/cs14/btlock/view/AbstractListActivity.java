package ecnu.cs14.btlock.view;

import android.widget.ListView;

public abstract class AbstractListActivity extends InteractiveActivity{
    public abstract ListView getListView();

    public abstract void onSelected();
}
