package com.company.automation.base;

import com.company.automation.config.ConfigManager;
import com.company.automation.factory.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 5: BaseTest — the orchestrator
 * ============================================================================
 * WHY DOES THIS CLASS EXIST SEPARATELY FROM DriverFactory/DriverManager?
 *   Recall Module 4's Single Responsibility split:
 *     - DriverFactory : HOW to build a driver
 *     - DriverManager  : WHERE to store/retrieve it (thread-safe)
 *     - BaseTest       : WHEN to create/destroy it (test lifecycle timing)
 *   BaseTest is the glue — it doesn't know HOW a ChromeDriver is built
 *   internally, it just calls DriverFactory.createDriver() at the right
 *   moment and registers the result with DriverManager.
 *
 * WHY @BeforeMethod / @AfterMethod AND NOT @BeforeClass / @AfterClass?
 *   @BeforeClass/@AfterClass run ONCE per test CLASS — meaning all test
 *   METHODS in that class would share ONE browser session, causing
 *   leftover state (cookies, logged-in session, previous page) to bleed
 *   between tests. @BeforeMethod/@AfterMethod run before/after EVERY
 *   test method — guaranteeing total test isolation. This is a very
 *   common interview question: "Why not reuse the browser across tests
 *   for speed?" Answer: test isolation/reliability is prioritized over
 *   raw speed in Selenium regression suites; if speed matters more,
 *   the correct lever is PARALLEL execution (Module 7/8), not sharing
 *   browser state.
 *
 * WHY EVERY CUCUMBER STEP DEFINITION / PAGE OBJECT WON'T EXTEND THIS:
 *   In our Cucumber setup (Module 7), Hooks.java (not this BaseTest) will
 *   actually own the @Before/@After Cucumber-native annotations, because
 *   Cucumber has its OWN hook lifecycle separate from TestNG's. BaseTest
 *   as written here is the PLAIN TestNG version — useful for any
 *   non-BDD sanity tests (src/test/.../tests/) and to teach the lifecycle
 *   concept in isolation before we complicate it with Cucumber's DI
 *   (PicoContainer) in Module 7. Don't be confused when Module 7 looks
 *   structurally similar but uses different annotations.
 * ============================================================================
 */
public class BaseTest {

    protected WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        driver = DriverFactory.createDriver();
        DriverManager.setDriver(driver);

        // Apply framework-wide timeouts from our Module 3 Owner config —
        // centralizing this here means every test automatically gets
        // consistent wait behavior without repeating this in every class.
        driver.manage().timeouts().implicitlyWait(
                Duration.ofSeconds(ConfigManager.getConfig().implicitWaitInSeconds()));
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(ConfigManager.getConfig().pageLoadTimeoutInSeconds()));
        driver.manage().window().maximize();

        driver.get(ConfigManager.getConfig().baseUrl());

        // TODO (Module 8): replace System.out with Log4j2 LogUtils.info(...)
        System.out.println("[BaseTest] Browser launched -> " + ConfigManager.getConfig().baseUrl());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        // TODO (Module 8): capture screenshot here BEFORE quitting driver,
        // if the test result is a FAILURE (needs a TestNG ITestResult
        // parameter + a TestListener — covered when we build reporting).
        if (driver != null) {
            driver.quit();
        }
        // CRITICAL (see DriverManager's Module 4 notes): always clear the
        // ThreadLocal entry so a reused pooled thread never sees a stale,
        // already-quit driver reference.
        DriverManager.unload();
    }
}
