package com.example.imagepickerdemo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.imagepickerdemo.databinding.ActivityMainBinding
import com.yalantis.ucrop.UCrop
import gun0912.tedimagepicker.builder.TedImagePicker
import kotlinx.android.synthetic.main.activity_main.*

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
                .startMultiImage { uriList -> showMultiImage(uriList)}
        }
    }

    private fun showMultiImage(uriList: List<Uri>) {
        val intent = Intent(this,DisplayMultipleImagesActivity::class.java)
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
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            val uri = data?.let { UCrop.getOutput(it) }
            val intent = Intent(this, DisplayImageActivity::class.java)
            intent.putExtra("uri", uri?.path)
            startActivity(intent)
        }
    }
}