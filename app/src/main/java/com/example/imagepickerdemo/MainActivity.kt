package com.example.imagepickerdemo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.imagepickerdemo.databinding.ActivityMainBinding
import com.livinglifetechway.k4kotlin.core.startActivity
import com.yalantis.ucrop.UCrop
import gun0912.tedimagepicker.builder.TedImagePicker
import kotlinx.android.synthetic.main.activity_main.*
import net.alhazmy13.mediapicker.Video.VideoPicker


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        btnSingleImage.setOnClickListener {
            TedImagePicker.with(this)
                .start { uri -> showSingleImage(uri) }
        }

        btnMultipleImage.setOnClickListener {
            TedImagePicker.with(this)
                .startMultiImage { uriList -> showMultiImage(uriList) }
        }

        btnVideo.setOnClickListener {
            VideoPicker.Builder(this)
                .mode(VideoPicker.Mode.GALLERY)
                .directory(VideoPicker.Directory.DEFAULT)
                .extension(VideoPicker.Extension.MP4)
                .build()
        }

        btnMergeVideo.setOnClickListener {
            startActivity(Intent(this@MainActivity, MergeVideoActivity::class.java))
        }
    }

    private fun showMultiImage(uriList: List<Uri>) {
        val intent = Intent(this, DisplayMultipleImagesActivity::class.java)
        intent.putExtra("uriList", uriList as ArrayList)
        startActivity(intent)
    }

    private fun showSingleImage(uri: Uri) {
        UCrop
            .of(uri, uri)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                UCrop.REQUEST_CROP -> {
                    val uri = data?.let { UCrop.getOutput(it) }
                    val intent = Intent(this, DisplayImageActivity::class.java)
                    intent.putExtra("uri", uri?.path)
                    startActivity(intent)
                }


                VideoPicker.VIDEO_PICKER_REQUEST_CODE -> {
                    val mPaths: java.util.ArrayList<String>? =
                        data?.getStringArrayListExtra(VideoPicker.EXTRA_VIDEO_PATH)

                    val intent = Intent(this, TrimVideoActivity::class.java)
                    intent.putExtra("videoPath", mPaths.orEmpty()[0])
                    startActivity(intent)
                }
            }
        }
    }
}