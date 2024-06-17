import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File
import java.time.LocalDate
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

class SendEmailWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val emailAddress = inputData.getString("emailAddress")
        val filePath = inputData.getString("filePath")
        val emailSubject = inputData.getString("subject")
        val messageText = inputData.getString("messageText")

        return if (emailAddress != null && filePath != null && emailSubject != null && messageText != null) {
            try {
                sendEmail(emailAddress, File(filePath), emailSubject, messageText)
                Result.success()
            } catch (e: Exception) {
                Log.e("SendEmailWorker", "Error al enviar el correo", e)
                Result.retry()
            }
        } else {
            Result.failure()
        }
    }

    private fun sendEmail(emailAddress: String, file: File,emailSubject : String, messageText: String) {
        val username = "raymundolarasandoval@gmail.com"
        val password = "hwmgsnaxyzaeqfuy"

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

        val today = LocalDate.now().toString()


        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(username))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress))
            subject = emailSubject
            //setText(messageText)
        }

        // Crea la parte del cuerpo del mensaje de texto
        val textBodyPart = MimeBodyPart().apply {
            setText(messageText)
        }

        // Crea la parte del cuerpo del mensaje con el archivo adjunto
        val fileBodyPart = MimeBodyPart().apply {
            attachFile(file)
        }

        // Crea el multipart y agrega las partes del cuerpo del mensaje
        val multipart = MimeMultipart().apply {
            addBodyPart(textBodyPart)
            addBodyPart(fileBodyPart)
        }

        message.setContent(multipart)

        Transport.send(message)
        Log.i("SendEmailWorker", "Correo enviado exitosamente")
    }
}
