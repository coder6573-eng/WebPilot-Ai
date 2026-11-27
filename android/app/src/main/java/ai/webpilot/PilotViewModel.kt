package ai.webpilot

import android.app.Application
import android.util.Base64
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PilotEvent(val type:String,val message:String)

class PilotViewModel(app:Application):AndroidViewModel(app){
 private val client=OkHttpClient.Builder().readTimeout(0,TimeUnit.MILLISECONDS).build()
 var baseUrl by mutableStateOf("http://10.0.2.2:8000")
 var sessionId by mutableStateOf("")
 var connected by mutableStateOf(false)
 var status by mutableStateOf("idle")
 var message by mutableStateOf("Connect to your WebPilot backend.")
 var url by mutableStateOf("")
 var instruction by mutableStateOf("")
 var screenshot by mutableStateOf<ByteArray?>(null)
 var error by mutableStateOf("")
 val events= mutableStateListOf<PilotEvent>()
 private var socket:WebSocket?=null
 private var approvalId=""
 private val seenEvents=mutableSetOf<String>()

 fun connect(){viewModelScope.launch{
  try{
   val data=withContext(Dispatchers.IO){
    val req=Request.Builder().url("$baseUrl/api/sessions").post(ByteArray(0).toRequestBody(null)).build()
    client.newCall(req).execute().use{res->if(!res.isSuccessful)error("HTTP "+res.code);JSONObject(res.body?.string().orEmpty())}
   }
   sessionId=data.getString("session_id");connected=true;error="";message="Remote browser connected.";add("system",message);openStream()
  }catch(e:Exception){connected=false;error=e.message?:"Connection failed";message="Backend connection failed."}
 }}
 private fun openStream(){
  socket?.cancel()
  val endpoint=baseUrl.replaceFirst("https://","wss://").replaceFirst("http://","ws://")+"/api/sessions/$sessionId/stream"
  socket=client.newWebSocket(Request.Builder().url(endpoint).build(),object:WebSocketListener(){
   override fun onMessage(ws:WebSocket,text:String){
    try{
     val data=JSONObject(text);if(data.has("image"))screenshot=Base64.decode(data.getString("image"),Base64.DEFAULT)
     url=data.optString("url",url);status=data.optString("status",status);message=data.optString("message",message)
     val list=data.optJSONArray("events")
     if(list!=null)for(i in 0 until list.length()){
      val e=list.getJSONObject(i);add(e.optString("type"),e.optString("message"))
      if(e.optString("type")=="approval")approvalId=e.optString("approval_id")
     }
    }catch(_:Exception){}
   }
   override fun onFailure(ws:WebSocket,t:Throwable,res:Response?){connected=false;error=t.message?:"Live connection lost"}
  })
 }
 suspend fun runTask(){
  try{
   val result=post("/api/sessions/$sessionId/tasks",JSONObject().put("instruction",instruction.trim()).toString())
   if(result.optBoolean("accepted")){add("task","Task submitted.");instruction=""}else error=result.optString("detail")
  }catch(e:Exception){error=e.message?:"Task request failed"}
 }
 suspend fun stopTask(){
  try{post("/api/sessions/$sessionId/tasks/stop","{}");status="paused";message="Task stopped.";add("paused",message)}catch(e:Exception){error=e.message?:"Could not stop task"}
 }
 suspend fun resolveApproval(approved:Boolean){
  if(approvalId.isBlank())return
  try{post("/api/sessions/$sessionId/approvals/$approvalId",JSONObject().put("approved",approved).toString());add(if(approved)"approved" else "rejected",if(approved)"Approval sent." else "Action rejected.");approvalId=""}
  catch(e:Exception){error=e.message?:"Approval failed"}
 }
 suspend fun refresh(){
  try{val data=get("/api/sessions/$sessionId/screenshot");screenshot=Base64.decode(data.getString("image"),Base64.DEFAULT);url=data.optString("url",url)}
  catch(e:Exception){error=e.message?:"Refresh failed"}
 }
 private suspend fun post(path:String,body:String)=withContext(Dispatchers.IO){
  val req=Request.Builder().url(baseUrl+path).post(body.toRequestBody("application/json".toMediaType())).build()
  client.newCall(req).execute().use{res->val raw=res.body?.string().orEmpty();if(!res.isSuccessful)throw IllegalStateException("HTTP "+res.code+": "+raw);JSONObject(raw)}
 }
 private suspend fun get(path:String)=withContext(Dispatchers.IO){
  val req=Request.Builder().url(baseUrl+path).get().build()
  client.newCall(req).execute().use{res->val raw=res.body?.string().orEmpty();if(!res.isSuccessful)throw IllegalStateException("HTTP "+res.code+": "+raw);JSONObject(raw)}
 }
 private fun add(type:String,text:String){val key=type+"|"+text;if(text.isBlank()||!seenEvents.add(key))return;events.add(PilotEvent(type,text));while(events.size>50)events.removeAt(0)}
 override fun onCleared(){socket?.close(1000,"App closed");client.dispatcher.executorService.shutdown()}
}
