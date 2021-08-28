package com.bradhenry.discordvoicerecorder.listeners

import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderApplication.Companion.playerManager
import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderApplication.Companion.tracks
import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderProperties
import com.bradhenry.discordvoicerecorder.audiohandlers.AudioReceiveHandlerImpl
import com.bradhenry.discordvoicerecorder.audiohandlers.TrackScheduler
import com.bradhenry.discordvoicerecorder.tell
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.apache.commons.logging.LogFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ChatListener private constructor(private val properties: DiscordVoiceRecorderProperties, private val executorService: ExecutorService) : ListenerAdapter() {
    constructor(properties: DiscordVoiceRecorderProperties) : this(properties, Executors.newCachedThreadPool())

    private var isRecording = AtomicBoolean(false)
    private var startEvent: MessageReceivedEvent? = null

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val message = event.message.contentRaw
        if ((properties.channels != null && !properties.channels!!.contains(event.channel.id)) || !message.startsWith(properties.commandCharacter!!)) {
            return
        }
        val lowerMessage = message.substring(properties.commandCharacter!!.length).lowercase(Locale.getDefault()).trim()
        if (lowerMessage == "startmtg") {
            startRecordCommand(event)
        } else if (lowerMessage == "endmtg") {
            endRecordCommand(event)
        }
    }

    private fun endRecordCommand(event: MessageReceivedEvent) {
        if (event.member == null) {
            return
        }
        if(!isRecording.get()) {
            tell(event, "ERROR: I'm not currently recording.")
            return
        }

        if(startEvent != null && (event.author.idLong != startEvent!!.author.idLong && event.textChannel.idLong != startEvent!!.textChannel.idLong)) {
            tell(event, "ERROR: You are not in the same channel as the person who started the meeting, and you didn't start the meeting, so you can't end it.")
            return
        }


        executorService.submit {
            val audioManager = event.member!!.guild.audioManager
            val receiveHandler = audioManager.receivingHandler
            if (receiveHandler is AudioReceiveHandlerImpl) {
                receiveHandler.shutdown(event)
            }
            audioManager.sendingHandler = null
            audioManager.receivingHandler = null
            audioManager.closeAudioConnection()
            isRecording.set(false)
        }
    }

    private fun startRecordCommand(event: MessageReceivedEvent) {
        if (preventRecording(event) || event.member == null) {
            return
        }
        if(event.member?.voiceState == null || event.member?.voiceState?.channel == null) {
            tell(event, "ERROR: You are not currently in voice.")
            return
        }
        if(!isRecording.compareAndSet(false, true)) {
            tell(event, "ERROR: This bot is already recording somewhere, and only supports recording in one room at a time right now. You'll have wait for the current recording to end. Sorry!")
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
            tell(event, "ERROR: Couldn't create file when trying to record meeting!")
            isRecording.set(false)
            return
        }
        try {
            audioManager.receivingHandler = AudioReceiveHandlerImpl(properties, recordingFile)
            val audioPlayer = playerManager.createPlayer()
            val scheduler = TrackScheduler(audioPlayer)
            audioPlayer.addListener(scheduler)
            audioManager.sendingHandler = scheduler
            audioManager.openAudioConnection(channel)
            event.message.reply("Recording started in ${event.member!!.voiceState!!.channel!!.name}.").submit()
            tracks.values.forEach { scheduler.queue(it.track!!.makeClone()) }
        } catch (e: Exception) {
            LOG.error("Start recording failed", e)
            tell(event, "ERROR: Something went wrong trying to record the meeting. I may not have permissions to speak and listen in the channel you're in.")
            isRecording.set(false)
        }
        startEvent = event
    }

    private fun preventRecording(event: MessageReceivedEvent): Boolean {
        return event.channelType != ChannelType.TEXT // should be publicly known that the bot is recording
    }

    @Suppress("JAVA_CLASS_ON_COMPANION") companion object {private val LOG=LogFactory.getLog(javaClass.enclosingClass)}
}