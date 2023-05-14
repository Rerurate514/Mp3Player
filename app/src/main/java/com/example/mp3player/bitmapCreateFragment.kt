package com.example.mp3player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import kotlinx.android.synthetic.main.activity_music_listen.*
import kotlinx.android.synthetic.main.fragment_bitmap_create.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [bitmapCreateFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class bitmapCreateFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private val uiViewModel : UIViewModel by activityViewModels()
    private val musicInfoIns = RealmControlClass.MusicInfoIns
    private val musicListIns = RealmControlClass.MusicListIns
    private val musicTempIns = RealmControlClass.MusicTempIns

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
        return inflater.inflate(R.layout.fragment_bitmap_create, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val byteArray = uiViewModel.byteArray
        val bmp = RealmControlClass().castBitmapFromByteArray(byteArray!!)

        val customView = activity?.findViewById<CustomView>(R.id.musicCreateImage)
        if (bmp != null) customView?.insertBitmap(bmp)

        setScrollEnabled(false)
        setFinishButton.setOnClickListener{ finishCreateBitmap() }
        setDefaultButton.setOnClickListener { setDefaultBitmap() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun finishCreateBitmap(){
        val title = if(uiViewModel.activityMode == MAIN){
            musicInfoIns.getTitle(uiViewModel.playerMusicId)
        }
        else{
            musicListIns.getTitle(uiViewModel.playerMusicId)
        }

        val bmp = musicCreateImage.getViewCapture()
        val byteArray = bmp.let { RealmControlClass().castByteArrayFromBitmap(it) }

        musicInfoIns.insertData(
            title,
            RealmControlClass().pictureBitmapByteString(),
            byteArray
        )

        activity?.musicImage?.setImageBitmap(bmp)

        fragmentEnd()
    }

    private fun View.getViewCapture(): Bitmap{
        this.isDrawingCacheEnabled = true
        val cache = this.drawingCache
        val bmp = Bitmap.createBitmap(cache)
        this.isDrawingCacheEnabled = false
        return bmp
    }

    private fun setDefaultBitmap(){
        val title = if(uiViewModel.activityMode == MAIN){
            musicInfoIns.getTitle(uiViewModel.playerMusicId)
        }
        else{
            musicTempIns.getTitle(uiViewModel.playerMusicId)
        }

        musicInfoIns.insertData(
            title,
            RealmControlClass().pictureBitmapByteString(),
            byteArrayOf()
        )
        activity?.musicImage?.setImageResource(R.drawable.mp3_ui_picture_settingd)
        fragmentEnd()
    }

    private fun fragmentEnd(){
        setScrollEnabled(true)
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setScrollEnabled(enabled: Boolean){
        if(enabled){
            activity?.scrollView4?.visibility = View.VISIBLE
        }
        else{
            activity?.scrollView4?.visibility = View.GONE
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment bitmapCreateFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            bitmapCreateFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

class CustomView(context: Context, attrs: AttributeSet?): View(context, attrs){
    private var paint : Paint = Paint()
    var x1 : Float = 0F
    var y1 : Float = 0F
    var scale : Float = 1F
    private var bmp : Bitmap? = null
    private val mRenderMatrix : Matrix = Matrix()

    fun insertBitmap(_bmp: Bitmap){
        bmp = _bmp
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        mRenderMatrix.postScale(scale,scale)
        mRenderMatrix.preTranslate(x1,y1)
        Log.d("scaleBmp",".\nscale = $scale\nx=$x1,y=$y1")
        bmp?.let { canvas?.drawBitmap(it,mRenderMatrix,paint) }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(ev)
        mGestureDetector.onTouchEvent(ev)
        return true
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener(){
        override fun onScale(detector: ScaleGestureDetector): Boolean{
            scale = detector.scaleFactor
            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            super.onScaleEnd(detector)
            x1 = 0F
            y1 = 0F
            scale = 1F
        }
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener(){
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            x1 = -distanceX
            y1 = -distanceY
            invalidate()
            return true
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)
    private val mGestureDetector = GestureDetector(context, gestureListener)
}