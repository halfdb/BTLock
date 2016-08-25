package ecnu.cs14.btlock.view;

import ecnu.cs14.btlock.presenter.InitializeOperator;

public abstract class AbstractInitActivity extends InteractiveActivity {
    public abstract void initializeFinished(InitializeOperator.InitResult result);
    public abstract String inquireNickname();
}
