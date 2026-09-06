package com.company.automation.base;

import org.openqa.selenium.WebDriver;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 4: DriverManager (Singleton + ThreadLocal)
 * ============================================================================
 * THE PROBLEM THIS SOLVES:
 *   In parallel execution (multiple test threads running simultaneously),
 *   a single shared `static WebDriver driver` field would be overwritten
 *   by whichever thread runs last to call setDriver() — every other thread
 *   would then be silently controlling the WRONG browser window. This
 *   causes bizarre, hard-to-reproduce flaky failures that are a nightmare
 *   to debug (classic real-world horror story to mention in interviews).
 *
 * THE FIX: ThreadLocal<WebDriver>
 *   ThreadLocal does NOT store one shared value. Internally, each Thread
 *   object has its own private storage map, and ThreadLocal is just the
 *   KEY used to look up that thread's own private value. So:
 *     - Thread-1 calls driverThreadLocal.get() -> gets Thread-1's own driver
 *     - Thread-2 calls driverThreadLocal.get() -> gets Thread-2's own driver
 *   Same static field, same method call, completely isolated results.
 *
 * WHY IS THIS "SINGLETON" THEN, IF EACH THREAD GETS ITS OWN INSTANCE?
 *   Precise interview answer: this is a "Singleton PER THREAD" — exactly
 *   ONE WebDriver instance exists for the lifetime of a given thread
 *   (never two browsers accidentally opened in the same test thread), but
 *   the JVM as a whole can have multiple such singletons — one per active
 *   thread. This nuance trips up a lot of candidates — know it cold.
 *
 * WHY A SEPARATE CLASS INSTEAD OF EMBEDDING ThreadLocal IN BaseTest?
 *   Single Responsibility Principle — DriverManager's ONLY job is
 *   holding/returning/cleaning up the WebDriver reference. DriverFactory
 *   (next file) is responsible for actually CREATING the right browser
 *   instance. BaseTest (Module 5) just orchestrates calling both at the
 *   right lifecycle moments (@BeforeMethod / @AfterMethod).
 *
 * CRITICAL: remove() in tearDown() below
 *   If you forget to call remove(), and your test execution uses a
 *   THREAD POOL (which TestNG/Surefire parallel execution does — threads
 *   get REUSED across different test methods, not recreated fresh each
 *   time), a stale/closed WebDriver reference can leak into the next test
 *   that happens to run on that same reused thread. Forgetting this line
 *   is a real production bug — flag it in every code review.
 * ============================================================================
 */
public class DriverManager {

    // ThreadLocal holding one WebDriver reference PER THREAD.
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverManager() {
        // private constructor -> this class is never instantiated;
        // pure static utility access only.
    }

    /**
     * Called once per test thread (from BaseTest @BeforeMethod, Module 5)
     * right after DriverFactory creates the browser instance.
     */
    public static void setDriver(WebDriver driver) {
        driverThreadLocal.set(driver);
    }

    /**
     * Called from every Page Object / Step Definition that needs to
     * interact with the browser. Returns THIS thread's own driver.
     */
    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    /**
     * MUST be called in @AfterMethod (Module 5) after driver.quit().
     * Clears this thread's ThreadLocal entry to prevent memory leaks and
     * stale-driver bugs when the thread is reused by the pool.
     */
    public static void unload() {
        driverThreadLocal.remove();
    }
}
