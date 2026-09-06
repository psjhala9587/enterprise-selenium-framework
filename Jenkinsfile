/*
============================================================================
REVISION NOTES — MODULE 10: Jenkinsfile — Declarative Pipeline Deep-Dive
============================================================================
WHY "DECLARATIVE" PIPELINE (this style) INSTEAD OF "SCRIPTED" PIPELINE?
  Jenkins supports two Pipeline syntaxes:
    - Scripted: raw Groovy code, maximum flexibility, steeper learning
      curve, easy to write unmaintainable spaghetti.
    - Declarative (THIS FILE): a structured, opinionated format (pipeline
      { agent {} stages {} post {} }) that's easier to read, lint, and
      onboard new team members onto. Enterprise teams overwhelmingly
      prefer Declarative for exactly this readability/maintainability
      reason — reserve Scripted blocks (via `script {}`) only for the
      rare bit of logic Declarative's structure can't express cleanly.

WHY IS THIS FILE COMMITTED TO GIT (NOT CONFIGURED VIA JENKINS UI CLICKS)?
  "Pipeline as Code" — the ENTIRE build/test/deploy process is versioned
  alongside the application code it tests. A change to how tests run
  (new stage, different parameter) goes through the SAME code review /
  pull request process as any other code change — no undocumented,
  tribal-knowledge Jenkins UI configuration that only one person
  remembers how to reproduce.

parameters {} BLOCK:
  Once this file is committed and the pipeline job has run once, Jenkins
  auto-generates a "Build with Parameters" UI from this block (see
  JENKINS-SETUP.md Step 7) — connects DIRECTLY to Module 7's suite files
  and Module 3's environments, letting a non-technical stakeholder
  trigger a specific test run from a simple form, with zero knowledge of
  the underlying Maven commands.
============================================================================
*/

pipeline {

    agent any
    // In a larger org, `agent any` would typically become
    // `agent { label 'selenium-capable' }` — targeting build agents
    // specifically provisioned with browsers installed, rather than
    // letting Jenkins schedule this on an arbitrary/unsuitable agent.

    tools {
        // MUST match the exact names configured in JENKINS-SETUP.md Step 5.
        jdk 'JDK21'
        maven 'Maven3'
    }

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['qa', 'staging', 'prod'],
            description: 'Which environment config to run against (Module 3)'
        )
        choice(
            name: 'SUITE',
            choices: ['smoke', 'regression', 'full'],
            description: 'Which testng.xml + tag filter combo to run (Module 7)'
        )
        choice(
            name: 'BROWSER',
            choices: ['chrome', 'firefox', 'edge'],
            description: 'Browser to run against (Module 4 DriverFactory)'
        )
    }

    environment {
        // MODULE 10: translate the SUITE parameter into the actual
        // suiteXmlFile + cucumber tag combo Module 7 wired up. Using a
        // Groovy script {} block here (the "rare bit Declarative can't
        // express cleanly" mentioned above) just to compute these two
        // derived values from one dropdown choice.
        SUITE_XML = "${params.SUITE == 'smoke' ? 'testng-smoke.xml' : params.SUITE == 'regression' ? 'testng-regression.xml' : 'testng.xml'}"
        TAG_FILTER = "${params.SUITE == 'smoke' ? '@smoke' : params.SUITE == 'regression' ? '@regression' : ''}"
    }

    stages {

        stage('Checkout') {
            steps {
                // With "Pipeline script from SCM" (JENKINS-SETUP.md Step 6),
                // Jenkins ALREADY checks out the repo before this stage even
                // starts — this explicit `checkout scm` is often technically
                // redundant but is left here for CLARITY in the pipeline
                // visualization UI, and becomes NECESSARY if this pipeline
                // is ever converted to a Multibranch Pipeline pulling from
                // multiple repos.
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // Module 1/2's decision: mvn compile validates src/main/java
                // compiles cleanly BEFORE we spend time on test-compile or
                // execution — fail fast on a broken framework class.
                sh 'mvn clean compile'
            }
        }

        stage('Test Compile') {
            steps {
                sh 'mvn test-compile'
            }
        }

        stage('Execute Tests') {
            steps {
                script {
                    // MODULE 10: this is the exact command chain built up
                    // across Module 3 (-Denv), Module 7 (-DsuiteXmlFile,
                    // -Dcucumber.filter.tags) — Jenkins doesn't introduce
                    // any NEW execution mechanism, it just calls the same
                    // `mvn test` a developer runs locally, with parameters
                    // sourced from the dropdown instead of typed by hand.
                    def tagArg = TAG_FILTER ? "-Dcucumber.filter.tags=\"${TAG_FILTER}\"" : ''
                    sh """
                        mvn test \
                            -Denv=${params.ENVIRONMENT} \
                            -DsuiteXmlFile=${SUITE_XML} \
                            -Dbrowser=${params.BROWSER} \
                            ${tagArg}
                    """
                }
            }
        }
    }

    post {
        // ========================================================================
        // post {} runs REGARDLESS of stage success/failure — this is WHERE
        // reporting/notification/cleanup belongs, since we want a report
        // published and a team notified whether the run passed OR failed
        // (arguably notification matters MORE on failure).
        // ========================================================================

        always {
            // Module 8's ExtentReports HTML — published as a clickable tab
            // on the Jenkins build page via the HTML Publisher plugin
            // (JENKINS-SETUP.md Step 4).
            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'target/extent-reports',
                reportFiles: 'ExecutionReport.html',
                reportName: 'Extent Report'
            ])

            // Module 6's Cucumber HTML report, similarly published.
            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'target/cucumber-reports',
                reportFiles: 'cucumber.html',
                reportName: 'Cucumber Report'
            ])

            // TestNG's own native XML results feed Jenkins's built-in test
            // trend graphs (pass/fail counts over time, across builds) —
            // requires the TestNG Results Plugin (JENKINS-SETUP.md Step 4).
            testNG()

            // Module 8's Log4j2 rolling file log + any raw screenshots
            // (Module 8's ScreenshotUtils.captureAsFile) archived as raw
            // build artifacts — downloadable from the build page
            // independent of whichever HTML report tool someone opens.
            archiveArtifacts artifacts: 'target/logs/**, target/screenshots/**',
                              allowEmptyArchive: true
        }

        failure {
            // TODO: wire actual Slack/Teams/email notification here once
            // the org's notification channel/credentials are decided —
            // e.g. via the Credentials Binding plugin (JENKINS-SETUP.md
            // Step 4) to keep webhook URLs out of this committed file,
            // same secrets-handling principle we applied to Jira's API
            // token back in Module 9 before that integration was removed.
            echo "Build FAILED - environment=${params.ENVIRONMENT}, suite=${params.SUITE}. Wire notification here."
        }

        cleanup {
            // Runs after every OTHER post condition. Good place for
            // workspace cleanup on agents shared across multiple jobs.
            // MODULE 10 GOTCHA: a block with ONLY a comment inside it
            // (no real step) fails Groovy compilation with "No steps
            // specified for branch" — every post condition needs AT
            // LEAST one real step, even if it's just an echo. Uncomment
            // cleanWs() below (requires the "Workspace Cleanup" plugin)
            // once you're running on a shared/pooled agent; a dedicated
            // build agent doesn't strictly need this.
            echo "Cleanup stage - add cleanWs() here if using a shared/pooled agent."
            // cleanWs()
        }
    }
}