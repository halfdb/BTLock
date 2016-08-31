package ecnu.cs14.btlock.presenter;

import ecnu.cs14.btlock.model.Account;
import ecnu.cs14.btlock.model.Password;

import java.util.HashMap;

class ShareCode {
    private static final String ANCHOR = "btl:";
    private static final String DIVIDER = "|";

    public static final String KEY_ADDRESS = "ecnu.cs14.btlock.share.address";
    public static final String KEY_UID = "ecnu.cs14.btlock.share.uid";
    public static final String KEY_PASSWORD = "ecnu.cs14.btlock.share.password";

    static String generate(String address, Account account) {
        return ANCHOR + address + DIVIDER + Byte.toString(account.getUid()) + DIVIDER + account.getPassword().toString();
    }

    static HashMap<String, Object> parse(String share) {
        String entity = share.substring(share.indexOf(ANCHOR) + ANCHOR.length());
        String[] entities = entity.split("\\|");
        String address = entities[0];
        Byte uid = Byte.valueOf(entities[1]);
        Password pwd = new Password(entities[2]);

        HashMap<String, Object> map = new HashMap<>(3);
        map.put(KEY_ADDRESS, address);
        map.put(KEY_UID, uid);
        map.put(KEY_PASSWORD, pwd);

        return map;
    }
}
