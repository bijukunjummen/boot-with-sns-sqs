package sample.msg

import org.springframework.boot.context.properties.ConfigurationProperties
import software.amazon.awssdk.regions.Region

@ConfigurationProperties(prefix = "aws.sns")
data class SnsProperties(
        var endpoint: String = "",
        var region: String = Region.US_EAST_1.id(),
        var topicName: String = ""
)