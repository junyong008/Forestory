package com.yjy.forestory.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {

    // 카메라 촬영 등에 사용할 temp Image Uri 생성
    // 카메라 촬영시 먼저 임시 파일을 만들어 생성하고, 해당 파일에 촬영 이미지를 넣는 방식으로 촬영된 사진의 화질을 보장.
    fun createTempImageFile(context: Context): Uri? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Forestory_$timeStamp.jpg"

        // 안드로이드 버전에 따른 임시 이미지 파일 생성 방식 변경
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Forestory")
            }
            return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        } else {

            val storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Forestory")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            val imageFile = File(storageDir, fileName)

            return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", imageFile)
        }
    }


    // 이미지의 Uri를 받아 내부 저장소(/data/data/com.yjy.forestory/files)에 복사 및 저장 후 내부 저장소 이미지의 Uri 반환
    fun copyImageToInternalStorage(context: Context, sourceUri: Uri): Uri? {
        // 파일 이름 생성
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "ForestoryUpload_$timeStamp.jpg"

        try {
            // 입력받은 Uri의 이미지 파일을 어플 내부 디렉터리로 복사
            val outputDir = context.filesDir
            val outputFile = File(outputDir, fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(outputFile).use { output ->
                    inputStream.copyTo(output)
                }
            }

            // 복사한 이미지의 Uri 반환
            return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", outputFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    // 유저 프로필은 별도로 저장하여 데이터 복원시 삭제 방지
    fun saveUserProfileToInternalStorage(context: Context, sourceUri: Uri): Uri? {
        // 파일 이름 생성
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "ForestoryProfile_$timeStamp.jpg"

        try {
            // 입력받은 Uri의 이미지 파일을 어플 내부 디렉터리로 복사
            val outputDir = context.filesDir
            val outputFile = File(outputDir, fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(outputFile).use { output ->
                    inputStream.copyTo(output)
                }
            }

            // 복사한 이미지의 Uri 반환
            return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", outputFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    // 서버 전송을 위한 Uri -> Multipart 변환 함수
    fun uriToMultipart(context: Context, uri: Uri): MultipartBody.Part? {
        try {
            // 원본 이미지를 캐시 디렉터리에 임시 파일로 저장
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalFile = File(context.cacheDir, "temp_file") // 임시 파일 생성
            FileOutputStream(originalFile).use { fileOutputStream ->
                inputStream?.copyTo(fileOutputStream)
            }
            inputStream?.close()

            // 임시 파일로부터 Bitmap 생성
            val bitmap = BitmapFactory.decodeFile(originalFile.path)

            // 이미지 해상도 조정 및 압축
            val resizedBitmap = resizeImage(bitmap, 512, 512)
            val compressedFile = File(context.cacheDir, "temp_compressed_file")
            optimizeImageForUpload(resizedBitmap, compressedFile)

            // 최적화된 이미지를 MultipartBody.Part로 변환
            val requestFile: RequestBody = compressedFile.asRequestBody("image/*".toMediaTypeOrNull())
            return MultipartBody.Part.createFormData("image", compressedFile.name, requestFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun optimizeImageForUpload(originalBitmap: Bitmap, outputFile: File) {
        var quality = 100
        val maxSizeBytes = 1 * 1024 * 1024 // 최대 크기 1MB

        do {
            val byteArrayOutputStream = FileOutputStream(outputFile)
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            val fileSize = outputFile.length()

            if (fileSize <= maxSizeBytes) break // 파일 크기가 조건을 만족하면 중단

            quality -= 5 // 품질을 점진적으로 줄임
            byteArrayOutputStream.close()
        } while (quality > 0)
    }

    private fun resizeImage(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val bitmapRatio = width.toFloat() / height.toFloat()
        val maxRatio = maxWidth.toFloat() / maxHeight.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (bitmapRatio > maxRatio) {
            newWidth = maxWidth
            newHeight = (maxWidth / bitmapRatio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * bitmapRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}