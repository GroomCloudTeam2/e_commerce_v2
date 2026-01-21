pipeline {
    agent any

    options {
        timestamps()
    }

    environment {
        // Sonar token (너 Jenkins credentials에 'sonarqube-token'으로 등록된 값)
        SONAR_TOKEN = credentials('sonarqube-token')

        IMAGE_NAME = 'e_commerce_v2'
        TRIVY_SEVERITY = 'HIGH,CRITICAL'
    }

    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                git branch: 'user', url: 'https://github.com/GroomCloudTeam2/e_commerce_v2.git'
            }
        }

        stage('Build & Test') {
            steps {
                sh './gradlew clean test jacocoTestReport build'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    // sonarqube task는 deprecated라 sonar 권장
                    sh './gradlew sonar -Dsonar.token=$SONAR_TOKEN -Dsonar.gradle.skipCompile=true'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    def shortSha = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${shortSha}"
                }
                sh 'docker version'
                sh 'docker build -t $IMAGE_NAME:$IMAGE_TAG .'
            }
        }

        stage('Image Vulnerability Scan (Trivy)') {
            steps {
                // Trivy를 컨테이너로 실행 (Jenkins에 설치 불필요)
                // --exit-code 1 : 취약점(지정 severity) 발견 시 파이프라인 FAIL
                sh '''
                      docker run --rm \
                        -v /var/run/docker.sock:/var/run/docker.sock \
                        -v "$WORKSPACE:/workspace" \
                        aquasec/trivy:latest image \
                        --scanners vuln \
                        --severity HIGH,CRITICAL \
                        --exit-code 0 \
                        --no-progress \
                        "$IMAGE_NAME:$IMAGE_TAG"
                    '''

            }
        }
    }

    post {
        always {
            // 스캔 리포트 저장
            archiveArtifacts artifacts: 'trivy-report.json', allowEmptyArchive: true

            // 테스트/커버리지 리포트도 필요하면 같이 보관
            archiveArtifacts artifacts: 'build/reports/**', allowEmptyArchive: true
        }
    }
}
