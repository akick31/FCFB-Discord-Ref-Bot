# FCFB Discord Bot

The **FCFB Discord Bot** is a bot created for the **Fake College Football** game to play a number based college football game on Discord. This bot integrates with the larger **FCFB ecosystem**, acting as a bridge between the Discord platform and **FCFB-Arceus** (a Spring Boot backend). 

The bot handles user commands and is the frontend for the game that the user interacts with, it takes the numbers and sends the plays to **FCFB-Arceus** to process the game and get the results. It also exposes an endpoint that allows **FCFB-Arceus** to send requests back to the bot.

---

## Features
- **Discord Integration**: Built with [Kord](https://kord.dev/), a Kotlin library for Discord bots.
- **Backend Communication**: Connects seamlessly with **FCFB-Arceus** for real-time game data and updates.
- **Bidirectional Communication**: Exposes endpoints that allow **Arceus** to send requests to the bot.
- **Player Interaction**: Provides a user-friendly interface for players to interact with the game directly through Discord.

---

## Reporting Bugs or Issues
To report bugs or submit feature requests:
- **Preferred Method**: Use the Jira board at [https://fakecollegefootball.atlassian.net/jira/software/projects/FCFB/boards/1](https://fakecollegefootball.atlassian.net/jira/software/projects/FCFB/boards/1). You'll need to sign up for access.
- **Alternative Method**: If Jira isn’t your thing, feel free to submit an issue directly.

---

## Setup Instructions
### 1. Prerequisites
- **Java Development Kit (JDK)**: Version 17 or higher.
- **Gradle**: Installed on your system for building the project.
- **Discord Bot Token**: You’ll need a bot token from the Discord Developer Portal.

### 2. Clone the Repository
Clone the project to your local machine:
```bash
git clone https://github.com/fakecollegefootball/fcfb-discord-bot.git
cd fcfb-discord-bot
```

### 3. Configure Application Properties

The bot requires an application.properties file for configuration. This file contains sensitive information, including the bot token and the URL for FCFB-Arceus.

To get the required application.properties file:
- Contact me directly to receive a pre-configured file.

Once you have the file:
1.	Place it in the src/main/resources directory.
2. Double check the values for correctness, especially if you’re running a local instance of FCFB-Arceus.

# 4. Build the Project

Use Gradle to build the project:
```bash
gradle build
```

# 5. Run the Bot

After building, run the bot with:
```bash
gradle run
```

# 6. Verify
- Invite the bot to your Discord server using your bot token.
- Confirm it’s online and responding to commands.

---

## Development Notes
- **Kord**: The bot uses the [Kord library](https://kord.dev/) for interacting with the Discord API. Familiarity with Kord’s documentation is recommended if you plan to modify the bot.
- **Backend Integration**: The bot is tightly integrated with [FCFB-Arceus](https://github.com/akick31/FCFB-Arceus), and many commands rely on data fetched from or sent to Arceus. Make sure Arceus is running and accessible if you’re testing these features.
- **Exposed Endpoint**: The bot exposes a REST endpoint that allows Arceus to send requests to it. Ensure proper networking configuration if both are not running on the same machine.

---

## Contributing
Contributions are welcome! If you’d like to contribute to the project, please follow these guidelines:
- **Fork the Repository**: Create your own fork of the project.
- **Create a Branch**: Make your changes in a new branch.
- **Commit Changes**: Commit your changes with clear messages.
- **Push Changes**: Push your changes to your fork.
- **Submit a Pull Request**: Create a new pull request with your changes.
- **Code Review**: Your changes will be reviewed, and feedback will be provided.
- **Merge Changes**: Once approved, your changes will be merged into the main branch.

---

Feel free to reach out with any questions or concerns. Happy coding!

---

## License
This project is licensed under the MIT License. See the LICENSE file for more information.
