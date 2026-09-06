package com.company.automation.enums;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 4: Type-safe browser selection
 * ============================================================================
 * Same reasoning as EnvironmentType (Module 3): avoids magic strings like
 * "chrome"/"firefox" scattered across DriverFactory. The config file still
 * stores it as a String (browser=chrome) — we convert String -> enum via
 * BrowserType.valueOf(config.browser().toUpperCase()) inside DriverFactory.
 * ============================================================================
 */
public enum BrowserType {
    CHROME,
    FIREFOX,
    EDGE,
    SAFARI
    // TODO: add REMOTE if/when we wire up Selenium Grid / cloud execution
}
