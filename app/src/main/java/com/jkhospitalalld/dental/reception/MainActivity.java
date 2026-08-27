package com.jkhospitalalld.dental.reception;
import android.app.DownloadManager;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.webkit.*;
import android.widget.Toast;
import android.view.WindowManager;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.WindowCompat;
import androidx.appcompat.app.AppCompatActivity;
public class MainActivity extends AppCompatActivity {
 private static final String HOME="https://jkh-billand-pres.jkhospitalalld.workers.dev/reception.html";
 private WebView web;
 @Override protected void onCreate(Bundle b){
  super.onCreate(b);
  // Android 15+ edge-to-edge can make the IME overlap WebView content.
  // Keep the app window resizeable so focused HTML inputs stay above the keyboard.
  WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
  getWindow().setSoftInputMode(
    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
  );

  web=new WebView(this);
  web.setFocusable(true);
  web.setFocusableInTouchMode(true);
  setContentView(web);
  setup();
  web.loadUrl(startUrl(getIntent()));
  getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
   public void handleOnBackPressed(){if(web.canGoBack())web.goBack();else finish();}
  });
 }
 @Override protected void onNewIntent(Intent intent){
  super.onNewIntent(intent); setIntent(intent);
  web.loadUrl(startUrl(intent));
 }
 private String startUrl(Intent intent){
  Uri data = intent!=null? intent.getData() : null;
  if(data!=null){
   String u = data.toString();
   if(u.startsWith("https://jkh-billand-pres.jkhospitalalld.workers.dev")) return u;
  }
  return HOME;
 }
 private void setup(){
  WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
  s.setJavaScriptCanOpenWindowsAutomatically(true); s.setMediaPlaybackRequiresUserGesture(true);
  CookieManager.getInstance().setAcceptCookie(true);
  web.setWebChromeClient(new WebChromeClient());
  web.setOnFocusChangeListener((v, hasFocus) -> {
   if(hasFocus) keepFocusedFieldVisible();
  });
  web.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
   // When the IME changes the available viewport, ask the page to bring
   // the active input/textarea/select into view.
   keepFocusedFieldVisible();
  });
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return external(r.getUrl().toString());}
   @Override public boolean shouldOverrideUrlLoading(WebView v,String u){return external(u);}
  });
  web.setDownloadListener((url,userAgent,cd,mime,size)->{
   try{
    DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url));
    String c=CookieManager.getInstance().getCookie(url); if(c!=null)r.addRequestHeader("Cookie",c);
    r.addRequestHeader("User-Agent",userAgent); r.setMimeType(mime); r.setTitle(URLUtil.guessFileName(url,cd,mime));
    r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
    r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(url,cd,mime));
    ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
    Toast.makeText(this,"Download started",Toast.LENGTH_SHORT).show();
   }catch(Exception e){Toast.makeText(this,"Download could not be started",Toast.LENGTH_LONG).show();}
  });
 }
 private void keepFocusedFieldVisible(){
  if(web==null || !web.hasWindowFocus()) return;
  web.postDelayed(() -> web.evaluateJavascript(
    "(function(){var e=document.activeElement;if(e&&(e.tagName==='INPUT'||e.tagName==='TEXTAREA'||e.tagName==='SELECT')){try{e.scrollIntoView({block:'center',inline:'nearest'});}catch(x){e.scrollIntoView(true);}}})();",
    null), 120);
 }
 private boolean external(String u){
  if(u.startsWith("tel:")||u.startsWith("mailto:")||u.startsWith("whatsapp:")||
     u.startsWith("https://wa.me/")||u.startsWith("https://api.whatsapp.com/")||
     u.startsWith("https://maps.app.goo.gl/")||u.startsWith("geo:")){
   try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}catch(Exception e){}
   return true;
  }
  return !(u.startsWith("http://")||u.startsWith("https://"));
 }
}
