package com.kirochat.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private EditText inputField;
    private final List<String[]> history = new ArrayList<>();
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        prefs = getSharedPreferences("kiro_chat_prefs", MODE_PRIVATE);
        if (prefs.getString("server_url", "").isEmpty()) { Intent i = new Intent(this, SettingsActivity.class); i.putExtra("first_time", true); startActivity(i); }
        setupUI();
        addBubble("Halo! \uD83D\uDC4B Saya Kiro AI.\nKetik pesanmu untuk mulai!", false);
    }

    private void setupUI() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.parseColor("#1a1a2e"));
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout hdr = new FrameLayout(this); hdr.setBackgroundColor(Color.parseColor("#16213e"));
        hdr.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56))); hdr.setPadding(dp(16),0,dp(16),0);
        TextView t = new TextView(this); t.setText("\uD83E\uDD9E Kiro Chat"); t.setTextColor(Color.WHITE); t.setTextSize(20);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); tp.gravity = Gravity.CENTER_VERTICAL; t.setLayoutParams(tp); hdr.addView(t);
        TextView g = new TextView(this); g.setText("\u2699\uFE0F"); g.setTextSize(22);
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); gp.gravity = Gravity.CENTER_VERTICAL|Gravity.END; g.setLayoutParams(gp);
        g.setOnClickListener(v->startActivity(new Intent(this, SettingsActivity.class))); hdr.addView(g); root.addView(hdr);

        scrollView = new ScrollView(this); scrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)); scrollView.setFillViewport(true);
        chatContainer = new LinearLayout(this); chatContainer.setOrientation(LinearLayout.VERTICAL); chatContainer.setPadding(dp(12),dp(12),dp(12),dp(12)); scrollView.addView(chatContainer); root.addView(scrollView);

        LinearLayout ia = new LinearLayout(this); ia.setBackgroundColor(Color.parseColor("#16213e")); ia.setPadding(dp(12),dp(8),dp(12),dp(8)); ia.setGravity(Gravity.CENTER_VERTICAL);
        inputField = new EditText(this); inputField.setHint("Ketik pesan..."); inputField.setHintTextColor(Color.parseColor("#666")); inputField.setTextColor(Color.WHITE);
        inputField.setBackgroundColor(Color.parseColor("#0f3460")); inputField.setPadding(dp(16),dp(12),dp(16),dp(12)); inputField.setMaxLines(4);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); ip.setMargins(0,0,dp(8),0); inputField.setLayoutParams(ip);
        inputField.setImeOptions(EditorInfo.IME_ACTION_SEND); inputField.setOnEditorActionListener((v,id,e)->{if(id==EditorInfo.IME_ACTION_SEND){send();return true;}return false;}); ia.addView(inputField);
        TextView sb = new TextView(this); sb.setText("\u27A4"); sb.setTextSize(24); sb.setTextColor(Color.WHITE); sb.setBackgroundColor(Color.parseColor("#e94560"));
        sb.setPadding(dp(16),dp(10),dp(16),dp(10)); sb.setOnClickListener(v->send()); ia.addView(sb); root.addView(ia);
        setContentView(root);
    }

    private void send() {
        String msg = inputField.getText().toString().trim(); if(msg.isEmpty()) return;
        String url = prefs.getString("server_url",""); String secret = prefs.getString("app_secret","");
        if(url.isEmpty()){Toast.makeText(this,"Atur URL dulu!",Toast.LENGTH_LONG).show();return;}
        addBubble(msg, true); inputField.setText(""); history.add(new String[]{"user",msg});
        View typing = addBubble("Kiro berpikir...\uD83E\uDD14", false);
        exec.execute(()->{
            try { String reply = api(url,secret,msg); handler.post(()->{chatContainer.removeView((View)typing.getParent());addBubble(reply,false);history.add(new String[]{"assistant",reply});}); }
            catch(Exception e){handler.post(()->{chatContainer.removeView((View)typing.getParent());addBubble("\u274C "+e.getMessage(),false);});}
        });
    }

    private String api(String base, String secret, String msg) throws Exception {
        URL u = new URL(base.replaceAll("/$","")+"/chat"); HttpURLConnection c=(HttpURLConnection)u.openConnection();
        c.setRequestMethod("POST"); c.setRequestProperty("Content-Type","application/json");
        if(!secret.isEmpty()) c.setRequestProperty("Authorization","Bearer "+secret);
        c.setDoOutput(true); c.setConnectTimeout(30000); c.setReadTimeout(120000);
        JSONObject body = new JSONObject(); body.put("message", msg);
        JSONArray h = new JSONArray(); int s = Math.max(0, history.size()-10);
        for(int i=s;i<history.size();i++){JSONObject m=new JSONObject();m.put("role",history.get(i)[0]);m.put("content",history.get(i)[1]);h.put(m);}
        body.put("history", h);
        try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=c.getResponseCode(); BufferedReader r=new BufferedReader(new InputStreamReader(code>=200&&code<300?c.getInputStream():c.getErrorStream()));
        StringBuilder sb2=new StringBuilder(); String line; while((line=r.readLine())!=null) sb2.append(line); r.close();
        if(code>=200&&code<300) return new JSONObject(sb2.toString()).optString("reply","No response");
        throw new Exception("Error "+code);
    }

    private View addBubble(String text, boolean isUser) {
        LinearLayout w = new LinearLayout(this); LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wp.setMargins(0,dp(4),0,dp(4)); w.setLayoutParams(wp); w.setGravity(isUser?Gravity.END:Gravity.START);
        TextView b = new TextView(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setPadding(dp(14),dp(10),dp(14),dp(10));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if(isUser){b.setBackgroundColor(Color.parseColor("#e94560"));bp.setMargins(dp(48),0,0,0);}else{b.setBackgroundColor(Color.parseColor("#0f3460"));bp.setMargins(0,0,dp(48),0);}
        b.setLayoutParams(bp); w.addView(b); chatContainer.addView(w); scrollView.post(()->scrollView.fullScroll(View.FOCUS_DOWN)); return b;
    }

    private int dp(int d){return Math.round(d*getResources().getDisplayMetrics().density);}
}
