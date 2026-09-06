package com.company.automation.pages;

import com.company.automation.base.BasePage;
import com.company.automation.utils.ElementUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 5: Your FIRST real Page Object (sample/template)
 * ============================================================================
 * PAGE OBJECT MODEL (POM) — the core idea:
 *   Each web page (or logical component of a page) gets its OWN class that
 *   encapsulates:
 *     1. Locators for that page's elements (the @FindBy fields below)
 *     2. Actions a user can perform on that page (the methods below)
 *   Step Definitions (Module 7) NEVER contain raw driver.findElement()
 *   calls — they only call methods LIKE loginPage.enterUsername("bob").
 *   This means: if the login button's ID changes in the UI, you fix it
 *   in ONE place (this file) — not in every step definition/test that
 *   happens to click login.
 *
 * @FindBy vs driver.findElement() — WHY THE ANNOTATION APPROACH?
 *   @FindBy declares the locator strategy DECLARATIVELY (readable at a
 *   glance) and — combined with PageFactory.initElements() in BasePage —
 *   gives you LAZY element lookup (see BasePage notes) instead of an
 *   eager driver.findElement() call that fires immediately and can throw
 *   NoSuchElementException before the page has even loaded.
 *
 * THIS IS A TEMPLATE/SAMPLE — replace the locators below with your
 * actual application's real element IDs once you have a real AUT
 * (Application Under Test) to point this framework at.
 * ============================================================================
 */
public class LoginPage extends BasePage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "loginButton")
    private WebElement loginButton;

    @FindBy(css = ".error-message")
    private WebElement errorMessage;

    // Constructor is implicit here — BasePage's no-arg constructor already
    // does driver fetch + PageFactory.initElements(). Nothing extra needed
    // unless this page requires page-specific setup logic.

    public void enterUsername(String username) {
        ElementUtils.type(usernameInput, username);
    }

    public void enterPassword(String password) {
        ElementUtils.type(passwordInput, password);
    }

    public void clickLogin() {
        ElementUtils.click(loginButton);
    }

    /**
     * Example of a higher-level "workflow" method — combines multiple
     * low-level actions into one meaningful business action. Step
     * Definitions (Module 7) will call THIS method, not the three
     * individual methods above, keeping Gherkin steps clean and readable.
     */
    public void loginAs(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
    }

    public String getErrorMessageText() {
        return ElementUtils.getText(errorMessage);
    }
}
