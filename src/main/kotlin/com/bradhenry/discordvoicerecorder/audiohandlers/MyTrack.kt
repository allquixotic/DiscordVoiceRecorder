package com.bradhenry.discordvoicerecorder.audiohandlers

import com.sedmelluq.discord.lavaplayer.track.AudioTrack

data class MyTrack(val name: String, var track: AudioTrack?, val loopIt: Boolean )
