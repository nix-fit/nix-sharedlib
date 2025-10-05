package org.nix.sharedlib.logging

/**
 * Logging utils
 */
class LogUtils {

    public final static int LEVEL_ERROR = 5
    public final static int LEVEL_WARN = 4
    public final static int LEVEL_INFO = 3
    public final static int LEVEL_DEBUG = 2
    public final static int LEVEL_TRACE = 1

    protected Script script
    protected int logLevel = LEVEL_INFO

    LogUtils(Script script, int logLevel) {
        this.script = script
        this.logLevel = logLevel
    }

    void error(String message) {
        if (logLevel <= LEVEL_ERROR) {
            script.echo "[ERROR] ${message}"
        }
    }

    void warn(String message) {
        if (logLevel <= LEVEL_WARN) {
            script.echo "[WARN] ${message}"
        }
    }

    void info(String message) {
        if (logLevel <= LEVEL_INFO) {
            script.echo "[INFO] ${message}"
        }
    }

    void trace(String message) {
        if (logLevel <= LEVEL_TRACE) {
            script.echo "[TRACE] ${message}"
        }
    }

}
