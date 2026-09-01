package com.example.jizhang;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.jizhang.BaseActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.adapter.ChatAdapter;
import com.example.jizhang.ai.AiEngine;
import com.example.jizhang.util.ThemeManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 本地 AI 对话界面（独立，不与其他功能互通）
 */
public class AIChatActivity extends BaseActivity {

    private static final String PREFS = "ai_chat";
    private static final String KEY_MODEL_PATH = "model_path";
    private static final int REQ_PICK_MODEL = 100;

    private EditText etInput;
    private TextView tvModelStatus;
    private ChatAdapter adapter;
    private RecyclerView recycler;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.get(this).getThemeResId());
        super.onCreate(savedInstanceState);
        ThemeManager.get(this).apply(this);
        setContentView(R.layout.activity_ai_chat);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        tvModelStatus = findViewById(R.id.tv_model_status);
        etInput = findViewById(R.id.et_input);

        recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter();
        recycler.setAdapter(adapter);

        findViewById(R.id.btn_select_model).setOnClickListener(v -> pickModel());
        findViewById(R.id.btn_send).setOnClickListener(v -> send());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String path = prefs.getString(KEY_MODEL_PATH, null);
        if (path != null && new File(path).exists()) {
            loadModel(path);
        } else {
            updateStatus("未加载模型");
        }
    }

    private void pickModel() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQ_PICK_MODEL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_MODEL && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) copyAndLoad(uri);
        }
    }

    private void copyAndLoad(Uri uri) {
        updateStatus("正在复制模型…");
        executor.execute(() -> {
            try {
                File dir = new File(getExternalFilesDir(null), "models");
                if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("无法创建目录");
                File target = new File(dir, "model.gguf");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(target)) {
                    byte[] buf = new byte[256 * 1024];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                    }
                }
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putString(KEY_MODEL_PATH, target.getAbsolutePath()).apply();
                loadModel(target.getAbsolutePath());
            } catch (Exception e) {
                mainHandler.post(() -> {
                    updateStatus("复制失败");
                    Toast.makeText(this, "模型文件读取失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadModel(String path) {
        updateStatus("加载中…");
        executor.execute(() -> {
            boolean ok = AiEngine.loadModel(path, 2048, 4);
            mainHandler.post(() -> {
                if (ok) {
                    updateStatus("已就绪");
                    Toast.makeText(this, "模型加载完成", Toast.LENGTH_SHORT).show();
                } else {
                    updateStatus("加载失败");
                    Toast.makeText(this, "模型加载失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void send() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() || busy) return;
        if (!AiEngine.isLoaded()) {
            Toast.makeText(this, "请先选择并加载模型", Toast.LENGTH_SHORT).show();
            return;
        }

        etInput.setText("");
        adapter.add(new ChatAdapter.ChatMessage(true, text));
        adapter.add(new ChatAdapter.ChatMessage(false, "思考中…"));
        scrollToBottom();
        busy = true;

        executor.execute(() -> {
            String reply = AiEngine.complete(text,
                    "你是一个乐于助人的中文助手，请用简洁的语言回答。", 512);
            mainHandler.post(() -> {
                busy = false;
                if (reply == null || reply.trim().isEmpty()) {
                    adapter.updateLast("（生成失败）");
                } else {
                    adapter.updateLast(reply.trim());
                }
                scrollToBottom();
            });
        });
    }

    private void updateStatus(String s) {
        tvModelStatus.setText(s);
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            recycler.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(() -> AiEngine.release());
        executor.shutdown();
    }
}
