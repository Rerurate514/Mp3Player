package com.example.mp3player

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel

class UIViewModel : ViewModel() {
    private var _playerVolume : Int = 0
    val playerVolume : Int
        get() = _playerVolume

    private var _loopTF : Int = 0
    val loopTF : Int
        get() = _loopTF

    private var _playTF : Boolean = false
    val playTF : Boolean
        get() = _playTF

    private var _volumePicVisi : Int = 0
    val volumePicVisi : Int
        get() = _volumePicVisi

    private var _playerMusicId : Int = 0
    val playerMusicId : Int
        get() = _playerMusicId

    private var _activeListName : String = ""
    val activeListName : String
        get() = _activeListName

    private var _activityMode : Int = 0
    val activityMode : Int
        get() = _activityMode

    private var _isDriveMode : String = ""
    val isDriveMode : String
        get() = _isDriveMode

    private var _byteArray : ByteArray? = null
    val byteArray : ByteArray?
        get() = _byteArray

    private var _listTotalAndMusic : Int? = null
    val listTotalAndMusic : Int?
        get() = _listTotalAndMusic

    fun changeVolume(vol: Int){
        val volFloat = vol / 100.toFloat()
        player.setVolume(volFloat,volFloat)
        _playerVolume = vol
    }

    fun switchLoopTF(){
        when(_loopTF){
            0 -> _loopTF = 1
            1 -> _loopTF = 2
            2 -> _loopTF = 3
            3 -> _loopTF = 0
        }
    }

    fun switchPlayTF(){
        _playTF = _playTF == false
    }

    fun switchVolumePicVisi(){
        if(_volumePicVisi == 0) _volumePicVisi++
        else _volumePicVisi--
    }

    fun insertPlayerMusicId(id: Int){
        _playerMusicId = id
    }

    fun selectListName(listTitle: String){
        _activeListName = listTitle
        Log.d("viewModelActiveList","activeList -> $_activeListName")
    }

    fun nowMode(mode: Int){
        _activityMode = mode
        Log.d("viewModelActiveMode","activeMode -> $_activityMode")
    }

    fun insertByteArray(byteArray: ByteArray){
        _byteArray = byteArray
    }

    fun selectDriveMode(mode: String){
        _isDriveMode = mode
    }

    fun insertTotal(total: Int){
        _listTotalAndMusic = total
    }
}