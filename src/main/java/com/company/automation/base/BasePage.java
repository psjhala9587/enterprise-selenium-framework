package com.company.automation.base;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 5: BasePage — Page Object Model foundation
 * ============================================================================
 * WHY DOES EVERY PAGE OBJECT EXTEND THIS INSTEAD OF DECLARING ITS OWN
 * WebDriver FIELD?
 *   DRY principle (Don't Repeat Yourself). Without BasePage, every single
 *   Page Object class (LoginPage, DashboardPage, CheckoutPage...) would
 *   duplicate the same constructor boilerplate: fetch the driver, call
 *   PageFactory.initElements(). Centralizing it here means adding a new
 *   Page Object is just: "extend BasePage, declare @FindBy fields, done."
 *
 * WHERE DOES `driver` COME FROM?
 *   DriverManager.getDriver() — NOT a `new ChromeDriver()` call, and NOT
 *   a driver passed in from who-knows-where. This ties directly back to
 *   Module 4: whichever thread is currently running its own test gets
 *   ITS OWN thread-local driver instance automatically, with zero extra
 *   plumbing required in every Page Object.
 *
 * WHAT IS PageFactory.initElements() DOING?
 *   It scans this class (and any subclass) for fields annotated with
 *   @FindBy, and uses a dynamic proxy to lazily locate each WebElement
 *   only when it's actually interacted with (lazy initialization) —
 *   NOT immediately when the page object is constructed. This matters:
 *   if a @FindBy element isn't yet present in the DOM at construction
 *   time (common with dynamically loaded content), the page object can
 *   still be created without throwing — the element lookup is deferred
 *   until you actually call .click()/.sendKeys() on it. Classic interview
 *   question: "What's the benefit of Page Factory over plain
 *   driver.findElement() calls scattered in Page Objects?" Answer: lazy
 *   initialization + centralized/annotated element declaration + cleaner
 *   readability.
 * ============================================================================
 */
public class BasePage {

    protected WebDriver driver;

    public BasePage() {
        this.driver = DriverManager.getDriver();
        PageFactory.initElements(driver, this);
    }

    // TODO (Module 8, once ElementUtils exists): common reusable methods
    // every page object needs, e.g.:
    //   protected void click(WebElement element) { ... with explicit wait ... }
    //   protected void type(WebElement element, String text) { ... }
    //   protected boolean isDisplayed(WebElement element) { ... }
    // Centralizing these here (instead of in each Page Object) means any
    // future change to HOW we click/wait (e.g. switching wait strategy)
    // happens in ONE place.
}
