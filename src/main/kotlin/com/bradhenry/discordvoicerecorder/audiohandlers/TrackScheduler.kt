package com.bradhenry.discordvoicerecorder.audiohandlers

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderApplication.Companion.tracks
import org.apache.commons.logging.LogFactory

class TrackScheduler(private val player : AudioPlayer)
                    : AudioEventAdapter(), AudioSendHandler {
    private val queue : BlockingQueue<AudioTrack> = LinkedBlockingQueue()
    private val frame: MutableAudioFrame = MutableAudioFrame()
    private val buffer: ByteBuffer = ByteBuffer.allocate(1024)

    init {
        frame.setBuffer(buffer)
    }

    fun queue(track: AudioTrack) {
        if(!player.startTrack(track, true)) {
            queue.offer(track)
        }
    }

    private fun nextTrack() {
        val po = queue.poll()
        if(po != null) LOG.debug("Playing ${po.identifier}")
        player.startTrack(po, false)
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if(endReason.mayStartNext) {
            if(tracks.values.find { it.track!!.identifier == track.identifier }!!.loopIt) {
                tracks[track.identifier]!!.track = track.makeClone()
                queue(tracks[track.identifier]!!.track!!)
            }
            nextTrack()
        }
    }

    //AudioSendHandler

    override fun canProvide(): Boolean {
        return player.provide(frame)
    }

    override fun provide20MsAudio(): ByteBuffer? {
        return buffer.flip()
    }

    override fun isOpus(): Boolean {
        return true
    }

    @Suppress("JAVA_CLASS_ON_COMPANION") companion object {private val LOG=LogFactory.getLog(javaClass.enclosingClass)}
}