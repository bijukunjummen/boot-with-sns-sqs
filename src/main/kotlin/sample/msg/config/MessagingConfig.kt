package sample.msg.config

import com.fasterxml.jackson.databind.ObjectMapper
import sample.msg.SnsEventHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.StringUtils
import sample.msg.SnsProperties
import sample.msg.SqsProperties
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.SnsClientBuilder
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.SqsClientBuilder
import java.net.URI

@Configuration
class MessagingConfig {
    @Bean
    fun snsClient(snsProperties: SnsProperties): SnsClient {
        val snsClientBuilder: SnsClientBuilder = SnsClient
            .builder()
            .region(Region.of(snsProperties.region))

        if (StringUtils.hasText(snsProperties.endpoint)) {
            snsClientBuilder.endpointOverride(URI.create(snsProperties.endpoint))
        }
        return snsClientBuilder.build()
    }

    @Bean
    fun sqsClient(sqsProperties: SqsProperties): SqsClient {
        val sqsClientBuilder: SqsClientBuilder = SqsClient
            .builder()
            .region(Region.of(sqsProperties.region))

        if (StringUtils.hasText(sqsProperties.endpoint)) {
            sqsClientBuilder.endpointOverride(URI.create(sqsProperties.endpoint))
        }

        return sqsClientBuilder.build()
    }

    @Bean
    fun sqsAsyncClient(sqsProperties: SqsProperties): SqsAsyncClient {
        val sqsAsyncClientBuilder: SqsAsyncClientBuilder = SqsAsyncClient
            .builder()
            .region(Region.of(sqsProperties.region))

        if (StringUtils.hasText(sqsProperties.endpoint)) {
            sqsAsyncClientBuilder.endpointOverride(URI.create(sqsProperties.endpoint))
        }

        return sqsAsyncClientBuilder.build()
    }

    @Bean
    fun sampleListener(
        snsClient: SnsClient,
        sqsClient: SqsClient,
        objectMapper: ObjectMapper,
        sqsProperties: SqsProperties,
        snsProperties: SnsProperties
    ): SnsEventHandler {
        return SnsEventHandler(
            snsClient,
            sqsClient,
            objectMapper,
            snsProperties.topicName,
            sqsProperties.queueName
        )
    }
}