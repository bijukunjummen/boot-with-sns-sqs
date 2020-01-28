package sample.msg

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import reactor.util.function.Tuple2
import reactor.util.function.Tuples
import sample.msg.config.InfraMigrator
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SqsException
import java.time.Duration
import javax.annotation.PostConstruct

@Component
class MessageListener(
        private val sqsClient: SqsClient,
        private val sqsProperties: SqsProperties,
        private val infraMigrator: InfraMigrator) {

    private var queueUrl = ""

    @PostConstruct
    fun postConstruct() {
        infraMigrator.createMessagingInfra()
        val getQueueUrlRequest = GetQueueUrlRequest
                .builder()
                .queueName(sqsProperties.queueName)
                .build()

        queueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl()
    }

    fun listen(): Flux<Tuple2<Message, (Message) -> Boolean>> {
        return Flux.create { sink: FluxSink<Tuple2<Message, (Message) -> Boolean>> ->
            println("Queue URL : $queueUrl")
            while (true) {
                val receiveMessageRequest = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(5)
                        .waitTimeSeconds(10)
                        .build()
                val messages: List<Message> = sqsClient.receiveMessage(receiveMessageRequest).messages()

                if (messages.isNotEmpty()) {
                    messages.forEach {
                        sink.next(
                                Tuples.of(it, { msg: Message -> deleteQueueMessage(msg.receiptHandle()) }))
                    }
                } else {
                    LOGGER.info("No messages received..")
                }
            }
        }.retryBackoff(10, Duration.ofSeconds(10)).subscribeOn(Schedulers.boundedElastic("message-listener"))
    }

    private fun deleteQueueMessage(receiptHandle: String): Boolean {
        try {
            val deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build()
            sqsClient.deleteMessage(deleteMessageRequest)
            LOGGER.info("Deleted queue message handle={}", receiptHandle)
            return true
        } catch (e: SqsException) {
            LOGGER.warn("Error while deleting message from queue={}", receiptHandle, e)
            return false
        }
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(MessageListener::class.java)
    }
}