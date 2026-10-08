package com.smartpos
import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class MyApplication : Application(){
 override fun onCreate(){
  super.onCreate()
  FirebaseApp.initializeApp(this)
  try{ 
    FirebaseDatabase.getInstance("https://smartpos-83781-default-rtdb.firebasedatabase.app").setPersistenceEnabled(true)
  }catch(_:Exception){}
 }
}