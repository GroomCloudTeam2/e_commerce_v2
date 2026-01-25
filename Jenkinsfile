pipeline {
    agent any

    environment {
        // ===== Gradle =====
        GRADLE_USER_HOME = "${WORKSPACE}/.gradle"

        // ===== Docker =====
        IMAGE_NAME = "boxty123/ecommerce_v2"
        IMAGE_TAG  = "${BUILD_NUMBER}"

        // ===== SonarCloud =====
        SONAR_PROJECT_KEY = "GroomCloudTeam2_e_commerce_v2"
        SONAR_ORG = "groomcloudteam2"
        SONAR_HOST_URL = "https://sonarcloud.io"
    }

    options {
        timestamps()
    }

    stages {
        stage('Check Java Version') {
            steps {
                sh '''
                  java -version
                  ./gradlew -version
                '''
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        /* =========================
         * 1️⃣ Test
         * ========================= */
        stage('Test') {
            steps {
                sh '''
                  ./gradlew clean test jacocoTestReport
                '''
            }
            post {
                always {
                    junit 'build/test-results/test/**/*.xml'
                    archiveArtifacts artifacts: 'build/reports/jacoco/test/jacocoTestReport.xml'
                }
            }
        }

        /* =========================
         * 2️⃣ Build Jar
         * ========================= */
        stage('Build') {
            steps {
                sh './gradlew build -x test'
            }
        }
    }

    post {
        success {
            echo '✅ CI Pipeline Success'
        }
        failure {
            echo '❌ CI Pipeline Failed'
        }
    }
}
