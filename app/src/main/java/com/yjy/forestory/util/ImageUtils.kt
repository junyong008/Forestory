package com.yjy.forestory.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// Singleton으로 의존성을 주입하며, contentResolver의 의존성을 주입받아 사용한다.
// 자주 호출될 수 있다고 판단해 Singleton으로 구현.
@Singleton
class ImageUtils @Inject constructor(@ApplicationContext context: Context, private val contentResolver: ContentResolver) {

    private val context = context

    // 카메라 촬영 등에 사용할 temp Image Uri 생성
    // 카메라 촬영시 먼저 임시 파일을 만들어 생성하고, 해당 파일에 촬영 이미지를 넣는 방식으로 촬영된 사진의 화질을 보장.
    fun createTempImageFile(): Uri? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "Forestory_$timeStamp.jpg"

        // 안드로이드 버전에 따른 임시 이미지 파일 생성 방식 변경
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Forestory")
            }
            return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
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
    fun copyImageToInternalStorage(sourceUri: Uri): Uri? {
        // 파일 이름 생성
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "ForestoryUpload_$timeStamp.jpg"

        try {
            // 입력받은 Uri의 이미지 파일을 어플 내부 디렉터리로 복사
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val outputDir = context.filesDir
            val outputFile = File(outputDir, fileName)
            FileOutputStream(outputFile).use { output ->
                inputStream?.copyTo(output)
            }

            // 복사한 이미지의 Uri 반환
            return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", outputFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }
}