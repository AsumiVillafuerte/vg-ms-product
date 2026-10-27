def branchName() {
    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: env.gitlabSourceBranch ?: 'develop'
    return branch.replaceFirst(/^origin\//, '')
}

def notifySlack(String color, String message) {
    try {
        slackSend(
            channel: env.SLACK_CHANNEL,
            color: color,
            message: message
        )
    } catch (Exception e) {
        echo "No se pudo notificar a Slack: ${e.message}"
    }
}

pipeline {
    agent any

    tools {
        maven 'MAVEN'
    }

    environment {
        SERVICE_NAME       = 'vg-ms-products'
        SLACK_CHANNEL      = '#jenkins'
        SONAR_HOST_URL     = 'https://sonarcloud.io'
        SONAR_ORGANIZATION = 'asumi-penafiel-villafuerte'
        SONAR_PROJECT_KEY  = 'asumi-penafiel-villafuerte_vg-ms-products'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    echo "Microservicio: ${env.SERVICE_NAME}"
                    echo "Rama: ${branchName()}"
                }
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -ntp clean compile'
            }
        }

        stage('Test & Coverage') {
            steps {
                sh 'mvn -B -ntp verify'
                script {
                    if (!fileExists('target/site/jacoco/jacoco.xml')) {
                        error('No se genero el reporte JaCoCo. Revisa jacoco-maven-plugin en pom.xml.')
                    }
                    echo 'Reporte JaCoCo generado correctamente.'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
                }
            }
        }

        stage('Send Analysis to SonarCloud') {
            steps {
                withCredentials([
                    string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')
                ]) {
                    timeout(time: 10, unit: 'MINUTES') {
                        sh '''
                            set +x
                            mvn -B -ntp sonar:sonar \
                              -Dsonar.host.url="$SONAR_HOST_URL" \
                              -Dsonar.organization="$SONAR_ORGANIZATION" \
                              -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
                              -Dsonar.token="$SONAR_TOKEN" \
                              -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                        echo 'Analisis enviado a SonarCloud. No se valida el Quality Gate.'
                    }
                }
            }
        }

    }

    post {
        success {
            script {
                notifySlack(
                    'good',
                    """*${env.SERVICE_NAME} - BUILD EXITOSO*
Rama: ${branchName()}
Build: #${env.BUILD_NUMBER}
Reporte JaCoCo: generado
Analisis enviado a SonarCloud (sin validar Quality Gate)
Estado: SUCCESS
<${env.BUILD_URL}|Ver ejecucion en Jenkins>"""
                )
            }
        }
        failure {
            script {
                notifySlack(
                    'danger',
                    """*${env.SERVICE_NAME} - BUILD FALLIDO*
Rama: ${branchName()}
Build: #${env.BUILD_NUMBER}
Revisa los errores de compilacion, pruebas o envio a SonarCloud
Estado: FAILURE
<${env.BUILD_URL}console|Ver errores en Jenkins>"""
                )
            }
        }
        aborted {
            script {
                notifySlack(
                    'warning',
                    """*${env.SERVICE_NAME} - BUILD CANCELADO*
Rama: ${branchName()}
Build: #${env.BUILD_NUMBER}
Estado: ABORTED
<${env.BUILD_URL}|Ver ejecucion en Jenkins>"""
                )
            }
        }
    }
}
