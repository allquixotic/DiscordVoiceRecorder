package com.bradhenry.discordvoicerecorder.aws

import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderProperties
import org.apache.commons.logging.LogFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import java.io.File
import java.net.URI

class S3Uploader(private val properties: DiscordVoiceRecorderProperties) {

    private val credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.awsAccessKey, properties.awsSecretKey))
    private val client = S3Client.builder().credentialsProvider(credentials).region(Region.of(properties.awsRegion)).endpointOverride(URI.create(properties.awsEndpoint!!)).build()

    fun uploadFile(file: File): String {
        val putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.awsBucket)
                .key(file.name)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build()
        client.putObject(putObjectRequest, file.toPath())
        val theurl = getURL(properties.awsBucket, file.name)
        LOG.debug("Successfully uploaded to S3: $theurl")
        return theurl
    }

    private fun getURL(bucket: String?, key: String): String {
        return String.format("%s/%s/%s", properties.awsEndpoint, bucket, key)
    }

    @Suppress("JAVA_CLASS_ON_COMPANION") companion object {private val LOG=LogFactory.getLog(javaClass.enclosingClass)}
}