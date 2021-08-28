package com.bradhenry.discordvoicerecorder

import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderApplication.Companion.playerManager
import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderApplication.Companion.tracks
import com.bradhenry.discordvoicerecorder.audiohandlers.MyAudioLoadResultHandler
import com.bradhenry.discordvoicerecorder.audiohandlers.MyTrack
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import java.io.File
import java.util.*

@SpringBootApplication
@EnableConfigurationProperties(DiscordVoiceRecorderProperties::class)
open class DiscordVoiceRecorderApplication {
    companion object {
        val playerManager = DefaultAudioPlayerManager()
        val tracks: MutableMap<String, MyTrack> = Collections.synchronizedMap(LinkedHashMap())
    }
}

fun tell(event: MessageReceivedEvent, what: String) {
    event.textChannel.sendMessage(event.author.asMention + " $what").queue()
}

fun tellFile(event: MessageReceivedEvent, what: String, data: File) {
    tell(event, what)
    event.textChannel.sendFile(data).queue()
}

fun main(args: Array<String>) {
    val malr = MyAudioLoadResultHandler()
    tracks["mtg.mp3"] = MyTrack("mtg.mp3", null, false)
    AudioSourceManagers.registerRemoteSources(playerManager)
    playerManager.registerSourceManager(LocalAudioSourceManager())
    playerManager.loadItem("mtg.mp3", malr)
    SpringApplication.run(DiscordVoiceRecorderApplication::class.java, *args)
}