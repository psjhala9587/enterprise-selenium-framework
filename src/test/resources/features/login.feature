# ============================================================================
# REVISION NOTES — MODULE 6: Gherkin / Feature Files
# ============================================================================
# WHY THIS FILE HAS NO JAVA IN IT AT ALL:
#   A Business Analyst or Product Owner with ZERO coding knowledge should
#   be able to read this file and understand exactly what's being tested.
#   This is the whole point of BDD (Behavior Driven Development) — the
#   feature file is a shared, readable CONTRACT between business and
#   engineering about expected behavior.
#
# Given / When / Then:
#   Given -> sets up the initial context/state
#   When  -> the action/event being tested
#   Then  -> the expected outcome/assertion
#   And/But -> continuation of the previous step type (readability only,
#              Cucumber treats them identically to the step type above them)
#
# @smoke / @regression TAGS:
#   Used later (Module 7/10) to selectively run subsets of scenarios —
#   e.g. Jenkins can run ONLY @smoke tests on every commit (fast feedback),
#   and the FULL @regression suite only on a nightly schedule.
# ============================================================================

Feature: User Login
  As a registered user
  I want to log into the application
  So that I can access my account dashboard

  @smoke @login
  Scenario: Successful login with valid credentials
    Given the user is on the login page
    When the user logs in with username "standard_user" and password "secret_sauce"
    Then the user should be redirected to the dashboard page

  @regression @login
  Scenario Outline: Unsuccessful login with invalid credentials
    Given the user is on the login page
    When the user logs in with username "<username>" and password "<password>"
    Then an error message "<expectedError>" should be displayed

    Examples:
      | username      | password      | expectedError                          |
      | locked_user   | secret_sauce  | Sorry, this user has been locked out.  |
      | standard_user | wrong_pass    | Username and password do not match.    |
