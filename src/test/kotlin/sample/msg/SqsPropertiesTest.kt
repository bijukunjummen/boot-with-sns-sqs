package sample.msg

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
        classes = [SqsProperties::class],
        properties = ["aws.sqs.endpoint=http://some-test-end-point", "aws.sqs.region=sample-region"]
)
@EnableConfigurationProperties(value = [SqsProperties::class])
class SqsPropertiesTest {

    @Autowired
    lateinit var sqsProperties: SqsProperties

    @Test
    fun testSqsProperties() {
        assertThat(sqsProperties.endpoint).isEqualTo("http://some-test-end-point")
        assertThat(sqsProperties.region).isEqualTo("sample-region")
    }
}