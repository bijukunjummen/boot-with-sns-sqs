package sample.msg

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
        classes = [SnsProperties::class],
        properties = ["aws.sns.endpoint=http://some-test-end-point", "aws.sns.region=sample-region"]
)
@EnableConfigurationProperties(value = [SqsProperties::class])
class SnsPropertiesTest {

    @Autowired
    lateinit var snsProperties: SnsProperties

    @Test
    fun testSnsProperties() {
        assertThat(snsProperties.endpoint).isEqualTo("http://some-test-end-point")
        assertThat(snsProperties.region).isEqualTo("sample-region")
    }
}