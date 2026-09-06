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
