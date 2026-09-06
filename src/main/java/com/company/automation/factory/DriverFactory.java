package com.company.automation.factory;

import com.company.automation.config.ConfigManager;
import com.company.automation.enums.BrowserType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 4: Factory Design Pattern
 * ============================================================================
 * WHAT PROBLEM DOES THE FACTORY PATTERN SOLVE HERE?
 *   Without it, every test class would contain its own if/else browser
 *   creation logic — duplicated everywhere, painful to maintain, and a
 *   nightmare if (say) Chrome's headless flag syntax changes tomorrow.
 *   The Factory CENTRALIZES "how do I build a WebDriver instance" into
 *   ONE place. Callers just say "give me a driver" — they don't care HOW
 *   it's built internally. This is the textbook definition of the Factory
 *   pattern: "encapsulate object creation logic behind a single method."
 *
 * MODULE 5 UPDATE — using Selenium's INBUILT "Selenium Manager" instead
 * of the external Bonigarcia WebDriverManager library:
 *   Selenium needs a native driver BINARY (chromedriver.exe / geckodriver)
 *   matching your installed browser's version to talk to it. Since
 *   Selenium 4.6+, Selenium ships its OWN built-in "Selenium Manager"
 *   that does this automatically, with ZERO extra setup code — simply
 *   calling `new ChromeDriver()` silently triggers Selenium Manager
 *   behind the scenes to detect your browser version, download the
 *   matching driver binary, and wire it up. No `webdrivermanager` Maven
 *   dependency needed at all (Module 2's dependency stays in pom.xml as
 *   an optional/commented reference — enterprise CI setups sometimes
 *   still prefer Bonigarcia's version for finer proxy/caching control,
 *   but for learning AND for most real projects today, inbuilt Selenium
 *   Manager is genuinely sufficient — know BOTH for interviews).
 *
 * WHY headless MATTERS FOR JENKINS (preview of Module 10):
 *   Jenkins build agents are usually Linux servers with NO display/monitor
 *   attached. A normal browser window CANNOT open there. `headless=true`
 *   runs the browser with no visible UI — this is WHY our config-prod/
 *   config-staging properties (Module 3) default headless=true, while
 *   local qa dev config defaults headless=false (so YOU can visually watch
 *   the browser while developing/debugging a test).
 *
 * THIS METHOD RETURNS A PLAIN WebDriver, NOT via DriverManager directly —
 *   Single Responsibility again: DriverFactory only KNOWS HOW TO BUILD a
 *   driver. It's BaseTest's job (Module 5) to take this returned driver
 *   and hand it to DriverManager.setDriver(). Keeping these decoupled
 *   means we could reuse DriverFactory in a completely different context
 *   (e.g. a health-check script) without dragging in ThreadLocal logic.
 * ============================================================================
 */
public class DriverFactory {

    private DriverFactory() {
        // static utility class — no instantiation needed
    }

    public static WebDriver createDriver() {
        String configuredBrowser = ConfigManager.getConfig().browser().toUpperCase();
        BrowserType browserType = BrowserType.valueOf(configuredBrowser);
        boolean headless = ConfigManager.getConfig().headless();

        WebDriver driver;

        switch (browserType) {
            case CHROME -> {
                // NOTE: no explicit driver-binary setup call needed here —
                // Selenium's inbuilt Selenium Manager handles it automatically
                // the moment `new ChromeDriver(...)` is invoked below.
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                // TODO: add more enterprise-standard args as needed, e.g.
                //   options.addArguments("--disable-gpu", "--window-size=1920,1080",
                //                        "--no-sandbox"); // common on Jenkins Linux agents
                driver = new ChromeDriver(options);
            }
            case FIREFOX -> {
                // Selenium Manager handles the geckodriver binary automatically too.
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    options.addArguments("-headless");
                }
                driver = new FirefoxDriver(options);
            }
            case EDGE -> {
                // Selenium Manager handles the msedgedriver binary automatically too.
                driver = new EdgeDriver();
                // TODO: EdgeOptions + headless support, same pattern as above
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported browser configured: " + configuredBrowser +
                    " — check browser= value in your config-<env>.properties file."
            );
        }

        // TODO (Module 8/utils): apply implicit/page-load timeouts here from
        // ConfigManager.getConfig().implicitWaitInSeconds() etc., and call
        // driver.manage().window().maximize().

        return driver;
    }
}
