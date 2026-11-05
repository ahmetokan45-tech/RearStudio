package de.rearstudio

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*

object StudioUi {
    const val BG=0xff101116.toInt();const val PANEL=0xff1a1c23.toInt();const val TEXT=0xfff3f3f5.toInt();const val SUB=0xffa6a9b4.toInt()
    fun text(c:Context,key:String):String {val id=c.resources.getIdentifier("s_$key","string",c.packageName);return if(id!=0)c.getString(id)else key}
    fun dp(c:Context,n:Int)=(n*c.resources.displayMetrics.density).toInt()
    fun shape(c:Context,color:Int,radius:Int=18)=GradientDrawable().apply{setColor(color);cornerRadius=dp(c,radius).toFloat()}
    fun label(c:Context,value:String,size:Float=14f,muted:Boolean=false)=TextView(c).apply{text=value;textSize=size;setTextColor(if(muted)SUB else TEXT);setLineSpacing(dp(c,2).toFloat(),1f)}
    fun column(c:Context)=LinearLayout(c).apply{orientation=LinearLayout.VERTICAL}
    fun card(c:Context,parent:LinearLayout):LinearLayout=column(c).apply{background=shape(c,PANEL);setPadding(dp(c,18),dp(c,18),dp(c,18),dp(c,18));parent.addView(this,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(c,14)})}
    fun button(c:Context,parent:LinearLayout,title:String,primary:Boolean=false,action:()->Unit)=Button(c).apply{
        text=title;isAllCaps=false;textSize=14f;minHeight=dp(c,48);minimumWidth=0;minWidth=0
        setTextColor(if(primary)BG else TEXT);backgroundTintList=android.content.res.ColorStateList.valueOf(if(primary)StudioScene.load(c).accent else 0xff282a34.toInt())
        parent.addView(this,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(c,8)});setOnClickListener{action()}
    }
    fun title(c:Context,parent:LinearLayout,key:String){parent.addView(label(c,text(c,key),28f).apply{typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);setPadding(0,dp(c,14),0,dp(c,16))})}
}
