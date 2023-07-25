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
import java.util.*

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
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalFile = File(context.cacheDir, "temp_file") // 캐시 디렉토리에 임시 파일 생성

            val outputStream = FileOutputStream(originalFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output) // InputStream에서 OutputStream으로 데이터 복사
                }
            }

            // 이미지 압축
            val compressedFile = File(context.cacheDir, "temp_compressed_file")
            val bitmap = BitmapFactory.decodeFile(originalFile.path)

            var quality = 100
            val maxSizeBytes = 1 * 1024 * 1024 // 1MB를 최대 크기로 지정하고 압축

            // 원하는 파일 크기가 될때까지 quality를 줄여가면서 압축
            var streamLength = maxSizeBytes
            while (streamLength >= maxSizeBytes) {
                val fileOutputStream = FileOutputStream(compressedFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream)
                fileOutputStream.close()

                streamLength = compressedFile.length().toInt()
                quality -= 5
            }

            val requestFile: RequestBody = compressedFile.asRequestBody("image/*".toMediaTypeOrNull())
            return MultipartBody.Part.createFormData("image", compressedFile.name, requestFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}