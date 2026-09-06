# 🛠️ Maven Command Reference — Quick Revision

> Every command we've used across this framework build, with WHY, not just
> WHAT. This file grows module-by-module alongside REVISION-NOTES.md.
> Bookmark this — it's the cheat-sheet you'll actually reach for daily.

---

## Basic Build Lifecycle

```bash
mvn clean
```
Deletes the `target/` folder (all previous build output). Always safe to
run — everything in `target/` is regenerable.

```bash
mvn compile
```
Compiles ONLY `src/main/java` sources. Fails fast if main-side code
(DriverManager, ConfigManager, Page Objects, etc.) has errors — good
sanity check before even touching test code.

```bash
mvn test-compile
```
Compiles `src/main/java` AND `src/test/java` (step definitions, hooks,
runners). Catches compile errors in test code without actually EXECUTING
any tests yet.

```bash
mvn clean install
```
Full lifecycle: clean → compile → test → package → install into your
local `.m2` repository. Use when you want a completely fresh, verified
build — this is closest to what a Jenkins "Build" stage typically runs.

---

## Dependency Inspection (Module 2)

```bash
mvn dependency:tree
```
Prints the FULL dependency tree, including every transitive dependency
pulled in by Selenium/Cucumber/TestNG/etc. Use this to debug version
conflicts (Module 2's "Dependency Mediation" concept) — if two libraries
pull different versions of the same transitive dependency, this tree
shows you exactly which version Maven picked and why.

```bash
mvn dependency:tree -Dverbose
```
Same as above but ALSO shows the versions that were REJECTED during
conflict resolution (not just the winner) — more detail when actively
debugging a version clash.

---

## Running Tests (Module 3, 7)

```bash
mvn test
```
The command that actually EXECUTES tests via the Surefire plugin. With
our Module 7 pom.xml wiring, this reads `testng.xml` (our default suite)
and uses the `qa` Maven profile by default (all `env` values default to
`qa` unless overridden — see below).

```bash
mvn test -Denv=staging
```
Overrides which environment's config file Owner loads (Module 3) —
switches `EnvironmentConfig` to read `config-staging.properties` instead
of the default `config-qa.properties`. Command-line `-D` flags ALWAYS win
over Maven profile values (Module 7 Q3 — a common real-world gotcha).

```bash
mvn test -Pstaging
```
Activates the `staging` Maven PROFILE (Module 7) instead of passing a raw
`-D` flag — functionally similar outcome, cleaner to reference in a
Jenkins job config UI or a README.

```bash
mvn test -DsuiteXmlFile=testng-smoke.xml -Dcucumber.filter.tags="@smoke"
```
Runs ONLY the smoke suite (Module 7): points Surefire at the
`testng-smoke.xml` suite file (thread count tuned for a fast pipeline
stage) AND filters Cucumber scenarios down to only those tagged `@smoke`.
This exact command is what Jenkins's "Smoke Test" pipeline stage will run
(Module 10).

```bash
mvn test -DsuiteXmlFile=testng-regression.xml -Dcucumber.filter.tags="@regression"
```
Same idea, but the full regression suite (higher thread count, meant for
scheduled nightly runs where wall-clock time matters more than resource
frugality).

```bash
mvn test -Dcucumber.filter.tags="@smoke or @regression"
```
Cucumber tag EXPRESSION syntax — runs scenarios matching EITHER tag.
Other useful expressions: `"not @wip"` (exclude work-in-progress
scenarios), `"@smoke and @login"` (must have BOTH tags).

---

## Validation / Debugging

```bash
mvn validate
```
Checks the project structure and `pom.xml` are correct WITHOUT compiling
anything — fastest possible sanity check, good first step when something
feels broken.

```bash
mvn help:effective-pom
```
Prints the FULLY RESOLVED pom.xml — including anything inherited from
parent poms or injected by active profiles. Useful when you're not sure
WHICH profile's values are actually winning in a confusing multi-profile
setup.

<!-- Module 8 commands will be appended below -->

## Module 8 — Reports & Logs

```bash
mvn test
```
After Module 8, this now ALSO generates:
- `target/extent-reports/ExecutionReport.html` — open this in a browser
  after any run for a polished pass/fail dashboard with embedded
  screenshots on failure.
- `target/logs/automation.log` — full Log4j2 log of the run, useful when
  the console scrolled by too fast to read live.
- `target/cucumber-reports/cucumber.html` — Cucumber's own native report
  (from Module 6's `@CucumberOptions` plugin config), also has
  screenshots attached on failure via `scenario.attach()`.

```bash
mvn test -Dlog4j2.level=DEBUG
```
NOTE: this specific flag doesn't work out of the box with our current
`log4j2.xml` (it's not wired to read a system property) — the actual way
to get verbose debug logging is to temporarily edit `log4j2.xml`'s
`<Root level="INFO">` to `<Root level="DEBUG">`. Mentioned here as a
reminder: config-file-driven logging means the change happens in the XML
file, not via a magic command-line flag, unless you explicitly wire one.

```bash
mvn clean test
```
Good habit before checking report output — `clean` wipes the OLD
`target/extent-reports/` and `target/cucumber-reports/` first, so you're
never accidentally looking at a stale report from a previous run while
debugging why "the report didn't update."


## Module 9 — Jira Integration
> NOTE: Jira integration was removed from the framework after being built
> (see REVISION-NOTES.md). These commands are kept for reference in case
> it's rebuilt later — they won't do anything meaningful against the
> current codebase since `jira.*` config keys no longer exist.


```bash
mvn test -Djira.api.token=YOUR_REAL_TOKEN_HERE -Denv=staging
```
Runs against staging config (which has `jira.integration.enabled=true`)
while supplying the secret token ONLY as a runtime flag — never typed into
any committed `.properties` file. This is exactly how you'd test the Jira
integration locally before trusting Jenkins to inject it via Credentials
Store (Module 10).

```bash
mvn test -Djira.integration.enabled=false -Denv=staging
```
Temporarily force-disables Jira ticket creation even on an environment
where it's normally on — useful when you're debugging OTHER failures on
staging and don't want to spam the Jira project with tickets for known,
already-being-fixed issues. Remember: `system:properties` (Module 3) always
wins over the properties file value.

<!-- Module 10 commands will be appended below -->

## Module 10 — Jenkins-Equivalent Local Commands

These are the exact commands the Jenkinsfile runs on Jenkins's build
agent — useful to run LOCALLY first to debug a pipeline failure without
waiting on a full Jenkins build cycle.

```bash
mvn clean compile
```
Jenkins's "Build" stage — fails fast on broken src/main/java code.

```bash
mvn test-compile
```
Jenkins's "Test Compile" stage — fails fast on broken src/test/java code
(step definitions, hooks, runners) separately from main-side issues.

```bash
mvn test -Denv=qa -DsuiteXmlFile=testng-smoke.xml -Dbrowser=chrome -Dcucumber.filter.tags="@smoke"
```
Exactly what Jenkins's "Execute Tests" stage runs when you trigger
"Build with Parameters" with ENVIRONMENT=qa, SUITE=smoke, BROWSER=chrome.
Run this locally FIRST when a Jenkins build fails and you're not sure if
it's a genuine test failure or a CI-environment-specific issue (e.g.
missing browser on the agent).

```bash
mvn test -Denv=staging -DsuiteXmlFile=testng-regression.xml -Dbrowser=chrome -Dcucumber.filter.tags="@regression"
```
The nightly full-regression equivalent.
