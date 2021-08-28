package com.bradhenry.discordvoicerecorder.audiohandlers

import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

class SilentAudioSendHandlerImpl : AudioSendHandler {
    override fun canProvide(): Boolean {
        return true
    }

    override fun provide20MsAudio(): ByteBuffer? {
        return ByteBuffer.wrap(byteArrayOf(0xF8.toByte(), 0xFF.toByte(), 0xFE.toByte())) // Opus silence
    }

    override fun isOpus(): Boolean {
        return true
    }
}