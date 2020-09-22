package com.kbjay.scrollscaleimageview

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.kbjay.scrollscaleimageview.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val url =
        "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1600787278710&di=434a817617978f1d362807803f40e061&imgtype=0&src=http%3A%2F%2Fa3.att.hudong.com%2F14%2F75%2F01300000164186121366756803686.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        binding.clickEvent = this
        setContentView(binding.root)
        Glide.with(this).load(url).into(iv)
    }

    public fun clickImage(view: View) {
        println("click")
        var target = object : CustomTarget<Bitmap>() {
            override fun onLoadCleared(placeholder: Drawable?) {
                println("onLoadCleared")
            }

            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                kjView.showBitmap(resource)
            }

        }
        Glide.with(this).asBitmap().load(url).into(target)
    }

}