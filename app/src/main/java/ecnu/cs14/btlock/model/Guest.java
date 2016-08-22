package ecnu.cs14.btlock.model;

public class Guest extends Account{
    public Guest(byte uid, Password password) {
        super(uid, password);
    }

    public static Guest fromAccount(Account account) {
        return new Guest(account.getUid(), account.getPassword());
    }
}
