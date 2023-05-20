package com.example.mp3player

import android.net.Uri
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

class MusicPathRelationClass : AppCompatActivity() {
    fun getFilePathList(): MutableList<String> {
        val filesPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        val fileList = File(filesPath).list()
        val fileSize = fileList?.size
        val mp3FileList:  MutableList<String> = mutableListOf()

        for(i in 0 until fileSize!!){
            try {
                if (fileList[i].endsWith(".mp3")) {
                    mp3FileList += (fileList[i].toString() + '\n')
                    mp3FileList.sort()
                }
            }
            catch(e: Exception){ }
            mp3FileList.sort()
        }
        return mp3FileList
    }

    fun getMusicFile(musicName: String?): File{
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .path + "/" + musicName
        )
    }

    fun getMusicUri(musicName: String?): Uri{
        return onSuffix(
            Uri.fromFile(
                File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS).path + "/" + musicName
                )
            )
        )
    }

    fun getMusicString(musicName : String?): String{
        return onSuffix(
            Uri.fromFile(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        .path + "/" + musicName
                )
            )
        ).toString()
    }

    fun getMusicPath(musicName: String?): Path{
        return Path(
            onSuffix(
                Uri.fromFile(
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .path + "/" + musicName
                    )
                )
            ).toString()
        )
    }

    private fun onSuffix(musicUri: Uri): Uri {
        var musicTemp = musicUri.toString()
        musicTemp = musicTemp.replace("%0A","")
        musicTemp = musicTemp.replace("/file%3A","")
        musicTemp = musicTemp.replace("%2520","%20")
        return Uri.parse(musicTemp)
    }

    fun onSuffixBlank(title: String): String{
        var temp = title
        temp = temp.replace(" ","%20")
        temp = temp.replace("　","%20")
        return temp
    }
}