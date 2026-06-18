package com.zipflash.mrp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import com.zipflash.mrp.helper.CheckPerm;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;
import android.content.ClipData;
import android.content.ClipboardManager;

public class Shell extends BaseActivity {

    private TextView tvOutput;
    private EditText etCommand;
    private Button btnRun;
    private LinearLayout recentCommandsContainer;
    private String currentDir = "/";
    private List<String> commandHistory = new ArrayList<String>();
    private int historyIndex = -1;
    private boolean isUserScrolling = false;
    private long lastScrollTime = 0;
    private static final long SCROLL_DEBOUNCE_MS = 100; // Debounce interval
    private static final String PREFS_NAME = "shell_history";
    private static final String HISTORY_KEY = "cmds";
    private volatile boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.shell_main);

        tvOutput = (TextView) findViewById(R.id.tvOutput);
        etCommand = (EditText) findViewById(R.id.etCommand);
        btnRun = (Button) findViewById(R.id.btnRun);
        recentCommandsContainer = (LinearLayout) findViewById(R.id.recent_commands_container);

        tvOutput.setMovementMethod(new ScrollingMovementMethod());
        tvOutput.setTextIsSelectable(true);
        tvOutput.setFocusable(true);
        tvOutput.setFocusableInTouchMode(true);
        tvOutput.setVerticalScrollBarEnabled(true);

        // Detect user scrolling
        tvOutput.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
						case MotionEvent.ACTION_MOVE:
							isUserScrolling = true;
							break;
						case MotionEvent.ACTION_UP:
						case MotionEvent.ACTION_CANCEL:
							isUserScrolling = false;
							break;
					}
					return false;
				}
			});

        setupToolbar();
        loadHistory();
        setupRunButton();
        setupCommandHistory();
        updateRecentCommands();

        btnRun.setEnabled(false);
        etCommand.requestFocus();
        etCommand.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					btnRun.setEnabled(s.toString().trim().length() > 0 && !isRunning);
				}
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				@Override public void afterTextChanged(Editable s) {}
			});
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle("Shell");
        }
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(Color.WHITE);
        }
    }

    // LOAD and SAVE History in SharedPreferences
    private void loadHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String joined = prefs.getString(HISTORY_KEY, "");
        commandHistory.clear();
        if (!joined.isEmpty()) {
            String[] parts = joined.split("\n");
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].trim().isEmpty()) commandHistory.add(parts[i]);
            }
        }
        historyIndex = commandHistory.size();
    }

    private void saveHistory() {
        StringBuilder sb = new StringBuilder();
        for (String cmd : commandHistory) {
            sb.append(cmd).append("\n");
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(HISTORY_KEY, sb.toString()).apply();
    }

    private void setupRunButton() {
        btnRun.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final String cmd = etCommand.getText().toString().trim();
					if (!cmd.isEmpty()
                        && (commandHistory.isEmpty() || !cmd.equals(commandHistory.get(commandHistory.size()-1)))) {
						commandHistory.add(cmd);
					}
					historyIndex = commandHistory.size();
					saveHistory();
					handleCommand(cmd);
					updateRecentCommands();
					etCommand.setText("");
				}
			});
    }

    private void setupCommandHistory() {
        etCommand.setOnKeyListener(new OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						if (keyCode == KeyEvent.KEYCODE_DPAD_UP && historyIndex > 0) {
							historyIndex--;
							etCommand.setText(commandHistory.get(historyIndex));
							etCommand.setSelection(etCommand.getText().length());
							return true;
						} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && historyIndex < commandHistory.size() - 1) {
							historyIndex++;
							etCommand.setText(commandHistory.get(historyIndex));
							etCommand.setSelection(etCommand.getText().length());
							return true;
						} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && historyIndex == commandHistory.size() - 1) {
							historyIndex++;
							etCommand.setText("");
							return true;
						}
					}
					return false;
				}
			});
    }

    private String getPrompt() {
        return "user@device:" + currentDir + " $ ";
    }

    /** Adds up to last 8 recent commands as clickable buttons. */
    private void updateRecentCommands() {
        recentCommandsContainer.removeAllViews();
        int maxRecent = 8;
        int start = Math.max(0, commandHistory.size() - maxRecent);
        for (int i = commandHistory.size() - 1; i >= start; i--) {
            final String cmd = commandHistory.get(i);
            Button btn = new Button(this);
			btn.setText(cmd);
			btn.setSingleLine(true);
			btn.setEllipsize(android.text.TextUtils.TruncateAt.END);
			btn.setMaxWidth(dpToPx(150));
			btn.setTextColor(Color.WHITE);
			btn.setBackgroundResource(R.drawable.btn_normal);
			btn.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2)); // Compact padding
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			);
			params.setMargins(dpToPx(4), 0, dpToPx(4), 0); // Compact margins
			btn.setLayoutParams(params);
			btn.setAllCaps(false);
			btn.setTextSize(13);
            btn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						etCommand.setText(cmd);
						etCommand.setSelection(cmd.length());
					}
				});
            recentCommandsContainer.addView(btn);
        }
    }
	
	private int dpToPx(int dp) {
		float density = getResources().getDisplayMetrics().density;
		return Math.round((float) dp * density);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_shell, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear) {
            tvOutput.setText("");
            scrollToBottom();
            return true;
        }
        if (id == R.id.action_copy) {
            String text = tvOutput.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("ShellOutput", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Output copied", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleCommand(String command) {
        SpannableStringBuilder sp = new SpannableStringBuilder("\n" + getPrompt() + command + "\n");
        tvOutput.append(sp);
        if (command.startsWith("cd ")) {
            String newDir = command.substring(3).trim();
            String targetDir;
            if (newDir.equals("..")) {
                int lastSlash = currentDir.lastIndexOf('/');
                targetDir = (lastSlash > 0) ? currentDir.substring(0, lastSlash) : "/";
            } else if (newDir.startsWith("/")) {
                targetDir = newDir;
            } else {
                targetDir = currentDir.equals("/") ? "/" + newDir : currentDir + "/" + newDir;
            }

            java.io.File dir = new java.io.File(targetDir);
            if (dir.exists() && dir.isDirectory()) {
                currentDir = dir.getAbsolutePath();
                appendColoredLine("[#] Changed directory to: " + currentDir, Color.parseColor("#FFA726"));
            } else {
                appendColoredLine("[!] No such directory: " + targetDir, Color.RED);
            }
            scrollToBottom();
        } else {
            runCommand(command);
        }
    }

    // Append output lines, coloring error lines in red.
    private void runCommand(final String command) {
        isRunning = true;
        btnRun.setEnabled(false);
        new Thread(new Runnable() {
				@Override
				public void run() {
					final SpannableStringBuilder output = new SpannableStringBuilder();
					try {
						ShizukuRemoteProcess process = Shizuku.newProcess(
							new String[]{"sh", "-c", command}, null, currentDir);

						BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
						BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));

						String line;
						while ((line = input.readLine()) != null) {
							output.append(line).append("\n");
						}
						while ((line = error.readLine()) != null) {
							int start = output.length();
							output.append("[ERR] " + line + "\n");
							output.setSpan(
								new ForegroundColorSpan(Color.RED),
								start, output.length(),
								0
							);
						}

						process.waitFor();
						process.destroy();

						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									tvOutput.append(output);
									if (!isUserScrolling) {
										scrollToBottom();
									}
									isRunning = false;
									btnRun.setEnabled(etCommand.getText().toString().trim().length() > 0);
								}
							});
					} catch (final Exception e) {
						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									appendColoredLine("Exception: " + e.getMessage(), Color.RED);
									if (!isUserScrolling) {
										scrollToBottom();
									}
									isRunning = false;
									btnRun.setEnabled(etCommand.getText().toString().trim().length() > 0);
								}
							});
					}
				}
			}).start();
    }

    // Helper for colored output lines
    private void appendColoredLine(String text, int color) {
        SpannableStringBuilder sp = new SpannableStringBuilder(text + "\n");
        sp.setSpan(new ForegroundColorSpan(color), 0, sp.length(), 0);
        tvOutput.append(sp);
    }

    private void scrollToBottom() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScrollTime < SCROLL_DEBOUNCE_MS) {
            return;
        }
        lastScrollTime = currentTime;

        tvOutput.post(new Runnable() {
				@Override
				public void run() {
					if (tvOutput.getLayout() != null) {
						int scrollAmount = tvOutput.getLayout().getLineTop(tvOutput.getLineCount()) - tvOutput.getHeight();
						if (scrollAmount > 0) {
							tvOutput.scrollTo(0, scrollAmount);
						} else {
							tvOutput.scrollTo(0, 0);
						}
					}
				}
			});
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //saveHistory();
    }
}
