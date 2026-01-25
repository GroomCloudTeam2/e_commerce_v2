// pipeline {
//     agent any
//
//     environment {
//         // ===== Gradle =====
//         GRADLE_USER_HOME = "${WORKSPACE}/.gradle"
//
//         // ===== Docker =====
//         IMAGE_NAME = "boxty123/ecommerce_v2"
//         IMAGE_TAG  = "${BUILD_NUMBER}"
//
//         // ===== SonarCloud =====
//         SONAR_PROJECT_KEY = "GroomCloudTeam2_e_commerce_v2"
//         SONAR_ORG = "groomcloudteam2"
//         SONAR_HOST_URL = "https://sonarcloud.io"
//     }
//
//     options {
//         timestamps()
//     }
//
//     stages {
//         stage('Check Java Version') {
//             steps {
//                 sh '''
//                   java -version
//                   ./gradlew -version
//                 '''
//             }
//         }
//
//         stage('Checkout') {
//             steps {
//                 checkout scm
//             }
//         }
// //
// //         /* =========================
// //          * 1️⃣ Unit & Slice Tests ONLY
// //          * ========================= */
// //
// //         stage('Test') {
// //             steps {
// //                 sh '''
// //                   ./gradlew clean test jacocoTestReport
// //                 '''
// //             }
// //             post {
// //                 always {
// //                     junit 'build/test-results/test/**/*.xml'
// //                     archiveArtifacts artifacts: 'build/reports/jacoco/test/jacocoTestReport.xml'
// //                 }
// //             }
// //         }
// //
// //
// //         /* =========================
// //          * 2️⃣ SonarCloud Analysis
// //          * ========================= */
// //         stage('SonarCloud Analysis') {
// //             environment {
// //                 SONAR_TOKEN = credentials('sonarcloud-token')
// //             }
// //             steps {
// //                 withSonarQubeEnv('sonarcloud') {
// //                     sh '''
// //                       ./gradlew sonarqube \
// //                         -Dsonar.projectKey=$SONAR_PROJECT_KEY \
// //                         -Dsonar.organization=$SONAR_ORG \
// //                         -Dsonar.host.url=$SONAR_HOST_URL \
// //                         -Dsonar.token=$SONAR_TOKEN \
// //                         -Dsonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml
// //                     '''
// //                 }
// //             }
// //         }
// //
// //
// //         /* =========================
// //          * 3️⃣ Quality Gate
// //          * ========================= */
// //         stage('Quality Gate') {
// //             steps {
// //                 timeout(time: 3, unit: 'MINUTES') {
// //                     waitForQualityGate abortPipeline: true
// //                 }
// //             }
// //         }
//
//         /* =========================
//          * 4️⃣ Build Jar
//          * ========================= */
//         stage('Build') {
//             steps {
//                 sh './gradlew build -x test'
//             }
//         }
// //
// //         /* =========================
// //          * 5️⃣ Docker Build
// //          * ========================= */
// //         stage('Docker Build') {
// //             steps {
// //                 sh 'docker build -t $IMAGE_NAME:$IMAGE_TAG .'
// //             }
// //         }
// //
// //         /* =========================
// //          * 6️⃣ Trivy Image Scan
// //          * ========================= */
// //         stage('Trivy Scan') {
// //             steps {
// //                 sh '''
// //                     trivy image \
// //                       --severity HIGH,CRITICAL \
// //                       --exit-code 1 \
// //                       $IMAGE_NAME:$IMAGE_TAG
// //                 '''
// //             }
// //         }
// //     }
//
//     post {
//         success {
//             echo '✅ CI Pipeline Success'
//         }
//         failure {
//             echo '❌ CI Pipeline Failed'
//         }
//     }
// }


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
         * 4️⃣ Build Jar
         * ========================= */
        stage('Build') {
            steps {
                sh './gradlew build -x test'
            }
        }
    }  // ✅ 이게 빠져 있었음!!!

    post {
        success {
            echo '✅ CI Pipeline Success'
        }
        failure {
            echo '❌ CI Pipeline Failed'
        }
    }
}
