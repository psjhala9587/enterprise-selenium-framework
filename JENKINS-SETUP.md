# 🔧 Jenkins Setup Guide — Local Installation & Configuration

> Follow this top-to-bottom, in order. Each step unlocks the next.

---

## Step 1 — Prerequisites Check

Jenkins itself needs a JDK to run (separate from the JDK your Maven
project compiles against, though they can be the same installation).

```bash
java -version
```
You already have JDK 17 from Module 1 — that satisfies this requirement.

---

## Step 2 — Install Jenkins

### Windows
1. Download the Windows installer (`.msi`) from **jenkins.io/download**.
2. Run it — it installs Jenkins as a **Windows Service** (auto-starts on
   boot) and opens `http://localhost:8080` automatically once ready.

### Mac (via Homebrew)
```bash
brew install jenkins-lts
brew services start jenkins-lts
```

### Linux (Debian/Ubuntu)
```bash
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update
sudo apt-get install jenkins
sudo systemctl start jenkins
```

### Universal alternative (any OS, if you'd rather skip installers): the WAR file
```bash
java -jar jenkins.war --httpPort=8080
```
Download `jenkins.war` from jenkins.io first. This runs Jenkins directly
without installing it as a system service — good for a disposable
training instance you'll tear down later.

---

## Step 3 — Initial Unlock & Plugin Setup

1. Open `http://localhost:8080` in a browser.
2. Jenkins asks for an **initial admin password** — find it via:
    - Windows: `C:\Program Files\Jenkins\secrets\initialAdminPassword`
    - Mac/Linux: `/var/lib/jenkins/secrets/initialAdminPassword` (or
      check the terminal output if you ran the WAR file directly — it
      prints the password path there).
3. Choose **"Install suggested plugins"** — gets you Git, Pipeline,
   Credentials Binding, and other core plugins automatically.
4. Create your first admin user when prompted.

---

## Step 4 — Install ADDITIONAL Plugins (beyond the suggested set)

Go to **Manage Jenkins → Plugins → Available plugins**, search for and
install each of these (then restart Jenkins if prompted):

| Plugin | Why we need it |
|---|---|
| **Maven Integration** | Lets Jenkins understand Maven build steps/lifecycle natively |
| **TestNG Results Plugin** | Parses TestNG XML results into Jenkins's native test trend graphs |
| **HTML Publisher** | Publishes our ExtentReports HTML file (Module 8) as a clickable link on the build page |
| **Cucumber Reports** | Renders Cucumber's own JSON/HTML output (Module 6) with a nicer Jenkins-native UI |
| **Pipeline** | Usually already installed via "suggested plugins" — lets Jenkins read a `Jenkinsfile` |
| **Credentials Binding** | Securely injects secrets (API tokens, etc.) into pipeline stages without printing them in console logs |

---

## Step 5 — Configure Global Tools

Go to **Manage Jenkins → Tools**:
1. Under **JDK installations** — add one, name it exactly `JDK17`
   (matches what we'll reference in the `Jenkinsfile`), and either point
   it at your existing JDK install path or let Jenkins auto-install one.
2. Under **Maven installations** — add one, name it exactly `Maven3`,
   same idea (auto-install or point at your existing Maven).

> **Why exact names matter**: our `Jenkinsfile` (Module 10, next) references
> tools by these exact names in its `tools {}` block — a typo here means
> the pipeline fails at the very first stage with a confusing
> "tool not found" error.

---

## Step 6 — Create the Pipeline Job

1. Jenkins Dashboard → **New Item**.
2. Name it (e.g. `enterprise-selenium-framework`), select **Pipeline**,
   click OK.
3. Under **Pipeline** section at the bottom:
    - Definition: **Pipeline script from SCM**
    - SCM: **Git**
    - Repository URL: your Git repo's URL (push this project to GitHub/
      GitLab/Bitbucket first if you haven't)
    - Script Path: `Jenkinsfile` (default — matches the file we build next)
4. Save.

---

## Step 7 — Parameterize the Build (do this AFTER the Jenkinsfile exists)

Once the `Jenkinsfile` (Module 10) is committed and this job has run at
least once, Jenkins auto-detects the `parameters {}` block inside it and
exposes them as a **"Build with Parameters"** button — no manual UI
configuration needed for this. This is the payoff of using a
DECLARATIVE pipeline (parameters live in code, versioned in Git) instead
of manually clicking through Jenkins UI checkboxes that nobody remembers
to document.

<!-- Module 10 troubleshooting notes will be appended here if needed -->

## Common Gotchas (real errors hit during this training, and their fixes)

### `fatal: couldn't find remote ref refs/heads/master`
**Cause**: Jenkins job's "Branches to build" field defaults to `*/master`,
but modern Git hosting (GitHub/GitLab/Bitbucket) defaults new repos to a
branch named `main`, not `master`. Jenkins fails at the CHECKOUT step —
before it ever reads the `Jenkinsfile` — which is why it looks like
"nothing happened."

**Fix**: Job → Configure → Pipeline section → "Branches to build" →
change `*/master` to `*/main` (or whatever `git branch -a` shows locally
as your actual default branch — match it exactly, case-sensitive). Save,
rebuild.

**Lesson**: when a Jenkins build fails with ZERO stages executing (no
"Checkout"/"Build"/etc. shown at all in the pipeline view), the failure
is almost always in SCM configuration itself, not in the `Jenkinsfile` —
always check Console Output's very first lines before assuming the
pipeline logic is broken.

### `No steps specified for branch @ line N... cleanup { ... }`
**Cause**: a Groovy compilation error, not a config or logic error. EVERY
stage/block in a Declarative Pipeline (`stages`, and each condition
inside `post` — `always`, `failure`, `cleanup`, etc.) MUST contain at
least one real step. A block with ONLY a comment inside it (e.g.
`cleanup { // cleanWs() }`) is syntactically an EMPTY block as far as
Groovy's parser is concerned — comments don't count as content — and the
entire pipeline fails to even START, before any stage runs.

**Fix**: add at least one real step (even a harmless `echo "..."`) inside
any block you want to keep as a placeholder for later, instead of
commenting out its only line.

**Lesson**: this is a great one to recognize by its ERROR TYPE alone —
`org.codehaus.groovy.control.MultipleCompilationErrorsException` means
the Jenkinsfile itself won't even PARSE (a syntax problem), which is a
completely different class of bug from an SCM/checkout failure or an
actual test failure further down the pipeline. Learning to read WHICH
category an error falls into (parse error vs SCM error vs test failure)
dramatically speeds up debugging — you immediately know which part of
the system to look at.

### `Cannot run program "sh"` / `CreateProcess error=2, The system cannot find the file specified`
**Cause**: the `sh` step invokes a Unix/Linux shell (`/bin/sh`) — it does
NOT exist on a Windows Jenkins agent. Our original `Jenkinsfile` hardcoded
`sh` for every Maven command, which works fine on a Linux Jenkins agent
but fails immediately on Windows.

**Fix**: use Jenkins's built-in `isUnix()` function to detect the agent's
OS at runtime and branch to `sh` (Unix) or `bat` (Windows) accordingly:
```groovy
script {
    if (isUnix()) {
        sh 'mvn clean compile'
    } else {
        bat 'mvn clean compile'
    }
}
```
Also avoid backslash (`\`) line continuation in multi-line shell commands
if the SAME command string might run through either `sh` or `bat` —
Windows batch uses `^` for line continuation, not `\`. Simplest fix:
build the whole Maven command as ONE single-line string, sidestepping
the mismatch entirely.

**Lesson**: a pipeline that only ever ran on Linux CI agents in someone's
tutorial can silently assume `sh` everywhere — always check what OS your
ACTUAL Jenkins agent is before copying a Jenkinsfile from an
online example. `isUnix()`-branching is the real enterprise pattern for
any pipeline that might run across mixed Windows/Linux build agents.

### `Specified HTML directory '...\target\extent-reports' does not exist` / `Did not find any matching files` (testNG report)
**Not actually a NEW bug** — these warnings appear in the `post {}` block
whenever the `Execute Tests` stage never ran successfully (e.g. because
an EARLIER stage like `Build` failed first, as happened here due to the
`sh`/`bat` issue above). No tests ran, so `target/extent-reports/`,
`target/cucumber-reports/`, and `target/surefire-reports/testng-results.xml`
were never generated in the first place — `post { always { ... } }` still
runs and tries to publish them regardless, and correctly reports they're
missing rather than crashing the whole build a second time.

**Lesson**: once the REAL earlier failure (in this case, the `sh`/`bat`
issue) is fixed and a stage actually executes `mvn test` successfully,
these report-publishing warnings disappear on their own — don't chase
them as a separate bug until you've confirmed the actual test execution
stage succeeded.

### `Cannot invoke "TakesScreenshot.getScreenshotAs(...)" because "driver" is null`
**Cause**: a genuine framework bug, only surfaced once a real test
actually ran end-to-end. `Hooks.tearDown()` (Cucumber's `@After`) quits
the driver and clears `DriverManager`'s ThreadLocal (Module 4) as part of
the SCENARIO's own execution. TestNG only reports the test RESULT to
listeners (`ExtentReportListener.onTestFailure()`) AFTER every `@After`
hook for that test has already completed — meaning by the time
`onTestFailure()` tried `ScreenshotUtils.captureAsFile(DriverManager.getDriver(), ...)`,
the driver was already quit and the ThreadLocal already `null`.

**Fix**: capture the screenshot ONCE, inside `Hooks`, while the driver is
still alive (right before `driver.quit()`), and store the resulting file
path in a NEW `ThreadLocal<String>` inside `ScreenshotUtils`
(`setLastCapturedPath`/`getLastCapturedPath`/`clearLastCapturedPath`).
`ExtentReportListener.onTestFailure()` now reads that already-saved path
instead of attempting a second, doomed-to-fail live capture — and clears
it immediately after use (same reasoning as `DriverManager.unload()`: a
reused pooled thread must never see a stale path from an unrelated
earlier test).

**Lesson**: this is the most valuable bug of the whole build to
understand deeply — it's a **hook execution-order** issue, not a typo or
config mistake. General principle for ANY TestNG+Cucumber (or even plain
TestNG) framework: **`@After`/`@AfterMethod` hooks always finish before
any `ITestListener` method fires for that same test** — so a listener can
NEVER reliably use a live resource (driver, DB connection, etc.) that a
hook already tore down. Any state a listener needs must be captured
DURING the hook (while the resource is alive) and handed off via some
holder (here, a `ThreadLocal`) — never fetched fresh inside the listener
itself. This exact pattern — capture-then-hand-off via ThreadLocal — is
now used FOUR times in this framework (driver, ExtentTest, and now
screenshot path) for the same underlying reason.