package com.example.imagepickerdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.imagepickerdemo.databinding.ActivityDisplayImageBinding
import kotlinx.android.synthetic.main.activity_display_image.*

class DisplayImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDisplayImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_display_image)

        Glide.with(this)
            .load(intent.getStringExtra("uri"))
            .into(ivImage)
    }
}