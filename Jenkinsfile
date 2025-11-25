pipeline {
    agent any // Specifies that the pipeline can run on any available agent
    tools {
        // Assumes you have configured a Maven and JDK tool in "Manage Jenkins" -> "Global Tool Configuration"
        maven 'maven-3.8'
        jdk 'java-17'
    }
    stages {
        stage('Build') {
            steps {
                // This command cleans the project and packages it into a JAR file, skipping tests temporarily
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Test') {
            steps {
                // This command runs the unit tests defined in your project
                sh 'mvn test'
            }
            post {
                // Archives the test results for display in the Jenkins UI
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
    }
    post {
        // Actions that run after the pipeline completes
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
