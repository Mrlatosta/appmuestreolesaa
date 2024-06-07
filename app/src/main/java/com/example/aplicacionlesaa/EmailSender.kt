package com.example.aplicacionlesaa

import android.os.AsyncTask
import android.util.Log
import java.io.File
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart


     class SendEmailTask(val emailAddress: String, val file: File) : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            try {
                val username = "raymundolarasandoval@gmail.com"
                val password = "izjbdqovigvjtlra"

                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(username, password)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(username))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress))
                    subject = "Muestras PDF"
                    setText("Adjunto el PDF con las muestras.")
                }

                val mimeBodyPart = MimeBodyPart().apply {
                    attachFile(file)
                }

                val multipart = MimeMultipart().apply {
                    addBodyPart(mimeBodyPart)
                }

                message.setContent(multipart)

                Transport.send(message)
                Log.i("SendEmail", "Correo enviado exitosamente")
            } catch (e: Exception) {
                Log.e("SendEmail", "Error al enviar el correo", e)
            }
            return null
        }
    }
