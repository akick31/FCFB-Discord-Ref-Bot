pipeline {
    agent any

    environment {
        IMAGE_NAME = 'fcfb-discord-ref-bot'
        CONTAINER_NAME = 'FCFB-Discord-Ref-Bot'
        DOCKERFILE = 'Dockerfile'
        CONFIG_PROPERTIES = './src/main/resources/config.properties'
        API_URL = credentials('API_URL')
        DISCORD_TOKEN = credentials('DISCORD_TOKEN')
        DISCORD_GUILD_ID = credentials('DISCORD_GUILD_ID')
        DISCORD_FORUM_CHANNEL_ID = credentials('DISCORD_FORUM_CHANNEL_ID')
    }

    stages {
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
                        docker run -d --restart=always --name ${CONTAINER_NAME} \\
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
