package com.company.automation.hooks;

import com.company.automation.base.DriverManager;
import com.company.automation.config.ConfigManager;
import com.company.automation.factory.DriverFactory;
import com.company.automation.factory.PageObjectManager;
import com.company.automation.utils.LogUtils;
import com.company.automation.utils.ScreenshotUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 6: Cucumber Hooks vs TestNG BaseTest (Module 5)
 * ============================================================================
 * WHY DOES THIS LOOK SO SIMILAR TO BaseTest, BUT WITH DIFFERENT ANNOTATIONS?
 *   Cucumber has its OWN independent hook lifecycle — @Before/@After here
 *   are io.cucumber.java annotations, NOT org.testng.annotations. They run
 *   once per SCENARIO (Cucumber's unit of execution), conceptually
 *   equivalent to TestNG's per-METHOD hooks in BaseTest. In our final
 *   framework, Cucumber scenarios are what Surefire/TestNG actually
 *   executes as "test methods" (via the Runner in this same module) — so
 *   Hooks.java is effectively the REAL BaseTest for our BDD flow.
 *   BaseTest (Module 5) remains useful only for non-BDD plain sanity tests.
 *
 * THE CRITICAL NEW ROLE OF THIS CLASS: Dependency Injection source
 *   Notice `pageObjectManager` is a plain INSTANCE field (not static).
 *   Because cucumber-picocontainer (Module 2) is on our classpath,
 *   Cucumber creates exactly ONE instance of THIS Hooks class per
 *   scenario, and if a Step Definition class's CONSTRUCTOR asks for a
 *   Hooks parameter, PicoContainer injects THIS SAME instance into it.
 *   That's how LoginSteps (below) gets access to the SAME
 *   PageObjectManager (and therefore the same WebDriver) that THIS Hooks
 *   instance set up — with zero static fields, fully thread-safe for
 *   parallel scenario execution.
 *
 * @Before(order = 1) / execution order:
 *   Cucumber supports multiple @Before methods with an `order` attribute
 *   (lower runs first) — useful later if we need e.g. a database seed
 *   step to run BEFORE the browser even launches. Not needed yet, but
 *   good to know for interviews ("How do you control multiple hook
 *   execution order in Cucumber?").
 *
 * Scenario PARAMETER in @After:
 *   Cucumber injects the current Scenario object automatically. We use
 *   it here to check scenario.isFailed() — this is EXACTLY where
 *   screenshot-on-failure logic will plug in during Module 8
 *   (scenario.attach(screenshotBytes, "image/png", "failure-screenshot")).
 * ============================================================================
 */
public class Hooks {

    private static final Logger logger = LogUtils.getLogger(Hooks.class);
    private WebDriver driver;
    private PageObjectManager pageObjectManager;

    @Before
    public void setUp(Scenario scenario) {
        driver = DriverFactory.createDriver();
        DriverManager.setDriver(driver);

        driver.manage().timeouts().implicitlyWait(
                Duration.ofSeconds(ConfigManager.getConfig().implicitWaitInSeconds()));
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(ConfigManager.getConfig().pageLoadTimeoutInSeconds()));
        driver.manage().window().maximize();

        driver.get(ConfigManager.getConfig().baseUrl());

        pageObjectManager = new PageObjectManager(driver);

        logger.info("Starting scenario: {}", scenario.getName());
    }

    @After
    public void tearDown(Scenario scenario) {
        // MODULE 8: on failure, capture a screenshot and attach it DIRECTLY
        // to the Cucumber scenario report (scenario.attach) — this shows up
        // inline in the Cucumber HTML report (TestRunner's "plugin" config,
        // Module 6) IN ADDITION TO the separate ExtentReports HTML report
        // (Module 8's ExtentReportListener, which captures its own
        // screenshot independently at the TestNG level). Two different
        // report tools, two independent screenshot captures — intentional
        // redundancy since either report might be the one a teammate
        // actually opens when investigating a failure.
        if (scenario.isFailed()) {
            logger.error("Scenario FAILED: {}", scenario.getName());
            byte[] screenshot = ScreenshotUtils.captureAsBytes(driver);
            scenario.attach(screenshot, "image/png", scenario.getName());

            // NOTE: Module 9 built out automatic Jira bug-ticket creation
            // here (JiraApiClient.createBugTicket + attachScreenshot,
            // guarded by ConfigManager.getConfig().jiraIntegrationEnabled())
            // and it was later deliberately removed pending a future
            // revisit. See REVISION-NOTES.md's Module 9 section for the
            // full design (duplicate-ticket landmine, secrets handling,
            // auth approach) if/when this gets rebuilt.
        }
        if (driver != null) {
            driver.quit();
        }
        DriverManager.unload();
    }

    /**
     * This is THE method PicoContainer's injection depends on being
     * accessible — Step Definition classes call hooks.getPageObjectManager()
     * after receiving this same Hooks instance via their own constructor.
     */
    public PageObjectManager getPageObjectManager() {
        return pageObjectManager;
    }
}
