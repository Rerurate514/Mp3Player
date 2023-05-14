package com.example.mp3player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_delegate_music_activity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


/**
 * A simple [Fragment] subclass.
 * Use the [DelegateMusicActivityFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DelegateMusicActivityFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val uiViewModel : UIViewModel by activityViewModels()
    private val handler : Handler = Handler(Looper.getMainLooper())
    private var musicData : MusicData = MusicData("","",40,"",null)
    private val musicInfoIns = RealmControlClass.MusicInfoIns

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_delegate_music_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentInit()

        playButtonInDelegate.setOnClickListener { onPlayTap() }
        seekBarInDelegate.setOnSeekBarChangeListener( object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(p2) seekBarSlide(p1)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }

    private fun fragmentInit(){
        musicData = musicInfoIns.getMusicData(uiViewModel.playerMusicId)

        musicTitleInDelegate.text = musicData.title
        pictureSet()
        seekbarRunning()
    }

    private fun seekbarRunning(){
        lifecycleScope.launch(Dispatchers.Default){
            handler.postDelayed( object : Runnable {
                override fun run() {
                    val duration = player.duration
                    val current = player.currentPosition
                    seekBarSetUp(duration,current)
                    player.setOnCompletionListener {
                        Log.d("a23","awwawawawwwwa")
                        delegateFragmentInit()
                    }

                    handler.postDelayed(this, 100)
                }
            },100)
        }
    }

    fun delegateFragmentInit() {
        try {
            when (uiViewModel.loopTF) {
                0 -> {
                    playButtonInDelegate.setImageResource(R.drawable.mp3_ui_urprobutton)
                }
                2 -> {
                    uiViewModel.insertPlayerMusicId(uiViewModel.playerMusicId)
                }
                3 -> {
                    uiViewModel.insertPlayerMusicId(MusicListenActivity().returnView())
                }
            }
            fragmentInit()
        }
        catch(e : Exception) { }
    }

    private fun seekBarSetUp(duration: Int,current: Int){
        seekBarInDelegate.max = duration
        seekBarInDelegate.progress = current
    }

    private fun seekBarSlide(current : Int){
        player.seekTo(current)
    }

    private fun pictureSet(){
        if(musicData.bitmap != null) musicImageInDelegate.setImageBitmap(musicData.bitmap)
    }

    private fun onPlayTap(){
        when(uiViewModel.playTF){
            false -> {
                playButtonInDelegate.setImageResource(R.drawable.mp3_ui_music_stop_button)
                player.start()
                uiViewModel.switchPlayTF()
            }
            true -> {
                playButtonInDelegate.setImageResource(R.drawable.mp3_ui_urprobutton)
                player.pause()
                uiViewModel.switchPlayTF()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(isAdded) resources.getString(R.string.app_name)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DelegateMusicActivityFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DelegateMusicActivityFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}