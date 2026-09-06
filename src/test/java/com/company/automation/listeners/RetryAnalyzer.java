package com.company.automation.listeners;

import com.company.automation.constants.FrameworkConstants;
import com.company.automation.utils.LogUtils;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 8: RetryAnalyzer — handling FLAKY tests
 * ============================================================================
 * WHAT'S A "FLAKY" TEST, PRECISELY?
 *   A test that fails intermittently for reasons UNRELATED to a real bug —
 *   a slow network causing an element to render 200ms later than usual, a
 *   third-party ad script injecting a temporary overlay, a CI agent under
 *   heavy load. Real bugs should NEVER be silently retried away — the
 *   retry mechanism exists ONLY to absorb environmental noise, not to
 *   hide genuine defects. This distinction is important to articulate in
 *   interviews: retry is a pragmatic tool for INFRASTRUCTURE flakiness,
 *   not a substitute for fixing an actually-broken test or feature.
 *
 * HOW retry(ITestResult result) IS CALLED:
 *   TestNG calls this method automatically after ANY test failure, IF
 *   this RetryAnalyzer is wired to that test (see AnnotationTransformer
 *   below for HOW it gets wired to every test automatically, since
 *   manually adding `retryAnalyzer = RetryAnalyzer.class` to every single
 *   @Test annotation would be exactly the kind of repeated boilerplate
 *   this whole framework tries to avoid). Returning `true` tells TestNG
 *   "run this test again"; `false` means "stop, this failure is final."
 *
 * WHY A retryCount FIELD RESETS PER RetryAnalyzer INSTANCE:
 *   TestNG creates a NEW RetryAnalyzer instance for EACH test method, so
 *   this counter naturally starts fresh per test — no manual reset logic
 *   needed, and no cross-test contamination of retry counts.
 * ============================================================================
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger logger = LogUtils.getLogger(RetryAnalyzer.class);
    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < FrameworkConstants.RETRY_COUNT) {
            retryCount++;
            logger.warn("Retrying test '{}' - attempt {} of {}",
                    result.getName(), retryCount, FrameworkConstants.RETRY_COUNT);
            return true;
        }
        return false;
    }
}
