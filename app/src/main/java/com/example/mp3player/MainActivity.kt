package com.example.mp3player

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.cardview.widget.CardView
import androidx.core.view.size
import com.example.mp3player.databinding.ActivityMainBinding
import java.security.SecureRandom
import io.realm.RealmObject
import kotlinx.android.synthetic.main.activity_main.*

var player = MediaPlayer()      //mediaPlayerインスタンスの生成
var appCreated : Boolean = false

const val MAIN = 0
const val LIST = 1
const val MUSIC_IN_LIST = 2
const val REGISTER = 3

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val uiViewModel : UIViewModel by viewModels()
    private var musicData = MusicData("","",40,"",null)
    private val musicInfoIns = RealmControlClass.MusicInfoIns
    private val musicListIns = RealmControlClass.MusicListIns
    private val musicTempIns = RealmControlClass.MusicTempIns

    private var resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ result : ActivityResult ->
        if(result.resultCode == RESULT_OK){
            val resultData = result.data
            resultData?.let{ openDrive(it) }
        }
    }

    @SuppressLint("Recycle", "RtlHardcoded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        uiViewModel.nowMode(MAIN)

        val mode = intent.getIntExtra("mode", MAIN)
        uiViewModel.nowMode(mode)

        appCreated = intent.getBooleanExtra("appCreated",false)
        intent.getStringExtra("selectListName")?.let { uiViewModel.selectListName(it) }

        uiViewModel.insertPlayerMusicId(intent.getIntExtra("MUSIC_ID",0))

        val loopMode = intent.getIntExtra("LOOP_TF",0)

        for(i in 0 .. loopMode) uiViewModel.switchLoopTF()

        if(player.isPlaying) openDelegateFragment()

        if(uiViewModel.activityMode != MUSIC_IN_LIST) manageModeSetUp(0)
        else {
            val title = uiViewModel.activeListName
            val list = musicListIns.getStringAny(title,null)
            onIntoList(title,list)
        }
    }

    private fun manageModeSetUp(anyValue : Any){
        binding.updateTable.removeAllViews()
        binding.addTable.removeAllViews()
        RealmControlClass().realmDataBaseInit(MusicListTemp())
        when(uiViewModel.activityMode){
            MAIN -> {
                mainOfScrollViewInit()
            }
            LIST -> {
                listOfScrollViewInit()
            }
            MUSIC_IN_LIST -> {
                musicListOfScrollViewInit(anyValue as String)
            }
            REGISTER -> {
                registerOfScrollViewInit()
            }
        }
        buttonInit()
    }

    private fun buttonInit(){
        binding.leftTopButton.setOnClickListener{ onLeftButton() }
        binding.centralTopButton.setOnClickListener{ onCentralButton() }
        binding.rightTopButton.setOnClickListener{ onRightButton() }
    }

    private fun updateTap(){
        binding.updateTable.removeAllViews()
        binding.addTable.removeAllViews()
        RealmControlClass().fetchFile()
        RealmControlClass().alignRealmObject(MusicInfo())
        manageModeSetUp(0)
    }

    private fun deleteRealmFromList(id: Int){
        val realmObject : RealmObject = MusicListTemp()

        RealmControlClass().deleteRealmObjectInId(realmObject,id)
        musicTempIns.writeListFromTemp(uiViewModel.activeListName)
        val musicList : String = musicListIns.getStringAny(uiViewModel.activeListName,null)

        manageModeSetUp(musicList)
    }

    private fun onIntoList(title: String,musicListOriginal: String){
        val musicList : List<String> = RealmControlClass().listSplit(musicListOriginal)
        if(musicListOriginal != "") musicTempIns.createMusicListTemp(musicList)

        uiViewModel.selectListName(title)
        uiViewModel.nowMode(MUSIC_IN_LIST)
        bundleButtonImageChange()
        buttonInit()
        manageModeSetUp(musicListOriginal)
    }

    private fun onDeleteListButton(title: String){
        val realmObject = MusicListInfo()
        RealmControlClass().deleteRealmObjectInTitle(realmObject,title)
    }

    @SuppressLint("SetTextI18n")
    private fun onLeftButton(){
        when(uiViewModel.activityMode){
            MAIN -> {
                uiViewModel.nowMode(LIST)
                manageModeSetUp(0)
                bundleButtonImageChange()
            }
            LIST -> {
                uiViewModel.nowMode(MAIN)
                manageModeSetUp(0)
                bundleButtonImageChange()
            }
            MUSIC_IN_LIST -> {
                uiViewModel.selectListName("")
                uiViewModel.nowMode(LIST)
                manageModeSetUp(0)
                bundleButtonImageChange()
            }
            REGISTER -> {
                val listTitle = musicListIns.getStringAny(uiViewModel.activeListName,null)

                uiViewModel.nowMode(MUSIC_IN_LIST)
                buttonInit()
                viewEnabled(binding.centralTopButton,true)
                viewEnabled(binding.rightTopButton,true)
                onIntoList(uiViewModel.activeListName,listTitle)
                bundleButtonImageChange()
            }
        }
    }

    private fun onRightButton(){
        when(uiViewModel.activityMode){
            MAIN -> onDriveTap()
            LIST -> addList()
            MUSIC_IN_LIST -> {
                uiViewModel.nowMode(REGISTER)
                manageModeSetUp(0)
            }
            else -> {}
        }
    }

    private fun onCentralButton(){
        val max : Int
        val secureRandom = SecureRandom()
        val shuffleId : Int

        when(uiViewModel.activityMode){
            MAIN -> {
                val intent = Intent(this,MusicListenActivity::class.java)
                max = RealmControlClass().getMaxRealmData(1,MusicInfo())
                shuffleId = if(max != 0){ secureRandom.nextInt(max) }
                else{ 0 }

                intent.putExtra("ACTIVE_LIST", "")
                intent.putExtra("NOW_MUSIC_ID", shuffleId)
                intent.putExtra("RETURN_BUTTON_MODE",MAIN)

                startActivity(intent)
            }
            LIST -> {
                max = RealmControlClass().getMaxRealmData(0,MusicListInfo())
                shuffleId = secureRandom.nextInt(max)
                val listTitle : String = musicTempIns.getTitle(shuffleId)
                val musicListOriginal = musicListIns.getStringAny(listTitle,null)

                uiViewModel.selectListName(listTitle)
                uiViewModel.nowMode(MUSIC_IN_LIST)

                val musicList : List<String> = RealmControlClass().listSplit(musicListOriginal)
                if(musicList[0] != "") {
                    musicTempIns.createMusicListTemp(musicList)
                    onCentralButton()
                }
            }
            MUSIC_IN_LIST -> {
                max = RealmControlClass().getMaxRealmData(0,MusicListTemp())
                shuffleId = secureRandom.nextInt(max)

                musicListenAcMove(shuffleId)
            }
            else -> {}
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bundleButtonImageChange(){
        val total = uiViewModel.listTotalAndMusic
        when(uiViewModel.activityMode){
            MAIN -> {
                binding.musicTotalText.text = "MUSIC TOTAL = $total"
                binding.leftTopButton.setImageResource(R.drawable.mp3_ui_list)
                binding.centralTopButton.setImageResource(R.drawable.mp3_ui_music_shuffle_button)
                binding.rightTopButton.setImageResource(R.drawable.mp3_ui_google_drive_button)
                binding.modeView.setImageResource(R.drawable.mp3_ui_main_mode)
            }
            LIST -> {
                binding.musicTotalText.text = "LIST TOTAL = $total"
                binding.leftTopButton.setImageResource(R.drawable.mp3_ui_return)
                binding.centralTopButton.setImageResource(R.drawable.mp3_ui_music_shuffle_button)
                binding.rightTopButton.setImageResource(R.drawable.mp3_ui_list_make)
                binding.modeView.setImageResource(R.drawable.mp3_ui_list_mode)
            }
            MUSIC_IN_LIST -> {
                binding.musicTotalText.text = "MUSIC TOTAL = $total"
                binding.leftTopButton.setImageResource(R.drawable.mp3_ui_return)
                binding.centralTopButton.setImageResource(R.drawable.mp3_ui_music_shuffle_button)
                binding.rightTopButton.setImageResource(R.drawable.mp3_ui_music_register_button)
                binding.modeView.setImageResource(R.drawable.mp3_ui_music_mode)
            }
            REGISTER -> {
                binding.musicTotalText.text = "REGISTER TOTAL = $total"
                binding.leftTopButton.setImageResource(R.drawable.mp3_ui_return)
                binding.modeView.setImageResource(R.drawable.mp3_ui_register_mode)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun mainOfScrollViewInit() {
        val intent = Intent(this, MusicListenActivity::class.java)

        val realmObjectMusicInfo: RealmObject = MusicInfo()
        var mp3FileUriString: String
        var musicTitle: String

        val max = RealmControlClass().getMaxRealmData(1, realmObjectMusicInfo)
        val total = RealmControlClass().getMaxRealmData(0, realmObjectMusicInfo)

        val nullCheck = RealmControlClass().realmNullCheck(realmObjectMusicInfo)

        if(nullCheck == 1) RealmControlClass().newCreateRealm()

        createUpdateButton()

        for (i in 0..max) {
            musicData = musicInfoIns.getMusicData(i)
            musicTitle = musicInfoIns.getTitle(i)

            val playMusicButton = ImageButton(this).also {
                it.scaleType = ImageView.ScaleType.FIT_CENTER
                it.setImageResource(R.drawable.mp3_ui_urprobutton)
                it.setOnClickListener {
                    intent.putExtra("ACTIVE_LIST", "")
                    intent.putExtra("NOW_MUSIC_ID", i)
                    intent.putExtra("RETURN_BUTTON_MODE", MAIN)
                    startActivity(intent)
                    uiViewModel.switchPlayTF()
                    uiViewModel.switchLoopTF()
                }
            }

            val cardView = CardView(this).also{
                it.radius = 20F
            }

            val imageView = ImageView(this).also {
                setBitmap(it)
            }

            val space = Space(this)

            val textView = TextView(this).also {
                it.text = musicTitle
                it.gravity = Gravity.CENTER_VERTICAL
            }

            val tableRow = TableRow(this)
            binding.addTable.addView(tableRow)
            tableRow.addView(playMusicButton, 200, 200)
            tableRow.addView(cardView, 200, 200)
            cardView.addView(imageView, 200, 200)
            tableRow.addView(space, 20, 210)
            tableRow.addView(textView, 660, 240)//860,240

            if (!appCreated) {
                mp3FileUriString = MusicPathRelationClass().getMusicString(musicTitle)
                RealmControlClass().insertRealmInfo(
                    i,
                    musicTitle,
                    mp3FileUriString,
                    total,
                    realmObjectMusicInfo
                )
                Log.d("realmBefore", "num = ${i}\ttitle = $musicTitle\tpath = $musicTitle\n\n")
            }
        }

        createAllDeleteButton()
        uiViewModel.insertTotal(total)
        binding.musicTotalText.text = "TOTAL MUSIC = $total"
    }

    @SuppressLint("SetTextI18n")
    private fun listOfScrollViewInit(){
        val realmObject = MusicListInfo()
        var total = 0

        RealmControlClass().alignRealmObject(realmObject)

        if(1 == RealmControlClass().realmNullCheck(realmObject)) return

        for(i in 0 .. RealmControlClass().getMaxRealmData(1,MusicListInfo())) {
            val title = musicListIns.getTitle(i)
            val textView = TextView(this).also{
                it.text = musicListIns.getTitle(i)
                it.gravity = Gravity.CENTER_VERTICAL
            }
            val intoListButton = ImageButton(this).also{
                it.scaleType = ImageView.ScaleType.FIT_CENTER
                it.setImageResource(R.drawable.mp3_ui_urprobutton)
                it.setOnClickListener{
                    onIntoList(
                        title,
                        musicListIns.getStringAny(title,null)
                    )
                }
            }
            val deleteListButton = ImageButton(this).also{
                it.scaleType = ImageView.ScaleType.FIT_CENTER
                it.setImageResource(R.drawable.mp3_ui_delete)
                it.setOnClickListener{
                    AlertDialog.Builder(this).apply{
                        this.setTitle("remove this List?")
                        this.setPositiveButton("ok") { dialog, _ ->
                            onDeleteListButton(musicTempIns.getTitle(i))
                            manageModeSetUp(0)
                            dialog.dismiss()
                        }
                        this.setNegativeButton("no") { dialog, _ ->
                            dialog.dismiss()
                        }
                        this.show()
                    }
                }
            }

            val tableRow = TableRow(this)
            binding.addTable.addView(tableRow)
            tableRow.addView(intoListButton,200,200)
            tableRow.addView(textView,660,200)//680
            tableRow.addView(deleteListButton,200,200)

            total++
        }
        uiViewModel.insertTotal(total)
        binding.musicTotalText.text = "LIST TOTAL = $total"
    }

    @SuppressLint("SetTextI18n")
    private fun musicListOfScrollViewInit(musicListOriginal: String){
        val musicList : List<String> = RealmControlClass().listSplit(musicListOriginal)
        var total = 0

        if(musicList[0] == "") {
            uiViewModel.insertTotal(total)
            binding.musicTotalText.text = "MUSIC TOTAL = $total"
            return
        }

        musicTempIns.createMusicListTemp(musicList)

        for(i in musicList.indices){
            musicData = musicInfoIns.getMusicData(musicList[i])
            val textView = TextView(this).also{
                it.text = musicList[i]
                it.gravity = Gravity.CENTER_VERTICAL
            }
            val playMusicButton = ImageButton(this).also{
                it.scaleType = ImageView.ScaleType.FIT_CENTER
                it.setImageResource(R.drawable.mp3_ui_urprobutton)
                it.setOnClickListener{
                    musicListenAcMove(i)
                }
            }
            val cardView = CardView(this).also{
                it.radius = 20F
            }

            val imageView = ImageView(this).also {
                setBitmap(it)
            }

            val space = Space(this)

            val deleteListButton = ImageButton(this).also{
                it.scaleType = ImageView.ScaleType.FIT_CENTER
                it.setImageResource(R.drawable.mp3_ui_delete)
                it.setOnClickListener{
                    AlertDialog.Builder(this).apply{
                        this.setTitle("remove this music?")
                        this.setPositiveButton("ok") { dialog, _ ->
                            deleteRealmFromList(i)
                            dialog.dismiss()
                        }
                        this.setNegativeButton("no") { dialog, _ ->
                            dialog.dismiss()
                        }
                        this.show()
                    }
                }
            }

            val tableRow = TableRow(this)
            binding.addTable.addView(tableRow)
            tableRow.addView(playMusicButton,200,200)
            tableRow.addView(cardView, 200, 200)
            cardView.addView(imageView, 200, 200)
            tableRow.addView(space, 20, 210)
            tableRow.addView(textView,460,240)//680
            tableRow.addView(deleteListButton,200,200)

            total++
        }

        uiViewModel.insertTotal(total)
        binding.musicTotalText.text = "MUSIC TOTAL = $total"
    }

    @SuppressLint("SetTextI18n")
    private fun registerOfScrollViewInit(){
        var total = 0
        binding.modeView.setImageResource(R.drawable.mp3_ui_register_mode)
        uiViewModel.nowMode(REGISTER)
        buttonInit()

        viewEnabled(binding.centralTopButton,false)
        viewEnabled(binding.rightTopButton,false)

        val realmObject : RealmObject = MusicInfo()

        for(i in 0 .. RealmControlClass().getMaxRealmData(1,realmObject)) {
            musicData = musicInfoIns.getMusicData(i)
            val musicTitle = musicInfoIns.getTitle(i)
            val textView = TextView(this).also{
                it.text = musicTitle
                it.gravity = Gravity.CENTER_VERTICAL
            }
            val registerMusicButton = ImageButton(this).also{
                it.scaleType = ImageView.ScaleType.FIT_CENTER
                it.setImageResource(R.drawable.mp3_ui_music_register_button)
                it.setOnClickListener{
                    musicListIns.insertData(musicTitle, "add", uiViewModel.activeListName)
                    addMusicTemp(musicTitle)
                }
            }

            val cardView = CardView(this).also{
                it.radius = 20F
            }

            val imageView = ImageView(this).also {
                setBitmap(it)
            }

            val space = Space(this)

            val tableRow = TableRow(this)
            binding.addTable.addView(tableRow)
            tableRow.addView(registerMusicButton,200,200)
            tableRow.addView(cardView, 200, 200)
            cardView.addView(imageView, 200, 200)
            tableRow.addView(space, 20, 210)
            tableRow.addView(textView,660,240)//680

            total++
        }

        uiViewModel.insertTotal(total)
        binding.musicTotalText.text = "REGISTER TOTAL = $total"
    }

    private fun onDriveTap(){
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "audio/*"

            resultLauncher.launch(intent)
        }
        catch (e: Exception){}
    }

    private fun openDrive(resultData : Intent){
        val uri = resultData.data
        var fileName = ""

        resultData.data?.let{ titleUri ->
            contentResolver.query(titleUri, null, null, null, null)
        }?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            fileName = cursor.getString(nameIndex)
        }

        val intent = Intent(this,MusicListenActivity::class.java)
        intent.putExtra("RETURN_BUTTON_MODE",MAIN)
        intent.putExtra("ACTIVITY_MODE","drive")
        intent.putExtra("MUSIC_URI",uri.toString())
        intent.putExtra("TITLE_DRIVE",fileName)
        intent.putExtra("ACTIVE_LIST","")
        startActivity(intent)
    }

    private fun createUpdateButton(){
        val updateButton = ImageButton(this).also{
            it.setImageResource(R.drawable.mp3_ui_update)
            it.scaleType = ImageView.ScaleType.FIT_CENTER
            it.setOnClickListener{
                updateTap()
            }
        }

        val tableRowUp = TableRow(this)
        binding.updateTable.addView(tableRowUp)
        tableRowUp.addView(updateButton,1073,213)
    }

    private fun createAllDeleteButton(){
        val updateButton = ImageButton(this).also{
            it.setImageResource(R.drawable.mp3_ui_music_register_button)
            it.scaleType = ImageView.ScaleType.FIT_CENTER
            it.setOnClickListener{
                RealmControlClass().realmDataBaseInit(MusicInfo())
            }
        }

        val tableRowUp = TableRow(this)
        binding.updateTable.addView(tableRowUp)
        tableRowUp.addView(updateButton,1073,213)
    }

    private fun setBitmap(it : ImageView){
        var bmp = musicData.bitmap
        val scale = 0.23F
        val matrix = Matrix()
        matrix.postScale(scale,scale)
        if (bmp != null) bmp = Bitmap.createBitmap(bmp,0,0,bmp.width,bmp.height,matrix,true)
        it.scaleType = ImageView.ScaleType.CENTER
        it.setImageBitmap(bmp)
    }

    private fun addList(){
        val editText = AppCompatEditText(this)
        val realmObject : RealmObject = MusicListInfo()
        val viewTotal = binding.addTable.size
        AlertDialog.Builder(this).apply{
            this.setTitle("new list name")
            this.setView(editText)
            this.setPositiveButton("ok") { dialog, _ ->
                RealmControlClass().insertRealmInfo(viewTotal,
                    editText.text.toString(),
                    "",
                    viewTotal + 1,
                    realmObject)

                manageModeSetUp(0)
                dialog.dismiss()
            }
            this.show()
        }
    }

    private fun addMusicTemp(title: String){
        val realmObject : RealmObject = MusicListTemp()
        val total = RealmControlClass().getMaxRealmData(0,realmObject)

        RealmControlClass().insertRealmInfo(
            total,
            title,
            "",
            total + 1,
            realmObject
        )
    }

    private fun musicListenAcMove(id: Int){
        val intent = Intent(this,MusicListenActivity::class.java)

        intent.putExtra("NOW_MUSIC_ID",id)
        intent.putExtra("ACTIVE_LIST",uiViewModel.activeListName)
        intent.putExtra("RETURN_BUTTON_MODE", uiViewModel.activityMode)

        startActivity(intent)
        uiViewModel.switchPlayTF()
        uiViewModel.switchLoopTF()
    }

    private fun viewEnabled(view : View, enabled : Boolean){
        if(enabled) view.visibility = View.VISIBLE
        else view.visibility = View.INVISIBLE
    }

    private fun openDelegateFragment(){
//        val tag = "mainFragment"
//        var fragment = supportFragmentManager.findFragmentByTag(tag)
//        if (fragment == null) {
//            fragment = DelegateMusicActivityFragment()
//            supportFragmentManager.beginTransaction().apply {
//                add(R.id.delegateFragmentContainer, fragment, tag)
//            }.commit()
//        }
    }

//    private fun deleteDelegateFragment(){
//        try {
//            val tag = "mainFragment"
//            var fragment = supportFragmentManager.findFragmentByTag(tag)
//            if (fragment == null) {
//                fragment = DelegateMusicActivityFragment()
//                supportFragmentManager.beginTransaction().apply {
//                    remove(fragment)
//                }.commit()
//            }
//        }
//        catch(e : Exception){ }
//    }
}

