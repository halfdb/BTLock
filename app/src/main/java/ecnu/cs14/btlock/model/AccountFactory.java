package ecnu.cs14.btlock.model;

public class AccountFactory {
    public static Account newInstance(byte uid, Password password) {
        switch (CommandCode.uid.getGuestNum(uid)){
            case 0:
                return new User(uid, password);
            default:
                return new Guest(uid, password);
        }
    }
}
