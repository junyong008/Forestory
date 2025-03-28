package com.yjy.forestory.feature.crop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageOptions
import com.yjy.forestory.databinding.ActivityCropBinding
import com.yjy.forestory.util.getParcelableCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri = intent.getParcelableCompat<Uri>(EXTRA_IMAGE_URI)
        val cropOptions = intent.getParcelableCompat<CropImageOptions>(EXTRA_CROP_OPTIONS)

        if (uri == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        cropOptions?.let { binding.cropImageView.setImageCropOptions(it) }
        binding.cropImageView.setImageUriAsync(uri)

        binding.buttonCrop.setOnClickListener {
            binding.cropImageView.setOnCropImageCompleteListener { _, result ->
                if (!result.isSuccessful) {
                    setResult(RESULT_CANCELED)
                    finish()
                    return@setOnCropImageCompleteListener
                }

                val croppedUri = result.uriContent
                val intent = Intent().apply {
                    putExtra(EXTRA_CROPPED_URI, croppedUri)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
            binding.cropImageView.croppedImageAsync()
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_CROP_OPTIONS = "extra_crop_options"
        const val EXTRA_CROPPED_URI = "extra_cropped_uri"
    }
}
