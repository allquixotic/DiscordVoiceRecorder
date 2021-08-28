package com.bradhenry.discordvoicerecorder.audiohandlers

import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderProperties
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import java.util.concurrent.ExecutorService
import net.dv8tion.jda.api.audio.CombinedAudio
import java.io.IOException
import net.dv8tion.jda.api.audio.UserAudio
import java.lang.Runnable
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult
import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.github.kokorin.jaffree.ffmpeg.UrlOutput
import com.bradhenry.discordvoicerecorder.audiohandlers.AudioReceiveHandlerImpl
import com.github.kokorin.jaffree.ffmpeg.UrlInput
import org.apache.commons.logging.LogFactory
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors

class AudioReceiveHandlerImpl(private val properties: DiscordVoiceRecorderProperties, private val file: File) : AudioReceiveHandler {
    private val outputStream: OutputStream
    private var canReceive = true
    private val executorService: ExecutorService
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
    fun shutdown() {
        executorService.submit {
            canReceive = false
            try {
                outputStream.flush()
                outputStream.close()
                val absolutePath = file.absolutePath
                val outputPath = absolutePath.substring(0, absolutePath.length - 3) + properties.recordingFormat
                val output = FFmpeg.atPath().addInput(UrlInput.fromUrl("file://$absolutePath")).addArguments("-ac", "2").addArguments("-ar", "48000").addArguments("-f", "s16be")
                        .setOverwriteOutput(true).addOutput(UrlOutput.toPath(Path.of(outputPath))).execute()
                Files.delete(file.toPath())
            } catch (e: IOException) {
                LOG.error("Error shutting down recording", e)
            }
        }
    }

    companion object {
        private val LOG = LogFactory.getLog("AudioReceiveHandlerImpl")
    }

    init {
        outputStream = FileOutputStream(file)
        executorService = Executors.newCachedThreadPool()
    }
}