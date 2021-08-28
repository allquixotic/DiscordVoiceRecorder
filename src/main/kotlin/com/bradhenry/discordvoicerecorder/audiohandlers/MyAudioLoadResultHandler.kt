package com.bradhenry.discordvoicerecorder.audiohandlers

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderApplication.Companion.tracks

class MyAudioLoadResultHandler : AudioLoadResultHandler{
    override fun trackLoaded(track: AudioTrack?) {
        tracks[track!!.identifier]?.track = track
    }

    override fun playlistLoaded(playlist: AudioPlaylist?) {
        playlist?.tracks?.forEach { trackLoaded(it) }
    }

    override fun noMatches() {
        TODO("Not yet implemented")
    }

    override fun loadFailed(exception: FriendlyException?) {
        TODO("Not yet implemented")
    }
}