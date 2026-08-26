package com.jkhospitalalld.dental.reception;
import android.app.DownloadManager;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.webkit.*;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
public class MainActivity extends AppCompatActivity {
 private static final String HOME="https://jkh-dental-website.jkhospitalalld.workers.dev/reception.html";
 private WebView web;
 @Override protected void onCreate(Bundle b){
  super.onCreate(b); web=new WebView(this); setContentView(web); setup();
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
   if(u.startsWith("https://jkh-dental-website.jkhospitalalld.workers.dev")) return u;
  }
  return HOME;
 }
 private void setup(){
  WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
  s.setJavaScriptCanOpenWindowsAutomatically(true); s.setMediaPlaybackRequiresUserGesture(true);
  CookieManager.getInstance().setAcceptCookie(true);
  web.setWebChromeClient(new WebChromeClient());
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

