package com.company.automation.factory;

import com.company.automation.pages.LoginPage;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 6: Page Object Manager pattern
 * ============================================================================
 * PROBLEM WITHOUT THIS CLASS:
 *   Every Step Definition method that needs LoginPage would write
 *   `new LoginPage()`. Harmless-looking, but multiplied across dozens of
 *   step definition classes, it means: (a) repeated object creation for a
 *   page you might reference 10 times in one scenario, and (b) no single
 *   place to control HOW page objects get created if that ever needs
 *   central logic (e.g. logging every page object instantiation).
 *
 * THE PATTERN: a simple cache (Map<Class, Object>) keyed by page class.
 *   First call to getLoginPage() creates a NEW LoginPage and stores it in
 *   the map. Every SUBSequent call within the SAME scenario returns the
 *   cached instance instead of creating a new one. This is sometimes
 *   called a lightweight "object pool" or "registry" pattern.
 *
 * WHY IS THIS SAFE FOR PARALLEL EXECUTION?
 *   Because PageObjectManager itself is NOT static/shared globally — a
 *   fresh instance is created per scenario by Hooks.java (see below), and
 *   PicoContainer injects THAT specific instance only into the step
 *   definition classes participating in THAT scenario. Different parallel
 *   threads/scenarios each get their own PageObjectManager, which in turn
 *   wraps that thread's own DriverManager-supplied WebDriver (Module 4).
 *   Three layers of thread-isolation stacking correctly: DriverManager ->
 *   PageObjectManager -> Step Definitions. Be ready to draw this out on
 *   a whiteboard in interviews.
 * ============================================================================
 */
public class PageObjectManager {

    private final WebDriver driver;
    private final Map<Class<?>, Object> pageCache = new HashMap<>();

    public PageObjectManager(WebDriver driver) {
        this.driver = driver;
    }

    public LoginPage getLoginPage() {
        return (LoginPage) pageCache.computeIfAbsent(LoginPage.class, cls -> new LoginPage());
    }

    // TODO: as more Page Objects are added (DashboardPage, CheckoutPage...),
    // add one getter each following the exact same computeIfAbsent pattern.
    // Note `driver` field above isn't directly used yet since BasePage
    // pulls it from DriverManager itself (Module 5) — kept here as an
    // explicit reference for clarity/future use (e.g. if a Page Object's
    // constructor needs to take driver as an explicit parameter instead).
}
