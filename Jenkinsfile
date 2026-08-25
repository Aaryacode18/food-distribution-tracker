pipeline {
    agent any
    environment {
        JAVA_HOME = "/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home"
        PATH = "${JAVA_HOME}/bin:/opt/homebrew/bin:${env.PATH}"
    }
    stages {
        stage('Checkout') {
            steps {
                echo 'Downloading source code from GitHub'
                git branch: 'main',
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
                echo 'Deploying application to Tomcat'
                sh '''
                cp -f target/food-distribution-tracker.war /opt/homebrew/opt/tomcat/libexec/webapps/food-distribution-tracker.war
                '''
            }
        }
        stage('Verify') {
            steps {
                echo 'Checking deployed application'
                sh '''
                sleep 8
                curl --fail http://localhost:8081/food-distribution-tracker/
                '''
            }
        }
    }
    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
        success {
            echo 'CI/CD PIPELINE SUCCESSFUL'
        }
        failure {
            echo 'CI/CD PIPELINE FAILED'
        }
    }
}
