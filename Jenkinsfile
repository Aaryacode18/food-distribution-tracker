pipeline {
    agent any
    parameters {
        string(name: 'BRANCH', defaultValue: 'main',
               description: 'Branch to build')
        string(name: 'DEPLOY_ENV', defaultValue: 'local',
               description: 'Target environment name (local/staging/prod)')
        string(name: 'TOMCAT_WEBAPPS', defaultValue: '/opt/homebrew/opt/tomcat/libexec/webapps',
               description: 'Tomcat webapps directory')
        string(name: 'APP_CONTEXT', defaultValue: 'food-distribution-tracker',
               description: 'Application context path')
        string(name: 'STAGING_CONTEXT', defaultValue: 'food-distribution-tracker-staging',
               description: 'Throwaway context the new WAR is deployed to for browser testing')
        string(name: 'APP_PORT', defaultValue: '8081',
               description: 'Tomcat HTTP port')
        string(name: 'JAVA_HOME_PATH', defaultValue: '/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home',
               description: 'JDK used to run Maven')
        booleanParam(name: 'RUN_SELENIUM', defaultValue: true,
               description: 'Run the Selenium browser suite against the staged build')
    }
    environment {
        JAVA_HOME = "${params.JAVA_HOME_PATH}"
        PATH = "${JAVA_HOME}/bin:/opt/homebrew/bin:${env.PATH}"
        APP_URL = "http://localhost:${params.APP_PORT}/${params.APP_CONTEXT}/"
        STAGING_URL = "http://localhost:${params.APP_PORT}/${params.STAGING_CONTEXT}/"
        WAR_FILE = "${params.APP_CONTEXT}.war"
        STAGING_WAR_FILE = "${params.STAGING_CONTEXT}.war"
    }
    stages {
        stage('Checkout') {
            steps {
                echo "Downloading source code from GitHub (branch: ${params.BRANCH})"
                git branch: "${params.BRANCH}",
                    url: 'https://github.com/Aaryacode18/food-distribution-tracker.git'
            }
        }
        stage('Compile') {
            steps {
                echo 'Compiling Java application'
                sh 'mvn clean compile'
            }
        }
        stage('Unit Test') {
            steps {
                echo 'Executing JUnit tests'
                sh 'mvn test'
            }
        }
        stage('Package') {
            steps {
                echo 'Creating WAR file'
                sh 'mvn package -DskipTests'
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
            }
        }
        stage('Deploy to Staging') {
            steps {
                echo "Staging ${env.WAR_FILE} at ${env.STAGING_URL} for browser testing"
                sh """
                cp -f target/${env.WAR_FILE} ${params.TOMCAT_WEBAPPS}/${env.STAGING_WAR_FILE}
                """
                // Tomcat deploys asynchronously, so poll rather than sleeping a fixed amount.
                sh """
                for i in \$(seq 1 30); do
                    if curl --fail --silent ${env.STAGING_URL} > /dev/null; then
                        echo "Staging is up after \${i} attempt(s)"
                        exit 0
                    fi
                    sleep 2
                done
                echo "Staging never became healthy at ${env.STAGING_URL}"
                exit 1
                """
            }
        }
        stage('Browser Test') {
            when {
                expression { return params.RUN_SELENIUM }
            }
            steps {
                echo "Running Selenium browser suite against ${env.STAGING_URL}"
                sh """
                mvn test -Pselenium -Dapp.url=${env.STAGING_URL}
                """
            }
        }
        stage('Deploy') {
            steps {
                echo "Promoting ${env.WAR_FILE} to ${params.TOMCAT_WEBAPPS} (env: ${params.DEPLOY_ENV})"
                sh """
                cp -f target/${env.WAR_FILE} ${params.TOMCAT_WEBAPPS}/${env.WAR_FILE}
                """
            }
        }
        stage('Verify') {
            steps {
                echo "Checking deployed application at ${env.APP_URL}"
                sh """
                sleep 8
                curl --fail ${env.APP_URL}
                """
            }
        }
    }
    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
            // Screenshots are only produced when a browser test fails, so this
            // archive is legitimately empty on a green build.
            archiveArtifacts artifacts: 'target/selenium-screenshots/*.png',
                             allowEmptyArchive: true, fingerprint: true
        }
        success {
            echo 'CI/CD PIPELINE SUCCESSFUL'
        }
        failure {
            echo 'CI/CD PIPELINE FAILED - deployment was not promoted'
        }
        cleanup {
            // Never leave the throwaway context behind, on any outcome.
            sh """
            rm -f ${params.TOMCAT_WEBAPPS}/${env.STAGING_WAR_FILE}
            rm -rf ${params.TOMCAT_WEBAPPS}/${params.STAGING_CONTEXT}
            echo "Removed staging context ${params.STAGING_CONTEXT}"
            """
        }
    }
}
