package com.example.jizhang;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.jizhang.BaseActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.adapter.ColorPickerAdapter;
import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.Note;
import com.example.jizhang.util.NaturalLangParser;
import com.example.jizhang.util.Palette;
import com.example.jizhang.util.ThemeManager;

/**
 * 笔记编辑页（新建 / 编辑）
 */
public class NoteEditActivity extends BaseActivity {

    public static final String EXTRA_ID = "extra_id";
    public static final String EXTRA_TODO = "extra_todo";

    private DatabaseHelper db;
    private EditText etTitle, etContent;
    private CheckBox cbTodo;
    private ColorPickerAdapter colorAdapter;

    private long editingId = -1;
    private long createTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.get(this).getThemeResId());
        super.onCreate(savedInstanceState);
        ThemeManager.get(this).apply(this);
        setContentView(R.layout.activity_note_edit);
        db = new DatabaseHelper(this);

        editingId = getIntent().getLongExtra(EXTRA_ID, -1);
        createTime = System.currentTimeMillis();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> save());

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        cbTodo = findViewById(R.id.cb_todo);

        RecyclerView gridColor = findViewById(R.id.grid_color);
        gridColor.setLayoutManager(new GridLayoutManager(this, 9));
        gridColor.setNestedScrollingEnabled(false);
        colorAdapter = new ColorPickerAdapter();
        gridColor.setAdapter(colorAdapter);

        if (editingId > 0) {
            fillForEdit();
        } else {
            // 新建时根据来源 tab 设置默认 todo 状态（待办 tab 默认勾选）
            cbTodo.setChecked(getIntent().getIntExtra(EXTRA_TODO, 0) == 1);
        }

        findViewById(R.id.btn_quick).setOnClickListener(v -> showNaturalInput());
    }

    private void fillForEdit() {
        Note n = db.getNote(editingId);
        if (n == null) return;
        etTitle.setText(n.title);
        etContent.setText(n.content);
        colorAdapter.setSelectedIndex(n.colorIndex);
        cbTodo.setChecked(n.todo > 0);
        createTime = n.createTime;
    }

    private void save() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "标题或内容不能都为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (title.isEmpty()) {
            // 没有标题就用内容前 16 字
            title = content.length() > 16 ? content.substring(0, 16) : content;
        }

        Note n = new Note();
        n.id = editingId;
        n.title = title;
        n.content = content;
        n.colorIndex = colorAdapter.getSelectedIndex();
        n.todo = cbTodo.isChecked() ? 1 : 0;
        n.createTime = createTime;
        n.updateTime = System.currentTimeMillis();

        if (editingId > 0) {
            db.updateNote(n);
            Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
        } else {
            db.insertNote(n);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void showNaturalInput() {
        EditText input = new EditText(this);
        input.setHint("例如：明天下午3点开会，后天交房租");
        input.setMinLines(2);
        new AlertDialog.Builder(this)
                .setTitle("快捷输入（自然语言）")
                .setView(input)
                .setPositiveButton("解析", (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) return;
                    NaturalLangParser.NoteResult r = NaturalLangParser.parseNote(text);
                    String title = r.text.isEmpty() ? text : r.text;
                    etTitle.setText(title);
                    if (etContent.getText().toString().trim().isEmpty()) {
                        etContent.setText(text);
                    }
                    if (r.time != null) {
                        cbTodo.setChecked(true);
                        Toast.makeText(this, "已识别时间：" + Palette.formatDateTime(r.time)
                                + "，并设为待办", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "已解析（未识别到时间）", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}