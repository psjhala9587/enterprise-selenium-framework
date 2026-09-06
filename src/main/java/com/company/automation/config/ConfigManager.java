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

    /*
     * ========================================================================
     * MODULE 10 BUG FIX: explicitly seed Owner's variable-expansion engine
     * ========================================================================
     * ROOT CAUSE OF THE REAL BUG THIS FIXES: Owner's ${env} placeholder
     * inside EnvironmentConfig's @Config.Sources is supposed to resolve
     * from the JVM system property named "env" (set via -Denv=qa). In
     * practice, propagating a -D flag from the `mvn` command line, through
     * Maven's property resolution, through a FORKED Surefire test JVM, and
     * finally into Owner's internal variable-expansion engine is a long
     * chain — and on this Windows/Jenkins setup, that chain broke somewhere
     * (confirmed via Module 10's diagnostic logging: environment()
     * returned "qa" only because it happens to match our @DefaultValue,
     * but base.url() — which has NO default — came back null, proving
     * config-qa.properties was never actually located on the classpath).
     *
     * THE FIX: ConfigFactory.setProperty(key, value) is Owner's OWN
     * documented API for explicitly registering a value into its variable-
     * expansion engine — bypassing the fragile system-property-propagation
     * chain entirely. We read the "env" system property ourselves (with a
     * safe fallback to "qa"), then hand it to Owner directly, in the SAME
     * JVM, immediately before creating the config proxy. This removes an
     * entire layer of "did the -D flag actually survive the fork" risk.
     *
     * We still read System.getProperty("env", "qa") first — so `-Denv=...`
     * on the command line (Module 3/7) still works exactly as documented
     * everywhere else in this framework; we've only made Owner's OWN
     * internal resolution of that value more direct and reliable.
     * ========================================================================
     */
    static {
        String env = System.getProperty("env", "qa");
        ConfigFactory.setProperty("env", env);
    }

    // Eagerly initialized ONCE when this class is first loaded by the JVM.
    // Thread-safe by default because static initializers are guaranteed
    // by the JVM to run exactly once. IMPORTANT: this line must come AFTER
    // the static block above — static initializers run top-to-bottom in
    // declaration order, so ConfigFactory.setProperty("env", ...) is
    // guaranteed to have already run by the time ConfigFactory.create()
    // executes and needs to resolve the ${env} placeholder.
    private static final EnvironmentConfig config = ConfigFactory.create(EnvironmentConfig.class);

    // Private constructor prevents instantiation — this class only exposes
    // static access, we never need an "instance" of ConfigManager itself.
    private ConfigManager() {
    }

    public static EnvironmentConfig getConfig() {
        return config;
    }
}