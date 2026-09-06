package com.company.automation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 8: LogUtils — why a wrapper instead of calling
 * LogManager.getLogger() directly everywhere?
 * ============================================================================
 * We COULD have every class call `LogManager.getLogger(ThisClass.class)`
 * directly — it would work fine. This thin wrapper exists purely for
 * CONSISTENCY and a single point of change: if the org ever migrates
 * logging frameworks (Log4j2 -> something else), only THIS file changes,
 * not every class that logs something.
 *
 * WHY PASS THE CALLING CLASS IN (getLogger(SomeClass.class)) INSTEAD OF A
 * GLOBAL STATIC LOGGER?
 *   Log4j2 uses the class name as the LOGGER NAME in output (see
 *   %logger{36} in log4j2.xml's pattern) — this is how you can tell, just
 *   from reading a log line, WHICH class produced it (e.g. seeing
 *   "DriverFactory" vs "LoginSteps" in the log immediately tells you
 *   where in the framework something happened). A single global logger
 *   would lose this context entirely.
 * ============================================================================
 */
public class LogUtils {

    private LogUtils() {
    }

    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }
}
