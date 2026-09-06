package com.company.automation.stepdefinitions;

import com.company.automation.base.DriverManager;
import com.company.automation.factory.PageObjectManager;
import com.company.automation.hooks.Hooks;
import com.company.automation.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 6: Step Definitions + PicoContainer DI in action
 * ============================================================================
 * THE CONSTRUCTOR BELOW IS THE ENTIRE DI MECHANISM:
 *   public LoginSteps(Hooks hooks) { ... }
 *   Cucumber (via cucumber-picocontainer, Module 2) sees this constructor
 *   parameter and automatically supplies THE SAME Hooks instance that was
 *   created for the currently-running scenario (Hooks.setUp() already ran
 *   as a @Before hook by the time any step executes). We never write
 *   `new Hooks()` ourselves — the container handles it.
 *
 *   If a SECOND step definition class (say, DashboardSteps) ALSO declares
 *   `public DashboardSteps(Hooks hooks)`, PicoContainer injects the exact
 *   SAME Hooks/PageObjectManager instance into it too — meaning
 *   LoginSteps and DashboardSteps silently share the same browser session
 *   within one scenario, with zero manual wiring and zero static fields.
 *
 * WHY NO driver.findElement() ANYWHERE IN THIS FILE:
 *   Step definitions orchestrate WORKFLOW ("log in", "verify error"), they
 *   never touch raw Selenium locators directly — that responsibility
 *   belongs entirely to Page Objects (Module 5/6). This file reads almost
 *   like the Gherkin steps themselves — that's intentional and a strong
 *   signal of a well-layered framework in an interview code review.
 *
 * GHERKIN <-> METHOD MATCHING:
 *   Cucumber matches feature file step text against the string inside
 *   @Given/@When/@Then using regex-like capture groups {string}. Values
 *   inside quotes in the .feature file are auto-passed as method
 *   parameters, in order, type-converted automatically ({string} -> String,
 *   {int} -> int, etc.).
 * ============================================================================
 */
public class LoginSteps {

    private final PageObjectManager pageObjectManager;

    // Constructor injection — see class-level notes above.
    public LoginSteps(Hooks hooks) {
        this.pageObjectManager = hooks.getPageObjectManager();
    }

    @Given("the user is on the login page")
    public void the_user_is_on_the_login_page() {
        // Hooks.setUp() already navigated to ConfigManager base URL — this
        // step exists mainly for Gherkin readability. If login required
        // extra navigation (e.g. clicking a "Sign In" link first), that
        // logic would live here, delegated to a Page Object method.
    }

    @When("the user logs in with username {string} and password {string}")
    public void the_user_logs_in_with_username_and_password(String username, String password) {
        LoginPage loginPage = pageObjectManager.getLoginPage();
        loginPage.loginAs(username, password);
    }

    @Then("the user should be redirected to the dashboard page")
    public void the_user_should_be_redirected_to_the_dashboard_page() {
        // TODO (once a real AUT/DashboardPage exists, Module 6 follow-up):
        // replace this with pageObjectManager.getDashboardPage() and assert
        // a dashboard-specific element is displayed, rather than a raw URL
        // check — URL checks are brittle if routing changes.
        String currentUrl = DriverManager.getDriver().getCurrentUrl();
        Assert.assertTrue(currentUrl.contains("dashboard") || currentUrl.contains("inventory"),
                "Expected to land on dashboard/inventory page but URL was: " + currentUrl);
    }

    @Then("an error message {string} should be displayed")
    public void an_error_message_should_be_displayed(String expectedError) {
        LoginPage loginPage = pageObjectManager.getLoginPage();
        String actualError = loginPage.getErrorMessageText();
        Assert.assertTrue(actualError.contains(expectedError),
                "Expected error containing: " + expectedError + " but got: " + actualError);
    }
}
