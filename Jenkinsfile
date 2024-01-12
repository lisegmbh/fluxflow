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
                    branch 'feature/6-add-build-script'
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
                    branch 'feature/6-add-build-script'
                }
            }
            steps {
                container('gradle') {
                    dir('library') {
                        withCredentials([usernamePassword(credentialsId: 'fluxflow-snapshot-publisher', usernameVariable: 'MAVEN_USER', passwordVariable: 'MAVEN_PASSWORD')]) {
                            sh "publishMavenPublicationToSnapshotRepository -PprojVersion=0.0.0-SNAPSHOT -PsnapshotUsername='$MAVEN_USER' -PsnapshotPassword='$MAVEN_PASSWORD'"
                        }
                    }
                }
            }
        }
    }
}