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
    }
}