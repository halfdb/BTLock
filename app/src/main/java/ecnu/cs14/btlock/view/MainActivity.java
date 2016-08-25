package ecnu.cs14.btlock.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import ecnu.cs14.btlock.R;
import ecnu.cs14.btlock.presenter.MainOperator;

public class MainActivity extends AbstractMainActivity {

    MainOperator presenter;

    Button unlockButton;
    Button deviceListButton;
    Button accountManagementButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        unlockButton = (Button) findViewById(R.id.unlockButton);
        deviceListButton = (Button) findViewById(R.id.deviceListButton);
        accountManagementButton = (Button) findViewById(R.id.accountButton);

        setContentView(R.layout.activity_main);
        deviceListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startListActivity();
            }
        });
        accountManagementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAccountActivity();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter = new MainOperator(this);
        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.unlock();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.prepareForUnlock();
    }

    @Override
    public void disableUnlock() {
        unlockButton.setClickable(false);
        unlockButton.setEnabled(false);
    }

    @Override
    public void startListActivity() {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    @Override
    public void startInitializingActivity() {
        Intent intent = new Intent(this, InitActivity.class);
        startActivity(intent);
    }

    public void startAccountActivity() {
        Intent intent = new Intent(this, AccountActivity.class);
        startActivity(intent);
    }

    @Override
    public void enableUnlock() {
        unlockButton.setClickable(true);
        unlockButton.setEnabled(true);
    }

    @Override
    public void waitUnlocking() {
        unlockButton.setClickable(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_main_item1:
                startListActivity();
                break;
            case R.id.menu_main_item2:
                startAccountActivity();
                break;
        }
        return true;
    }
}
