package com.jkhospitalalld.dental.reception;

import android.app.DownloadManager;
import android.content.*;
import android.graphics.Rect;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
 private static final String HOME="https://jkh-billand-pres.jkhospitalalld.workers.dev/reception.html";
 private WebView web;
 private int screenHeightPx;
 private boolean keyboardState=false;

 @Override protected void onCreate(Bundle b){
  super.onCreate(b);

  WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
  getWindow().setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

  screenHeightPx = getResources().getDisplayMetrics().heightPixels;

  web=new WebView(this);
  web.setFocusable(true);
  web.setFocusableInTouchMode(true);
  setContentView(web);
  setup();

  /*
   * IMPORTANT:
   * Do not use OnGlobalLayoutListener here. It creates a feedback loop:
   * resizing WebView changes the visible frame, which triggers another resize,
   * which causes the violent up/down shaking seen during keyboard animation.
   *
   * WindowInsets is the single source of truth for IME visibility.
   */
  ViewCompat.setOnApplyWindowInsetsListener(web, (v, insets) -> {
   boolean open = insets.isVisible(WindowInsetsCompat.Type.ime());

   if (open != keyboardState) {
    keyboardState = open;
    applyKeyboardLayout(open);
   }

   return insets;
  });

  ViewCompat.requestApplyInsets(web);

  web.loadUrl(startUrl(getIntent()));

  getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
   public void handleOnBackPressed(){
    if(web.canGoBack()) web.goBack(); else finish();
   }
  });
 }

 private void applyKeyboardLayout(boolean open){
  if(web==null) return;

  ViewGroup.LayoutParams lp=web.getLayoutParams();

  if(open){
   // Exact requested behavior: 50% of the physical screen height.
   // Width is never modified.
   int target=Math.max(1, screenHeightPx / 2);
   lp.height=target;
  }else{
   lp.height=ViewGroup.LayoutParams.MATCH_PARENT;
  }

  web.setLayoutParams(lp);

  String js="(function(){"+
      "document.documentElement.classList.toggle('keyboard-open',"+open+");"+
      "document.body.classList.toggle('keyboard-open',"+open+");"+
      "})();";

  // One update only after the Android layout has settled.
  web.postDelayed(()->{
   if(web!=null) web.evaluateJavascript(js,null);
   if(open) keepFocusedFieldVisible();
  },100);
 }

 @Override protected void onNewIntent(Intent intent){
  super.onNewIntent(intent);
  setIntent(intent);
  web.loadUrl(startUrl(intent));
 }

 private String startUrl(Intent intent){
  Uri data = intent!=null ? intent.getData() : null;
  if(data!=null){
   String u=data.toString();
   if(u.startsWith("https://jkh-billand-pres.jkhospitalalld.workers.dev")) return u;
  }
  return HOME;
 }

 private void setup(){
  WebSettings s=web.getSettings();
  s.setJavaScriptEnabled(true);
  s.setDomStorageEnabled(true);
  s.setDatabaseEnabled(true);
  s.setJavaScriptCanOpenWindowsAutomatically(true);
  s.setMediaPlaybackRequiresUserGesture(true);

  CookieManager.getInstance().setAcceptCookie(true);
  web.setWebChromeClient(new WebChromeClient());

  web.setOnFocusChangeListener((v,hasFocus)->{
   if(hasFocus) keepFocusedFieldVisible();
  });

  web.setWebViewClient(new WebViewClient(){
   @Override public void onPageFinished(WebView view,String url){
    super.onPageFinished(view,url);
    injectKeyboardStyle();
    if(keyboardState) keepFocusedFieldVisible();
   }

   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
    return external(r.getUrl().toString());
   }

   @Override public boolean shouldOverrideUrlLoading(WebView v,String u){
    return external(u);
   }
  });

  web.setDownloadListener((url,userAgent,cd,mime,size)->{
   try{
    DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url));
    String c=CookieManager.getInstance().getCookie(url);
    if(c!=null) r.addRequestHeader("Cookie",c);
    r.addRequestHeader("User-Agent",userAgent);
    r.setMimeType(mime);
    r.setTitle(URLUtil.guessFileName(url,cd,mime));
    r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
    r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
        URLUtil.guessFileName(url,cd,mime));
    ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
    Toast.makeText(this,"Download started",Toast.LENGTH_SHORT).show();
   }catch(Exception e){
    Toast.makeText(this,"Download could not be started",Toast.LENGTH_LONG).show();
   }
  });
 }

 private void injectKeyboardStyle(){
  if(web==null) return;

  String js="(function(){"+
      "if(window.__jkhKeyboardFixV3)return;"+
      "window.__jkhKeyboardFixV3=true;"+
      "var s=document.createElement('style');"+
      "s.id='jkh-keyboard-fix-v3';"+
      "s.textContent='html,body{width:100%;max-width:100%;}'+"+
      "'html.keyboard-open,body.keyboard-open{height:100%!important;min-height:0!important;}';"+
      "document.head.appendChild(s);"+
      "})();";

  web.evaluateJavascript(js,null);
 }

 private void keepFocusedFieldVisible(){
  if(web==null || !web.hasWindowFocus()) return;

  web.postDelayed(()->web.evaluateJavascript(
    "(function(){var e=document.activeElement;"+
    "if(e&&(e.tagName==='INPUT'||e.tagName==='TEXTAREA'||e.tagName==='SELECT')){"+
    "try{e.scrollIntoView({block:'center',inline:'nearest',behavior:'instant'});}"+
    "catch(x){e.scrollIntoView(true);}}})();",null),150);
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
