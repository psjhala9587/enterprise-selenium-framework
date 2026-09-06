package com.company.automation.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 8: AnnotationTransformer — auto-wiring RetryAnalyzer
 * ============================================================================
 * THE PROBLEM THIS SOLVES:
 *   The "normal" way to use a RetryAnalyzer is manually annotating every
 *   single test: `@Test(retryAnalyzer = RetryAnalyzer.class)`. In a
 *   Cucumber+TestNG setup specifically, we don't even control the @Test
 *   annotation directly — it's applied INTERNALLY by
 *   AbstractTestNGCucumberTests' runScenario() method (Module 6/7), so we
 *   have no line of our own code to manually add retryAnalyzer to anyway.
 *
 * THE FIX: IAnnotationTransformer
 *   TestNG calls transform() for EVERY @Test method it discovers, right
 *   before running it, giving us a chance to MODIFY that test's
 *   annotation programmatically. Setting `annotation.setRetryAnalyzer(...)`
 *   here means EVERY test in the entire suite gets retry behavior
 *   automatically — zero per-test annotation needed, and it works
 *   transparently even for Cucumber's internally-generated @Test methods.
 *
 * HOW TESTNG KNOWS TO USE THIS CLASS AT ALL:
 *   It must be registered in testng.xml's <listeners> block (Module 8,
 *   see testng.xml update below) — an IAnnotationTransformer is just
 *   another kind of TestNG Listener under the hood.
 * ============================================================================
 */
public class AnnotationTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                           Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
