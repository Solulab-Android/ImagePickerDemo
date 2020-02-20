package com.example.imagepickerdemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import com.livinglifetechway.k4kotlin.core.hide
import com.livinglifetechway.k4kotlin.core.show
import kotlinx.android.synthetic.main.activity_merge_video.*
import kotlinx.android.synthetic.main.activity_merge_video.videoView
import net.alhazmy13.mediapicker.Video.VideoPicker
import java.io.*
import java.util.*
import kotlin.collections.ArrayList


class MergeVideoActivity : AppCompatActivity() {
    val TAG = "MergeVideoActivity"
    var videoPathOne = ""
    var videoPathTwo = ""
    private var filesList: ArrayList<String> = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merge_video)


        setListener()
    }

    private fun setListener() {
        btnSelectVideo.setOnClickListener {
            VideoPicker.Builder(this)
                .mode(VideoPicker.Mode.GALLERY)
                .directory(VideoPicker.Directory.DEFAULT)
                .extension(VideoPicker.Extension.MP4)
                .build()
        }


        btnMergeVideo.setOnClickListener {
            val ffmpeg = FFmpeg.getInstance(this)
            progressBar.show()
            concatVideoCommand(ffmpeg)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                VideoPicker.VIDEO_PICKER_REQUEST_CODE -> {
                    val mPaths: java.util.ArrayList<String>? =
                        data?.getStringArrayListExtra(VideoPicker.EXTRA_VIDEO_PATH)

                    if (videoPathOne == "") {
                        videoPathOne = mPaths.orEmpty()[0]
                        filesList.add(mPaths.orEmpty()[0])
                        tvVideoPathOne.text = mPaths.orEmpty()[0]
                    } else {
                        videoPathTwo = mPaths.orEmpty()[0]
                        filesList.add(mPaths.orEmpty()[0])
                        tvVideoPathTwo.text = mPaths.orEmpty()[0]
                    }
                }
            }
        }
    }


    private fun concatVideoCommand(fFmpeg: FFmpeg) {

        val mediaStorageDir = File("/storage/emulated/0/mediapicker/videos/mergedVideo")
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
            }
        }

        val mFile = File(
            mediaStorageDir.path + File.separator +
                    "VID_" + System.currentTimeMillis() + ".mp4"
        )
        val combinedVideoPath = mFile.absolutePath

        val stringBuilder = StringBuilder()
        val filterComplex = StringBuilder()


        val ultrafast = "-c:v,libx264,-preset,ultrafast,"


        filterComplex.append(ultrafast).append("-filter_complex,")
        for (i in 0 until filesList.size) {
            stringBuilder.append("-i" + "," + filesList.get(i) + ",")
            filterComplex.append("[").append(i).append(":v").append(i).append("] [").append(i)
                .append(":a").append(i)
                .append("] ")
        }


        filterComplex.append("concat=n=").append(filesList.size).append(":v=1:a=1 [v] [a]")
        val inputCommand =
            stringBuilder.toString().split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        val filterCommand =
            filterComplex.toString().split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()


        val destinationCommand = arrayOf("-map", "[v]", "-map", "[a]", mFile.absolutePath)

        fFmpeg.loadBinary(object : FFmpegLoadBinaryResponseHandler {
            override fun onFinish() {
            }

            override fun onSuccess() {
                mergeVideo(
                    combine(inputCommand, filterCommand, destinationCommand),
                    fFmpeg,
                    mFile
                )
            }

            override fun onFailure() {
            }

            override fun onStart() {
            }
        })



        Log.e(
            TAG,
            "concatVideoCommand: " + Arrays.toString(
                combine(
                    inputCommand,
                    filterCommand,
                    destinationCommand
                )
            )
        )
    }


    fun combine(arg1: Array<String>, arg2: Array<String>, arg3: Array<String>): Array<String?> {
        val result = arrayOfNulls<String>(arg1.size + arg2.size + arg3.size)
        System.arraycopy(arg1, 0, result, 0, arg1.size)
        System.arraycopy(arg2, 0, result, arg1.size, arg2.size)
        System.arraycopy(arg3, 0, result, arg1.size + arg2.size, arg3.size)
        return result
    }

    private fun mergeVideo(
        command: Array<String?>,
        mFFMpeg: FFmpeg,
        combinedFile: File
    ) {
        try {
            mFFMpeg.execute(command, object : ExecuteBinaryResponseHandler() {
                override fun onFailure(s: String) {
                    Log.d(TAG, "FAILED with output : $s")
                }

                override fun onSuccess(s: String) {
                    Log.d(TAG, "SUCCESS with output : $s")
                    setUpVideoPlayer(combinedFile.absolutePath)
                }

                override fun onProgress(s: String) {
                    Log.d(TAG, "Progress command : ffmpeg $command")
                }

                override fun onStart() {
                    Log.d(TAG, "Started command : ffmpeg $command")
                }

                override fun onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg $command")
                    progressBar.hide()
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun setUpVideoPlayer(videoPath: String?) {
        videoView.show()
        val mediaController = MediaController(this)
        mediaController.setMediaPlayer(videoView)
        videoView.setMediaController(mediaController)

        videoView.setVideoPath(videoPath)
        videoView.setOnPreparedListener { videoView.start() }
    }
}
