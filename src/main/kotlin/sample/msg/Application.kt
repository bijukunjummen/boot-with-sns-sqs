package sample.msg

import com.nike.content.notary.message.SnsEventReceiver
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

@SpringBootApplication
@EnableConfigurationProperties(value = [SnsProperties::class, SqsProperties::class])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Component
class MessageListenerRunner(private val eventReceiver: SnsEventReceiver) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        eventReceiver.listen()
            .subscribeOn(Schedulers.newElastic("pollingThread"))
            .publishOn(Schedulers.parallel(), 5)
            .subscribe({ (message: String, deleteHandle: () -> Unit) ->
                LOGGER.info("Processed Message $message")
                deleteHandle()
            }, { t -> LOGGER.error(t.message, t) })
    }

    companion object {
        val LOGGER = loggerFor<MessageListenerRunner>()
    }

}