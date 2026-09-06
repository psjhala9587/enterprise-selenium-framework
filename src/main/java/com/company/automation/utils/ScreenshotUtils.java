package com.company.automation.utils;

import com.company.automation.constants.FrameworkConstants;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 8: ScreenshotUtils — two capture methods, two uses
 * ============================================================================
 * WHY BOTH captureAsBytes() AND captureAsFile()?
 *   - captureAsBytes(): used to EMBED a screenshot directly INTO an HTML
 *     report (ExtentReports, Module 8 below) or a Cucumber
 *     `scenario.attach(bytes, "image/png", name)` call — no disk file
 *     needed, everything lives inside the report itself.
 *   - captureAsFile(): saves a standalone .png to target/screenshots/ —
 *     useful when you want Jenkins to archive raw screenshot files as
 *     build artifacts (Module 10's `archiveArtifacts` step) independent
 *     of whatever HTML report tool is in use, or for manually inspecting
 *     a failure without opening the full report.
 *
 * TakesScreenshot INTERFACE:
 *   Not every WebDriver implementation guarantees screenshot support, so
 *   Selenium requires an explicit cast: ((TakesScreenshot) driver). All
 *   modern browser drivers (Chrome/Firefox/Edge) DO support it — this
 *   cast is safe in practice for a UI test framework.
 * NOTE: file copying uses plain java.nio.file.Files instead of pulling in
 *   Apache Commons IO — a whole extra Maven dependency (Module 2 lessons
 *   applied: don't add a dependency for something the JDK already does
 *   natively in 2 lines). Know this trade-off for interviews: "did you
 *   need that library, or did you reach for it out of habit?"
 * ============================================================================
 */
public class ScreenshotUtils {

    private static final Logger logger = LogUtils.getLogger(ScreenshotUtils.class);

    private ScreenshotUtils() {
    }

    public static byte[] captureAsBytes(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    public static String captureAsFile(WebDriver driver, String testName) {
        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = testName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".png";
        String destinationPath = FrameworkConstants.SCREENSHOT_DIR + fileName;

        try {
            Files.createDirectories(Paths.get(FrameworkConstants.SCREENSHOT_DIR));
            Files.copy(srcFile.toPath(), Paths.get(destinationPath), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Screenshot saved: {}", destinationPath);
        } catch (IOException e) {
            // Deliberately just log, don't rethrow — a failed screenshot
            // capture should never mask the ORIGINAL test failure that
            // triggered this call in the first place.
            logger.error("Failed to save screenshot for test: {}", testName, e);
        }

        return destinationPath;
    }
}
