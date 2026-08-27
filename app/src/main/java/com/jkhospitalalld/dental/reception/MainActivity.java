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
 private int normalWebHeight = ViewGroup.LayoutParams.MATCH_PARENT;

 @Override protected void onCreate(Bundle b){
  super.onCreate(b);

  // Keep the content area separate from the IME. We handle the IME height
  // explicitly below so the WebView always has a usable typing area.
  WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
  getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

  web=new WebView(this);
  web.setFocusable(true);
  web.setFocusableInTouchMode(true);
  setContentView(web);
  setup();

  // Force the WebView to occupy the available area above the keyboard.
  // When the keyboard closes, restore full height. Width is never changed.
  ViewCompat.setOnApplyWindowInsetsListener(web, (v, insets) -> {
   Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
   boolean keyboardOpen = insets.isVisible(WindowInsetsCompat.Type.ime());
   ViewGroup.LayoutParams lp = web.getLayoutParams();

   if (keyboardOpen && ime.bottom > 0) {
    int rootHeight = web.getRootView().getHeight();
    int targetHeight = Math.max(1, rootHeight - ime.bottom);
    lp.height = targetHeight;
    web.setLayoutParams(lp);
    syncPageViewport(true, targetHeight);
   } else {
    lp.height = normalWebHeight;
    web.setLayoutParams(lp);
    syncPageViewport(false, web.getHeight());
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

  web.getViewTreeObserver().addOnGlobalLayoutListener(this::onKeyboardLayoutChanged);

  web.setWebViewClient(new WebViewClient(){
   @Override public void onPageFinished(WebView view,String url){
    super.onPageFinished(view,url);
    injectKeyboardViewportScript();
    keepFocusedFieldVisible();
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

 private void onKeyboardLayoutChanged(){
  if(web==null) return;
  Rect r=new Rect();
  web.getWindowVisibleDisplayFrame(r);
  int full=web.getRootView().getHeight();
  int visible=r.height();
  boolean keyboardOpen=full-visible > Math.max(180,(int)(full*0.18));

  if(keyboardOpen){
   int target=Math.max(1,visible);
   ViewGroup.LayoutParams lp=web.getLayoutParams();
   if(lp.height!=target){
    lp.height=target;
    web.setLayoutParams(lp);
   }
   syncPageViewport(true,target);
   keepFocusedFieldVisible();
  }else{
   ViewGroup.LayoutParams lp=web.getLayoutParams();
   if(lp.height!=normalWebHeight){
    lp.height=normalWebHeight;
    web.setLayoutParams(lp);
   }
   syncPageViewport(false,web.getHeight());
  }
 }

 private void syncPageViewport(boolean keyboardOpen,int height){
  if(web==null) return;
  String js="(function(){"+
      "document.documentElement.classList.toggle('keyboard-open',"+keyboardOpen+");"+
      "document.body.classList.toggle('keyboard-open',"+keyboardOpen+");"+
      "document.documentElement.style.setProperty('--jkh-visible-height','"+Math.max(1,height)+"px');"+
      "document.body.style.setProperty('--jkh-visible-height','"+Math.max(1,height)+"px');"+
      "})();";
  web.postDelayed(()->web.evaluateJavascript(js,null),50);
 }

 private void injectKeyboardViewportScript(){
  if(web==null) return;
  String js="(function(){"+
      "if(window.__jkhKeyboardFix)return; window.__jkhKeyboardFix=true;"+
      "var style=document.createElement('style');"+
      "style.id='jkh-keyboard-fix';"+
      "style.textContent='html,body{width:100%;max-width:100%;}'+"+
      "'html.keyboard-open,body.keyboard-open{height:var(--jkh-visible-height,50vh)!important;min-height:0!important;}'+"+
      "'body.keyboard-open{overflow-y:auto!important;}';"+
      "document.head.appendChild(style);"+
      "var vv=window.visualViewport;"+
      "if(vv){vv.addEventListener('resize',function(){"+
      "document.documentElement.style.setProperty('--jkh-visible-height',vv.height+'px');"+
      "document.body.style.setProperty('--jkh-visible-height',vv.height+'px');"+
      "});}"+
      "})();";
  web.evaluateJavascript(js,null);
 }

 private void keepFocusedFieldVisible(){
  if(web==null || !web.hasWindowFocus()) return;
  web.postDelayed(()->web.evaluateJavascript(
    "(function(){var e=document.activeElement;"+
    "if(e&&(e.tagName==='INPUT'||e.tagName==='TEXTAREA'||e.tagName==='SELECT')){"+
    "try{e.scrollIntoView({block:'center',inline:'nearest'});}"+
    "catch(x){e.scrollIntoView(true);}}})();",null),120);
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
