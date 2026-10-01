package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
class SalesActivity:Activity(){
data class P(val c:String,val n:String,val s:Double,var q:Int,val sh:String,val d:String)
data class C(val p:P,var q:Int)
val all=mutableListOf<P>()
val fil=mutableListOf<P>()
val cart=mutableListOf<C>()
lateinit var spShop:Spinner
lateinit var spDept:Spinner
lateinit var edSearch:EditText
lateinit var layProd:LinearLayout
lateinit var layCart:LinearLayout
lateinit var tvTotal:TextView
lateinit var tvRec:TextView
lateinit var edTend:EditText
lateinit var tvChange:TextView
val shops=mutableListOf<Pair<String,String>>()
val depts=mutableListOf<String>()
fun sid()=if(spShop.selectedItemPosition in shops.indices)shops[spShop.selectedItemPosition].first else "main"
fun tot():Double{var t=0.0;for(x in cart)t+=x.p.s*x.q;return t}
fun calc(){
val t=tot()
val te=edTend.text.toString().toDoubleOrNull()?:0.0
val ch=te-t
if(te==0.0)tvChange.text="CHANGE: \$0.00"
else if(ch<0)tvChange.text="NEED \$"+String.format("%.2f",-ch)
else tvChange.text="CHANGE: \$"+String.format("%.2f",ch)
}
fun load(){
all.clear()
try{
val pr=getSharedPreferences("stock_"+sid(),Context.MODE_PRIVATE)
for((code,v) in pr.all){
try{
val a=v.toString().split("|")
if(a.size>=4){
val n=a[0]
val s=a[2].toDoubleOrNull()?:0.0
val q=a[3].toIntOrNull()?:0
val d=if(a.size>5)a[5]else"G"
if(q>0)all.add(P(code,n,s,q,sid(),d))
}
}catch(_:Exception){}
}
}catch(_:Exception){}
depts.clear()
depts.add("ALL")
for(d in all.map{it.d}.distinct())depts.add(d)
spDept.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,depts)
filt()
}
fun filt(){
fil.clear()
val q=edSearch.text.toString().trim().lowercase()
val ds=if(spDept.selectedItemPosition in depts.indices)depts[spDept.selectedItemPosition] else "ALL"
for(p in all){
if((q.isEmpty()||p.n.lowercase().contains(q)||p.c.lowercase().contains(q))&&(ds=="ALL"||p.d==ds))fil.add(p)
}
showP()
}
fun showP(){
layProd.removeAllViews()
if(fil.isEmpty()){
layProd.addView(TextView(this).apply{text="No stock";setPadding(20,30,20,30)})
return
}
for(p in fil){
val row=LinearLayout(this).apply{orientation=0;setPadding(10,10,10,10);setBackgroundColor(Color.WHITE)}
row.addView(TextView(this).apply{text=p.n+"\n"+p.c+" "+p.q+" \$"+p.s;layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
row.addView(Button(this).apply{text="ADD";setOnClickListener{add(p)}})
layProd.addView(row)
}
}
fun add(p:P){
val ex=cart.find{it.p.c==p.c}
if(ex!=null){if(ex.q<p.q)ex.q++}else cart.add(C(p,1))
showC()
}
fun showC(){
layCart.removeAllViews()
var t=0.0
var tq=0
var rt=""
for(it in cart){
val l=it.p.s*it.q
t+=l
tq+=it.q
val r=LinearLayout(this).apply{orientation=0;setPadding(8,8,8,8);setBackgroundColor(Color.WHITE)}
r.addView(TextView(this).apply{text=it.p.n+" \$"+it.p.s+" x"+it.q+"=\$"+String.format("%.2f",l);layoutParams=LinearLayout.LayoutParams(0,-2,1f);setTypeface(null,Typeface.BOLD)})
val b1=Button(this).apply{text="-"}
val tv=TextView(this).apply{text=""+it.q;setPadding(8,8,8,8);setTypeface(null,Typeface.BOLD)}
val b2=Button(this).apply{text="+"}
b1.setOnClickListener{if(it.q>1){it.q--;showC()}else{cart.remove(it);showC()}}
b2.setOnClickListener{if(it.q<it.p.q){it.q++;showC()}}
r.addView(b1)
r.addView(tv)
r.addView(b2)
layCart.addView(r)
rt+=it.p.n+" x"+it.q+"=\$"+String.format("%.2f",l)+"\n"
}
if(cart.isEmpty())tvRec.text="RECEIPT: No items"
else tvRec.text=rt+"----\nTOTAL: \$"+String.format("%.2f",t)+" ITEMS:"+tq
tvTotal.text="TOTAL: \$"+String.format("%.2f",t)+" | "+tq+" items"
calc()
}
fun sale(){
if(cart.isEmpty())return
val t=tot()
val te=edTend.text.toString().toDoubleOrNull()?:0.0
if(te<t){Toast.makeText(this,"Less",1).show();return}
val ch=te-t
Toast.makeText(this,"OK Change \$"+String.format("%.2f",ch),1).show()
try{
val pr=getSharedPreferences("stock_"+sid(),Context.MODE_PRIVATE)
for(it in cart){
val old=pr.getString(it.p.c,"")?:""
if(old.contains("|")){
val a=old.split("|").toMutableList()
val oq=a.getOrNull(3)?.toIntOrNull()?:0
a[3]=(oq-it.q).toString()
pr.edit().putString(it.p.c,a.joinToString("|")).apply()
}
}
}catch(_:Exception){}
cart.clear()
edTend.setText("")
showC()
load()
}
override fun onCreate(b:Bundle?){
super.onCreate(b)
try{
val ps=getSharedPreferences("shops_db",Context.MODE_PRIVATE)
for((k,v) in ps.all)shops.add(Pair(k,k))
}catch(_:Exception){}
if(shops.isEmpty())shops.add(Pair("main","Main"))
val root=LinearLayout(this).apply{orientation=1;setPadding(10,10,10,10)}
root.addView(TextView(this).apply{text="BUILD 157 RECEIPT";setTypeface(null,Typeface.BOLD);textSize=16f})
spShop=Spinner(this)
spShop.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,shops.map{it.second})
root.addView(spShop)
spDept=Spinner(this)
depts.add("ALL")
spDept.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,depts)
root.addView(spDept)
edSearch=EditText(this).apply{hint="Search"}
root.addView(edSearch)
layProd=LinearLayout(this).apply{orientation=1}
root.addView(ScrollView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,0,1f);addView(layProd)})
root.addView(TextView(this).apply{text="CART";setBackgroundColor(Color.BLACK);setTextColor(Color.WHITE);setPadding(6,6,6,6)})
layCart=LinearLayout(this).apply{orientation=1}
root.addView(ScrollView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,0,1f);addView(layCart)})
tvRec=TextView(this).apply{text="RECEIPT";setBackgroundColor(Color.parseColor("#EEE"));setPadding(10,10,10,10);setTypeface(null,Typeface.BOLD)}
root.addView(tvRec)
tvTotal=TextView(this).apply{text="TOTAL \$0";setBackgroundColor(Color.parseColor("#DBEAFE"));setPadding(10,10,10,10);setTypeface(null,Typeface.BOLD)}
root.addView(tvTotal)
edTend=EditText(this).apply{hint="Tendered";inputType=2;layoutParams=LinearLayout.LayoutParams(-1,-2)}
root.addView(edTend)
tvChange=TextView(this).apply{text="CHANGE \$0";setPadding(10,10,10,10);setTypeface(null,Typeface.BOLD)}
root.addView(tvChange)
root.addView(Button(this).apply{text="COMPLETE SALE";setBackgroundColor(Color.parseColor("#16A34A"));setTextColor(Color.WHITE);setOnClickListener{sale()}})
spShop.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
override fun onItemSelected(a:AdapterView<*>?,v:View?,p:Int,i:Long){load()}
override fun onNothingSelected(a:AdapterView<*>?){}
}
spDept.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
override fun onItemSelected(a:AdapterView<*>?,v:View?,p:Int,i:Long){filt()}
override fun onNothingSelected(a:AdapterView<*>?){}
}
edSearch.addTextChangedListener(object:TextWatcher{
override fun afterTextChanged(s:Editable?){filt()}
override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
})
edTend.addTextChangedListener(object:TextWatcher{
override fun afterTextChanged(s:Editable?){calc()}
override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
})
setContentView(root)
load()
}
}
