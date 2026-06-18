package com.zipflash.mrp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.zipflash.mrp.helper.CheckPerm;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import rikka.shizuku.Shizuku;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.widget.LinearLayout;



public class Modules extends BaseActivity {

    private RecyclerView recyclerModules;
    private ModulesAdapter adapter;
    private List<Module> moduleList;
    private RelativeLayout loadingView;
    private ProgressBar progressBar;
    private RelativeLayout errorView;
    private Button btnRetry;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private static List<Module> cachedModuleList = null; // Static cache
    // Removed lastFetchTime and CACHE_DURATION_MS - no staleness check

    private boolean isFetching = false; // New flag to prevent multiple API calls

    private static final String VERCEL_ENDPOINT = "https://zip-flash-modules.vercel.app/api/modules";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modules);

        // Shizuku permission check
        /*if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
		 startActivity(new Intent(this, CheckPerm.class));
		 finish();
		 return;
		 }*/

        setupToolbar();
        setupViews();

        // Check cached data first
        if (cachedModuleList != null && !cachedModuleList.isEmpty()) {
            moduleList = new ArrayList<>(cachedModuleList);
            recyclerModules.setVisibility(View.VISIBLE);
            loadingView.setVisibility(View.GONE);
            errorView.setVisibility(View.GONE);

            adapter = new ModulesAdapter(Modules.this, moduleList);
            recyclerModules.setLayoutManager(new LinearLayoutManager(Modules.this));
            recyclerModules.setAdapter(adapter);
        }

        // Then try to fetch new modules if internet is available
        loadModulesAsync();

    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Modules");
        }
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(Color.WHITE);
        }
    }

    private void setupViews() {
        recyclerModules = findViewById(R.id.recyclerModules);
        loadingView = findViewById(R.id.loadingView);
        progressBar = findViewById(R.id.progressBar);
        if (progressBar != null && progressBar.getIndeterminateDrawable() != null) {
            progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor("#CCCCCC"), android.graphics.PorterDuff.Mode.SRC_IN);
        }

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerModules.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);

        moduleList = new ArrayList<>();

        errorView = findViewById(R.id.errorView);
        btnRetry = findViewById(R.id.btnRetry);

        

        btnRetry.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					errorView.setVisibility(View.GONE);
					loadingView.setVisibility(View.VISIBLE);
					loadModulesAsync();
				}
			});

        // Replaced lambda with anonymous inner class
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
				@Override
				public void onRefresh() {
					if (adapter != null) {
						adapter.filter("");
					}
					loadModulesAsync();
				}
			});

        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.white,
            android.R.color.holo_red_light,
            android.R.color.holo_blue_light
        );
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.parseColor("#111111"));
    }

    
	

	
    private boolean isConnected() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
        return false;
    }

    private void loadModulesAsync() {
        if (isFetching) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        isFetching = true;

        final boolean isListEmpty = moduleList == null || moduleList.isEmpty();

        runOnUiThread(new Runnable() {
				@Override
				public void run() {
					errorView.setVisibility(View.GONE);

					if (isListEmpty) {
						loadingView.setVisibility(View.VISIBLE); // center spinner
						recyclerModules.setVisibility(View.GONE);
						
					
					} else {
						// Keep current list visible during swipe refresh
						loadingView.setVisibility(View.GONE);
						recyclerModules.setVisibility(View.VISIBLE);
						swipeRefreshLayout.setRefreshing(true); // top spinner
					}
				}
			});

        // If no internet and modules already cached, just stop here
        if (!isConnected() && !isListEmpty) {
            runOnUiThread(new Runnable() {
					@Override
					public void run() {
						swipeRefreshLayout.setRefreshing(false); // stop top spinner
					
					}
				});
            isFetching = false;
            return;
        }

        // If no internet and no cached modules, show warning
        if (!isConnected() && isListEmpty) {
            runOnUiThread(new Runnable() {
					@Override
					public void run() {
						loadingView.setVisibility(View.GONE);
						recyclerModules.setVisibility(View.GONE);
						errorView.setVisibility(View.VISIBLE);
						TextView errorMessage = errorView.findViewById(R.id.errorMessage);
						errorMessage.setText("No internet connection. Please check Wi-Fi or mobile data.");
						swipeRefreshLayout.setRefreshing(false);
							}
				});
            isFetching = false;
            return;
        }

        // Proceed with network fetch if internet is available
        new Thread(new Runnable() {
				@Override
				public void run() {
					HttpURLConnection conn = null;
					try {
						URL url = new URL(VERCEL_ENDPOINT);
						conn = (HttpURLConnection) url.openConnection();
						conn.setRequestMethod("GET");
						conn.setConnectTimeout(10_000);
						conn.setReadTimeout(10_000);

						int code = conn.getResponseCode();
						if (code != 200) throw new Exception("HTTP error code: " + code);

						BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
						StringBuilder sb = new StringBuilder();
						String line;
						while ((line = br.readLine()) != null) sb.append(line);
						br.close();

						JSONArray arr = new JSONArray(sb.toString());
						moduleList.clear();
						for (int i = 0; i < arr.length(); i++) {
							JSONObject o = arr.getJSONObject(i);
							moduleList.add(new Module(
											   o.optString("title", ""),
											   o.optString("linkText", "Download"),
											   o.optString("url", ""),
											   o.optString("description", ""),
											   i
										   ));
						}

						// Update cache
						cachedModuleList = new ArrayList<>(moduleList);

						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									swipeRefreshLayout.setRefreshing(false);
									loadingView.setVisibility(View.GONE);
									errorView.setVisibility(View.GONE);

									if (moduleList.isEmpty()) {
										errorView.setVisibility(View.VISIBLE);
										TextView errorMessage = errorView.findViewById(R.id.errorMessage);
										errorMessage.setText("Failed to load modules. Please try again later.");
										recyclerModules.setVisibility(View.GONE);
										// NEW: Hide ads on empty list
										
									} else {
										recyclerModules.setVisibility(View.VISIBLE);
										if (adapter == null) {
											adapter = new ModulesAdapter(Modules.this, moduleList);
											recyclerModules.setLayoutManager(new LinearLayoutManager(Modules.this));
											recyclerModules.setAdapter(adapter);
										} else {
											adapter.filter(""); // refresh adapter
										}
										// NEW: Load and show ads when data is available
										//
									}

									isFetching = false;
								}
							});

					} catch (final Exception e) {
						e.printStackTrace();
						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									swipeRefreshLayout.setRefreshing(false);
									loadingView.setVisibility(View.GONE);

									if (!moduleList.isEmpty()) {
										// Keep showing cached modules
										recyclerModules.setVisibility(View.VISIBLE);
										errorView.setVisibility(View.GONE);
										// NEW: Show ads with cached data
										//
									} else {
										recyclerModules.setVisibility(View.GONE);
										errorView.setVisibility(View.VISIBLE);
										TextView errorMessage = errorView.findViewById(R.id.errorMessage);
										errorMessage.setText(!isConnected()
															 ? "No internet connection. Please check Wi-Fi or mobile data."
															 : "Failed to load modules. Please try again later.");
										// NEW: Hide ads on error
										//
									}

									isFetching = false;
								}
							});
					} finally {
						if (conn != null) conn.disconnect();
					}
				}
			}).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // NEW: Clean up WebView to avoid memory leaks
     
        // Removed cachedModuleList = null; - keep cache for process lifetime
    }

    // ... (Rest of the code remains unchanged: onCreateOptionsMenu, onOptionsItemSelected, showSortPopup, onSupportNavigateUp, Module class, ModulesAdapter class)

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_modules, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search modules...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
				@Override
				public boolean onQueryTextSubmit(String query) {
					return false;
				}

				@Override
				public boolean onQueryTextChange(String newText) {
					if (adapter != null) {
						adapter.filter(newText);
					}
					return true;
				}
			});

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sort) {
            showSortPopup();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSortPopup() {
        PopupMenu popup = new PopupMenu(this, toolbar, Gravity.END);
        popup.getMenuInflater().inflate(R.menu.menu_sort, popup.getMenu());
        // Replaced lambda with anonymous inner class
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					int which = -1;
					if (item.getItemId() == R.id.sort_az) {
						which = 0;
					} else if (item.getItemId() == R.id.sort_za) {
						which = 1;
					} else if (item.getItemId() == R.id.sort_latest) {
						which = 2;
					}
					if (adapter != null && which != -1) {
						adapter.sortBy(which);
					}
					return true;
				}
			});
        popup.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private static class Module {
        String title;
        String linkText;
        String url;
        String description;
        int index;

        Module(String title, String linkText, String url, String description, int index) {
            this.title = title;
            this.linkText = linkText;
            this.url = url;
            this.description = description;
            this.index = index;
        }
    }

    private class ModulesAdapter extends RecyclerView.Adapter<ModulesAdapter.ModuleViewHolder> {
        private final Context context;
        private final List<Module> modules;
        private final List<Module> allModules;
        private int lastPosition = -1;
		private int expandedPosition = -1;

        ModulesAdapter(Context context, List<Module> modules) {
            this.context = context;
            this.modules = new ArrayList<>(modules);
            this.allModules = new ArrayList<>(modules);
        }
		
        @Override
        public ModuleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_modules, parent, false);
            return new ModuleViewHolder(view);
        }

		@Override
		public void onBindViewHolder(final ModuleViewHolder holder, final int position) {
			final Module module = modules.get(position);

			holder.title.setText(module.title);

			// Show or hide details based on expanded position
			final boolean isExpanded = position == expandedPosition;
			holder.detailsContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
			holder.detailsTitle.setText(module.title);
			holder.detailsDesc.setText(module.description != null && !module.description.isEmpty()
									   ? module.description : "No description");

			// Main row click/Tap: expand this row; collapse previous
			holder.itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (expandedPosition == position) {
							expandedPosition = -1;
						} else {
							int prev = expandedPosition;
							expandedPosition = position;
							notifyItemChanged(prev);
						}
						notifyItemChanged(position);
					}
				});

			holder.linkIcon.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						try {
							String url = "https://zip-flash-modules.vercel.app/zf/" + module.index;
							Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
							((Activity) context).startActivity(browserIntent);
						} catch (Exception ex) {
							ex.printStackTrace();
							Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show();
						}
					}
				});

			// Animation as before (optional)
			if (position > lastPosition) {
				Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
				holder.itemView.startAnimation(animation);
				lastPosition = position;
			} else {
				holder.itemView.clearAnimation();
			}
		}

        @Override
        public int getItemCount() {
            return modules.size();
        }

        public void filter(String text) {
            modules.clear();
            if (text == null || text.trim().isEmpty()) {
                modules.addAll(allModules);
            } else {
                String query = text.toLowerCase().trim();
                for (Module m : allModules) {
                    if (m.title.toLowerCase().contains(query) ||
                        m.description.toLowerCase().contains(query)) {
                        modules.add(m);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public void sortBy(int sortType) {
            if (sortType == 0) {
                Collections.sort(modules, new Comparator<Module>() {
						@Override
						public int compare(Module m1, Module m2) {
							return m1.title.compareToIgnoreCase(m2.title);
						}
					});
            } else if (sortType == 1) {
                Collections.sort(modules, new Comparator<Module>() {
						@Override
						public int compare(Module m1, Module m2) {
							return m2.title.compareToIgnoreCase(m1.title);
						}
					});
            } else if (sortType == 2) {
                Collections.sort(modules, new Comparator<Module>() {
						@Override
						public int compare(Module m1, Module m2) {
							return Integer.compare(m2.index, m1.index);
						}
					});
            }
            notifyDataSetChanged();
        }

        

		class ModuleViewHolder extends RecyclerView.ViewHolder {
			TextView title;
			ImageView linkIcon;
			LinearLayout detailsContainer;
			TextView detailsTitle, detailsDesc;

			ModuleViewHolder(View itemView) {
				super(itemView);
				title = itemView.findViewById(R.id.moduleTitle);
				linkIcon = itemView.findViewById(R.id.moduleDownloadIcon);
				detailsContainer = itemView.findViewById(R.id.detailsContainer);
				detailsTitle = itemView.findViewById(R.id.detailsTitle);
				detailsDesc = itemView.findViewById(R.id.detailsDesc);
			}
		}
		
		
        
    }
}
