package com.example.imagepickerdemo


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.util.TypedValue
import android.widget.FrameLayout
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.imagepickerdemo.databinding.ActivityTrimVideoBinding
import com.example.imagepickerdemo.databinding.ItemExtractVideoImageBinding
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.github.nitrico.lastadapter.LastAdapter
import com.livinglifetechway.k4kotlin.core.hide
import com.livinglifetechway.k4kotlin.core.show
import com.waynell.videorangeslider.RangeSlider
import com.yalantis.ucrop.util.FileUtils.getPath
import kotlinx.android.synthetic.main.activity_trim_video.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


enum class VideoOperation { VideoTrim, VideoImageExtract }


class TrimVideoActivity : AppCompatActivity() {
    val TAG = "TrimVideoActivity"

    var currentOperation = VideoOperation.VideoImageExtract

    lateinit var binding: ActivityTrimVideoBinding
    var videoFrameFilepath = ""
    var mDuration: Long = 0
    private var milliSecondValuePerFrame: Long = 0
    private var mCurrentStartPos: Long = 0
    private var mCurrentEndPos: Long = 0
    private var mShouldStop = false
    private var interval = 7000
    private var numThumbs = 0
    private var mVideoPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_trim_video)

        init()
    }

    private fun init() {
        val ffmpeg = FFmpeg.getInstance(this)
        mVideoPath = intent.getStringExtra("videoPath")

        loadFFmpegLibrary(ffmpeg)

        recExtractedImage.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        setVideo(mVideoPath)
        setListener(mVideoPath, ffmpeg)

        mDuration = getVideoDuration(mVideoPath)

        Handler().postDelayed({
            getFrameCountToBeExtract(mDuration)
        }, 3000)

//        extractImagesVideo(ffmpeg, 0, mDuration.toInt(), videoPath)
    }


    private fun extractVideoFrame(): ArrayList<Bitmap> {
        val f = arrayListOf<Bitmap>()
        val tempStringList = arrayListOf<Int>()
        val retriever = MediaMetadataRetriever()

        retriever.setDataSource(mVideoPath)

        for (i in 0..numThumbs) {
            f.add(
                retriever.getFrameAtTime(
                    (i * 1000000).toLong(),
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
            )
            tempStringList.add(i)
        }


        //Set extracted image recycler view adapter
        LastAdapter(f, BR.item)
            .map<Bitmap, ItemExtractVideoImageBinding>(R.layout.item_extract_video_image) {
                onBind {
                    it.binding.ivExtractedImage.setImageBitmap(it.binding.item)
                }
            }
            .into(recExtractedImage)

        //Adjust seekbar width according to recycler view item length
        val width =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                40f,
                resources.displayMetrics
            )

        rangeSlider.layoutParams = FrameLayout.LayoutParams(
            (width * f.size).toInt(),
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        rangeSlider.setTickCount((mDuration / 1000).toInt())
        rangeSlider.setRangeIndex(0, (mDuration / 1000).toInt())


        rangeSlider.setRangeChangeListener { rangeSlider: RangeSlider?, leftIndex: Int, rightIndex: Int ->
            tvLeft.text = "${getTrimmedVideoDuration(leftIndex)}"
            tvRight.text = "${getTrimmedVideoDuration(rightIndex)}"

            tvTrimDuration.text =
                "Duration : ${getTrimmedVideoDuration(rightIndex - leftIndex)}"

            mShouldStop = true

            if (mCurrentStartPos != (leftIndex * 1000).toLong()) {
                mCurrentStartPos = (leftIndex * 1000).toLong()
                videoView.seekTo(mCurrentStartPos.toInt())
            }

            mCurrentEndPos = (rightIndex * 1000).toLong()
        }

        retriever.release()
        progressBar.hide()
        return f
    }

    private fun setListener(videoPath: String, ffmpeg: FFmpeg) {
        btnTrim.setOnClickListener {
            currentOperation = VideoOperation.VideoTrim
            executeCutVideoCommand(ffmpeg, videoPath, mCurrentStartPos, mCurrentEndPos)
        }
    }

    private fun getVideoDuration(videoPath: String): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, Uri.fromFile(File(videoPath)))

//        val videoLengthInMs =
//            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toInt() * 1000

        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        return time.toLong()
    }


    private fun setVideo(videoPath: String, isShowController: Boolean = false) {
        setUpVideoPlayer(videoPath, isShowController)
    }

    private fun setUpVideoPlayer(videoPath: String?, showController: Boolean) {
        val mediaController = MediaController(this)
        mediaController.setMediaPlayer(videoView)

        if (showController)
            videoView.setMediaController(mediaController)
        else
            videoView.setMediaController(null)

        videoView.setVideoPath(videoPath)
        videoView.setOnPreparedListener { videoView.start() }
    }

    /**
     * Extract frame images from video
     * @param ffmpeg FFmpeg instace
     * @param startMs start time in milli second
     * @param endMs end time in milli second
     * @param selectedVideoPath video path from which extratc images
     */
    private fun extractImagesVideo(
        ffmpeg: FFmpeg,
        startMs: Int,
        endMs: Int,
        selectedVideoPath: String
    ) {
        val moviesDir: File = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val filePrefix = "extract_picture"
        val fileExtn = ".jpg"
        val yourRealPath: String =
            getPath(this@TrimVideoActivity, Uri.fromFile(File(selectedVideoPath)))

        var dir = File(moviesDir, "VideoEditor")
        var fileNo = 0
        while (dir.exists()) {
            fileNo++
            dir = File(moviesDir, "VideoEditor$fileNo")
        }

        dir.mkdir()
        videoFrameFilepath = dir.absolutePath
        val dest = File(dir, "$filePrefix%03d$fileExtn")
        Log.d(TAG, "startTrim: src: $yourRealPath")
        Log.d(TAG, "startTrim: dest: " + dest.absolutePath)

        val ultrafast = "-c:v,libx264,-preset,ultrafast"

        val frameDivision = getFrameCountToBeExtract(mDuration)

        val complexCommand = arrayOf(
            "-y",
            "-i",
            yourRealPath,
            "-an",
            "-r",
            "1/${frameDivision}",
            "-ss",
            "" + startMs / 1000,
            "-t",
            "" + (endMs - startMs) / 1000,
            dest.absolutePath
        )


        val c2 = arrayOf(
            "-y",
            "-i",
            yourRealPath,
            "-r",
            "1/${frameDivision}",
            "-an",
            "-ss",
            "" + startMs / 1000,
            "-t",
            "" + (endMs - startMs) / 1000,
            "-s",
            " 318x180",
            "-vsync",
            "1",
            "-threads",
            "4",
            dest.absolutePath
        )


        Log.e(TAG, "Extract command : " + Arrays.toString(c2))
        executeFFmpegBinary(ffmpeg, c2, dest)
    }

    private fun getFrameCountToBeExtract(duration: Long): Int {
        if (duration <= 7000) {
            interval = 1
            numThumbs = (duration / 1000).toInt()
            extractVideoFrame()
            return 1
        } else {
            milliSecondValuePerFrame = duration / 7000
            interval = milliSecondValuePerFrame.toInt()
            numThumbs = 7
            extractVideoFrame()
            return milliSecondValuePerFrame.toInt()
        }
    }


    private fun executeCutVideoCommand(
        ffmpeg: FFmpeg,
        realPath: String,
        startMs: Long,
        endMs: Long
    ) {
        val destPath = "/storage/emulated/0/mediapicker/videos/trimmedVideo/"
        val externalStoragePublicDirectory = File(destPath)
        if (if (!externalStoragePublicDirectory.exists()) externalStoragePublicDirectory.mkdir() else true) {
            val filePrefix = realPath.substring(realPath.lastIndexOf("."))
            val destFileName = "cut_video"
            val isFastMode = true


            var dest: File = if (filePrefix == ".webm" || filePrefix == ".mkv") File(
                externalStoragePublicDirectory,
                "$destFileName.mp4"
            ) else File(externalStoragePublicDirectory, destFileName + filePrefix)


            var fileNo = 0
            while (dest.exists()) {
                fileNo++
                dest = if (filePrefix == ".webm" || filePrefix == ".mkv") File(
                    externalStoragePublicDirectory,
                    "$destFileName$fileNo.mp4"
                ) else File(externalStoragePublicDirectory, destFileName + fileNo + filePrefix)
            }



            Log.d(TAG, "startTrim: src: $realPath")
            Log.d(TAG, "startTrim: dest: " + dest.absolutePath)
            Log.d(TAG, "startTrim: startMs: $startMs")
            Log.d(TAG, "startTrim: endMs: $endMs")

            val filePath = dest.absolutePath

            val complexCommand =
                if (isFastMode) if (filePrefix == ".webm" || filePrefix == ".mkv" || filePrefix == ".m4v" || filePrefix == ".mov") arrayOf(
                    "-ss",
                    "" + startMs / 1000,
                    "-y",
                    "-i",
                    realPath,
                    "-preset",
                    "ultrafast",
                    "-t",
                    "" + (endMs - startMs) / 1000,
                    "-vcodec",
                    "mpeg4",
                    "-b:v",
                    "2097152",
                    "-b:a",
                    "48000",
                    "-ac",
                    "2",
                    "-ar",
                    "22050",
                    "-strict",
                    "-2",
                    filePath
                ) else arrayOf(
                    "-y",
                    "-i",
                    realPath,
                    "-preset",
                    "ultrafast",
                    "-ss",
                    "" + startMs / 1000,
                    "-t",
                    "" + (endMs - startMs) / 1000,
                    "-c",
                    "copy",
                    filePath
                ) else if (filePrefix == ".webm" || filePrefix == ".mkv" || filePrefix == ".m4v" || filePrefix == ".mov") arrayOf(
                    "-ss",
                    "" + startMs / 1000,
                    "-y",
                    "-i",
                    realPath,
                    "-t",
                    "" + (endMs - startMs) / 1000,
                    "-vcodec",
                    "mpeg4",
                    "-b:v",
                    "2097152",
                    "-b:a",
                    "48000",
                    "-ac",
                    "2",
                    "-ar",
                    "22050",
                    "-strict",
                    "-2",
                    filePath
                ) else arrayOf(
                    "-y",
                    "-i",
                    realPath,
                    "-ss",
                    "" + startMs / 1000,
                    "-t",
                    "" + (endMs - startMs) / 1000,
                    "-c",
                    "copy",
                    filePath
                )
            executeFFmpegBinary(ffmpeg, complexCommand, dest)
        }
    }

    /**
     * Initially load ffmpeg library
     */
    private fun loadFFmpegLibrary(
        ffmpeg: FFmpeg
    ) {
        ffmpeg.loadBinary(object : FFmpegLoadBinaryResponseHandler {
            override fun onFinish() {
            }

            override fun onSuccess() {
            }

            override fun onFailure() {
            }

            override fun onStart() {
            }
        })
    }


    /**
     * Execute ffmpeg command
     * @param ffmpeg Fmpeg instace
     * @param command command to be execute
     * @param destination destination file
     */
    private fun executeFFmpegBinary(ffmpeg: FFmpeg, command: Array<String>, destination: File) {
        try {
            ffmpeg.execute(command, object : ExecuteBinaryResponseHandler() {
                override fun onFailure(s: String) {
                    Log.d(TAG, "FAILED with output : $s")
                }

                override fun onSuccess(s: String) {
                    Log.d(TAG, "SUCCESS with output : $s")
                    if (currentOperation == VideoOperation.VideoImageExtract) {
//                        setUpExtractedImageList()
                    } else {
                        setVideo(destination.path, true)
                        containerExtractedImage.hide()
//                        setUpVideoPlayer(destination.path)
                    }
                }

                override fun onProgress(s: String) {
                    Log.d(TAG, "Progress command : ffmpeg $command")
                }

                override fun onStart() {
                    Log.d(TAG, "Started command : ffmpeg $command")
                    progressBar.show()
                }

                override fun onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg $command")
                    progressBar.hide()
                }
            })
        } catch (e: FFmpegCommandAlreadyRunningException) {
            Log.d(TAG, "In exception : ffmpeg $e")
        }
    }


    /**
     * Set up recycler view for extracted image(Extract by ffmpef command, currently not used) from video
     */
    /*private fun setUpExtractedImageList() {
        val f = ArrayList<String>()

        val dir = File(videoFrameFilepath)
        val listFile: Array<File>? = dir.listFiles()

        for (e in listFile.orEmpty()) {
            f.add(e.absolutePath)
        }


////        //Set extracted image recycler view adapter
        LastAdapter(f, BR.item)
            .map<String, ItemExtractVideoImageBinding>(R.layout.item_extract_video_image) {
                onBind {
                    val bmp = BitmapFactory.decodeFile(it.binding.item)
                    it.binding.ivExtractedImage.setImageBitmap(bmp)
                }
            }
            .into(recExtractedImage)

        //Adjust seekbar width according to recycler view item length
        val width =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                40f,
                resources.displayMetrics
            )

        rangeSlider.layoutParams = FrameLayout.LayoutParams(
            (width * f.size).toInt(),
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        rangeSlider.setTickCount((mDuration / 1000).toInt())
        rangeSlider.setRangeIndex(0, (mDuration / 1000).toInt())


        rangeSlider.setRangeChangeListener { rangeSlider: RangeSlider?, leftIndex: Int, rightIndex: Int ->
            tvLeft.text = "${getTrimmedVideoDuration(leftIndex)}"
            tvRight.text = "${getTrimmedVideoDuration(rightIndex)}"

            tvTrimDuration.text =
                "Duration : ${getTrimmedVideoDuration(rightIndex - leftIndex)}"

            mShouldStop = true

            if (mCurrentStartPos != (leftIndex * 1000).toLong()) {
                mCurrentStartPos = (leftIndex * 1000).toLong()
                videoView.seekTo(mCurrentStartPos.toInt())
            }

            mCurrentEndPos = (rightIndex * 1000).toLong()
        }
    }*/

    private fun trackProgress() {
        Thread(Runnable {
            run {
                while (mShouldStop) {
                    if (videoView != null && videoView.isPlaying) {
                        if (videoView.currentPosition >= mCurrentEndPos) {
                            videoView.stopPlayback()
                            mShouldStop = false
                        }
                        try {
                            Thread.sleep(1000)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }).start()
    }


    /**
     * Get video duration runtime while trimming video
     * @param difference difference between range
     */
    private fun getTrimmedVideoDuration(difference: Int): String {
        return if (difference > 60) {
            val i = difference / 60
            if (i > 60) {
                getTrimmedVideoDuration(difference)
            } else {
                "$i:${difference % 60}"
            }
        } else if (difference == 60) {
            "1:00"
        } else {
            "0:${difference}"
        }
    }
}
