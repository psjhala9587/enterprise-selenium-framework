package com.company.automation.constants;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 8: Why centralize constants at all?
 * ============================================================================
 * Without this class, values like "how many screenshots directory path" or
 * "how many retry attempts" get copy-pasted across multiple files. The day
 * someone needs to change the retry count from 2 to 3, they either find
 * every copy (error-prone) or — more realistically — miss one, leaving an
 * inconsistent framework. ONE source of truth, referenced everywhere.
 *
 * Why `public static final` and a private constructor?
 *   `static final` -> compile-time constant, accessible without
 *   instantiating this class. Private constructor -> prevents anyone from
 *   accidentally doing `new FrameworkConstants()` (there's no reason to;
 *   this class holds no instance state).
 * ============================================================================
 */
public class FrameworkConstants {

    private FrameworkConstants() {
    }

    // ---- Retry Analyzer (Module 8) ----
    public static final int RETRY_COUNT = 2;

    // ---- Screenshot paths (Module 8) ----
    public static final String SCREENSHOT_DIR = System.getProperty("user.dir") + "/target/screenshots/";

    // ---- Reporting paths (Module 8) ----
    public static final String EXTENT_REPORT_DIR = System.getProperty("user.dir") + "/target/extent-reports/";
    public static final String EXTENT_REPORT_NAME = "ExecutionReport.html";

    // ---- Test data paths (Module 8/9, when ExcelUtils/JsonUtils are built out) ----
    public static final String TEST_DATA_DIR = System.getProperty("user.dir") + "/src/test/resources/testdata/";

    // TODO (Module 9): JIRA_API_ENDPOINT_SUFFIX or similar shared constants
    // once JiraApiClient is built, IF the values are truly static/never
    // environment-dependent (env-dependent values belong in EnvironmentConfig
    // / Module 3's properties files instead, NOT here — don't duplicate
    // config concerns into constants).
}
