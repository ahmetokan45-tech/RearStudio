package de.rearstudio

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import android.view.View

/** App-private picked video. Muted, looping, and released whenever the view is hidden/detached. */
class StudioVideoLayer(c:Context,private val profile:()->StudioProfile):TextureView(c),TextureView.SurfaceTextureListener {
    private var player:MediaPlayer?=null
    private var output:Surface?=null
    private var key=""
    private var attached=false
    private val handler=Handler(Looper.getMainLooper())
    private val pulse=object:Runnable{override fun run(){if(!attached)return;update();handler.postDelayed(this,700)}}
    init{isOpaque=false;surfaceTextureListener=this}
    private fun release(){player?.let{try{it.release()}catch(_:RuntimeException){}};player=null;output?.release();output=null;key=""}
    private fun update(){val p=profile();val next=p.photo.takeIf{p.background=="video"&&windowVisibility==View.VISIBLE&&isShown}?:""
        if(next==key)return
        release();if(next.isBlank()||!isAvailable)return
        key=next;val texture=surfaceTexture?:return;output=Surface(texture)
        try{player=MediaPlayer().apply{setDataSource(next);setSurface(output);isLooping=true;setVolume(0f,0f);setOnPreparedListener{if(attached&&windowVisibility==View.VISIBLE)it.start()};setOnErrorListener{_,_,_->this@StudioVideoLayer.release();true};prepareAsync()}}catch(_:Exception){release()}
    }
    override fun onAttachedToWindow(){super.onAttachedToWindow();attached=true;handler.post(pulse)}
    override fun onDetachedFromWindow(){attached=false;handler.removeCallbacks(pulse);release();super.onDetachedFromWindow()}
    override fun onWindowVisibilityChanged(visibility:Int){super.onWindowVisibilityChanged(visibility);if(visibility!=View.VISIBLE)release()else if(attached)update()}
    override fun onSurfaceTextureAvailable(surface:SurfaceTexture,width:Int,height:Int){update()}
    override fun onSurfaceTextureSizeChanged(surface:SurfaceTexture,width:Int,height:Int){}
    override fun onSurfaceTextureDestroyed(surface:SurfaceTexture):Boolean{release();return true}
    override fun onSurfaceTextureUpdated(surface:SurfaceTexture){}
}
