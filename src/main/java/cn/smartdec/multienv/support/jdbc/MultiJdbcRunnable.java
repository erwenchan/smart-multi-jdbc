package cn.smartdec.multienv.support.jdbc;

public class MultiJdbcRunnable implements Runnable {

    private final String envKey;

    private Runnable runnable;

    public MultiJdbcRunnable(Runnable runnable) {
        this.envKey = MultiJdbcContext.getEnvKey();
        this.runnable = runnable;
    }

    @Override
    public void run() {
        final String envKeyOld = MultiJdbcContext.getEnvKey();
        MultiJdbcContext.setEnvKey(this.envKey);
        try {
            runnable.run();
        } finally {
            MultiJdbcContext.clear();
            if (envKeyOld != null) {
                MultiJdbcContext.setEnvKey(envKeyOld);
            }
        }
    }

}
