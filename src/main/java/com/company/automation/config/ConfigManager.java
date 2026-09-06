package com.company.automation.config;

import com.company.automation.exceptions.FrameworkException;
import org.aeonbits.owner.ConfigFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
     * MODULE 10 BUG FIX (v2): bypass Owner's @Sources classpath resolution
     * entirely — manually load the properties file with plain Java APIs
     * ========================================================================
     * BACKGROUND: EnvironmentConfig's @Config.Sources originally used
     * Owner's own "classpath:config/config-${env}.properties" placeholder
     * mechanism. On this training's actual Windows/Jenkins environment,
     * that mechanism reliably failed to locate the file — CONFIRMED across
     * two separate attempted fixes (first assuming a system-property
     * propagation issue, then explicitly forcing the value via
     * ConfigFactory.setProperty("env", ...) — base.url STILL came back
     * null both times). That ruled out variable-SUBSTITUTION as the cause;
     * something deeper in Owner's classpath-loading itself wasn't working
     * as documented in this specific setup.
     *
     * RATHER THAN CONTINUE DEBUGGING A THIRD-PARTY LIBRARY'S INTERNALS
     * BLIND, we fall back to something we can fully control and reason
     * about: plain ClassLoader.getResourceAsStream() (the same mechanism
     * literally every Java resource-loading tutorial uses, extremely
     * well-understood, effectively impossible to get subtly wrong) to read
     * the correct config-<env>.properties file ourselves, then feed every
     * key/value pair into Owner via ConfigFactory.setProperty() — a method
     * we independently confirmed WORKS (environment() correctly reflected
     * "qa" every time). This sidesteps Owner's @Sources/classpath
     * resolution path completely for OUR properties, while still using
     * Owner for what it's genuinely good at: the typed, @Key-mapped
     * accessor interface itself.
     *
     * LESSON: when a library's declarative "magic" doesn't behave as
     * documented in your specific environment, and you've already spent
     * real effort trying the officially-documented fixes, the pragmatic
     * senior-engineer move is to drop to a lower-level API you can fully
     * verify and control — don't keep guessing at framework internals
     * indefinitely. This is a legitimate, common real-world pattern, not
     * a "hack" — plenty of production Owner-based frameworks load
     * properties this way deliberately, precisely for this kind of
     * cross-environment reliability.
     * ========================================================================
     */
    static {
        String env = System.getProperty("env", "qa");
        loadPropertiesForEnvironment(env);
    }

    private static void loadPropertiesForEnvironment(String env) {
        String resourcePath = "config/config-" + env + ".properties";
        try (InputStream inputStream = ConfigManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new FrameworkException(
                        "Could not find '" + resourcePath + "' on the classpath. Check it exists under "
                                + "src/test/resources/config/ and that 'mvn test-compile' actually copied it "
                                + "to target/test-classes/config/.");
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            for (String key : properties.stringPropertyNames()) {
                ConfigFactory.setProperty(key, properties.getProperty(key));
            }
        } catch (IOException e) {
            throw new FrameworkException("Failed to read " + resourcePath, e);
        }
    }

    // Eagerly initialized ONCE when this class is first loaded by the JVM.
    // Thread-safe by default because static initializers are guaranteed
    // by the JVM to run exactly once. IMPORTANT: this line must come AFTER
    // the static block above — static initializers run top-to-bottom in
    // declaration order, so every property from config-<env>.properties is
    // guaranteed to already be registered with ConfigFactory by the time
    // ConfigFactory.create() executes and builds the EnvironmentConfig proxy.
    private static final EnvironmentConfig config = ConfigFactory.create(EnvironmentConfig.class);

    // Private constructor prevents instantiation — this class only exposes
    // static access, we never need an "instance" of ConfigManager itself.
    private ConfigManager() {
    }

    public static EnvironmentConfig getConfig() {
        return config;
    }
}