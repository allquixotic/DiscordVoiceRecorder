package com.bradhenry.discordvoicerecorder.aws

import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderProperties
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import java.io.File

class S3Uploader(private val properties: DiscordVoiceRecorderProperties) {
    fun uploadFile(file: File): String {
        val client = S3Client.builder().build()
        val putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.awsBucket)
                .key(file.name)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build()
        client.putObject(putObjectRequest, file.toPath())
        return getURL(properties.awsBucket, file.name)
    }

    private fun getURL(bucket: String, key: String): String {
        // https://bucket.s3.amazonaws.com/key
        return String.format("https://%s.s3.amazonaws.com/%s", bucket, key)
    }
}