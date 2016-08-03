package ecnu.cs14.btlock.model;

public class BTLockManager {
    private static BTLockManager instance = new BTLockManager();

    public static BTLockManager getInstance() {
        return instance;
    }

    private BTLockManager() { }

    private BTLock lock = null;

    public void connectLock(String address){
        if (lock != null) {
            disconnectLock();
        }
    }

    public void disconnectLock(){

        lock = null;
    }
}
