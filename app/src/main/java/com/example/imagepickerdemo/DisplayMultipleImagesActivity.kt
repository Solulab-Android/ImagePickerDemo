package com.example.imagepickerdemo

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.example.imagepickerdemo.databinding.ActivityDisplayMultipleImagesBinding
import com.example.imagepickerdemo.databinding.ImageSliderItemBinding
import kotlinx.android.synthetic.main.activity_display_multiple_images.*


class DisplayMultipleImagesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDisplayMultipleImagesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_display_multiple_images)

        val uriList = intent.getParcelableArrayListExtra<Uri>("uriList")

        viewPager.adapter = ImageSliderAdapter(this,uriList as ArrayList<Uri>)
    }

    class ImageSliderAdapter(private val context: Context, uriList : ArrayList<Uri>) : PagerAdapter() {


        private var inflater: LayoutInflater? = null
        private val list= uriList

        override fun isViewFromObject(view: View, `object`: Any): Boolean {

            return view === `object`
        }

        override fun getCount(): Int {

            return list.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {

            inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val binding: ImageSliderItemBinding = DataBindingUtil.inflate(
                inflater!!, R.layout.image_slider_item, container, false
            )
            Glide.with(context)
                .load(list[position])
                .into(binding.imageViewSlide)

            val vp = container as ViewPager
            vp.addView(binding.root, 0)
            return binding.root
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val vp = container as ViewPager
            val view = `object` as View
            vp.removeView(view)
        }

    }
}