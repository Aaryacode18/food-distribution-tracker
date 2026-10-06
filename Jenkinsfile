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
        booleanParam(name: 'RUN_DOCKER', defaultValue: true,
                description: 'Build, publish and run a container after the tests pass')
        string(name: 'DOCKER_REGISTRY', defaultValue: 'localhost:5001',
                description: 'Registry the versioned image is pushed to (local registry:2 on 5001)')
        string(name: 'DOCKER_IMAGE', defaultValue: 'food-distribution-tracker',
                description: 'Image name, also used as the app context path inside the container')
        string(name: 'CONTAINER_NAME', defaultValue: 'food-tracker-ci',
                description: 'Name of the container the pipeline deploys')
        string(name: 'CONTAINER_PORT', defaultValue: '8082',
                description: 'Host port the container publishes (8080 is Jenkins, 8081 is host Tomcat)')
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
        // Everything below runs only after the browser gate is green, so a
        // container can never be published or started from a build that the
        // tests rejected.
        stage('Docker Build') {
            when {
                expression { return params.RUN_DOCKER }
            }
            steps {
                echo "Building versioned image ${params.DOCKER_REGISTRY}/${params.DOCKER_IMAGE}:build-${env.BUILD_NUMBER}"
                sh """
                docker build \
                    -t ${params.DOCKER_REGISTRY}/${params.DOCKER_IMAGE}:build-${env.BUILD_NUMBER} \
                    -t ${params.DOCKER_REGISTRY}/${params.DOCKER_IMAGE}:latest \
                    .
                """
            }
        }
        stage('Docker Publish') {
            when {
                expression { return params.RUN_DOCKER }
            }
            steps {
                echo "Pushing image tags to ${params.DOCKER_REGISTRY}"
                sh """
                docker push ${params.DOCKER_REGISTRY}/${params.DOCKER_IMAGE}:build-${env.BUILD_NUMBER}
                docker push ${params.DOCKER_REGISTRY}/${params.DOCKER_IMAGE}:latest
                """
            }
        }
        stage('Docker Deploy') {
            when {
                expression { return params.RUN_DOCKER }
            }
            steps {
                echo "Replacing ${params.CONTAINER_NAME} with build ${env.BUILD_NUMBER} on port ${params.CONTAINER_PORT}"
                sh """
                docker rm -f ${params.CONTAINER_NAME} 2>/dev/null || true
                docker run -d --name ${params.CONTAINER_NAME} \
                    -p ${params.CONTAINER_PORT}:8080 \
                    --restart=always \
                    ${params.DOCKER_REGISTRY}/${params.DOCKER_IMAGE}:build-${env.BUILD_NUMBER}
                """
            }
        }
        stage('Verify Container') {
            when {
                expression { return params.RUN_DOCKER }
            }
            steps {
                echo "Checking the container at http://localhost:${params.CONTAINER_PORT}/${params.APP_CONTEXT}/"
                sh """
                for i in \$(seq 1 30); do
                    if curl --fail --silent http://localhost:${params.CONTAINER_PORT}/${params.APP_CONTEXT}/ > /dev/null; then
                        echo "Container answered after \${i} attempt(s)"
                        docker ps --filter name=${params.CONTAINER_NAME} --format '{{.Names}} {{.Status}} {{.Ports}}'
                        exit 0
                    fi
                    sleep 2
                done
                echo "Container never became healthy"
                docker logs ${params.CONTAINER_NAME}
                exit 1
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
