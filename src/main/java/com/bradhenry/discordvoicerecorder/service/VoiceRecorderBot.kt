package com.bradhenry.discordvoicerecorder.service

import org.springframework.beans.factory.annotation.Autowired
import com.bradhenry.discordvoicerecorder.DiscordVoiceRecorderProperties
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import java.lang.InterruptedException
import java.lang.RuntimeException
import javax.security.auth.login.LoginException
import com.bradhenry.discordvoicerecorder.listeners.ChatListener
import org.springframework.stereotype.Service

@Service
class VoiceRecorderBot @Autowired internal constructor(properties: DiscordVoiceRecorderProperties) {
    init {
        val bot: JDA
        bot = try {
            JDABuilder.createDefault(properties.botToken)
                    .setAutoReconnect(true)
                    .build()
                    .awaitReady()
        } catch (e: InterruptedException) {
            throw RuntimeException("Couldn't initialize bot, startup failed", e)
        } catch (e: LoginException) {
            throw RuntimeException("Couldn't initialize bot, startup failed", e)
        }
        val chatListener = ChatListener(properties)
        bot.addEventListener(chatListener)
    }
}