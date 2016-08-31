package ecnu.cs14.btlock.view;

public abstract class AbstractMainActivity extends InteractiveActivity{
    public abstract void startListActivity();

    public abstract void startInitializingActivity();

    public abstract void enableUnlock();
    public abstract void disableUnlock();
    public abstract void waitUnlocking();
    public abstract int getLockChoice();
    public abstract int chooseShare(String[] nicknames);
}
