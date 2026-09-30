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
        string(name: 'APP_PORT', defaultValue: '8081',
               description: 'Tomcat HTTP port')
        string(name: 'JAVA_HOME_PATH', defaultValue: '/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home',
               description: 'JDK used to run Maven')
        booleanParam(name: 'RUN_SELENIUM', defaultValue: false,
               description: 'Run the Selenium browser suite against the deployed app')
    }
    environment {
        JAVA_HOME = "${params.JAVA_HOME_PATH}"
        PATH = "${JAVA_HOME}/bin:/opt/homebrew/bin:${env.PATH}"
        APP_URL = "http://localhost:${params.APP_PORT}/${params.APP_CONTEXT}/"
        WAR_FILE = "${params.APP_CONTEXT}.war"
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
        stage('Test') {
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
        stage('Deploy') {
            steps {
                echo "Deploying ${env.WAR_FILE} to ${params.TOMCAT_WEBAPPS} (env: ${params.DEPLOY_ENV})"
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
            junit allowEmptyResults: false, testResults: 'target/surefire-reports/*.xml'
        }
        success {
            echo 'CI/CD PIPELINE SUCCESSFUL'
        }
        failure {
            echo 'CI/CD PIPELINE FAILED'
        }
    }
}
