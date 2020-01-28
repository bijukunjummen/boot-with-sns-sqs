package sample.msg

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.SqsException

@SpringBootApplication
@EnableConfigurationProperties(value = [SnsProperties::class, SqsProperties::class])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Component
class MessageListenerRunner(private val messageListenerRunner: MessageListener): ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        messageListenerRunner.listen()
                .map { tup ->
                    val message = tup.t1
                    val deleteHandle: (Message) -> Boolean = tup.t2
                    println("Receieved message: ${message.body()}")
                    deleteHandle(message)

                }
                .subscribe()
    }

}