package com.company.automation.utils;

import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebElement;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 8: ElementUtils — why wrap click()/sendKeys() at
 * all instead of calling them directly in Page Objects?
 * ============================================================================
 * Three enterprise reasons, all pointing at the same theme (centralize
 * change so it happens in ONE place):
 *
 *   1. CONSISTENT WAITING: every click() call here automatically waits
 *      for clickability first (Module 8's WaitUtils) — a Page Object
 *      author doesn't have to remember to add a wait every single time,
 *      eliminating an entire category of "flaky test because element
 *      wasn't ready yet" bugs.
 *
 *   2. CONSISTENT LOGGING: every interaction gets logged automatically
 *      (Module 8's LogUtils) — when a test fails at 2 AM on a Jenkins
 *      nightly run, the log file (not just a stack trace) tells you the
 *      LAST 5 successful actions before the failure, dramatically
 *      speeding up debugging.
 *
 *   3. FUTURE-PROOFING: if the framework ever needs to change HOW every
 *      click happens (e.g. switching to JS-executor clicks as a fallback
 *      for elements that resist normal Selenium clicks — a very common
 *      real-world need), this is the ONLY file that needs to change.
 *      Every Page Object automatically benefits without modification.
 * ============================================================================
 */
public class ElementUtils {

    private static final Logger logger = LogUtils.getLogger(ElementUtils.class);

    private ElementUtils() {
    }

    public static void click(WebElement element) {
        WaitUtils.waitForClickability(element);
        logger.info("Clicking element: {}", element);
        element.click();
    }

    public static void type(WebElement element, String text) {
        WaitUtils.waitForVisibility(element);
        logger.info("Typing '{}' into element: {}", text, element);
        element.clear();
        element.sendKeys(text);
    }

    public static String getText(WebElement element) {
        WaitUtils.waitForVisibility(element);
        String text = element.getText();
        logger.info("Read text '{}' from element: {}", text, element);
        return text;
    }

    public static boolean isDisplayed(WebElement element) {
        try {
            return WaitUtils.waitForVisibility(element).isDisplayed();
        } catch (Exception e) {
            // Deliberately swallow: "is this displayed?" should return
            // false on a timeout, not blow up the test with an exception —
            // that's the whole point of an isDisplayed() check.
            logger.warn("Element not displayed within wait timeout: {}", element);
            return false;
        }
    }

    // TODO: as real Page Objects surface the need, add select-dropdown
    // helpers, JS-executor click fallback, drag-and-drop wrapper, etc.
    // Same YAGNI note as WaitUtils — build what's actually needed.
}
