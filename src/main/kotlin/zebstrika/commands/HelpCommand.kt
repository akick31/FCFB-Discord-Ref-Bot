package zebstrika.commands

import dev.kord.core.entity.Message
import utils.Logger

class HelpCommand {
    suspend fun execute(message: Message) {
        Logger.debug("Received help command: {}", message.content)
        message.channel.createMessage("pong")
        Logger.debug("Finished processing help command: {}", message.content)
    }
}
