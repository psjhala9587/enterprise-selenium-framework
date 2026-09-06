package com.company.automation.config;

import org.aeonbits.owner.Config;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 3: Aeonbits Owner Deep-Dive
 * ============================================================================
 * WHAT IS THIS FILE?
 *   An interface — NOT a class. Owner generates a runtime implementation of
 *   this interface using a Java Dynamic Proxy. You never write the
 *   implementation yourself.
 *
 * @Config.Sources — tells Owner WHERE to load property values from.
 *   The "system:properties" entry checks JVM system properties FIRST
 *   (e.g. values passed via -Denv=qa on the command line).
 *   The classpath entry uses ${env} as a PLACEHOLDER: Owner substitutes it
 *   with the "env" system property at runtime.
 *   ==> This is THE mechanism Jenkins hooks into later:
 *       mvn test -Denv=qa   -->  Owner loads config-qa.properties
 *       mvn test -Denv=prod -->  Owner loads config-prod.properties
 *   Default fallback below is "qa" so local runs work without any flag.
 *
 * @Key — maps a Java method to a specific key in the .properties file.
 *   Without @Key, Owner would default to using the exact method name as
 *   the property key (camelCase methods don't match dot.separated keys
 *   well, so we always specify @Key explicitly for clarity).
 *
 * @DefaultValue — fallback value if the key is missing from the properties
 *   file entirely. Prevents NullPointerExceptions from a missing key;
 *   fails safe instead of failing silently later, deep inside a test.
 *
 * TYPE-SAFETY BONUS: notice getImplicitWaitInSeconds() returns an `int`,
 * not a String. Owner automatically converts the property file's
 * "10" (text) into an int for you. Try this with plain java.util.Properties
 * and you'll be writing Integer.parseInt() everywhere manually.
 * ============================================================================
 */
@Config.Sources({
        "system:properties",
        "classpath:config/config-${env}.properties"
})
public interface EnvironmentConfig extends Config {

    @Key("base.url")
    String baseUrl();

    @Key("browser")
    @DefaultValue("chrome")
    String browser();

    @Key("headless")
    @DefaultValue("false")
    boolean headless();

    @Key("implicit.wait.seconds")
    @DefaultValue("10")
    int implicitWaitInSeconds();

    @Key("explicit.wait.seconds")
    @DefaultValue("20")
    int explicitWaitInSeconds();

    @Key("page.load.timeout.seconds")
    @DefaultValue("30")
    int pageLoadTimeoutInSeconds();

    @Key("env")
    @DefaultValue("qa")
    String environment();

    // NOTE: Jira integration config (jira.integration.enabled, jira.url,
    // jira.project.key, jira.email, jira.api.token) was built out in
    // Module 9 and then DELIBERATELY REMOVED per a later decision to defer
    // it. See REVISION-NOTES.md's "Module 9" section for the full design
    // (auth approach, secrets handling via system:properties, the
    // duplicate-ticket landmine) if/when this gets revisited.
}
