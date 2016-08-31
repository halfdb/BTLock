package ecnu.cs14.btlock.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import ecnu.cs14.btlock.R;
import ecnu.cs14.btlock.presenter.MainOperator;

public class MainActivity extends AbstractMainActivity {
    static final String TAG = MainActivity.class.getSimpleName();

    private MainOperator presenter;

    private Button unlockButton;
    private Button deviceListButton;
    private Button accountManagementButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        unlockButton = (Button) findViewById(R.id.unlockButton);
        deviceListButton = (Button) findViewById(R.id.deviceListButton);
        accountManagementButton = (Button) findViewById(R.id.accountButton);

        deviceListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startListActivity();
            }
        });
        deviceListButton.setVisibility(View.INVISIBLE);
        accountManagementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAccountActivity();
            }
        });
        accountManagementButton.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter = new MainOperator(this);
        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        presenter.unlock();
                    }
                }).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.checkClip();
        presenter.prepareForUnlock();
        presenter.updateUnlockState();
    }

    @Override
    public void disableUnlock() {
        unlockButton.setClickable(false);
        unlockButton.setEnabled(false);
    }

    @Override
    public void startListActivity() {
        Log.i(TAG, "startListActivity");
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
            case R.id.menu_main_item3:
                startWaiting();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        presenter.share();
                        stopWaiting();
                    }
                }).start();
                break;
            case R.id.menu_main_item4:
                presenter.forgetCurrentLock();
        }
        return true;
    }

    private int lockChoice = -1;
    public int getLockChoice() {
        return lockChoice;
    }
    @Override
    public int chooseShare(final String[] nicknames) {
        lockChoice = -1;
        final Object lock = new Object();
        synchronized (lock) {
            runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.choose_lock_to_share)
                        .setSingleChoiceItems(nicknames, -1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                lockChoice = which;
                            }
                        })
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                synchronized (lock) {
                                    lock.notify();
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                lockChoice = -1;
                                synchronized (lock) {
                                    lock.notify();
                                }
                            }
                        })
                        .setCancelable(true)
                        .show();
                }
            });
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return lockChoice;
    }
}
