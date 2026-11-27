package ai.webpilot

import android.os.Bundle
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private val bg=Color(0xFF0B1220); private val panel=Color(0xFF142033)
private val card=Color(0xFF1D2D45); private val blue=Color(0xFF79AFFF)
private val muted=Color(0xFF91A3BA); private val ink=Color(0xFFF2F6FC)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{PilotApp()}}
}

@Composable fun PilotApp(vm:PilotViewModel= viewModel()){
 var tab by remember{mutableStateOf("agent")}; var settings by remember{mutableStateOf(false)}
 MaterialTheme(colorScheme=darkColorScheme(primary=blue,background=bg,surface=panel,onSurface=ink)){
  Surface(Modifier.fillMaxSize(),color=bg){Column{
   Row(Modifier.fillMaxWidth().background(panel).padding(16.dp),verticalAlignment=Alignment.CenterVertically){
    Icon(Icons.Default.Explore,null,tint=blue,modifier=Modifier.size(30.dp)); Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)){Text("WebPilot",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("AUTONOMOUS BROWSER AGENT",fontSize=9.sp,color=muted,letterSpacing=1.3.sp)}
    Box(Modifier.size(7.dp).clip(CircleShape).background(if(vm.connected)Color(0xFF4AD6A0) else Color(0xFFFFB45E)))
    Text(if(vm.connected)" ONLINE" else " OFFLINE",fontSize=10.sp,color=muted)
    IconButton(onClick={settings=true}){Icon(Icons.Default.Settings,null,tint=muted)}
   }
   if(tab=="agent") AgentScreen(vm,Modifier.weight(1f)) else BrowserScreen(vm,Modifier.weight(1f))
   Row(Modifier.fillMaxWidth().background(panel).navigationBarsPadding().padding(vertical=7.dp),horizontalArrangement=Arrangement.SpaceEvenly){
    NavButton("Agent",Icons.Default.SmartToy,tab=="agent"){tab="agent"}
    NavButton("Browser",Icons.Default.Language,tab=="browser"){tab="browser"}
   }
  }
  if(settings) Settings(vm){settings=false}
 }}
}

@Composable private fun NavButton(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,active:Boolean,onClick:()->Unit){
 Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable{onClick()}.padding(horizontal=40.dp,vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally){
  Icon(icon,null,tint=if(active)blue else muted);Text(label,color=if(active)blue else muted,fontSize=11.sp)
 }
}
@Composable private fun PanelCard(content:@Composable ColumnScope.()->Unit){
 Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(panel).padding(14.dp),content=content)
}
@Composable private fun AgentScreen(vm:PilotViewModel,modifier:Modifier=Modifier){
 val scope= rememberCoroutineScope()
 Column(modifier.fillMaxSize().padding(16.dp)){
  Text("Your workspace",fontSize=23.sp,fontWeight=FontWeight.Bold);Text("Give WebPilot a goal. Watch it work in Browser.",fontSize=12.sp,color=muted)
  Spacer(Modifier.height(14.dp))
  Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){
   PanelCard{Text("SESSION",fontSize=9.sp,color=muted);Text(if(vm.sessionId.isBlank())"Not started" else "Connected",fontWeight=FontWeight.SemiBold)}
   PanelCard{Text("TASK STATUS",fontSize=9.sp,color=muted);Text(vm.status.replace('_',' ').replaceFirstChar{it.uppercase()},fontWeight=FontWeight.SemiBold)}
  }
  Spacer(Modifier.height(12.dp))
  PanelCard{
   Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.AutoAwesome,null,tint=blue);Spacer(Modifier.width(8.dp));Column{Text("Agent activity",fontWeight=FontWeight.SemiBold);Text(vm.message,color=muted,fontSize=11.sp)}}
   Spacer(Modifier.height(8.dp))
   LazyColumn(Modifier.heightIn(max=180.dp)){items(vm.events.reversed()){e->
    Row(Modifier.padding(vertical=4.dp),verticalAlignment=Alignment.Top){Box(Modifier.padding(top=5.dp).size(6.dp).clip(CircleShape).background(blue));Spacer(Modifier.width(8.dp));Column{Text(e.message,fontSize=12.sp);Text(e.type.uppercase(),fontSize=9.sp,color=muted)}}
   }}
  }
  Spacer(Modifier.weight(1f))
  PanelCard{
   Text("What should I do?",fontWeight=FontWeight.SemiBold)
   Spacer(Modifier.height(9.dp))
   BasicTextField(value=vm.instruction,onValueChange={vm.instruction=it},modifier=Modifier.fillMaxWidth().heightIn(min=85.dp).clip(RoundedCornerShape(12.dp)).background(bg).padding(12.dp),textStyle=androidx.compose.ui.text.TextStyle(color=ink,fontSize=14.sp),decorationBox={inner->Box{if(vm.instruction.isBlank())Text("Research, compare, navigate, collect information…",color=muted,fontSize=12.sp);inner()}})
   Spacer(Modifier.height(9.dp))
   Button(onClick={scope.launch{vm.runTask()}},enabled=vm.connected&&vm.instruction.isNotBlank()&&vm.status!="running"&&vm.status!="awaiting_approval",modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)){
    Icon(Icons.Default.PlayArrow,null);Spacer(Modifier.width(5.dp));Text("Run task")
   }
   if(vm.status=="awaiting_approval"){
    Text("The agent is waiting for approval. Review the browser before proceeding.",color=Color(0xFFFFC879),fontSize=11.sp)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     Button(onClick={scope.launch{vm.resolveApproval(true)}},modifier=Modifier.weight(1f)){Text("Approve")}
     OutlinedButton(onClick={scope.launch{vm.resolveApproval(false)}},modifier=Modifier.weight(1f)){Text("Reject")}
    }
   }
   if(vm.error.isNotBlank())Text(vm.error,color=Color(0xFFFF9A9A),fontSize=10.sp)
  }
 }
}
@Composable private fun BrowserScreen(vm:PilotViewModel,modifier:Modifier=Modifier){
 val scope= rememberCoroutineScope()
 Column(modifier.fillMaxSize().padding(15.dp)){
  Text("Remote browser",fontSize=23.sp,fontWeight=FontWeight.Bold);Text("Live view of the remote Chromium session.",fontSize=12.sp,color=muted)
  Spacer(Modifier.height(12.dp))
  Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(card).padding(10.dp),verticalAlignment=Alignment.CenterVertically){
   Icon(Icons.Default.Public,null,tint=blue);Spacer(Modifier.width(8.dp));Text(vm.url.ifBlank{"Waiting for browser…"},fontSize=11.sp,maxLines=1,modifier=Modifier.weight(1f));IconButton(onClick={scope.launch{vm.refresh()}}){Icon(Icons.Default.Refresh,null,tint=muted)}
  }
  Spacer(Modifier.height(10.dp))
  Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF070B12)),Alignment.Center){
   val image=remember(vm.screenshot){try{vm.screenshot?.let{BitmapFactory.decodeByteArray(it,0,it.size)?.asImageBitmap()}}catch(_:Exception){null}}
   if(image!=null)Image(image,null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
   else Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Language,null,tint=muted,modifier=Modifier.size(42.dp));Text("Browser preview",fontWeight=FontWeight.SemiBold);Text("Connect a backend in Settings.",fontSize=11.sp,color=muted)}
  }
  Spacer(Modifier.height(10.dp))
  OutlinedButton(onClick={scope.launch{vm.refresh()}},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Visibility,null);Spacer(Modifier.width(7.dp));Text("Refresh browser view")}
 }
}
@Composable private fun Settings(vm:PilotViewModel,onClose:()->Unit){
 var address by remember{mutableStateOf(vm.baseUrl)}
 AlertDialog(onDismissRequest=onClose,containerColor=panel,title={Text("Backend connection")},text={
  Column{Text("Backend URL",color=muted,fontSize=12.sp);OutlinedTextField(value=address,onValueChange={address=it},singleLine=true);Text("Emulator: http://10.0.2.2:8000 · Phone: server LAN IP or HTTPS URL",fontSize=10.sp,color=muted);if(vm.error.isNotBlank())Text(vm.error,color=Color.Red,fontSize=11.sp)}
 },confirmButton={TextButton(onClick={vm.baseUrl=address.trim().trimEnd('/');onClose();vm.connect()}){Text("Connect")}},dismissButton={TextButton(onClick=onClose){Text("Cancel")}})
}
