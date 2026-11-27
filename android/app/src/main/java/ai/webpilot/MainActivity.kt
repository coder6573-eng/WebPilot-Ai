package ai.webpilot
import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
private val bg=Color(0xFF0B1220);private val panel=Color(0xFF142033);private val card=Color(0xFF1D2D45);private val blue=Color(0xFF79AFFF);private val muted=Color(0xFF91A3BA);private val ink=Color(0xFFF2F6FC)
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{PilotApp()}}}
@Composable fun PilotApp(vm:PilotViewModel=viewModel()){
 var settings by remember{mutableStateOf(false)}
 val context=LocalContext.current
 LaunchedEffect(Unit){vm.ensureBrowser(context)}
 MaterialTheme(colorScheme=darkColorScheme(primary=blue,background=bg,surface=panel,onSurface=ink)){Surface(Modifier.fillMaxSize(),color=bg){Column{
  Row(Modifier.fillMaxWidth().background(panel).statusBarsPadding().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Explore,null,tint=blue,modifier=Modifier.size(29.dp));Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)){Text("WebPilot",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("STANDALONE BROWSER AGENT",fontSize=9.sp,color=muted)};Box(Modifier.size(8.dp).clip(CircleShape).background(if(vm.keySaved)Color(0xFF4AD6A0) else Color(0xFFFFB45E)));IconButton(onClick={settings=true}){Icon(Icons.Default.Settings,null,tint=muted)}}
  Box(Modifier.weight(1f).fillMaxWidth()){if(vm.tab=="agent")AgentScreen(vm)else BrowserScreen(vm)}
  Row(Modifier.fillMaxWidth().background(panel).navigationBarsPadding().padding(7.dp),horizontalArrangement=Arrangement.SpaceEvenly){Nav("Agent",Icons.Default.SmartToy,vm.tab=="agent"){vm.tab="agent"};Nav("Browser",Icons.Default.Language,vm.tab=="browser"){vm.tab="browser"}}
 }};if(settings)Settings(vm){settings=false}}
}
@Composable private fun Nav(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,active:Boolean,click:()->Unit){Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable{click()}.padding(horizontal=42.dp,vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,tint=if(active)blue else muted);Text(label,color=if(active)blue else muted,fontSize=11.sp)}}
@Composable private fun Card(mod:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Column(mod.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(panel).padding(13.dp),content=content)}
@Composable private fun AgentScreen(vm:PilotViewModel){Column(Modifier.fillMaxSize().padding(15.dp)){
 Text("Agent workspace",fontSize=23.sp,fontWeight=FontWeight.Bold);Text("Tell WebPilot what to do on the web.",fontSize=12.sp,color=muted);Spacer(Modifier.height(12.dp))
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Card(Modifier.weight(1f)){Text("MODEL",fontSize=9.sp,color=muted);Text(vm.model,fontSize=12.sp,fontWeight=FontWeight.SemiBold)};Card(Modifier.weight(1f)){Text("STATUS",fontSize=9.sp,color=muted);Text(vm.status,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}}
 Spacer(Modifier.height(10.dp));Card(Modifier.weight(1f)){Text(vm.message,fontSize=12.sp,color=muted);Spacer(Modifier.height(8.dp));LazyColumn(Modifier.weight(1f,false)){items(vm.events.reversed()){e->Column(Modifier.padding(vertical=4.dp)){Text(e.message,fontSize=12.sp);Text(e.type.uppercase(),fontSize=9.sp,color=muted)}}}}
 Spacer(Modifier.height(10.dp));Card{Text("Your instruction",fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));BasicTextField(vm.instruction,{vm.instruction=it},Modifier.fillMaxWidth().heightIn(min=75.dp).background(bg,RoundedCornerShape(10.dp)).padding(12.dp),textStyle=androidx.compose.ui.text.TextStyle(color=ink,fontSize=14.sp),decorationBox={inner->Box{if(vm.instruction.isBlank())Text("Search, research, compare, browse…",color=muted,fontSize=12.sp);inner()}})
 Spacer(Modifier.height(8.dp));Button(onClick={vm.runTask()},enabled=vm.keySaved&&!vm.busy&&vm.instruction.isNotBlank(),modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.PlayArrow,null);Text(if(vm.busy)"Working…" else "Run agent")}
 if(vm.busy)OutlinedButton(onClick={vm.stopTask()},modifier=Modifier.fillMaxWidth()){Text("Stop")}
 if(vm.error.isNotBlank())Text(vm.error,color=Color(0xFFFF9999),fontSize=10.sp)
 if(!vm.keySaved)Text("Save a Gemini API key in Settings to enable the agent.",color=Color(0xFFFFC879),fontSize=11.sp)
 }
}}
@SuppressLint("SetJavaScriptEnabled")
@Composable private fun BrowserScreen(vm:PilotViewModel){var address by remember{mutableStateOf(vm.currentUrl)}
 Column(Modifier.fillMaxSize().padding(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("Browser",fontSize=22.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick={vm.refresh()}){Icon(Icons.Default.Refresh,null,tint=blue)}}
 Row(Modifier.fillMaxWidth().background(card,RoundedCornerShape(12.dp)).padding(horizontal=9.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Lock,null,tint=muted);BasicTextField(address,{address=it},Modifier.weight(1f).padding(8.dp),singleLine=true,textStyle=androidx.compose.ui.text.TextStyle(color=ink,fontSize=12.sp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Go,keyboardType=KeyboardType.Uri),keyboardActions=KeyboardActions(onGo={vm.navigate(address)}));IconButton(onClick={vm.navigate(address)}){Icon(Icons.Default.ArrowForward,null,tint=blue)}}
  Spacer(Modifier.height(8.dp));AndroidView(factory={ctx->(vm.webView?:WebView(ctx).apply{settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.loadsImagesAutomatically=true;settings.useWideViewPort=true;settings.loadWithOverviewMode=true;webChromeClient=WebChromeClient();webViewClient=object:WebViewClient(){override fun onPageFinished(view:WebView,url:String){vm.currentUrl=url;vm.pageTitle=view.title?:url}};loadUrl(vm.currentUrl)}.also{vm.attach(it)})},modifier=Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),update={vm.attach(it)})
 }}
@Composable private fun Settings(vm:PilotViewModel,close:()->Unit){AlertDialog(onDismissRequest=close,containerColor=panel,title={Text("Settings")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
 Text("Google Gemini API key",fontWeight=FontWeight.SemiBold);OutlinedTextField(vm.keyInput,{vm.keyInput=it},singleLine=true,label={Text("API key")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
 Text("Encrypted on this device. Sent directly to Google's Gemini API when a task runs.",fontSize=10.sp,color=muted)
 OutlinedTextField(vm.modelInput,{vm.modelInput=it},singleLine=true,label={Text("Model")},modifier=Modifier.fillMaxWidth());Text("Default: gemini-2.5-flash",fontSize=10.sp,color=muted)
 if(vm.keySaved)Text("Key saved securely.",color=Color(0xFF4AD6A0),fontSize=11.sp);if(vm.error.isNotBlank())Text(vm.error,color=Color.Red,fontSize=11.sp)
 }},confirmButton={TextButton(onClick={vm.saveSettings();if(vm.keySaved)close()}){Text("Save")}},dismissButton={Row{TextButton(onClick={vm.clearKey()}){Text("Remove key")};TextButton(onClick=close){Text("Close")}}})}
