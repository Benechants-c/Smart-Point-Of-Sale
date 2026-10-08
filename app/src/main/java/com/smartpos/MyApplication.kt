package com.smartpos
import android.app.Application

class MyApplication : Application(){
 override fun onCreate(){
  super.onCreate()
  // ✅ NO Firebase init - We use Cloudflare Worker now (TelOne-proof)
  // Firebase was causing 7 second freeze + block
 }
}
