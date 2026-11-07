package de.rearstudio

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*

object StudioUi {
    const val BG=0xff090c12.toInt();const val PANEL=0xff161d29.toInt();const val TEXT=0xfff5f7fa.toInt();const val SUB=0xffa8b4c4.toInt()
    fun text(c:Context,key:String):String {val id=c.resources.getIdentifier("s_$key","string",c.packageName);return if(id!=0)c.getString(id)else key}
    fun dp(c:Context,n:Int)=(n*c.resources.displayMetrics.density).toInt()
    fun shape(c:Context,color:Int,radius:Int=18)=GradientDrawable().apply{setColor(color);cornerRadius=dp(c,radius).toFloat()}
    fun label(c:Context,value:String,size:Float=14f,muted:Boolean=false)=TextView(c).apply{text=value;textSize=size;setTextColor(if(muted)SUB else TEXT);setLineSpacing(dp(c,2).toFloat(),1f)}
    fun column(c:Context)=LinearLayout(c).apply{orientation=LinearLayout.VERTICAL}
    fun premiumSurface(c:Context,accent:Int=0xfff59b23.toInt())=GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(0xff273142.toInt(),PANEL,0xff10151f.toInt())
    ).apply {
        cornerRadius=dp(c,22).toFloat()
        setStroke(dp(c,1),0xff455164.toInt())
    }
    fun card(c:Context,parent:LinearLayout):LinearLayout=column(c).apply{
        background=premiumSurface(c)
        elevation=dp(c,3).toFloat()
        setPadding(dp(c,18),dp(c,18),dp(c,18),dp(c,18))
        parent.addView(this,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(c,14)})
    }
    fun button(c:Context,parent:LinearLayout,title:String,primary:Boolean=false,action:()->Unit)=Button(c).apply{
        text=title;isAllCaps=false;textSize=13f;minHeight=dp(c,42);minimumWidth=0;minWidth=0
        typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
        val accent=if(primary)StudioScene.load(c).accent else 0xff263144.toInt()
        setTextColor(if(primary)BG else TEXT)
        backgroundTintList=null
        background=GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            if(primary) intArrayOf(accent,accent) else intArrayOf(0xff344158.toInt(),0xff1b2433.toInt())
        ).apply{cornerRadius=dp(c,13).toFloat();setStroke(dp(c,1),if(primary)accent else 0xff46546b.toInt())}
        setPadding(dp(c,8),0,dp(c,8),0)
        parent.addView(this,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(c,8)});setOnClickListener{action()}
    }
    fun title(c:Context,parent:LinearLayout,key:String){parent.addView(label(c,text(c,key),28f).apply{typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);setPadding(0,dp(c,14),0,dp(c,16))})}
}
