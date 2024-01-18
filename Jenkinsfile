pipeline {
    agent {
        kubernetes {
            cloud 'kubernetes-azure'
            defaultContainer 'jnlp'
            inheritFrom 'plain'
            yamlFile './build/agent.yml'
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
                    dir('library') {
                        sh 'gradle build'
                    }
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
                                file(credentialsId: '', variable: 'KEYRING_FILE'),
                                string(credentialsId: '', variable: 'KEYRING_PASSWORD'),
                                usernamePassword(credentialsId: 'fluxflow-snapshot-publisher', usernameVariable: 'MAVEN_USER', passwordVariable: 'MAVEN_PASSWORD')
                        ]) {
                            sh "gradle publishMavenPublicationToSnapshotRepository" +
                                    " -PprojVersion=0.0.0-SNAPSHOT" +
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
                                file(credentialsId: '', variable: 'KEYRING_FILE'),
                                string(credentialsId: '', variable: 'KEYRING_PASSWORD'),
                                usernamePassword(credentialsId: '', passwordVariable: 'MAVEN_PASSWORD', usernameVariable: 'MAVEN_USER')
                        ]) {
                            sh "gradle publishMavenPublicationToStagingRepository" +
                                    " -PprojVersion=\"$TAG_NAME\"" +
                                    " -PstagingUsername=\"\$MAVEN_USER\"" +
                                    " -PstagingPassword=\"\$MAVEN_PASSWORD\"" +
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