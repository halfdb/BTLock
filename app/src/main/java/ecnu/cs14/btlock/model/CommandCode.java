package ecnu.cs14.btlock.model;

public final class CommandCode {

    private static byte rawBinary(int i) {
        if (i <= 0x7f) {
            return (byte) i;
        } else if (i <= 0xFF) {
            return (byte) (i - 0x100);
        } else {
            return 0;
        }
    }

    public static final byte PREFIX_MASK_5BIT = rawBinary(0xF8);
    public static final byte PREFIX_MASK_3BIT = rawBinary(0xE0);

    public static final class uid {
        public static final byte UID_10 = getUidOf(1, 0);
        public static final byte UID_11 = getUidOf(1, 1);
        public static final byte UID_12 = getUidOf(1, 2);
        public static final byte UID_13 = getUidOf(1, 3);
        public static final byte UID_20 = getUidOf(2, 0);
        public static final byte UID_21 = getUidOf(2, 1);
        public static final byte UID_22 = getUidOf(2, 2);
        public static final byte UID_23 = getUidOf(2, 3);
        public static final byte UID_30 = getUidOf(3, 0);
        public static final byte UID_31 = getUidOf(3, 1);
        public static final byte UID_32 = getUidOf(3, 2);
        public static final byte UID_33 = getUidOf(3, 3);
        public static final byte UID_40 = getUidOf(4, 0);
        public static final byte UID_41 = getUidOf(4, 1);
        public static final byte UID_42 = getUidOf(4, 2);
        public static final byte UID_43 = getUidOf(4, 3);
        public static final byte UID_50 = getUidOf(5, 0);
        public static final byte UID_51 = getUidOf(5, 1);
        public static final byte UID_52 = getUidOf(5, 2);
        public static final byte UID_53 = getUidOf(5, 3);

        public static byte getUidOf(int u, int g) {
            return (byte)((u << 2) | g);
        }

        public static int getUserNum(byte uid) {
            return ((uid >> 2) & 0x7);
        }

        public static int getGuestNum(byte uid) {
            return (uid & 0x3);
        }

        private uid() { }
    }

    public static final class unlock {
        public static final byte CMD_PREFIX = 0x20;

        public static byte getCmdUnlock(int u, int g) {
            return (byte) (uid.getUidOf(u, g) | CMD_PREFIX);
        }

        public static byte getCmdUnlock(int uid) {
            return (byte) (uid | CMD_PREFIX);
        }

        public static final byte CMD_ACK = rawBinary(0xA0);

        private unlock() { }
    }

    public static final class account {
        public static final byte CMD_ADD_PREFIX = 0x08;
        public static byte getCmdAdd(int u) {
            return (byte) (CMD_ADD_PREFIX | u);
        }
        public static final byte CMD_ADD_ACK = rawBinary(0x88);
        
        public static final byte CMD_DEL_PREFIX = 0x18;
        public static byte getCmdDel(int u) {
            return (byte) (CMD_DEL_PREFIX | u);
        }
        public static final byte CMD_DEL_ACK = rawBinary(0x98);

        public static final byte CMD_NEW_PWD_PREFIX = 0x40;
        public static byte getCmdNewPwd(int u, int g) {
            return (byte) (uid.getUidOf(u, g) | CMD_NEW_PWD_PREFIX);
        }
        public static byte getCmdNewPwd(int uid) {
            return (byte) (uid | CMD_NEW_PWD_PREFIX);
        }
        public static final byte CMD_NEW_PWD_ACK = rawBinary(0xC0);

        private account() { }
    }
}
