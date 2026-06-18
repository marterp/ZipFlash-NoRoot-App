package com.zipflash.mrp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

public class ModuleManagerAdapter extends RecyclerView.Adapter<ModuleManagerAdapter.ViewHolder> {

    private Context context;
    private List<File> modules;
    private TextView emptyView;

    public ModuleManagerAdapter(Context ctx, List<File> modules, TextView emptyView) {
        this.context = ctx;
        this.modules = modules;
        this.emptyView = emptyView;
        toggleEmptyView();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_module, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final File moduleDir = modules.get(position);
        final String moduleName = moduleDir.getName();

        holder.txtName.setText(moduleName);

        final SharedPreferences prefs = context.getSharedPreferences("modules_state", Context.MODE_PRIVATE);

        if (!prefs.contains(moduleName)) {
            saveModuleState(moduleName, true); // default ON after flash
        }
        boolean enabled = prefs.getBoolean(moduleName, true);

        holder.switchEnable.setOnCheckedChangeListener(null);
        holder.switchEnable.setChecked(enabled);

        // Enable / Disable
        holder.switchEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
					buttonView.setEnabled(false);

					// Check Shizuku permission
					if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
						((Activity) context).runOnUiThread(new Runnable() {
								@Override
								public void run() {
									buttonView.setEnabled(true);
								}
							});
						return;
					}

					if (isChecked) {
						if (!prefs.getBoolean(moduleName, true)) {
							runScriptWithShizuku(moduleDir, "run.sh", "Enabling " + moduleName + "...", new OnScriptFinishedListener() {
									@Override
									public void onFinished() {
										((Activity) context).runOnUiThread(new Runnable() {
												@Override
												public void run() {
													saveModuleState(moduleName, true);
													int pos = getSafeAdapterPosition(holder);
													if (pos >= 0) safeNotifyItemChanged(pos);
													buttonView.setEnabled(true);
												}
											});
									}

									@Override
									public void onError(final String error) {
										((Activity) context).runOnUiThread(new Runnable() {
												@Override
												public void run() {
													int pos = getSafeAdapterPosition(holder);
													if (pos >= 0) safeNotifyItemChanged(pos);
													Toast.makeText(context, "Error enabling " + moduleName + ": " + error, Toast.LENGTH_LONG).show();
													buttonView.setEnabled(true);
												}
											});
									}
								});
						} else {
							buttonView.setEnabled(true);
						}
					} else {
						runScriptWithShizuku(moduleDir, "revert.sh", "Disabling " + moduleName + "...", new OnScriptFinishedListener() {
								@Override
								public void onFinished() {
									((Activity) context).runOnUiThread(new Runnable() {
											@Override
											public void run() {
												saveModuleState(moduleName, false);
												int pos = getSafeAdapterPosition(holder);
												if (pos >= 0) safeNotifyItemChanged(pos);
												buttonView.setEnabled(true);
											}
										});
								}

								@Override
								public void onError(final String error) {
									((Activity) context).runOnUiThread(new Runnable() {
											@Override
											public void run() {
												int pos = getSafeAdapterPosition(holder);
												if (pos >= 0) safeNotifyItemChanged(pos);
												Toast.makeText(context, "Error disabling " + moduleName + ": " + error, Toast.LENGTH_LONG).show();
												buttonView.setEnabled(true);
											}
										});
								}
							});
					}
				}
			});

        // Delete
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

					LayoutInflater inflater = LayoutInflater.from(context);
					View dialogView = inflater.inflate(R.layout.dialog_module_remove, null);

					Button btnYes = dialogView.findViewById(R.id.btnYes);
					Button btnNo = dialogView.findViewById(R.id.btnNo);
					TextView messageText = dialogView.findViewById(R.id.message);

					if (messageText != null) {
						messageText.setText("Are you sure you want to remove the module " + moduleName + "?");
					}

					final AlertDialog dialog = new AlertDialog.Builder(context)
                        .setView(dialogView)
                        .setCancelable(true)
                        .create();

					if (dialog.getWindow() != null) {
						dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
					}

					btnYes.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								holder.btnDelete.setEnabled(false);
								dialog.dismiss(); // ✅ Close confirmation immediately

								if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
									((Activity) context).runOnUiThread(new Runnable() {
											@Override
											public void run() {
												Toast.makeText(context, "Shizuku is not available. Please grant permissions.", Toast.LENGTH_LONG).show();
												holder.btnDelete.setEnabled(true);
											}
										});
									return;
								}

								// Run revert first (safe if missing)
								// Always try revert, but remove module regardless of result
								runScriptWithShizuku(moduleDir, "revert.sh", "Removing " + moduleName + "...", new OnScriptFinishedListener() {
										@Override
										public void onFinished() {
											removeModuleCompletely(holder, moduleDir, moduleName);
										}

										@Override
										public void onError(final String error) {
											// Still remove even if revert fails (no toast)
											removeModuleCompletely(holder, moduleDir, moduleName);
										}
									});
							}
						});

					btnNo.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								dialog.dismiss();
							}
						});

					dialog.show();
				}
			});
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }

    // ---------- ViewHolder ----------
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        Switch switchEnable;
        ImageButton btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.moduleName);
            switchEnable = itemView.findViewById(R.id.moduleSwitch);
            btnDelete = itemView.findViewById(R.id.moduleDelete);
        }
    }

    // ---------- Helpers ----------
    private void saveModuleState(String moduleName, boolean enabled) {
        context.getSharedPreferences("modules_state", Context.MODE_PRIVATE)
			.edit().putBoolean(moduleName, enabled).apply();
    }

    private int getSafeAdapterPosition(ViewHolder holder) {
        int pos = holder.getAdapterPosition();
        if (pos == RecyclerView.NO_POSITION) return -1;
        return pos;
    }

    private void deleteRecursive(final File fileOrDir) {
        try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                throw new Exception("Shizuku is not available or permission not granted");
            }
            ShizukuRemoteProcess rm = Shizuku.newProcess(new String[]{"rm", "-rf", fileOrDir.getAbsolutePath()}, null, null);
            StringBuilder err = new StringBuilder();
            BufferedReader er = new BufferedReader(new InputStreamReader(rm.getErrorStream()));
            String line;
            while ((line = er.readLine()) != null) err.append(line).append("\n");
            er.close();
            rm.waitFor();
            if (rm.exitValue() != 0) throw new Exception(err.toString());
            rm.destroy();
        } catch (final Exception e) {
            ((Activity) context).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(context, "Error deleting " + fileOrDir.getName() + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
					}
				});
        }
    }
	
	private void removeModuleCompletely(final ViewHolder holder, final File moduleDir, final String moduleName) {
		((Activity) context).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					int pos = getSafeAdapterPosition(holder);
					deleteRecursive(moduleDir);
					saveModuleState(moduleName, false);
					if (pos >= 0 && pos < modules.size()) {
						modules.remove(pos);
						notifyItemRemoved(pos);
						toggleEmptyView();
					}
					holder.btnDelete.setEnabled(true);
					//Toast.makeText(context, "Module \"" + moduleName + "\" removed.", Toast.LENGTH_SHORT).show();
				}
			});
	}

    private void runScriptWithShizuku(final File moduleDir, final String scriptName,
                                      final String dialogMessage, final OnScriptFinishedListener listener) {

        View loadingView = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        final TextView tvLoadingMessage = loadingView.findViewById(R.id.tvLoadingMessage);
        tvLoadingMessage.setText(dialogMessage);

        final AlertDialog dialog = new AlertDialog.Builder(context)
			.setView(loadingView)
			.setCancelable(false)
			.create();

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ((Activity) context).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					dialog.show();
				}
			});

        new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
							throw new Exception("Shizuku not available or no permission");

						if (!moduleDir.exists() || !moduleDir.isDirectory())
							throw new Exception("Module directory not found: " + moduleDir.getAbsolutePath());

						final File scriptSrc = new File(moduleDir, scriptName);

						// Skip revert if missing
						if (scriptName.equals("revert.sh") && !scriptSrc.exists()) {
							((Activity) context).runOnUiThread(new Runnable() {
									@Override
									public void run() {
										dialog.dismiss();
										if (listener != null) listener.onFinished();
									}
								});
							return;
						}

						if (!scriptSrc.exists())
							throw new Exception("Script not found: " + scriptSrc.getAbsolutePath());

						ShizukuRemoteProcess chmod = Shizuku.newProcess(
                            new String[]{"chmod", "755", scriptSrc.getAbsolutePath()}, null, null);
						chmod.waitFor();
						chmod.destroy();

						String cmd = "cd " + moduleDir.getAbsolutePath() + " && ./" + scriptName;
						ShizukuRemoteProcess exec = Shizuku.newProcess(new String[]{"sh", "-c", cmd}, null, null);

						BufferedReader out = new BufferedReader(new InputStreamReader(exec.getInputStream()));
						StringBuilder output = new StringBuilder();
						String line;
						while ((line = out.readLine()) != null) output.append(line).append("\n");
						out.close();

						BufferedReader err = new BufferedReader(new InputStreamReader(exec.getErrorStream()));
						final StringBuilder errorOutput = new StringBuilder();
						while ((line = err.readLine()) != null) errorOutput.append(line).append("\n");
						err.close();

						final int exitCode = exec.waitFor();
						exec.destroy();

						((Activity) context).runOnUiThread(new Runnable() {
								@Override
								public void run() {
									dialog.dismiss();
									if (exitCode == 0) {
										if (listener != null) listener.onFinished();
									} else {
										if (listener != null)
											listener.onError("Script exited with code " + exitCode + ": " + errorOutput.toString());
									}
								}
							});

					} catch (final Exception e) {
						((Activity) context).runOnUiThread(new Runnable() {
								@Override
								public void run() {
									dialog.dismiss();
									if (listener != null) listener.onError("Error: " + e.getMessage());
								}
							});
					}
				}
			}).start();
    }

    private void toggleEmptyView() {
        if (emptyView != null) {
            if (modules == null || modules.isEmpty())
                emptyView.setVisibility(View.VISIBLE);
            else
                emptyView.setVisibility(View.GONE);
        }
    }

    private void safeNotifyItemChanged(int position) {
        if (position != RecyclerView.NO_POSITION && position < getItemCount())
            notifyItemChanged(position);
    }

    public interface OnScriptFinishedListener {
        void onFinished();
        void onError(String error);
    }
}
