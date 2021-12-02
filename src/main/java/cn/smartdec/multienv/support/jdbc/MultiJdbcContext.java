package cn.smartdec.multienv.support.jdbc;

import java.util.function.Supplier;

public final class MultiJdbcContext {

    public static final String ENV_KEY = "envKey";

    public static final String ENV_KEY_ADMIN = "ENV_ADMIN";

    private static final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    public static void setEnvKey(String envKey) {
        threadLocal.set(envKey);
    }

    public static String getEnvKey() {
        return threadLocal.get();
    }

    public static void clear() {
        threadLocal.remove();
    }

    public static <T> T wrapEnvKey(String envKey, Supplier<T> fun) {
        final String envKeyOld = getEnvKey();
        setEnvKey(envKey); // 切换envKey
        try {
            return fun.get();
        } finally {
            clear();
            if (null != envKeyOld) {
                setEnvKey(envKeyOld); // 恢复原来的envKey
            }
        }
    }

    public static <T> T wrapAdmin(Supplier<T> fun) {
        return wrapEnvKey(ENV_KEY_ADMIN, fun);
    }

    private MultiJdbcContext() {
    }

}
