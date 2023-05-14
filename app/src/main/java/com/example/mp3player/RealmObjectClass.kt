package com.example.mp3player

import android.graphics.Bitmap
import android.net.Uri
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class MusicInfo : RealmObject(){
    @PrimaryKey
    var id : Int = 0
    var title : String = ""
    var musicPath : String = ""
    var autoSound : Int = 40
    var lyrics : String = ""
    var pictureBitmapByte : ByteArray = byteArrayOf()
}

open class MusicListInfo : RealmObject(){
    @PrimaryKey
    var id : Int = 0
    var title : String = ""
    var musicList : String = "" //format -> {TITLE1,TITLE2,TITLE3}
}

open class MusicListTemp : RealmObject(){
    @PrimaryKey
    var id : Int = 0
    var title : String = ""
}