pipeline {
    agent any

    environment {
        IMAGE_NAME = 'fcfb-discord-ref-bot'
        CONTAINER_NAME = 'FCFB-Discord-Ref-Bot'
        DOCKERFILE = 'Dockerfile'
        CONFIG_PROPERTIES = './src/main/resources/config.properties'
        API_URL = credentials('ARCEUS_API_URL')
        DISCORD_TOKEN = credentials('REFBOT_DISCORD_TOKEN')
        DISCORD_GUILD_ID = credentials('DISCORD_GUILD_ID')
        DISCORD_FORUM_CHANNEL_ID = credentials('DISCORD_FORUM_CHANNEL_ID')
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out the Ref Bot project...'
                checkout scm
            }
        }
        stage('Get Version') {
            steps {
                script {
                    // Get the latest Git tag
                    def latestTag = sh(script: "git describe --tags --abbrev=0", returnStdout: true).trim()

                    // If there are no tags, default to 1.0.0
                    if (!latestTag) {
                        latestTag = '1.0.0'
                    }

                    // Print the version
                    echo "Current Version: ${latestTag}"

                    // Set the version to an environment variable for use in later stages
                    env.VERSION = latestTag

                    // Set the build description
                    currentBuild.description = "Version: ${env.VERSION}"
                    currentBuild.displayName = "Build #${env.BUILD_NUMBER} - Version: ${env.VERSION}"
                }
            }
        }
        stage('Stop and Remove Existing Bot') {
            steps {
                script {
                    echo 'Stopping and removing the existing Ref Bot instance...'
                    sh """
                        docker stop ${CONTAINER_NAME} || echo "Ref Bot is not running."
                        docker rm ${CONTAINER_NAME} || echo "No old Ref Bot instance to remove."
                    """
                }
            }
        }

        stage('Build') {
            steps {
                echo 'Creating the properties file...'
                script {
                    def propertiesContent = """
                        discord.bot.token=${env.DISCORD_TOKEN}
                        discord.game.forum.id=${env.DISCORD_FORUM_CHANNEL_ID}
                        discord.guild.id=${env.DISCORD_GUILD_ID}
                        api.url=${env.API_URL}
                    """.stripIndent()

                    writeFile file: "${env.CONFIG_PROPERTIES}", text: propertiesContent
                }

                echo 'Building the Ref Bot project...'
                sh './gradlew clean build'
            }
        }

        stage('Build New Docker Image') {
            steps {
                script {
                    echo 'Building the new Ref Bot Docker image...'
                    sh """
                        docker build -t ${IMAGE_NAME}:${DOCKERFILE} .
                    """
                }
            }
        }

        stage('Run New Ref Bot Container') {
            steps {
                script {
                    echo 'Starting the new Ref Bot container...'
                    sh """
                        docker run -d -p 1211:1211 --restart=always --name ${CONTAINER_NAME} \\
                            --env-file ${CONFIG_PROPERTIES} \\
                            ${IMAGE_NAME}:${DOCKERFILE}
                    """
                }
            }
        }

        stage('Cleanup Docker System') {
            steps {
                script {
                    echo 'Pruning unused Docker resources...'
                    sh 'docker system prune -a --force'
                }
            }
        }
    }

    post {
        success {
            echo 'Ref Bot has been successfully deployed!'
        }
        failure {
            echo 'An error occurred during the Ref Bot deployment.'
        }
    }
}
