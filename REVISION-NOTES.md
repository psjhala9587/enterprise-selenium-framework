# 📘 Enterprise Automation Framework — My Revision Notes

> This file grows module by module. Read this before any interview —
> it's your own framework, explained by you, to future-you.

---

## Module 0 — Mental Model
- Layered architecture: **Feature file → Step Definitions → Page Objects →
  Utils/Base classes → Driver Manager → Config Layer**
- Design principle behind this: **Separation of Concerns**
  - UI change? -> fix ONE Page Object.
  - Environment change? -> fix ONE config file.
  - Browser handling change? -> fix DriverManager only.

## Module 1 — Maven Basics & Project Skeleton
- **Why Maven over plain JDK?**
  1. Automated dependency management (pulls jars from Maven Central)
  2. Standard directory layout recognized industry-wide
  3. Standard build lifecycle: `validate -> compile -> test -> package
     -> verify -> install -> deploy` — Jenkins triggers the SAME phases,
     so local and CI behavior stay identical.
- **GAV Coordinates** = groupId + artifactId + version -> uniquely identifies
  a project (or any dependency) in the Maven ecosystem.
- **Folder structure created:**
  ```
  src/main/java/com/company/automation/
      base/            -> DriverManager, BasePage, BaseTest (Module 5)
      config/          -> Aeonbits Owner config interfaces (Module 4)
      constants/       -> hardcoded values centralized (Module 8)
      enums/           -> BrowserType, EnvironmentType (Module 5)
      factory/         -> DriverFactory, PageObjectManager (Module 5/6)
      listeners/       -> TestNG listeners, retry logic (Module 8)
      pages/           -> Page Object classes (Module 6)
      utils/           -> reusable technical helpers (Module 8)
      exceptions/      -> custom exception classes (Module 8)
      integrations/jira/     -> Jira REST API client (Module 9)
      integrations/jenkins/  -> Jenkins helper utils (Module 10)

  src/test/java/com/company/automation/
      runners/            -> TestNG + @CucumberOptions runner (Module 7)
      stepdefinitions/    -> Cucumber glue code (Module 7)
      hooks/              -> @Before/@After hooks (Module 7)
      tests/              -> optional non-BDD sanity tests

  src/test/resources/
      features/    -> .feature files (Gherkin) (Module 7)
      config/      -> environment .properties files (Module 4)
      testdata/    -> JSON/Excel data-driven files (Module 8)
  ```
- **`.gitignore` rule of thumb**: never commit generated build artifacts
  (`target/`), IDE files (`.idea/`), or secrets. This matters later in
  Module 9/10 when we handle Jira tokens and Jenkins credentials.

---

## Module 2 — Dependency Management Deep-Dive
- **`<scope>test</scope>`** — dependency used only for compiling/running
  tests, NOT bundled into a production artifact. Since our whole project
  IS a test framework, almost everything is test-scoped.
- **Transitive Dependencies** — declaring `selenium-java` auto-pulls
  everything IT depends on. Run `mvn dependency:tree` to visualize this.
- **Dependency Mediation (interview favorite)** — when two libraries pull
  different versions of the SAME transitive dependency, Maven resolves it
  via "nearest wins" (declaration closest to your own pom wins; if tied on
  depth, first-declared wins).
- **`<dependencyManagement>` vs `<dependencies>`** — in multi-module
  enterprise repos, a PARENT pom declares versions in
  `<dependencyManagement>` WITHOUT adding the dependency itself. Child
  modules then declare the dependency WITHOUT a version tag and inherit it
  — guarantees every module uses the same version. (We're single-module
  for now, but MUST be able to explain this in interviews.)
- **Why WebDriverManager if Selenium 4.6+ has built-in Selenium Manager?**
  Enterprise CI containers often need finer control — caching driver
  binaries, proxy support, pinning exact driver versions — which
  WebDriverManager gives more explicitly.
- **Why does Log4j2 need TWO artifacts (api + core)?** `log4j-api` = the
  interfaces your code codes against; `log4j-core` = the actual
  implementation loaded at runtime. Separation lets you swap logging
  backends without changing application code (a classic example of
  "coding to an interface, not an implementation").
- **Reused dependency insight**: REST Assured isn't just for API testing
  modules — we'll reuse it for our `JiraApiClient` in Module 9. Avoids
  pulling in a redundant HTTP library.
- **Build plugins added**: `maven-compiler-plugin` (enforces Java version
  at compile time) and `maven-surefire-plugin` (the plugin that ACTUALLY
  runs tests via `mvn test` — this is the exact command Jenkins will call
  in Module 10).

<!-- Module 3 notes will be appended here -->

## Module 3 — Environment Configuration with Aeonbits Owner
- **Problem with plain `java.util.Properties`**: no compile-time safety
  (typos in key names fail silently at runtime), no automatic type
  conversion (everything is a String, manual parsing everywhere).
- **Owner's approach**: define a Java **interface**, annotate methods —
  Owner generates the implementation via a **Dynamic Proxy** at runtime.
  You never hand-write the implementation.
- **Key annotations**:
  - `@Config.Sources({...})` — declares WHERE to load properties from, in
    priority order. `"system:properties"` checks JVM `-D` flags FIRST.
  - `${env}` placeholder inside the classpath path — Owner substitutes
    this with the `env` system property at runtime. This is the exact
    mechanism Jenkins uses later: `-Denv=qa` → loads `config-qa.properties`
    automatically. No if/else branching in Java code needed.
  - `@Key("...")` — maps a Java method to a specific properties file key.
  - `@DefaultValue("...")` — fail-safe fallback if a key is missing,
    instead of a NullPointerException surfacing deep inside a test.
- **One file per environment** (`config-qa.properties`,
  `config-staging.properties`, `config-prod.properties`) — a non-technical
  teammate can change a URL/timeout without touching Java code.
- **`ConfigManager`** — a lightweight Singleton-style wrapper
  (`ConfigFactory.create(...)` called exactly ONCE via a static final
  field) so the whole framework shares one config instance instead of
  rebuilding it repeatedly. This previews the full Singleton pattern
  we'll build properly (with `ThreadLocal`) in Module 5 for `DriverManager`.
- **Usage anywhere**: `ConfigManager.getConfig().baseUrl()`

<!-- Module 4 notes will be appended here -->

## Module 4 — Singleton Pattern + ThreadLocal (DriverManager) + Factory Pattern
- **The parallel execution problem**: a single shared `static WebDriver`
  field gets overwritten by whichever thread runs last — other threads end
  up silently controlling the WRONG browser. Causes flaky, hard-to-reproduce
  failures.
- **`ThreadLocal<WebDriver>` fix**: each Thread has its own private storage;
  ThreadLocal is just the KEY to look up THAT thread's own value. Same
  static field, same method call, fully isolated results per thread.
- **Precise term for this pattern**: "Singleton PER THREAD" — exactly ONE
  driver instance per thread's lifetime, but the JVM can have many such
  singletons (one per active thread). This nuance is a common interview
  trip-up — know it cold.
- **`unload()` / `ThreadLocal.remove()` is CRITICAL**: TestNG/Surefire
  parallel execution uses a **thread pool** — threads get REUSED across
  test methods. Forgetting `.remove()` after `.quit()` can leak a stale
  driver reference into the next test reusing that thread. Real production
  bug — always flag in code review.
- **Factory Pattern (`DriverFactory`)**: centralizes "how do I build a
  WebDriver" into ONE place instead of duplicating if/else browser logic
  in every test class. Textbook definition: encapsulate object creation
  logic behind a single method/class.
- **`WebDriverManager.chromedriver().setup()`**: auto-downloads the native
  driver binary matching the installed browser version — avoids manual
  downloads and version-mismatch errors on CI machines.
- **`headless=true` and Jenkins**: Jenkins build agents are typically
  headless Linux servers with no display attached — a normal browser
  window CANNOT open there. That's why prod/staging config defaults
  `headless=true` while local qa config uses `headless=false` (so you can
  visually watch the browser while developing tests).
- **Separation of concerns reinforced**: `DriverFactory` only knows HOW to
  build a driver; it does NOT call `DriverManager.setDriver()` itself.
  `BaseTest` (Module 5) is the orchestrator that wires factory output into
  DriverManager at the right lifecycle moment.

<!-- Module 5 notes will be appended here -->

## Module 5 — BaseTest, BasePage, Page Factory + a real bug we caught
- **Selenium Manager (inbuilt, Selenium 4.6+)** vs Bonigarcia
  WebDriverManager: inbuilt Selenium Manager needs ZERO setup code —
  `new ChromeDriver()` alone triggers it to detect your browser version
  and download/wire the matching driver binary. We switched DriverFactory
  to use this, removing the external `webdrivermanager` dependency (kept
  commented in pom.xml for reference — enterprises sometimes still prefer
  Bonigarcia's version for finer proxy/caching control on locked-down CI).
- **REAL BUG CAUGHT: Maven scope mismatch.** `scope=test` dependencies are
  ONLY visible on the TEST classpath — `src/main/java` cannot compile
  against them. Since `DriverManager`/`DriverFactory`/`BasePage` (importing
  `org.openqa.selenium.*`) live in `src/main/java` as reusable framework
  classes, `selenium-java` (and `rest-assured`, for the same reason ahead
  of Module 9's Jira client) had to be changed from `scope=test` to
  default/compile scope. **Rule of thumb: any dependency imported by a
  class under `src/main/java` must be default (compile) scope — only
  TestNG/Cucumber, used exclusively under `src/test/java`, stay
  `scope=test`.**
- **`BaseTest` (TestNG, lives in `src/test/java`)** — the orchestrator:
  - `@BeforeMethod`/`@AfterMethod` (not `@BeforeClass`/`@AfterClass`) —
    guarantees total test isolation (fresh browser per test method,
    no leftover cookies/session bleeding between tests). If raw speed
    matters more than isolation, the correct lever is PARALLEL execution
    (Module 7/8), not reusing browser state.
  - Calls `DriverFactory.createDriver()` then `DriverManager.setDriver()`
    — BaseTest doesn't know HOW the driver is built, just WHEN.
  - Applies Module 3's config-driven timeouts centrally, so every test
    gets consistent wait behavior automatically.
  - `tearDown()` always calls `driver.quit()` THEN `DriverManager.unload()`
    — order matters (quit first, then clear the ThreadLocal reference).
- **`BasePage`** — every Page Object extends this. Fetches the CURRENT
  thread's driver via `DriverManager.getDriver()` (no manual wiring per
  page) and calls `PageFactory.initElements(driver, this)`.
- **Page Factory (`@FindBy` + `PageFactory.initElements`)** — gives LAZY
  element lookup: elements aren't located immediately at object
  construction, only when actually interacted with. Interview Q: "Why
  Page Factory over plain `driver.findElement()`?" → lazy init + cleaner
  declarative locators + centralized element declarations.
- **Page Object Model discipline**: Page Objects hold locators + actions
  for ONE page; Step Definitions (Module 7) never contain raw
  `driver.findElement()` calls — they call meaningful workflow methods
  like `loginPage.loginAs(user, pass)` instead.

<!-- Module 6 notes will be appended here -->

## Module 6 — Cucumber BDD Wiring: Feature Files, Step Defs, PicoContainer DI
- **Why not static fields to share the driver across Step Def classes?**
  Statics are shared globally across the JVM — breaks thread-safety in
  parallel runs, same class of problem as Module 4's shared-WebDriver bug.
- **`cucumber-picocontainer` = Dependency Injection for step definitions.**
  If two Step Definition classes both declare a constructor accepting the
  same class (e.g. `Hooks`), PicoContainer automatically injects the SAME
  shared instance into both — scoped to ONE scenario, thread-safe for
  parallel execution, with zero manual wiring.
- **`Hooks.java` (Cucumber's own `@Before`/`@After`, NOT TestNG's)** — runs
  once per SCENARIO. This is the REAL orchestrator for our BDD flow (Module
  5's `BaseTest` only matters for non-BDD plain sanity tests now). Hooks
  exposes `getPageObjectManager()` — the method PicoContainer-injected
  Step Def classes call to get shared access to the same driver session.
- **`Scenario` parameter in `@After`** — Cucumber auto-injects the current
  scenario; `scenario.isFailed()` is exactly where screenshot-on-failure +
  Jira auto-ticket-creation will plug in (Module 8/9).
- **`PageObjectManager`** — a simple `Map<Class, Object>` cache so
  `getLoginPage()` doesn't `new LoginPage()` every single call within one
  scenario — first call creates & caches, later calls reuse. Thread-safe
  because a FRESH `PageObjectManager` instance is created per scenario by
  Hooks, not shared globally. Three layers of thread isolation stack
  correctly: `DriverManager` → `PageObjectManager` → Step Definitions.
- **Feature files (Gherkin)** — Given/When/Then/And/But — written so a
  non-technical Product Owner can read and understand test intent. Tags
  (`@smoke`, `@regression`) enable selective execution later via Jenkins
  parameters (Module 10), without hardcoding which scenarios run.
- **Step Definitions never call `driver.findElement()` directly** — they
  only call Page Object workflow methods (`loginPage.loginAs(...)`),
  keeping this layer thin and readable.
- **`TestRunner extends AbstractTestNGCucumberTests`** (not
  `@RunWith(Cucumber.class)`, which is the JUnit-flavored approach) — since
  our framework standardized on TestNG for parallel execution and listener
  support. `@CucumberOptions` wires `features`, `glue` (must list EVERY
  package with step defs AND hooks), `plugin` (report output), and later
  `tags` (dynamically overridden by Jenkins, not hardcoded here).
- **Parallel execution hook previewed**: overriding `scenarios()` with
  `@DataProvider(parallel = true)` is what lets TestNG run multiple
  Cucumber scenarios concurrently — left commented until Module 7/8 wires
  up `testng.xml`'s thread count setting.

<!-- Module 7 notes will be appended here -->

## Module 7 — testng.xml, Parallel Execution, Tag-Based Suite Strategy
- **`@DataProvider(parallel = true)` override on `TestRunner.scenarios()`**
  — `AbstractTestNGCucumberTests` exposes every Cucumber Scenario as a
  DataProvider row; marking it `parallel = true` lets TestNG run multiple
  rows (scenarios) CONCURRENTLY on separate threads.
- **Actual thread count lives in `testng.xml`** (`data-provider-thread-
  count="N"`), NOT in Java code — keeps execution tuning changeable
  without recompiling, same design philosophy as Module 3's environment
  config files.
- **Why this is safe only because of earlier modules**: every layer
  (`DriverManager`'s ThreadLocal — Module 4, a fresh `PageObjectManager`
  per scenario via `Hooks` — Module 6) was built thread-safe from day
  one. Flipping `parallel = true` on a framework using static WebDriver
  fields would immediately cause cross-thread browser corruption —
  thread-safety must be designed in from the start, not bolted on later.
- **Tag-based suite strategy**: `testng.xml` (default/combined, thread
  count 3), `testng-smoke.xml` (thread count 2, paired with
  `-Dcucumber.filter.tags="@smoke"`, meant for fast per-commit pipeline
  stages), `testng-regression.xml` (thread count 6, paired with
  `-Dcucumber.filter.tags="@regression"`, meant for scheduled nightly runs
  where wall-clock time matters more than resource frugality).
- **IMPORTANT NUANCE**: TestNG XML files do NOT filter Cucumber tags
  themselves — Cucumber's own runtime reads the `cucumber.filter.tags`
  SYSTEM PROPERTY at execution time, with priority over anything
  hardcoded in `@CucumberOptions(tags = ...)`. The XML files only control
  thread count / which Runner class runs; the tag filter is a separate
  `-D` flag passed alongside it.
- **`${suiteXmlFile}` Maven property + Surefire `<suiteXmlFiles>`
  config**: `mvn test` now actually reads our TestNG suite instead of
  Surefire's default `*Test.java` class-scanning behavior. Jenkins
  overrides this per pipeline stage:
  `mvn test -DsuiteXmlFile=testng-smoke.xml -Dcucumber.filter.tags="@smoke"`
- **Maven Profiles (`qa`/`staging`/`prod`)** — a convenience/readability
  layer over raw `-Denv=xxx` flags (`mvn test -Pstaging` reads cleaner in
  a Jenkins job config UI). Command-line `-Denv=xxx` ALWAYS wins over a
  profile's `<env>` value if both are somehow supplied (Maven property
  precedence). Surefire's `<systemPropertyVariables><env>${env}</env></...>`
  is what actually forwards this into the forked test JVM so Owner's
  `${env}` placeholder (Module 3) sees it.
- **Interview-ready answer for "how did you pick your thread count?"**:
  never say "arbitrary guess" — the grounded answer is "load-tested the
  CI agent and picked the highest count before flakiness/resource
  contention appeared."

<!-- Module 8 notes will be appended here -->

## Module 8 — Utilities, Constants, Log4j2, ExtentReports, Retry Analyzer
- **`FrameworkConstants`** — centralizes hardcoded values (retry count,
  screenshot/report paths) so a single change updates everywhere.
- **Log4j2 (`log4j2.xml` in `src/main/resources`)** — Console appender for
  live IDE viewing + RollingFile appender (`target/logs/`) that Jenkins
  archives as a build artifact for post-mortem debugging. Config lives in
  XML, not code — log LEVEL/format changes need zero recompilation.
- **`WaitUtils` — implicit vs explicit wait**: implicit wait (set globally
  in `BaseTest`/`Hooks`) is a blunt "wait up to N seconds for ANY
  `findElement()`"; explicit wait (`WebDriverWait` + `ExpectedConditions`)
  waits for a SPECIFIC condition (clickability, visibility) on a specific
  element. Selenium's docs officially warn against mixing both on one
  driver instance (can cause unpredictable combined wait times) — most
  enterprise frameworks still do it pragmatically, but know this
  trade-off; some senior engineers set implicit wait to 0 and rely purely
  on explicit waits for full predictability.
- **`ElementUtils`** — wraps click/type/getText with automatic waiting +
  logging, so every Page Object gets consistent behavior without
  repeating wait/log code. `LoginPage` (Module 5) now uses this instead of
  raw `WebElement` calls.
- **REAL BUG CAUGHT AGAIN**: `RetryAnalyzer` (uses `org.testng.*`,
  scope=test) was initially placed under `src/main/java` — same class of
  Maven scope mistake as Module 5. Moved `RetryAnalyzer`,
  `AnnotationTransformer`, and `ExtentReportListener` to `src/test/java`
  instead. **Pattern to remember**: any class using a `scope=test`
  dependency (TestNG, Cucumber) MUST live under `src/test/java` —
  no exceptions, regardless of what the "ideal" folder structure diagram
  suggested back in Module 1.
- **`RetryAnalyzer` (`IRetryAnalyzer`)** — retries a test up to
  `FrameworkConstants.RETRY_COUNT` times. Retry exists to absorb
  ENVIRONMENTAL flakiness (slow network, a stray overlay), never to mask
  a genuinely broken feature/test — know this distinction cold for
  interviews.
- **`AnnotationTransformer` (`IAnnotationTransformer`)** — auto-wires
  `RetryAnalyzer` onto EVERY test at runtime via `transform()`, since
  Cucumber+TestNG's internally-generated `@Test` methods (from
  `AbstractTestNGCucumberTests`) can't be manually annotated one-by-one.
- **`ExtentReportListener` (`ITestListener`)** — drives the HTML report:
  `onTestFailure` auto-captures a screenshot via `ScreenshotUtils` and
  attaches it to the report. ExtentReports vs Allure trade-off: Extent =
  single self-contained HTML file, simpler; Allure = richer
  step-by-step/historical trend analytics but needs a separate CLI report
  generation step.
- **`ExtentTest` wrapped in `ThreadLocal`** — same exact pattern as
  Module 4's `DriverManager` — a plain `ExtentTest` isn't safe to share
  across parallel threads. Recognizing this PATTERN REUSE (same shared-
  mutable-state problem, same ThreadLocal fix, applied a second time) is
  a strong signal in interviews.
- **`ScreenshotUtils`** — `captureAsBytes()` for embedding directly into
  reports (ExtentReports AND `scenario.attach()` for Cucumber's own HTML
  report — Hooks now does BOTH, intentional redundancy since either
  report might be what a teammate opens first); `captureAsFile()` saves a
  standalone `.png` for Jenkins to archive as a raw build artifact,
  independent of any report tool. Used plain `java.nio.file.Files` instead
  of pulling in Apache Commons IO — a whole extra dependency for one file
  copy the JDK already does natively.
- **`FrameworkException`** — custom unchecked (`RuntimeException`-based)
  exception type, so logs/callers can distinguish genuine framework
  failures from generic NPEs. Unchecked (not `Exception`) so failures
  propagate cleanly up to fail the test, without forcing every method in
  the call chain to `throws`-declare it.
- **`testng.xml` `<listeners>` block** — this is HOW TestNG discovers
  `AnnotationTransformer`/`ExtentReportListener` at all; added to all
  three suite files (default, smoke, regression).

<!-- Module 9 notes will be appended here -->

## Module 9 — Jira Integration: Auto Bug-Ticket Creation on Failure
> **STATUS UPDATE**: this was fully built (config, `JiraApiClient`, `Hooks`
> wiring), then DELIBERATELY REMOVED from the codebase pending a future
> revisit — no `JiraApiClient.java`, no `jira.*` config keys currently
> exist in the framework. Notes below are kept intact as an interview
> reference / rebuild guide, not a description of current code.

- **The duplicate-ticket landmine**: one Cucumber scenario failure triggers
  BOTH `Hooks.tearDown()` (Cucumber's own lifecycle) AND TestNG's
  `ExtentReportListener.onTestFailure()` (since `AbstractTestNGCucumberTests`
  wraps each scenario as an internal `@Test`). Calling Jira ticket creation
  from both would file TWO duplicate tickets per failure. **Decision: `Hooks`
  is the single authoritative call site** (richer context — Scenario name/
  tags — than `ITestResult` alone); `ExtentReportListener` stays scoped
  purely to reporting.
- **`jiraIntegrationEnabled()` defaults to `false`** — a cloned framework
  must never accidentally start filing real tickets. Enabled per-environment
  in properties files (e.g. staging=true where nightly regression runs,
  prod=false, qa=false for local dev) — a deliberate business decision per
  environment, not a blanket default.
- **Secrets handling — `jira.api.token` is NEVER in a committed properties
  file.** Owner's `@Config.Sources` lists `"system:properties"` BEFORE the
  classpath properties file (Module 3) — so a real token passed via
  `-Djira.api.token=xxxx` (local) or injected by Jenkins Credentials Store
  (Module 10) is found FIRST, and the committed file never needs to hold a
  real secret. This is the same mechanism from Module 3, now put to its
  most important use.
- **Basic Auth with email + API TOKEN, not a real password** — Jira Cloud's
  documented approach. API tokens are individually revocable and
  scoped/logged separately from interactive logins — "Basic Auth" here
  does NOT mean insecure, it means the credential itself is a revocable
  token rather than an account password.
- **Reusing REST Assured (Module 2 dependency)** for a completely different
  purpose (hitting Jira's REST API, not the AUT) — one HTTP client library,
  two uses. Flagged explicitly back in Module 2 as a forward-looking
  decision.
- **`Map<String, Object>` payload instead of a dedicated POJO** — REST
  Assured (backed by Jackson) auto-serializes a Map to JSON; for a small,
  rarely-changing payload shape, a Map avoids an extra class. A POJO
  becomes worth it once a payload has many optional/nested fields or gets
  reused across multiple methods — know both approaches.
- **`X-Atlassian-Token: no-check` header** — REQUIRED on attachment
  uploads as CSRF-style protection; Jira rejects the request without it
  regardless of valid auth. Classic "worked in Postman, failed from code"
  gotcha until you know this exists.
- **`JiraApiClient.createBugTicket()` throws on failure; `Hooks` wraps the
  call in try/catch** — deliberate two-layer design: the client throws
  loudly because a broken Jira integration deserves visibility, but the
  CALLER (Hooks) catches it so a Jira outage/expired token never crashes
  the test run or masks the ORIGINAL test failure that triggered the call.

<!-- Module 10 notes will be appended here -->

## Module 10 — Jenkins Installation + Declarative Pipeline
- **Declarative vs Scripted Pipeline**: Declarative (`pipeline { agent {}
  stages {} post {} }`) is a structured, opinionated, easier-to-read/lint
  format — the enterprise default. Scripted is raw Groovy, more flexible
  but easy to turn into unmaintainable spaghetti. Reserve `script {}`
  blocks inside Declarative only for logic the structure can't express
  cleanly (e.g. our `SUITE_XML`/`TAG_FILTER` derivation from one dropdown).
- **"Pipeline as Code"** — the `Jenkinsfile` is committed to Git alongside
  the framework it tests. A change to the CI process goes through the
  SAME PR/code-review process as any other code change — no
  tribal-knowledge Jenkins UI clicks that only one person remembers.
- **`tools {}` block names (`JDK17`, `Maven3`) must EXACTLY match** the
  names configured in Jenkins's Global Tools config (`JENKINS-SETUP.md`
  Step 5) — a typo here fails the pipeline at the very first stage with a
  confusing "tool not found" error.
- **`parameters {}` block** — once committed and run once, Jenkins
  auto-generates a "Build with Parameters" UI with zero manual
  configuration. This connects DIRECTLY to earlier modules: `ENVIRONMENT`
  → Module 3's `-Denv`, `SUITE` → Module 7's `testng-*.xml` +
  `cucumber.filter.tags` combo, `BROWSER` → Module 4's `DriverFactory`.
- **`Execute Tests` stage is NOT a new execution mechanism** — it runs the
  exact same `mvn test -Denv=... -DsuiteXmlFile=... -Dcucumber.filter.tags=...`
  command a developer would run locally (Module 7), just parameter-sourced
  from a dropdown instead of typed by hand. This is the payoff of all the
  earlier config-driven design work — Jenkins didn't require inventing
  anything new, only calling what already worked locally.
- **`post {}` block runs regardless of stage success/failure** — the right
  place for reporting/notification/cleanup, since a report should publish
  and a team should be notified whether the run passed OR failed
  (arguably more important on failure).
- **`publishHTML` (HTML Publisher plugin)** — publishes Module 8's
  ExtentReports AND Module 6's Cucumber HTML report as clickable tabs on
  the Jenkins build page.
- **`testNG()` step (TestNG Results Plugin)** — feeds Jenkins's native
  pass/fail trend graphs across builds over time from TestNG's own XML
  results.
- **`archiveArtifacts`** — archives Module 8's Log4j2 log file and raw
  screenshots as downloadable build artifacts, independent of whichever
  HTML report tool someone opens first.
- **Secrets in Jenkins**: the `failure` block's notification TODO
  reiterates Module 9's secrets-handling principle — any webhook
  URL/credential belongs in Jenkins's Credentials Binding plugin, never
  hardcoded in the committed `Jenkinsfile`, same reasoning as keeping
  `jira.api.token` out of committed properties files.

<!-- Course complete — see the closing summary for what's next -->

