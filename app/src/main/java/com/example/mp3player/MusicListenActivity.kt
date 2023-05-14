package com.example.mp3player

import android.annotation.SuppressLint
import android.content.*
import android.graphics.*
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.lifecycle.lifecycleScope
import com.example.mp3player.databinding.ActivityMusicListenBinding
import kotlinx.android.synthetic.main.activity_music_listen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.ObjectInput
import java.security.SecureRandom

class MusicListenActivity : AppCompatActivity(){
    private lateinit var binding: ActivityMusicListenBinding
    private val uiViewModel : UIViewModel by viewModels()
    lateinit var musicData : MusicData
    private val handler : Handler = Handler(Looper.getMainLooper())
    private val musicInfoIns = RealmControlClass.MusicInfoIns
    private val musicListIns = RealmControlClass.MusicListIns
    private val musicTempIns = RealmControlClass.MusicTempIns

    private var resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ result : ActivityResult ->
        if(result.resultCode == RESULT_OK){
            val resultData = result.data
            resultData?.let{ openImage(it) }
        }
    }

    @SuppressLint("SetTextI18n", "InvalidWakeLockTag", "WakelockTimeout")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_listen)
        binding = ActivityMusicListenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFormat(PixelFormat.TRANSPARENT)

        run()

        val returnButtonMode = intent.getIntExtra("RETURN_BUTTON_MODE", MAIN)

        val activityMode = intent.getStringExtra("ACTIVITY_MODE")
        val uriFromDrive : String? = intent.getStringExtra("MUSIC_URI")
        val titleFromDrive : String? = intent.getStringExtra("TITLE_DRIVE")

        val activeListName = intent.getStringExtra("ACTIVE_LIST")!!
        val nowMusicId = intent.getIntExtra("NOW_MUSIC_ID",0)
        val title = musicTempIns.getTitle(nowMusicId)

        uiViewModel.selectListName(activeListName)
        uiViewModel.nowMode(returnButtonMode)
        uiViewModel.insertPlayerMusicId(nowMusicId)

        binding.loopButton.setOnClickListener{ onLoopTap() }
        binding.musicPlayButton.setOnClickListener{ onPlayTap() }
        binding.settingButton.setOnClickListener{ onSettingTap() }
        binding.nextMusicbutton.setOnClickListener{ musicMoveInTemp("next") }
        binding.backMusicbutton.setOnClickListener{ musicMoveInTemp("back") }
        binding.returnActivityButton.setOnClickListener{ onReturnTap( returnButtonMode ) }

        if(activityMode == "drive") {
            val musicUri = Uri.parse(uriFromDrive)
            uiViewModel.selectDriveMode("drive")
            activityInitFromDrive(titleFromDrive!!, musicUri!!)
        }
        else {
            activityInit()
        }

        Log.d("activeInitMethod","title -> $title ::: activeList -> $activeListName")
    }

    private fun activityInit(){
        musicData = if(uiViewModel.activityMode == MAIN){
            musicInfoIns.getMusicData(
                musicInfoIns.getTitle(uiViewModel.playerMusicId)
            )
        }
        else{
            musicInfoIns.getMusicData(
                musicTempIns.getTitle(uiViewModel.playerMusicId)
            )
        }

        Log.d("musicDataClass","." +
                "\ntitle      ->  ${musicData.title}" +
                "\nmusicPath  ->  ${musicData.musicPath}" +
                "\nautoVol    ->  ${musicData.autoVol}" +
                "\nlyrics     ->  ${musicData.lyrics}" +
                "\nbitmap     ->  ${musicData.bitmap}")

        val activeListName = uiViewModel.activeListName
        val title = musicData.title

        val musicUri : Uri = Uri.parse(musicData.musicPath)

        uiViewModel.selectListName(activeListName)

        binding.musicName.text = title
        binding.activeListName.text = uiViewModel.activeListName
        setMusicImage()

        playerInit(musicUri)
        lyricsInit()

        viewEnabledBundle(true)

        binding.musicVolumeButton.setOnClickListener{ onMusicAudioVolume() }
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(p2) onSeekBarSlide(p1)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        binding.navigationDrawer.bringToFront()
        binding.navigationDrawer.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.drawer_menu_auto_sound -> { onAutoSoundSet(title) }
                R.id.drawer_menu_lyricsSetting -> { onLyricsSet(title) }
                R.id.drawer_menu_nameChange -> { onNameChange(title) }
                R.id.drawer_menu_pictureSetting -> { onPictureSet() }
                else -> {}
            }
            false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun activityInitFromDrive(title: String, musicUri: Uri){
        musicData = MusicData(
            title,
            "",
            40,
            "when you get music from googleDrive, \nlyricsText is not displayed",
            null
        )

        Log.d("musicDataClass","." +
                "\ntitle      ->  ${musicData.title}" +
                "\nmusicPath  ->  ${musicData.musicPath}" +
                "\nautoVol    ->  ${musicData.autoVol}" +
                "\nlyrics     ->  ${musicData.lyrics}" +
                "\nbitmap     ->  ${musicData.bitmap}")

        val activeListName = uiViewModel.activeListName

        uiViewModel.selectListName(activeListName)

        binding.musicName.text = title
        binding.activeListName.text = "From googleDrive"
        binding.musicImage.setImageResource(R.drawable.mp3_ui_google_drive_button)

        playerInit(musicUri)
        lyricsInit()

        viewEnabledBundle(false)

        binding.musicVolumeButton.setOnClickListener{ onMusicAudioVolume() }
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(p2) onSeekBarSlide(p1)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }

    private fun viewEnabledBundle(enabled: Boolean){
        viewEnabled(binding.nextMusicbutton,enabled)
        viewEnabled(binding.backMusicbutton,enabled)
        viewEnabled(binding.settingButton,enabled)
    }

    @SuppressLint("SetTextI18n")
    private fun run() {
        lifecycleScope.launch(Dispatchers.Default) {
            handler.postDelayed(object : Runnable {
                override fun run() {
                    val duration = player.duration
                    val current = player.currentPosition

                    binding.CurrentDuration.text = "${current / 1000} s / ${duration / 1000} s"
                    seekBarSetUp(duration, current)

                    volumePicChange()
                    //Log.d("currentPos", ".\n$current / $duration")

                    player.setOnCompletionListener {
                        when (uiViewModel.loopTF) {
                            0 -> binding.musicPlayButton.setImageResource(R.drawable.mp3_ui_urprobutton)
                            2 -> {
                                 //uiViewModel.insertPlayerMusicId(uiViewModel.playerMusicId + 1)
                                 musicMoveInTemp("next")
                            }
                            3 -> musicMoveInTemp("shuffle")
                        }
                    }

                    handler.postDelayed(this, 100)
                }
            }, 100)
        }
    }

    private fun musicMoveInTemp(direction: String){//next or back or shuffle
        val max = if(uiViewModel.activityMode == MAIN) {
            RealmControlClass().getMaxRealmData(1, MusicInfo())
        } else{
            RealmControlClass().getMaxRealmData(1, MusicListTemp())
        }
        val secureRandom = SecureRandom()

        var nextId = uiViewModel.playerMusicId + 1
        var backId = uiViewModel.playerMusicId - 1
        val shuffleId = if(max != 0){ secureRandom.nextInt(max) }
                        else{ 0 }

        if(nextId == max + 1) nextId = 0
        if(backId == -1) backId = max
        //while(shuffleId == uiViewModel.playerMusicId){ shuffleId = secureRandom.nextInt(max) }

        Log.d("moveMusicSuc","move -> Success ::: direction -> $direction\n" +
                "nextId     =    $nextId\n" +
                "backId     =    $backId\n" +
                "shuffleId  =    $shuffleId\n" +
                "***************** MOVE SUCCESS *****************")

        when(direction){
            "next" -> {
                uiViewModel.insertPlayerMusicId(nextId)
                activityInit()
            }
            "back" -> {
                uiViewModel.insertPlayerMusicId(backId)
                activityInit()
            }
            "shuffle" -> {
                uiViewModel.insertPlayerMusicId(shuffleId)
                activityInit()
            }
            else -> {}
        }
    }

    private fun onAutoSoundSet(title: String){
        var volumeFun : Int = uiViewModel.playerVolume
        val seekBar : SeekBar = AppCompatSeekBar(this).apply {
            this.max = 100
            this.progress = volumeFun
            this.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    if(p2) volumeFun = p1
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }

        AlertDialog.Builder(this).apply {
            this.setTitle("set your auto volume")
            this.setMessage("This set as the volume is being played now.")
            this.setView(seekBar)
            this.setPositiveButton("ok") { dialog, _ ->
                musicInfoIns.insertData(title,RealmControlClass().autoSoundString(), volumeFun)
                dialog.dismiss()
            }
            this.show()
        }
    }

    private fun onLyricsSet(title: String){
        val editText = AppCompatEditText(this).apply {
            this.setText(binding.lyricsText.text)
        }
        AlertDialog.Builder(this).apply {
            this.setTitle("set your lyrics")
            this.setView(editText)
            this.setPositiveButton("ok") { dialog, _ ->
                editText.text?.let {
                    musicInfoIns.insertData(
                        title,
                        RealmControlClass().lyricsString(),
                        it.toString()
                    )
                    activityInit()
                }
                dialog.dismiss()
            }
            this.show()
        }
    }

    private fun onNameChange(title: String){
        val editText = AppCompatEditText(this).apply{
            this.setText(title)
        }
        var afterString : String = "/storage/emulated/0/Download/"
        var afterFile : File?
        val beforeFile : File? = MusicPathRelationClass().getMusicUri(title).path?.let { File(it) }
        var realmInsertPath : String = "file:///storage/emulated/0/Download/"

        AlertDialog.Builder(this).apply {
            this.setTitle("change your mp3File name")
            this.setView(editText)
            this.setPositiveButton("ok") { dialog, _ ->
                val afterEditText = editText.text.toString()
                afterString += afterEditText

                afterFile = (
                            Uri.parse(MusicPathRelationClass().onSuffixBlank(afterString))
                        ).path?.let { File( it) }

                Log.d("a23",afterFile.toString())

                try {
                    afterFile?.let { beforeFile?.renameTo(it) }
                    realmInsertPath += afterFile

                    val musicFile = MusicPathRelationClass().getMusicFile(title)

                    musicInfoIns.insertData(
                        title,
                        RealmControlClass().musicPathString(),
                        realmInsertPath
                    )
                    musicInfoIns.insertData(
                        title,
                        "title",
                        editText.text.toString()
                    )
                }
                catch (e :Exception){ }
                dialog.dismiss()
            }
            this.show()
        }
    }

    private fun onPictureSet(){
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"

            resultLauncher.launch(intent)
        }
        catch (e: Exception){}
    }

    private fun openImage(resultData: Intent){
        val uri : Uri? = resultData.data
        val pfDescriptor : ParcelFileDescriptor? = uri?.let { contentResolver.openFileDescriptor(it,"r") }
        var bmp : Bitmap? = null

        if(pfDescriptor != null) {
            val fileDescriptor = pfDescriptor.fileDescriptor
            bmp = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            pfDescriptor.close()
        }
        val byteArray = bmp?.let { RealmControlClass().castByteArrayFromBitmap(it) }

        val tag = "mainFragment"
        uiViewModel.insertByteArray(byteArray!!)

        closeDrawer()
        var fragment = supportFragmentManager.findFragmentByTag(tag)
        if (fragment == null) {
            fragment = bitmapCreateFragment()
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragmentContainer, fragment, tag)
            }.commit()
        }

        Log.d("openImage","openImageFunctionFrom >>>" +
                "\nreceived Uri   = $uri" +
                "\nbyteArray      = $byteArray")

        setMusicImage()
    }

    private fun setMusicImage(){
        val bmp : Bitmap? = musicData.bitmap

        if(bmp != null) binding.musicImage.setImageBitmap(bmp)
        else binding.musicImage.setImageResource(R.drawable.mp3_ui_picture_settingd)
    }

    private fun onLoopTap() {
        Log.d("loopMode","loopMode -> ${uiViewModel.loopTF}")
        when(uiViewModel.loopTF){
            0 -> {//off
                binding.loopButton.setImageResource(R.drawable.mp3_ui_loop_button_on)
                player.isLooping = true
                uiViewModel.switchLoopTF()
            }
            1 -> {//loop
                player.isLooping = false
                if(uiViewModel.isDriveMode == "drive"){
                    binding.loopButton.setImageResource(R.drawable.mp3_ui_loop_button_off)
                    uiViewModel.switchLoopTF()
                    uiViewModel.switchLoopTF()
                    uiViewModel.switchLoopTF()
                }
                else{
                    binding.loopButton.setImageResource(R.drawable.mp3_ui_loop_list_button)
                    uiViewModel.switchLoopTF()
                }
            }
            2 -> {//listLoop
                binding.loopButton.setImageResource(R.drawable.mp3_ui_music_shuffle_button)
                uiViewModel.switchLoopTF()
            }
            3 -> {//shuffle
                binding.loopButton.setImageResource(R.drawable.mp3_ui_loop_button_off)
                uiViewModel.switchLoopTF()
            }
        }
    }

    private fun onPlayTap() {
        when(uiViewModel.playTF){
            false -> {
                binding.musicPlayButton.setImageResource(R.drawable.mp3_ui_music_stop_button)
                player.start()
                uiViewModel.switchPlayTF()
            }
            true -> {
                binding.musicPlayButton.setImageResource(R.drawable.mp3_ui_urprobutton)
                player.pause()
                uiViewModel.switchPlayTF()
            }
        }
    }

    private fun onSettingTap() {
        binding.DrawerLayout.openDrawer(navigationDrawer)
    }

    private fun onReturnTap(mode : Int) {
        val intent = Intent(this,MainActivity::class.java)
        intent.putExtra("appCreated",true)
        intent.putExtra("MUSIC_ID",uiViewModel.playerMusicId)
        intent.putExtra("LOOP_TF",uiViewModel.loopTF)
        when(mode){
            MAIN -> {
                intent.putExtra("mode", MAIN)
                startActivity(intent)
            }
            LIST -> {
                intent.putExtra("mode", LIST)
                startActivity(intent)
            }
            MUSIC_IN_LIST -> {
                intent.putExtra("mode", MUSIC_IN_LIST)
                intent.putExtra("selectListName",uiViewModel.activeListName)
                startActivity(intent)
            }
            else -> {
                startActivity(intent)
            }
        }
    }

    private fun onMusicAudioVolume() {
        val tag = "mainFragment"
        var fragment = supportFragmentManager.findFragmentByTag(tag)
        if (fragment == null) {
            fragment = musicVolumeFragment()
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragmentContainer, fragment, tag)
            }.commit()
        }
        uiViewModel.switchVolumePicVisi()
    }


    private fun onSeekBarSlide(current: Int) {
        player.seekTo(current)
    }

    private fun seekBarSetUp(duration: Int, current: Int){
        seekBar.max = duration
        seekBar.progress = current
    }


    override fun onResume() {
        super.onResume()
        when(uiViewModel.playTF){
            false -> {
                if(player.isPlaying) player.pause()
            }
            true -> {
                if(!player.isPlaying) player.start()
            }
        }
    }

    private fun playerInit(musicUri: Uri){
        try{
            uiViewModel.switchPlayTF()
            player.stop()
            player.release()
            player = MediaPlayer.create(this@MusicListenActivity,musicUri)
            player.start()
            requestVisibleBehind(true)

            val volume : Int = musicData.autoVol

            uiViewModel.changeVolume(volume)
        }
        catch(e : Exception) {}
    }

    private fun lyricsInit(){
        val lyrics : String = musicData.lyrics

        binding.lyricsText.text = lyrics
    }

    private fun closeDrawer(){
        binding.DrawerLayout.closeDrawer(binding.navigationDrawer)
    }

    private fun volumePicChange(){
        if(uiViewModel.volumePicVisi == 1) {
            if (uiViewModel.playerVolume == 0) {
                musicVolumeButton.setImageResource(R.drawable.mp3_ui_sound_control_mute_on)
            } else {
                musicVolumeButton.setImageResource(R.drawable.mp3_ui_sound_control_unmute_on)
            }
        }
        else{
            if (uiViewModel.playerVolume == 0) {
                musicVolumeButton.setImageResource(R.drawable.mp3_ui_sound_control_mute_off)
            } else {
                musicVolumeButton.setImageResource(R.drawable.mp3_ui_sound_control_unmute_off)
            }
        }
    }

    private fun viewEnabled(view : View, enabled : Boolean){
        if(enabled) view.visibility = View.VISIBLE
        else view.visibility = View.GONE
    }

    fun returnView() : Int {
        return uiViewModel.playerMusicId
    }
}

data class MusicData(
    val title:String,
    val musicPath:String,
    val autoVol:Int,
    val lyrics:String,
    val bitmap:Bitmap?
    )

