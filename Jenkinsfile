pipeline {
    agent {
        kubernetes {
            cloud 'kubernetes-azure'
            defaultContainer 'jnlp'
            inheritFrom 'plain'
            yamlFile './build/agent.yml'
            showRawYaml true
        }
    }

    stages {
        stage('Build library') {
            when {
                anyOf {
                    changeRequest()
                    branch 'develop'
                    branch 'main'
                    tag comparator: 'REGEXP', pattern: '^v\\d+\\.\\d+\\.\\d+$'
                }
            }
            steps {
                container('gradle') {
                    sh 'sh build/tests/wait-for-docker-test.sh'
                    sh 'sh build/wait-for-docker.sh'
                    dir('library') {
                        // buildSrc:test must be requested explicitly - Gradle no longer
                        // runs buildSrc tests as part of the main build.
                        sh 'gradle build buildSrc:test'
                    }
                }
            }
            post {
                always {
                    // Preflight can fail before XML exists. The mandatory Gradle
                    // security gate still rejects absent or skipped security tests.
                    junit testResults: 'library/**/build/test-results/**/*.xml',
                            allowEmptyResults: true
                    archiveArtifacts artifacts: 'library/**/build/reports/tests/**,library/**/build/test-results/**/*.xml',
                            allowEmptyArchive: true
                }
            }
        }
        stage('Publish snapshot') {
            when {
                anyOf {
                    branch 'develop'
                }
            }
            steps {
                container('gradle') {
                    dir('library') {
                        withCredentials([
                                file(credentialsId: 'fluxflow-keyring', variable: 'KEYRING_FILE'),
                                string(credentialsId: 'fluxflow-keyring-password', variable: 'KEYRING_PASSWORD'),
                                usernamePassword(credentialsId: 'fluxflow-snapshot-publisher', usernameVariable: 'MAVEN_USER', passwordVariable: 'MAVEN_PASSWORD')
                        ]) {
                            sh "gradle publishMavenPublicationToSnapshotRepository" +
                                    " -PsnapshotUsername=\"\$MAVEN_USER\"" +
                                    " -PsnapshotPassword=\"\$MAVEN_PASSWORD\"" +
                                    " -Psigning.keyId=73F5D362" +
                                    " -Psigning.password=\"\$KEYRING_PASSWORD\"" +
                                    " -Psigning.secretKeyRingFile=\"\$KEYRING_FILE\""
                        }
                    }
                }
            }
        }
        stage('Publish Maven Central Staging') {
            when {
                anyOf {
                    tag comparator: 'REGEXP', pattern: '^v\\d+\\.\\d+\\.\\d+$'
                }
            }
            steps {
                container('gradle') {
                    dir('library') {
                        withCredentials([
                                file(credentialsId: 'fluxflow-keyring', variable: 'KEYRING_FILE'),
                                string(credentialsId: 'fluxflow-keyring-password', variable: 'KEYRING_PASSWORD'),
                                usernamePassword(credentialsId: 'fluxflow-maven-central-publish-user', passwordVariable: 'MAVEN_PASSWORD', usernameVariable: 'MAVEN_USER')
                        ]) {
                            sh "gradle publishToMavenCentral" +
                                    " -PprojVersion=\"$TAG_NAME\"" +
                                    " -PmavenCentralUsername=\"\$MAVEN_USER\"" +
                                    " -PmavenCentralPassword=\"\$MAVEN_PASSWORD\"" +
                                    " -Psigning.keyId=73F5D362" +
                                    " -Psigning.password=\"\$KEYRING_PASSWORD\"" +
                                    " -Psigning.secretKeyRingFile=\"\$KEYRING_FILE\""
                        }
                    }
                }
            }
        }
    }
}
