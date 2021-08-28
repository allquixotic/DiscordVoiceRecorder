@file:Suppress("UnstableApiUsage")

package com.bradhenry.discordvoicerecorder.audiohandlers

import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderProperties
import com.bradhenry.discordvoicerecorder.aws.S3Uploader
import com.bradhenry.discordvoicerecorder.tell
import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.github.kokorin.jaffree.ffmpeg.UrlOutput
import com.google.common.io.Files
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import net.dv8tion.jda.api.audio.CombinedAudio
import net.dv8tion.jda.api.audio.UserAudio
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.apache.commons.logging.LogFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer

class AudioReceiveHandlerImpl(private val properties: DiscordVoiceRecorderProperties, private val file: File) : AudioReceiveHandler {
    private val outputStream: OutputStream
    private var canReceive = true
    private val executorService: ExecutorService
    @Suppress("UnstableApiUsage")
    private val df : SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    override fun canReceiveCombined(): Boolean {
        return canReceive
    }

    override fun canReceiveUser(): Boolean {
        return false
    }

    override fun handleCombinedAudio(combinedAudio: CombinedAudio) {
        val audioData = combinedAudio.getAudioData(1.0)
        try {
            outputStream.write(audioData)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun handleUserAudio(userAudio: UserAudio) {}

    fun shutdown(event: MessageReceivedEvent) {
        executorService.submit {
            canReceive = false
            try {
                outputStream.flush()
                outputStream.close()
                val absolutePath = file.absolutePath
                val timestampString = Files.getNameWithoutExtension(absolutePath)
                val dt = Date(timestampString.toLong())
                val outputFileName =  event.textChannel.name.replace("[^a-zA-Z0-9]", "") + "-" + df.format(dt)
                val outputPath = Path.of(file.absoluteFile.parent, "${outputFileName}.${properties.recordingFormat}").toAbsolutePath().toString()
                FFmpeg.atPath().addArgument("-vn").addArguments("-ac", "2").addArguments("-ar", "48000")
                    .addArguments("-f", "s16be").addArguments("-i", "file://$absolutePath")
                    .setOverwriteOutput(true).addOutput(UrlOutput.toPath(Path.of(outputPath)))
                    .addArgument("-vn").addArguments("-acodec", "libopus").addArguments("-cutoff", "20000")
                    .addArguments("-b:a", "96k").addArguments("-frame_duration", "60")
                    .addArguments("-vbr", "constrained").addArguments("-application", "voip").execute()
                java.nio.file.Files.delete(file.toPath())

                val fileUploader: Consumer<File> = if (properties.isUseAWS) {
                    Consumer { file: File? ->
                        val s3Uploader = S3Uploader(properties)
                        val url = s3Uploader.uploadFile(file!!)
                        tell(event, url)
                    }
                } else {
                    Consumer { file: File -> event.textChannel.sendFile(file, file.name).submit() }
                }
                fileUploader.accept(File(outputPath))
            } catch (e: IOException) {
                LOG.error("Error shutting down recording", e)
                tell(event, "ERROR: Something broke during converting/uploading your recording. The recording data may exist on the bot server; you might want to contact the bot owner to see if it can be recovered for you.")
            }
        }
    }

    @Suppress("JAVA_CLASS_ON_COMPANION") companion object {private val LOG=LogFactory.getLog(javaClass.enclosingClass)}

    init {
        outputStream = FileOutputStream(file)
        executorService = Executors.newCachedThreadPool()
    }
}