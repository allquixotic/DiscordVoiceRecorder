package com.bradhenry.discordvoicerecorder.listeners

import com.bradhenry.discordvoicerecorder.aws.S3Uploader.uploadFile
import com.bradhenry.discordvoicerecorder.audiohandlers.AudioReceiveHandlerImpl.shutdown
import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderProperties
import java.util.concurrent.ExecutorService
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.Executors
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.Locale
import net.dv8tion.jda.api.entities.TextChannel
import java.lang.Runnable
import com.bradhenry.discordvoicerecorder.aws.S3Uploader
import java.io.IOException
import com.bradhenry.discordvoicerecorder.listeners.ChatListener
import net.dv8tion.jda.api.managers.AudioManager
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import com.bradhenry.discordvoicerecorder.audiohandlers.AudioReceiveHandlerImpl
import com.bradhenry.discordvoicerecorder.audiohandlers.SilentAudioSendHandlerImpl
import net.dv8tion.jda.api.entities.ChannelType
import org.apache.commons.logging.LogFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.function.Consumer

class ChatListener private constructor(private val properties: DiscordVoiceRecorderProperties, private val executorService: ExecutorService) : ListenerAdapter() {
    constructor(properties: DiscordVoiceRecorderProperties) : this(properties, Executors.newCachedThreadPool()) {}

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val message = event.message.contentRaw
        if (message == null || !message.startsWith(properties.commandCharacter)) {
            return
        }
        val lowerMessage = message.substring(properties.commandCharacter.length).lowercase(Locale.getDefault())
        if (lowerMessage.startsWith("start")) {
            startRecordCommand(event)
        } else if (lowerMessage.startsWith("end")) {
            endRecordCommand(event)
        } else if (lowerMessage.startsWith("upload")) {
            uploadRecordingCommand(event)
        }
    }

    private fun uploadRecordingCommand(event: MessageReceivedEvent) {
        val textChannel = event.textChannel ?: return
        executorService.submit {
            val fileUploader: Consumer<File>
            fileUploader = if (properties.isUseAWS) {
                Consumer { file: File? ->
                    val s3Uploader = S3Uploader(properties)
                    val url = s3Uploader.uploadFile(file!!)
                    textChannel.sendMessage(url).submit()
                }
            } else {
                Consumer { file: File -> textChannel.sendFile(file, file.name).submit() }
            }
            try {
                val list = Files.list(File(properties.recordingPath).toPath())
                list.filter { path: Path -> path.toString().endsWith(properties.recordingFormat) }
                        .map { obj: Path -> obj.toFile() }
                        .max(Comparator.comparing { obj: File -> obj.lastModified() })
                        .ifPresent(fileUploader)
            } catch (e: IOException) {
                LOG.error("Uploading recording failed", e)
            }
        }
    }

    private fun endRecordCommand(event: MessageReceivedEvent) {
        if (event.member == null || event.member!!.guild == null) {
            return
        }
        executorService.submit {
            val audioManager = event.member!!.guild.audioManager
            val receiveHandler = audioManager.receivingHandler
            if (receiveHandler is AudioReceiveHandlerImpl) {
                receiveHandler.shutdown()
            }
            audioManager.sendingHandler = null
            audioManager.receivingHandler = null
            audioManager.closeAudioConnection()
        }
    }

    private fun startRecordCommand(event: MessageReceivedEvent) {
        if (preventRecording(event)) {
            return
        }
        if (event.guild == null || event.member == null || event.member!!.voiceState == null || event.member!!.voiceState!!.channel == null) {
            return
        }
        val channel = event.member!!.voiceState!!.channel
        val audioManager = event.guild.audioManager
        val recordingFilePath = properties.recordingPath + System.currentTimeMillis() + ".raw"
        val recordingFile = File(recordingFilePath)
        try {
            Files.createFile(recordingFile.toPath())
        } catch (e: IOException) {
            LOG.error("Start recording failed, couldn't create new file", e)
            return
        }
        try {
            audioManager.receivingHandler = AudioReceiveHandlerImpl(properties, recordingFile)
            audioManager.sendingHandler = SilentAudioSendHandlerImpl()
            audioManager.openAudioConnection(channel)
        } catch (e: IOException) {
            LOG.error("Start recording failed", e)
        }
    }

    private fun preventRecording(event: MessageReceivedEvent): Boolean {
        return event.channelType != ChannelType.TEXT // should be publicly known that the bot is recording
    }

    companion object {
        private val LOG = LogFactory.getLog("ChatListener")
    }
}