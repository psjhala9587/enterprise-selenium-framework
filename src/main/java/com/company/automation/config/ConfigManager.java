package com.company.automation.config;

import org.aeonbits.owner.ConfigFactory;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 3: Why a ConfigManager wrapper class?
 * ============================================================================
 * ConfigFactory.create(EnvironmentConfig.class) is what actually builds the
 * runtime proxy implementing our interface. We do NOT want every class in
 * the framework calling this line directly — that would mean:
 *   1. Repeating boilerplate everywhere.
 *   2. Creating the config MULTIPLE times unnecessarily (wasteful — the
 *      config never changes mid-test-run, so one instance should serve
 *      the entire suite).
 *
 * This is a PREVIEW of the Singleton pattern (full deep-dive with
 * ThreadLocal in Module 5 for DriverManager). Here it's simpler because
 * config is read-only and thread-safe by nature — no ThreadLocal needed,
 * a plain "eagerly initialized static final field" is enough.
 *
 * USAGE ANYWHERE IN THE FRAMEWORK:
 *   String url = ConfigManager.getConfig().baseUrl();
 * ============================================================================
 */
public class ConfigManager {

    // Eagerly initialized ONCE when this class is first loaded by the JVM.
    // Thread-safe by default because static initializers are guaranteed
    // by the JVM to run exactly once.
    private static final EnvironmentConfig config = ConfigFactory.create(EnvironmentConfig.class);

    // Private constructor prevents instantiation — this class only exposes
    // static access, we never need an "instance" of ConfigManager itself.
    private ConfigManager() {
    }

    public static EnvironmentConfig getConfig() {
        return config;
    }
}
