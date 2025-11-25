pipeline {
    agent any

    tools {
        maven 'maven-3.8.7'
        jdk 'java-21'
    }

    environment {
        DOCKERHUB = credentials('dockerhub-creds')
        IMAGE_NAME = "kelvincollins86/investment-backend"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('Docker Login') {
            steps {
                sh 'echo $DOCKERHUB_PSW | docker login -u $DOCKERHUB_USR --password-stdin'
            }
        }

        stage('Docker Push') {
            steps {
                sh "docker push ${IMAGE_NAME}:latest"
            }
        }
    }

    post {
        success {
            echo '🚀 Pipeline completed and image pushed to Docker Hub: kelvincollins86/investment-backend'
        }
        failure {
            echo '❌ Pipeline failed.'
        }
    }
}
