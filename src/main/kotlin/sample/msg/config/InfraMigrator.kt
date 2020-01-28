package sample.msg.config

import org.springframework.stereotype.Component
import sample.msg.SnsProperties
import sample.msg.SqsProperties
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.CreateTopicRequest
import software.amazon.awssdk.services.sns.model.CreateTopicResponse
import software.amazon.awssdk.services.sns.model.SubscribeRequest
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName

@Component
class InfraMigrator(
        private val sqsProperties: SqsProperties,
        private val snsProperties: SnsProperties,
        private val sqsClient: SqsClient,
        private val snsClient: SnsClient
) {

    fun createMessagingInfra() {
        val deadLetterArn: String = createDeadLetterQueue(sqsProperties.queueName)

        val queueArn: String = createQueue(sqsProperties.queueName, deadLetterArn)

        val createTopicResponse: CreateTopicResponse =
                snsClient.createTopic(CreateTopicRequest.builder().name(snsProperties.topicName).build())

        val topicArn: String = createTopicResponse.topicArn()

        val subscribeRequest: SubscribeRequest = SubscribeRequest.builder()
                .protocol("sqs")
                .topicArn(topicArn)
                .endpoint(queueArn)
                .build()

        snsClient.subscribe(subscribeRequest)
    }


    fun createDeadLetterQueue(originalQueueName: String): String {
        val createQueueRequest: CreateQueueRequest = CreateQueueRequest
                .builder()
                .queueName(originalQueueName + "-dead")
                .attributes(mapOf(QueueAttributeName.MESSAGE_RETENTION_PERIOD to "3600"))
                .build()

        val createQueueResponse: CreateQueueResponse = sqsClient.createQueue(createQueueRequest)

        val deadLetterArn: String = sqsClient.getQueueAttributes(
                GetQueueAttributesRequest.builder()
                        .queueUrl(createQueueResponse.queueUrl())
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build())
                .attributes().get(QueueAttributeName.QUEUE_ARN)!!

        return deadLetterArn
    }


    fun createQueue(queueName: String, deadLetterArn: String): String {
        val redrivePolicy: String = """
            | {
            |   "maxReceiveCount": "5",
            |   "deadLetterTargetArn": "$deadLetterArn"
            | }
        """.trimMargin()

        val createQueueRequest: CreateQueueRequest = CreateQueueRequest
                .builder()
                .queueName(queueName)
                .attributes(mapOf(QueueAttributeName.REDRIVE_POLICY to redrivePolicy))
                .build()

        val createQueueResponse: CreateQueueResponse = sqsClient.createQueue(createQueueRequest)

        val queueArn: String = sqsClient.getQueueAttributes(
                GetQueueAttributesRequest.builder()
                        .queueUrl(createQueueResponse.queueUrl())
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build())
                .attributes().get(QueueAttributeName.QUEUE_ARN)!!

        return queueArn
    }


}