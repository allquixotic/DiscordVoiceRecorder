package com.bradhenry.discordvoicerecorder

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.lang.NonNull
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix="dvr")
@Validated
class DiscordVoiceRecorderProperties {
    @get:NonNull
    @NonNull
    var botToken: String? = null

    @get:NonNull
    @NonNull
    var commandCharacter: String? = null

    var channels: Array<String>? = null

    @NonNull
    var recordingPath: String? = null

    @NonNull
    var recordingFormat: String? = null
    var isUseAWS = false
    var awsBucket: String? = null

    var awsEndpoint: String? = null
    var awsRegion: String? = null
    var awsAccessKey: String? = null
    var awsSecretKey: String? = null
}