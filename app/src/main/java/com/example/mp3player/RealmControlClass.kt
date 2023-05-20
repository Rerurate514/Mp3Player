package com.example.mp3player

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.mp3player.RealmControlClass.Companion.NullMusicData
import io.realm.Realm
import io.realm.RealmObject
import java.io.ByteArrayOutputStream

class RealmControlClass : AppCompatActivity() {
    private var realm: Realm = Realm.getDefaultInstance()

    companion object {
        const val Id = "id"
        const val Title = "title"
        const val MusicPath = "musicPath"
        const val AutoSound = "autoSound"
        const val Lyrics = "lyrics"
        const val PictureBitmapByte = "pictureBitmapByte"

        const val MusicList = "musicList"

        val NullMusicData = MusicData("", "", 40, "", null)
        val MusicInfoIns = RealmControlClass().MusicInfoControl()
        val MusicListIns = RealmControlClass().MusicListControl()
        val MusicTempIns = RealmControlClass().MusicTempControl()
    }

    private fun getObj(id: Int, realmObject: RealmObject) : RealmObject?{
        return realm.where(realmObject::class.java)
                .equalTo(Id, id)
                .findFirst()
    }

    private fun getObj(title: String, realmObject: RealmObject) : RealmObject?{
        return realm.where(realmObject::class.java)
            .equalTo(Title, title)
            .findFirst()
    }

    fun musicPathString(): String { return MusicPath }
    fun autoSoundString(): String { return AutoSound }
    fun lyricsString(): String { return Lyrics }
    fun musicListString(): String { return MusicList }
    fun pictureBitmapByteString(): String { return PictureBitmapByte }

    private fun destroyRealm(functionName: String) {
        Log.d("realmClose", "Realm -> Close // by $functionName")
        realm.close()
    }

    fun insertRealmInfo(id: Int, title: String, anyValue: Any, max: Int, realmObject: RealmObject) {
        realm.executeTransaction { realm
            var obj = getObj(id, realmObject)

            if (obj == null) obj = createRealmObject(id, realmObject)

            when (realmObject) {
                is MusicInfo -> MusicInfoIns.objInit(id, title, anyValue as String)
                is MusicListInfo -> MusicListIns.objInit(id, title, anyValue as String)
                is MusicListTemp -> MusicTempIns.objInit(id,title,null)
            }

            maxAboveDelete(obj, id, max)

            val realmMax = realm.where(realmObject::class.java).max(Id)
            Log.d("realmCheck", "id = $id\ttitle = $title\nby$realmObject")
            Log.d("realmObj", obj.toString())
            Log.d("realmMax", "max = $max realmMax = $realmMax")
        }
    }

    private fun createRealmObject(id: Int, realmObject: RealmObject): RealmObject {
        Log.d("realmCreate", "Realm -> Create : $realmObject // id = $id")
        realm.createObject(realmObject::class.java, id)
        Log.d("realmCreateSuc", "Realm -> Create >> Success : $realmObject // id = $id")
        return getObj(id,realmObject)!!
    }

    fun deleteRealmObjectInTitle(realmObject: RealmObject, title: String) {
        realm.executeTransaction {
            realm
            try {
                val obj = getObj(title,realmObject)!!

                obj.deleteFromRealm()
                Log.d("realmDelete", "realm -> Delete >> Success // $obj")
            } catch (e: Exception) { }

            //destroyRealm("deleteRealmObjectInTitle")
        }
        alignRealmObject(realmObject)
    }

    fun deleteRealmObjectInId(realmObject: RealmObject, id: Int) {
        realm.executeTransaction {
            realm
            try {
                val obj = getObj(id, realmObject)!!

                obj.deleteFromRealm()
                Log.d("realmDelete", "realm -> Delete >> Success // $obj")
            } catch (e: Exception) { }

            //destroyRealm("deleteRealmObjectInId")
        }
        alignRealmObject(realmObject)
    }

    fun alignRealmObject(realmObject: RealmObject) {
        val max = getMaxRealmData(1, realmObject) + 1
        for (i in 0 until max) {
            val obj = getObj(i,realmObject)
            var objFun = getObj(i,realmObject)

            if (obj == null) {
                for (j in 0 until max) {
                    objFun = realm.where(realmObject::class.java)
                        .equalTo(Id, i + j)
                        .findFirst()

                    if (objFun != null) break
                }
                dataBack(i, objFun, realmObject)
            }

            if(obj is MusicInfo && obj.title == "") {
                realm.executeTransaction {
                    obj.deleteFromRealm()
                }
            }

            Log.d("realmAligning", "realm -> aligning... $i times")
        }
        Log.d("realmAlignSuc", "realm -> align >> Success")
    }

    private fun dataBack(i: Int, objFun: RealmObject?, realmObject: RealmObject) {
        var temp = DataMoveTemp("", "")
        var obj: RealmObject

        if (objFun == null) return

        realm.executeTransaction { realm
            when (realmObject) {
                is MusicListInfo -> {
                    obj = createRealmObject(i, realmObject)
                    temp = DataMoveTemp((objFun as MusicListInfo).title, objFun.musicList)
                    (obj as MusicListInfo).title = temp.title
                    (obj as MusicListInfo).musicList = temp.value
                    objFun.deleteFromRealm()
                }
                is MusicListTemp -> {
                    obj = createRealmObject(i, realmObject)
                    temp = DataMoveTemp((objFun as MusicListTemp).title, "null")
                    (obj as MusicListTemp).title = temp.title
                    objFun.deleteFromRealm()
                }
            }
            Log.d("realmDataMove", "realm -> dataMove >> ${temp.title} : ${temp.value}")
        }
    }

    private fun maxAboveDelete(realmObject: RealmObject, id: Int, max: Int) {
        if (max < id + 1) {
            realm.executeTransaction { realm
                realm.where(realmObject::class.java)
                    .greaterThanOrEqualTo(Id, id)
                    .findAll()
                    .deleteAllFromRealm()
                Log.d("realmMaxDelete", "Realm -> Max.Delete : $realmObject")
            }
        }
    }

    fun fetchFile(){
        val musicInfo = MusicInfo()
        val musicPathList = MusicPathRelationClass().getFilePathList()
        var realmMax = getMaxRealmData(0,musicInfo)

        musicPathList.sort()

        alignRealmObject(musicInfo)

        for(i in 0 until musicPathList.size){
            val obj = realm.where(MusicInfo::class.java)
                .equalTo(Id, i)
                .findFirst()

            if(obj == null) {
                Log.d("fetch","newCreateRealm")
                newCreateRealm()
                return
            }

            Log.d("fetch","max = $realmMax ?== ${musicPathList.size}")

            if(realmMax == musicPathList.size) return

            Log.d("fetch","continue")

            if(musicPathList[i] != obj.title){
                realm.executeTransaction{ realm
                    createRealmObject(realmMax + 1,musicInfo)
                }

                Log.d("fetch","differentFrame : ${musicPathList[i]} ,arrayTimeFrame : $i")

                realmMax = getMaxRealmData(0,musicInfo)

                for(j in realmMax - 1 downTo i step 1){
                    dataFront(j)
                    Log.d("fetch", "dataFront : $j times")
                }

                realm.executeTransaction{ realm
                    obj.title = musicPathList[i]
                    obj.musicPath = MusicPathRelationClass().getMusicString(musicPathList[i])
                }
            }
        }
    }

    private fun dataFront(i : Int){
        try {
            val objTop = realm.where(MusicInfo::class.java)
                .equalTo(Id, i + 1)
                .findFirst()!!

            val objCentral = realm.where(MusicInfo::class.java)
                .equalTo(Id, i)
                .findFirst()!!

            val objBottom = realm.where(MusicInfo::class.java)
                .equalTo(Id, i - 1)
                .findFirst()!!

            realm.executeTransaction {
                realm
                objTop.title = objCentral.title
                objTop.musicPath = objCentral.musicPath
                objTop.autoSound = objCentral.autoSound
                objTop.lyrics = objCentral.lyrics
                objTop.pictureBitmapByte = objCentral.pictureBitmapByte

                objCentral.title = objBottom.title
                objCentral.musicPath = objBottom.musicPath
                objCentral.autoSound = objBottom.autoSound
                objCentral.lyrics = objBottom.lyrics
                objCentral.pictureBitmapByte = objBottom.pictureBitmapByte
            }
        }
        catch(e : Exception){}
    }

    fun newCreateRealm(){
        val musicPathList = MusicPathRelationClass().getFilePathList()
        val musicInfo = MusicInfo()

        for(i in 0 until musicPathList.size){
            insertRealmInfo(
                i,
                musicPathList[i],
                "",
                musicPathList.size,
                musicInfo
            )
        }
    }

    fun listSplit(list: String): List<String> {
        return list.split(",")
    }

    fun castByteArrayFromBitmap(bmp: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    fun castBitmapFromByteArray(byteArray: ByteArray): Bitmap? {
        val opt = BitmapFactory.Options()
        opt.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, opt)
    }

    fun realmDataBaseInit(realmObject: RealmObject) {
        val obj = realm.where(realmObject::class.java)
            .findAll()
        realm.executeTransaction { realm
            obj.deleteAllFromRealm()
        }
    }

    fun getMaxRealmData(mode: Int, realmObject: RealmObject): Int {
        val objMax = realm.where(realmObject::class.java)
            .max(Id)
        var objInt: Int = 0

        if (objMax != null) objInt = objMax.toInt()

        if (mode == 1) return objInt

        return objInt + 1
    }

    fun realmNullCheck(realmObject: RealmObject): Int {
        val checkId = 0
        val objFun = realm.where(realmObject::class.java)
            .equalTo(Id, checkId)
            .findFirst() ?: return 1
        //nullなら1
        return 0
    }

    inner class MusicInfoControl : RealmControl() {
        override fun <T> objInit(id: Int, title: String, any: T?) {
            try {
                val objFun = getObj(id, MusicInfo()) as MusicInfo
                objFun.title = title
                objFun.musicPath = any as String
            } catch (e: Exception) { }
        }

        override fun getTitle(id: Int): String {
            try {
                val objFun = realm.where(MusicInfo::class.java)
                    .equalTo(Id, id)
                    .findFirst()!!

                return objFun.title
            }
            catch(e : Exception){
                Log.d("a23",id.toString())
            }
            return ""
        }

        override fun getId(title: String): Int { return 0 }

        override fun <T> insertData(title: String, field: String, any: T) {
            realm.executeTransaction {
                val objFun = realm.where(MusicInfo::class.java)
                    .equalTo(Title, title)
                    .findFirst()!!

                when (field) {
                    Title -> objFun.title = any as String
                    MusicPath -> objFun.musicPath = any as String
                    AutoSound -> objFun.autoSound = any as Int
                    Lyrics -> objFun.lyrics = any as String
                    PictureBitmapByte -> objFun.pictureBitmapByte = any as ByteArray
                    else -> {}
                }
                Log.d("realmInsertMusicInfo", "Realm -> IMI : $any")
            }
            //destroyRealm("insertMusicInfoData")
        }

        override fun getMusicData(id: Int): MusicData {
            try {
                Log.d("realmSearch", "title -> $title")
                val objTemp = realm.where(MusicInfo::class.java)
                    .equalTo(Id, id)
                    .findFirst()!!

                val objFun = realm.copyFromRealm(objTemp)

                val bmp = castBitmapFromByteArray(objFun.pictureBitmapByte)

                return MusicData(
                    objFun.title,
                    objFun.musicPath,
                    objFun.autoSound,
                    objFun.lyrics,
                    bmp
                )
            } catch (e: Exception) { }
            return super.getMusicData(id)
        }

        override fun getMusicData(title: String): MusicData {
            try {
                Log.d("realmSearch", "title -> $title")

                val objFun = getObj(title,MusicInfo()) as MusicInfo

                val bmp = castBitmapFromByteArray(objFun.pictureBitmapByte)

                return MusicData(
                    objFun.title,
                    objFun.musicPath,
                    objFun.autoSound,
                    objFun.lyrics,
                    bmp
                )
            } catch (e: Exception) { }

            return super.getMusicData(title)
        }
    }

    inner class MusicListControl() : RealmControl() {
        override fun <T> objInit(id: Int, title: String, any: T?) {
            val objFun = realm.where(MusicListInfo::class.java)
                .equalTo(Id, id)
                .findFirst()

            if (objFun != null) {
                objFun.title = title
                objFun.musicList = any as String
            }
        }

        override fun getTitle(id: Int): String {
            val objFun = getObj(id,MusicListInfo()) as MusicListInfo
            return objFun.title
        }

        override fun getId(title: String): Int {
            val objFun = getObj(title,MusicListInfo()) as MusicListInfo
            return objFun.id
        }

        override fun getStringAny(title: String, field: String?): String {
            val objFun = getObj(title,MusicListInfo()) as MusicListInfo
            return objFun.musicList
        }

        override fun <T> insertData(title: String, field: String, any: T) {
            var titleFun: String
            var temp: String
            realm.executeTransaction {
                val objList = getObj(any as String,MusicListInfo()) as MusicListInfo
                when (field) {
                    "add" -> {
                        temp = objList.musicList

                        titleFun = if (temp != "") "$temp,$title"
                        else title

                        objList.musicList = titleFun
                        Log.d("realmListInsert", "realmInsert -> $titleFun")
                    }

                    "replace" -> {
                        objList.musicList = title
                    }
                }
            }
            //destroyRealm("insertMusicList")
        }
    }

    inner class MusicTempControl() : RealmControl(){
        override fun <T> objInit(id: Int, title: String, any: T?) {
            val objFun = getObj(id,MusicListTemp()) as MusicListTemp
            objFun.title = title
        }

        override fun getTitle(id: Int): String {
            val objFun = getObj(id,MusicListTemp())
            return if(objFun == null) ""
                else (objFun as MusicListTemp).title
        }

        override fun getId(title: String): Int { return 0 }

        override fun <T> insertData(title: String, field: String, any: T) { }

        fun createMusicListTemp(musicList: List<String>) {
            val realmObject = MusicListTemp()
            val objMax: Int = musicList.size

            maxAboveDelete(realmObject, objMax, objMax)

            for (i in musicList.indices) {
                insertRealmInfo(i, musicList[i], "", musicList.size, realmObject)
            }
        }

        fun writeListFromTemp(activeList: String) {
            val realmObject = MusicListTemp()
            val max = getMaxRealmData(1, realmObject)
            var temp: String = ""
            var title: String

            for (i in 0..max) {
                title = MusicTempIns.getTitle(i)
                temp = if (temp != "") "$temp,$title"
                else title
                MusicListIns.insertData(temp,"replace", activeList)
            }
        }
    }
}

abstract class RealmControl(){
    abstract fun <T> objInit(id: Int, title: String, any : T? = null)
    abstract fun getTitle(id : Int) : String
    abstract fun getId(title: String) : Int
    abstract fun <T> insertData(title: String, field: String, any: T)
    open fun getStringAny(id : Int, field: String?) : String? { return null }
    open fun getStringAny(title: String, field: String?) : String? { return null }
    open fun getMusicData(id : Int) : MusicData { return NullMusicData }
    open fun getMusicData(title: String) : MusicData { return NullMusicData }
}

data class DataMoveTemp(var title: String, var value: String)



