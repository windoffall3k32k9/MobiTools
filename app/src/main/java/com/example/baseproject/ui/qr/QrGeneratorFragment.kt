package com.example.baseproject.ui.qr

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentQrGeneratorBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

/**
 * Công cụ tạo mã QR ngoại tuyến từ văn bản và hỗ trợ lưu vào thư viện hoặc chia sẻ.
 */
class QrGeneratorFragment : BaseToolFragment<FragmentQrGeneratorBinding>() {

    override val toolId: String = "qr_generator"
    override val toolTitleResId: Int = R.string.tool_qr_generator_title

    private var generatedBitmap: Bitmap? = null

    override fun provideViewBinding(container: ViewGroup?): FragmentQrGeneratorBinding {
        return FragmentQrGeneratorBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        viewBinding.btnGenerate.setOnClickListener {
            val text = viewBinding.etQrInput.text?.toString()?.trim() ?: ""
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateQrCode(text)
        }

        viewBinding.btnSaveQr.setOnClickListener {
            saveQrToGallery()
        }

        viewBinding.btnShareQr.setOnClickListener {
            shareQrImage()
        }
    }

    // Sinh ảnh bitmap mã QR chuẩn từ chuỗi văn bản đầu vào.
    private fun generateQrCode(content: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            generatedBitmap = bitmap
            viewBinding.ivQrCode.setImageBitmap(bitmap)
            viewBinding.cardQrPreview.visibility = View.VISIBLE
            viewBinding.layoutQrActions.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi khi tạo mã: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Lưu ảnh mã QR vào thư viện hình ảnh qua MediaStore.
    private fun saveQrToGallery() {
        val bitmap = generatedBitmap ?: return
        val resolver = requireContext().contentResolver
        val filename = "QR_${System.currentTimeMillis()}.png"

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                Toast.makeText(requireContext(), R.string.qr_saved_successfully, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi khi lưu ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Chia sẻ tệp ảnh mã QR tới các ứng dụng khác qua Intent.
    private fun shareQrImage() {
        val bitmap = generatedBitmap ?: return
        try {
            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "qr_share.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ mã QR qua"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi chia sẻ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
