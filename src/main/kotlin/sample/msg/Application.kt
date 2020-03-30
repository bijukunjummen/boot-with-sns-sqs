package sample.msg

import org.slf4j.Logger
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@SpringBootApplication
@EnableConfigurationProperties(value = [SnsProperties::class, SqsProperties::class])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Component
class MessageListenerRunner(private val eventReceiver: SnsEventHandler) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        eventReceiver.handle(5) { message ->
            Mono.fromRunnable { LOGGER.info("Processed Message $message") }
        }
    }

    companion object {
        val LOGGER: Logger = loggerFor<MessageListenerRunner>()
    }

}