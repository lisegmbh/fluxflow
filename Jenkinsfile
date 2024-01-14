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
                        withCredentials([usernamePassword(credentialsId: 'fluxflow-snapshot-publisher', usernameVariable: 'MAVEN_USER', passwordVariable: 'MAVEN_PASSWORD')]) {
                            sh "gradle publishMavenPublicationToSnapshotRepository -PprojVersion=0.0.0-SNAPSHOT -PsnapshotUsername=\"\$MAVEN_USER\" -PsnapshotPassword=\"\$MAVEN_PASSWORD\""
                        }
                    }
                }
            }
        }
        stage('Publish stable') {
            when {
                anyOf {
                    tag comparator: 'REGEXP', pattern: '^v\\d+\\.\\d+\\.\\d+$'
                }
            }
            steps {
                container('gradle') {
                    dir('library') {
                        withCredentials([usernamePassword(credentialsId: 'fluxflow-snapshot-publisher', usernameVariable: 'MAVEN_USER', passwordVariable: 'MAVEN_PASSWORD')]) {
                            sh "gradle publishMavenPublicationToSnapshotRepository -PprojVersion=\"$TAG_NAME\" -PsnapshotUsername=\"\$MAVEN_USER\" -PsnapshotPassword=\"\$MAVEN_PASSWORD\""
                        }
                    }
                }
            }
        }
    }
}