package sample.msg.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SnsNotification (
    @field:JsonProperty("Message")
    val message:String
)