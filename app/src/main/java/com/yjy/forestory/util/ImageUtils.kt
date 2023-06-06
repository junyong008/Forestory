package com.yjy.forestory.util

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// Singleton으로 의존성을 주입하며, contentResolver의 의존성을 주입받아 사용한다.
// 자주 호출될 수 있다고 판단해 Singleton으로 구현.
@Singleton
class ImageUtils @Inject constructor(private val contentResolver: ContentResolver) {



    // 카메라 촬영 등에 사용할 temp Image Uri 생성
    // 카메라 촬영시 먼저 임시 파일을 만들어 생성하고, 해당 파일에 촬영 이미지를 넣는 방식으로 촬영된 사진의 화질을 보장.
    fun createTempImageFile(): Uri? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Forestory_${timeStamp}_")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Forestory")
        }

        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    fun uriToCompressedBitmap(uri: Uri, quality: Int): Bitmap? {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = inputStream?.use {
            BitmapFactory.decodeStream(it)
        }

        // 비트맵을 JPEG로 압축하여 파일 크기를 줄임
        val outputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        // 압축된 이미지를 다시 로드하여 최종 비트맵 반환
        val compressedBitmap = BitmapFactory.decodeStream(ByteArrayInputStream(outputStream.toByteArray()))

        return compressedBitmap
    }
}