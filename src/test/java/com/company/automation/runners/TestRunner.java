package com.company.automation.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 6: The Runner — where TestNG meets Cucumber
 * ============================================================================
 * WHY EXTEND AbstractTestNGCucumberTests INSTEAD OF @RunWith(Cucumber.class)?
 *   @RunWith(Cucumber.class) is the JUnit-flavored way to run Cucumber.
 *   Since our framework standardized on TestNG (Module 2 decision — better
 *   enterprise parallel execution support, DataProvider flexibility,
 *   built-in retry/listener APIs we'll use in Module 8), we use the
 *   cucumber-testng module's AbstractTestNGCucumberTests instead. This
 *   class internally converts each Cucumber Scenario into a TestNG
 *   @Test-like unit that Surefire can discover and execute.
 *
 * @CucumberOptions BREAKDOWN:
 *   features -> path to .feature files (relative to classpath root, i.e.
 *               src/test/resources)
 *   glue     -> package(s) Cucumber scans for Step Definition classes AND
 *               Hooks classes — MUST include every package containing
 *               @Given/@When/@Then/@Before/@After annotated classes.
 *   plugin   -> output formatters. "pretty" = readable console output;
 *               "html:target/cucumber-reports/cucumber.html" = basic HTML
 *               report (Module 8 will replace/supplement this with
 *               ExtentReports for a richer enterprise report).
 *   tags     -> filters WHICH scenarios run, using Cucumber's tag
 *               expression syntax (e.g. "@smoke", "@smoke or @regression",
 *               "not @wip"). Left commented for now — Module 7/10 will
 *               show how Jenkins passes this dynamically via
 *               -Dcucumber.filter.tags="@smoke" instead of hardcoding it
 *               here (hardcoding tags would defeat the point of a
 *               configurable Jenkins pipeline parameter).
 *   monochrome -> cleaner console output formatting (no ANSI artifacts
 *               in some CI console log viewers).
 *
 * PARALLEL EXECUTION HOOK (Module 7/8 deep dive):
 *   Overriding scenarios() below with @DataProvider(parallel = true) is
 *   what actually enables TestNG to execute multiple Cucumber scenarios
 *   CONCURRENTLY (each on its own thread, each with its own DriverManager
 *   ThreadLocal instance — this is WHY Module 4's ThreadLocal work matters
 *   so much). Thread count is controlled via a dataproviderthreadcount
 *   setting in testng.xml (added in Module 7).
 * ============================================================================
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {
                "com.company.automation.stepdefinitions",
                "com.company.automation.hooks"
        },
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber.html"
                // TODO (Module 8): add ExtentReports plugin/adapter here
        },
        monochrome = true
        // TAGS INTENTIONALLY NOT SET HERE — see MODULE 7 notes below on why
        // this is left to run-time system properties instead of hardcoding.
)
public class TestRunner extends AbstractTestNGCucumberTests {

    /**
     * ========================================================================
     * MODULE 7: Parallel scenario execution — ACTIVATED
     * ========================================================================
     * @DataProvider(parallel = true) tells TestNG: "each row this data
     * provider returns (i.e. each Cucumber Scenario) may run on its OWN
     * thread, concurrently with the others." super.scenarios() is
     * Cucumber's own implementation that converts every parsed Scenario
     * into a DataProvider row — we're not changing WHAT runs, only
     * ENABLING it to run in parallel.
     *
     * HOW MANY THREADS ACTUALLY GET USED?
     *   Controlled entirely by testng.xml's <suite ... data-provider-
     *   thread-count="N"> attribute (added below in Module 7) — NOT by
     *   any number in this Java file. This separation is intentional:
     *   Jenkins can point at a DIFFERENT testng.xml (or the same one with
     *   a different thread count) per environment/job WITHOUT touching
     *   Java code or recompiling anything — same design philosophy as
     *   Module 3's environment config files.
     *
     * WHY THIS IS SAFE NOW BUT WOULD HAVE BEEN DANGEROUS IN MODULE 3:
     *   Every layer this touches was BUILT for thread-safety from day
     *   one: DriverManager's ThreadLocal (Module 4), a fresh
     *   PageObjectManager per scenario via Hooks (Module 6), zero shared
     *   static mutable state anywhere. Flipping this flag on a framework
     *   that used static WebDriver fields would immediately produce
     *   cross-thread browser corruption — this is WHY thread-safety has
     *   to be designed in from the start, not bolted on later.
     * ========================================================================
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
