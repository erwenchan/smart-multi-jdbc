package cn.smartdec.multienv.support.jdbc;

import java.util.concurrent.Callable;

public class MultiJdbcCallable<V> implements Callable<V> {

    private final String envKey;

    private Callable<V> callable;

    public MultiJdbcCallable(Callable<V> callable) {
        this.envKey = MultiJdbcContext.getEnvKey();
        this.callable = callable;
    }

    @Override
    public V call() throws Exception {
        final String envKeyOld = MultiJdbcContext.getEnvKey();
        MultiJdbcContext.setEnvKey(this.envKey);
        try {
            return callable.call();
        } finally {
            MultiJdbcContext.clear();
            if (envKeyOld != null) {
                MultiJdbcContext.setEnvKey(envKeyOld);
            }
        }
    }
}
