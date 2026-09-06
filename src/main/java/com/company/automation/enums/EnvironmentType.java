package com.company.automation.enums;

/**
 * ============================================================================
 * REVISION NOTES — MODULE 3: Why an enum for environments?
 * ============================================================================
 * Instead of passing around raw Strings like "qa", "staging" (typo-prone,
 * no compile-time safety), we use a type-safe enum. Any place in the
 * framework that needs to branch on environment (rare, since Owner config
 * usually handles this transparently) references THIS enum instead of
 * magic strings.
 *
 * These names also map 1:1 to:
 *   - config-qa.properties / config-staging.properties / config-prod.properties
 *   - the Jenkins pipeline's "ENV" choice parameter (Module 10)
 * ============================================================================
 */
public enum EnvironmentType {
    QA,
    STAGING,
    PROD
    // TODO: add more environments here if the org needs them (e.g. DEV, UAT)
}
