package com.kirochat.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private EditText urlField, secretField;
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s); prefs = getSharedPreferences("kiro_chat_prefs", MODE_PRIVATE); setupUI();
    }

    private void setupUI() {
        ScrollView sv = new ScrollView(this); sv.setBackgroundColor(Color.parseColor("#1a1a2e")); sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(24),dp(24),dp(24),dp(24)); sv.addView(root);
        if(!getIntent().getBooleanExtra("first_time",false)){TextView b=new TextView(this);b.setText("\u2190 Kembali");b.setTextColor(Color.parseColor("#e94560"));b.setTextSize(16);b.setPadding(0,0,0,dp(16));b.setOnClickListener(v->finish());root.addView(b);}
        TextView t=new TextView(this);t.setText("\u2699\uFE0F Pengaturan");t.setTextColor(Color.WHITE);t.setTextSize(24);t.setPadding(0,0,0,dp(24));root.addView(t);
        root.addView(label("Server URL")); urlField=input("https://user-kiro-chat-backend.hf.space"); urlField.setText(prefs.getString("server_url","")); root.addView(urlField);
        root.addView(hint("URL Hugging Face Space kamu"));
        root.addView(label("App Secret")); secretField=input("Password proteksi"); secretField.setText(prefs.getString("app_secret","")); root.addView(secretField);
        root.addView(hint("Sama dengan APP_SECRET di HF Space"));
        root.addView(btn("\uD83D\uDCBE Simpan","#e94560",v->save())); root.addView(btn("\uD83D\uDD0C Test","#0f3460",v->test()));
        setContentView(sv);
    }

    private void save(){
        String url=urlField.getText().toString().trim(),sec=secretField.getText().toString().trim();
        if(url.isEmpty()){Toast.makeText(this,"URL kosong!",Toast.LENGTH_SHORT).show();return;}
        if(!url.startsWith("http"))url="https://"+url;
        prefs.edit().putString("server_url",url).putString("app_secret",sec).apply();
        Toast.makeText(this,"\u2705 Tersimpan!",Toast.LENGTH_SHORT).show();
        if(getIntent().getBooleanExtra("first_time",false))finish();
    }

    private void test(){
        String url=urlField.getText().toString().trim(); if(url.isEmpty()){Toast.makeText(this,"Isi URL!",Toast.LENGTH_SHORT).show();return;}
        if(!url.startsWith("http"))url="https://"+url; Toast.makeText(this,"Testing...",Toast.LENGTH_SHORT).show(); String fu=url;
        new Thread(()->{try{java.net.HttpURLConnection c=(java.net.HttpURLConnection)new java.net.URL(fu.replaceAll("/$","")+"/health").openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);int code=c.getResponseCode();
        runOnUiThread(()->Toast.makeText(this,code==200?"\u2705 OK!":"\u26A0\uFE0F "+code,Toast.LENGTH_LONG).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"\u274C "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();
    }

    private TextView label(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.parseColor("#ccc"));t.setTextSize(14);t.setPadding(0,dp(12),0,dp(6));return t;}
    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setHintTextColor(Color.parseColor("#555"));e.setTextColor(Color.WHITE);e.setBackgroundColor(Color.parseColor("#0f3460"));e.setPadding(dp(16),dp(14),dp(16),dp(14));e.setSingleLine(true);return e;}
    private TextView hint(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.parseColor("#888"));t.setTextSize(12);t.setPadding(0,dp(4),0,0);return t;}
    private TextView btn(String s,String bg,android.view.View.OnClickListener l){TextView b=new TextView(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(16);b.setGravity(Gravity.CENTER);b.setBackgroundColor(Color.parseColor(bg));b.setPadding(dp(20),dp(14),dp(20),dp(14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.setMargins(0,dp(16),0,0);b.setLayoutParams(p);b.setOnClickListener(l);return b;}
    private int dp(int d){return Math.round(d*getResources().getDisplayMetrics().density);}
}
