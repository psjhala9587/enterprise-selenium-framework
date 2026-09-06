package com.company.automation.utils;

import com.company.automation.base.DriverManager;
import com.company.automation.config.ConfigManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 8: WaitUtils — Implicit vs Explicit Wait
 * ============================================================================
 * RECALL BaseTest/Hooks (Module 5/6) ALREADY SET AN IMPLICIT WAIT
 *   (driver.manage().timeouts().implicitlyWait(...)) — so why do we need
 *   MORE wait logic here?
 *
 *   Implicit wait: tells the WebDriver to poll the DOM for UP TO N seconds
 *   whenever ANY findElement() call doesn't immediately find a match. It's
 *   a blunt, global setting — same wait duration applies to literally
 *   every element lookup in the entire framework, with no way to wait for
 *   a SPECIFIC condition (e.g. "wait until this element is CLICKABLE",
 *   not just "wait until it exists in the DOM").
 *
 *   Explicit wait (WebDriverWait + ExpectedConditions, this file): lets
 *   you wait for a SPECIFIC condition on a SPECIFIC element — visibility,
 *   clickability, text presence, etc. — with its own configurable timeout
 *   independent of the global implicit wait.
 *
 * ⚠️ INTERVIEW WARNING — MIXING IMPLICIT AND EXPLICIT WAITS:
 *   Selenium's official docs explicitly warn AGAINST mixing implicit and
 *   explicit waits on the SAME WebDriver instance — it can cause
 *   unpredictable wait times (e.g. an explicit wait silently waiting
 *   implicit-wait-duration + explicit-wait-duration in some edge cases).
 *   MOST enterprise frameworks still do this pragmatically (implicit wait
 *   as a low baseline safety net + explicit waits for specific
 *   conditions) — but you MUST be able to explain this trade-off if asked
 *   in an interview. Some senior engineers set implicit wait to 0 entirely
 *   and rely PURELY on explicit waits for full predictability — a valid
 *   alternative design choice, worth mentioning as an alternative you
 *   considered.
 * ============================================================================
 */
public class WaitUtils {

    private WaitUtils() {
    }

    private static WebDriverWait getWait() {
        WebDriver driver = DriverManager.getDriver();
        int timeout = ConfigManager.getConfig().explicitWaitInSeconds();
        return new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

    public static WebElement waitForVisibility(WebElement element) {
        return getWait().until(ExpectedConditions.visibilityOf(element));
    }

    public static WebElement waitForClickability(WebElement element) {
        return getWait().until(ExpectedConditions.elementToBeClickable(element));
    }

    public static boolean waitForInvisibility(WebElement element) {
        return getWait().until(ExpectedConditions.invisibilityOf(element));
    }

    // TODO: add waitForTextToBePresentInElement, waitForUrlContains, etc.
    // as real page interactions in your project surface the need for them —
    // resist the urge to pre-build every possible ExpectedConditions
    // wrapper before you actually need it (YAGNI principle).
}
