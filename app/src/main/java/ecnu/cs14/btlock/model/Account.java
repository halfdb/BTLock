package ecnu.cs14.btlock.model;

public class Account {
    private byte mUid;
    private Password mPassword;

    public Account(byte uid, Password password) {
        mUid = uid;
        mPassword = (Password) password.clone();
    }

    public byte getUid() {
        return mUid;
    }

    public Password getPassword() {
        return mPassword;
    }

    public void setPassword(Password password) {
        this.mPassword = (Password) password.clone();
    }
}
