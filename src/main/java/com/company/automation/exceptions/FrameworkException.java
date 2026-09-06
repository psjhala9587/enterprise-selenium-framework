package com.company.automation.exceptions;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 8: Custom Exceptions — why bother?
 * ============================================================================
 * Why not just throw a plain RuntimeException everywhere?
 *   A custom exception TYPE lets calling code (or a log/report) instantly
 *   distinguish "this failed because of a genuine FRAMEWORK problem" (bad
 *   config, missing element after every retry, a broken utility) from a
 *   generic NullPointerException or IllegalStateException that could mean
 *   almost anything. It also lets you catch SPECIFICALLY this type
 *   somewhere if you ever need special handling for framework-level
 *   failures vs application-level assertion failures.
 *
 * Extends RuntimeException (unchecked), not Exception (checked):
 *   Checked exceptions force every calling method up the chain to either
 *   catch or re-declare `throws`, which gets extremely noisy across a
 *   large test framework where most failures should simply propagate up
 *   and fail the test/scenario — not be recovered from mid-test.
 * ============================================================================
 */
public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
