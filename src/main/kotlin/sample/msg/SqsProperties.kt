package sample.msg

import org.springframework.boot.context.properties.ConfigurationProperties
import software.amazon.awssdk.regions.Region

@ConfigurationProperties(prefix = "aws.sqs")
data class SqsProperties(
        var endpoint: String = "",
        var region: String = "us-east-1",
        var queueName: String = ""
)