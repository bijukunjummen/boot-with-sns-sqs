package sample.msg

import com.fasterxml.jackson.databind.ObjectMapper
import io.vavr.control.Try
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.util.function.Tuple2
import reactor.util.function.Tuples
import sample.msg.model.SnsNotification
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.CreateTopicRequest
import software.amazon.awssdk.services.sns.model.SubscribeRequest
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.SqsException
import java.io.IOException
import java.util.function.Function
import javax.annotation.PostConstruct

class SnsEventHandler(
    private val snsClient: SnsClient,
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    private val topicName: String,
    private val queueName: String
) : EventHandler {

    @Volatile
    lateinit var queueUrl: String
    val taskScheduler: Scheduler = Schedulers.newElastic("taskHandler")

    /**
     * Generate a stream of raw message by by subscribing a queue to a topic
     *
     * @return a tuple with the string representation of the message
     * and the delete handle
     */
    override fun handle(concurrency: Int, task: (message: String) -> Mono<Void>) {
        Flux.generate { sink: SynchronousSink<List<Message>> ->
            val receiveMessageRequest: ReceiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(5)
                .waitTimeSeconds(10)
                .build()

            val messages: List<Message> = sqsClient.receiveMessage(receiveMessageRequest).messages()
            LOGGER.info("Received: $messages")
            sink.next(messages)
        }
            .flatMapIterable(Function.identity())
            .doOnError { t: Throwable -> LOGGER.error(t.message, t) }
            .retry()
            .map { snsMessage: Message ->
                val snsMessageBody: String = snsMessage.body()
                val snsNotification: SnsNotification = readSnsNotification(snsMessageBody)
                snsNotification.message to { deleteQueueMessage(snsMessage.receiptHandle(), queueUrl) }
            }
            .flatMap({ (message: String, deleteHandle: () -> Unit) ->
                task(message)
                    .then(Mono.fromSupplier { Try.of { deleteHandle() } })
                    .onErrorResume { t ->
                        LOGGER.error(t.message, t)
                        Mono.empty()
                    }
                    .then()
                    .subscribeOn(taskScheduler)
            }, concurrency)
            .subscribeOn(Schedulers.newElastic("subscribeThread"))
            .subscribe()
    }

    @PostConstruct
    fun init() { // Create topic
        val topic =
            snsClient.createTopic(CreateTopicRequest.builder().name(topicName).build())
        val (_: String, dlqArn: String) = createDeadLetterQueue(sqsClient, queueName)
        // Create main queue
        val (url: String, queueArn: String) = createQueue(sqsClient, queueName, dlqArn)
        queueUrl = url
        val subscribeRequest = SubscribeRequest.builder()
            .topicArn(topic.topicArn())
            .endpoint(queueArn) // For SQS protocol, the endpoint is queue ARN
            .protocol("sqs") // This is a required field
            .build()
        // Subscribe the queue with topic
        snsClient.subscribe(subscribeRequest)
    }

    private fun createDeadLetterQueue(sqsClient: SqsClient, queueName: String): Tuple2<String, String> {
        val createQueueRequest: CreateQueueRequest = CreateQueueRequest.builder()
            .queueName(queueName + DEAD_QUEUE_SUFFIX)
            .attributes(mapOf(QueueAttributeName.MESSAGE_RETENTION_PERIOD to "3600"))
            .build()
        return createSQSQueue(sqsClient, createQueueRequest)
    }

    private fun createQueue(sqsClient: SqsClient, queueName: String, deadLetterQueueARN: String)
            : Tuple2<String, String> {
        val reDrivePolicy =
            "{ \"maxReceiveCount\" : \"5\", \"deadLetterTargetArn\" : \"$deadLetterQueueARN\" }"
        val createQueueRequest = CreateQueueRequest.builder()
            .queueName(queueName)
            .attributes(
                mapOf(
                    QueueAttributeName.REDRIVE_POLICY to reDrivePolicy,   //in seconds
                    QueueAttributeName.VISIBILITY_TIMEOUT to "120"
                )
            )
            .build()
        return createSQSQueue(sqsClient, createQueueRequest)
    }

    private fun createSQSQueue(
        sqsClient: SqsClient,
        createQueueRequest: CreateQueueRequest
    ): Tuple2<String, String> {
        var queueUrl: String
        try {
            queueUrl = sqsClient.createQueue(createQueueRequest).queueUrl()
        } catch (e: QueueNameExistsException) {
            queueUrl = sqsClient.getQueueUrl(
                GetQueueUrlRequest.builder()
                    .queueName(createQueueRequest.queueName()).build()
            ).queueUrl()
            val queueAttributesRequest: SetQueueAttributesRequest = SetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributes(createQueueRequest.attributes())
                .build()
            sqsClient.setQueueAttributes(queueAttributesRequest)
        }
        val queueArn: String = getQueueARN(sqsClient, queueUrl)
        return Tuples.of(queueUrl, queueArn)
    }

    private fun getQueueARN(sqsClient: SqsClient, queueURL: String): String {
        val getQueueAttributesRequest: GetQueueAttributesRequest =
            GetQueueAttributesRequest
                .builder()
                .queueUrl(queueURL)
                .attributeNames(QueueAttributeName.QUEUE_ARN)
                .build()
        val queueAttributes: GetQueueAttributesResponse = sqsClient.getQueueAttributes(getQueueAttributesRequest)
        return queueAttributes.attributes()[QueueAttributeName.QUEUE_ARN]!!
    }

    private fun readSnsNotification(snsMessageBody: String): SnsNotification {
        return try {
            objectMapper.readValue(snsMessageBody, SnsNotification::class.java)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun deleteQueueMessage(receiptHandle: String, queueUrl: String) {
        try {
            val deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build()
            sqsClient.deleteMessage(deleteMessageRequest)
            LOGGER.info("Deleted queue message handle={}", receiptHandle)
        } catch (e: SqsException) {
            LOGGER.error("Error while deleting message from queue={}", receiptHandle, e)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SnsEventHandler::class.java)
        private const val DEAD_QUEUE_SUFFIX = "-dead"
    }

}