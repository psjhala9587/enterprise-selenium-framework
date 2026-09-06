package com.company.automation.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.company.automation.base.DriverManager;
import com.company.automation.constants.FrameworkConstants;
import com.company.automation.utils.LogUtils;
import com.company.automation.utils.ScreenshotUtils;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 8: ExtentReportListener — the enterprise HTML report
 * ============================================================================
 * WHY ITestListener INSTEAD OF WRITING REPORT CODE INTO BaseTest/Hooks?
 *   Same Single Responsibility principle as always: Hooks/BaseTest handle
 *   BROWSER lifecycle; a Listener handles REPORTING lifecycle. TestNG
 *   calls these methods automatically at the right moments for EVERY test
 *   in the suite — we never manually call onTestStart()/onTestFailure()
 *   ourselves anywhere. Wired via testng.xml's <listeners> block, same as
 *   AnnotationTransformer above.
 *
 * ExtentReports vs Allure (mentioned back in Module 2's TODO) — the
 * trade-off to know for interviews:
 *   - ExtentReports: single self-contained HTML file, easy to email/share,
 *     simpler setup, good enough for most teams.
 *   - Allure: richer step-by-step breakdown UI, better historical
 *     trend/flaky-test analytics across MULTIPLE runs, but requires a
 *     separate Allure command-line report generation step (not just a
 *     single HTML file) — more powerful, more moving parts.
 *   This framework picked ExtentReports for simplicity; a real project
 *   should make this choice based on team size/CI infrastructure maturity.
 *
 * ExtentReports THREAD-SAFETY FOR PARALLEL EXECUTION (Module 7 tie-in):
 *   A plain `ExtentTest` object is NOT inherently thread-safe to share
 *   across parallel threads. The standard fix is `ExtentTest` wrapped in
 *   a ThreadLocal — EXACT same pattern as Module 4's DriverManager. Notice
 *   the repetition of the ThreadLocal concept across this framework —
 *   that's not a coincidence, it's the SAME underlying problem (shared
 *   mutable state across parallel threads) solved the SAME way each time.
 *   Recognizing this pattern-reuse is a strong signal in interviews.
 * ============================================================================
 */
public class ExtentReportListener implements ITestListener {

    private static final Logger logger = LogUtils.getLogger(ExtentReportListener.class);
    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    @Override
    public void onStart(ITestContext context) {
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(
                FrameworkConstants.EXTENT_REPORT_DIR + FrameworkConstants.EXTENT_REPORT_NAME);
        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);
        logger.info("ExtentReports initialized -> {}", FrameworkConstants.EXTENT_REPORT_DIR);
    }

    @Override
    public void onTestStart(ITestResult result) {
        ExtentTest test = extentReports.createTest(result.getMethod().getMethodName());
        extentTest.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        extentTest.get().pass("Test passed.");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        // NOTE: Module 9 explored Jira ticket creation and deliberately
        // chose NOT to place it here (would duplicate Hooks.tearDown()'s
        // scenario.isFailed() firing for the same underlying failure).
        // Jira integration was later removed from the framework entirely
        // pending a future revisit — see REVISION-NOTES.md. This
        // listener's responsibility stays scoped purely to REPORTING.
        String screenshotPath = ScreenshotUtils.captureAsFile(
                DriverManager.getDriver(), result.getMethod().getMethodName());
        extentTest.get().fail(result.getThrowable());
        try {
            extentTest.get().fail("Screenshot on failure:",
                    MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
        } catch (Exception e) {
            logger.error("Could not attach screenshot to Extent report", e);
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        extentTest.get().skip("Test skipped: " + result.getThrowable());
    }

    @Override
    public void onFinish(ITestContext context) {
        extentReports.flush();
        extentTest.remove();
        logger.info("ExtentReports flushed to disk.");
    }
}
