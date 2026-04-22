package com.example.aplicacionlesaa

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.aplicacionlesaa.databinding.ActivityPdfViewerBinding
import java.io.File
import java.io.FileOutputStream

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var currentPageIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarPdf)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarPdf.setNavigationOnClickListener { finish() }

        binding.btnPrevPage.setOnClickListener {
            if (currentPageIndex > 0) {
                renderPage(currentPageIndex - 1)
            }
        }

        binding.btnNextPage.setOnClickListener {
            val total = pdfRenderer?.pageCount ?: 0
            if (currentPageIndex < total - 1) {
                renderPage(currentPageIndex + 1)
            }
        }

        openPdfFromRaw()
    }

    private fun openPdfFromRaw() {
        try {
            val outFile = File(cacheDir, "manual_muestreo.pdf")
            if (!outFile.exists()) {
                resources.openRawResource(R.raw.pop_lab_10_procedimiento_muestreo).use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            parcelFileDescriptor = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)

            if ((pdfRenderer?.pageCount ?: 0) > 0) {
                binding.tvPdfPending.visibility = View.GONE
                binding.ivPdfPage.visibility = View.VISIBLE
                binding.controlsContainer.visibility = View.VISIBLE
                renderPage(0)
            } else {
                showPending("No se encontraron paginas en el PDF.")
            }
        } catch (e: Exception) {
            showPending("No fue posible abrir el PDF: ${e.message}")
        }
    }

    private fun renderPage(index: Int) {
        val renderer = pdfRenderer ?: return
        if (index < 0 || index >= renderer.pageCount) return

        currentPage?.close()
        currentPage = renderer.openPage(index)

        val page = currentPage ?: return
        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        binding.ivPdfPage.setImageBitmap(bitmap)
        currentPageIndex = index
        binding.tvPageIndicator.text = "Pagina ${index + 1} de ${renderer.pageCount}"
        binding.btnPrevPage.isEnabled = index > 0
        binding.btnNextPage.isEnabled = index < renderer.pageCount - 1
    }

    private fun showPending(message: String) {
        binding.ivPdfPage.visibility = View.GONE
        binding.controlsContainer.visibility = View.GONE
        binding.tvPdfPending.visibility = View.VISIBLE
        binding.tvPdfPending.text = message
    }

    override fun onDestroy() {
        currentPage?.close()
        pdfRenderer?.close()
        parcelFileDescriptor?.close()
        currentPage = null
        pdfRenderer = null
        parcelFileDescriptor = null
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
