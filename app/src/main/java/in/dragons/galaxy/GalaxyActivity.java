package in.dragons.galaxy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;

import in.dragons.galaxy.fragment.FilterMenu;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class GalaxyActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    static protected boolean logout = false;
    private String Email;

    public static void cascadeFinish() {
        GalaxyActivity.logout = true;
    }

    protected void onCreateDrawer(Bundle savedInstanceState) {
        Log.v(getClass().getSimpleName(), "Starting activity");
        logout = false;
        if (((GalaxyApplication) getApplication()).isTv()) {
            requestWindowFeature(Window.FEATURE_OPTIONS_PANEL);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View header = navigationView.getHeaderView(0);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Email = sharedPreferences.getString(PlayStoreApiAuthenticator.PREFERENCE_EMAIL, "");
        try {
            if (Email.contains("yalp"))
                ((TextView) header.findViewById(R.id.usr_email)).setText(R.string.header_usr_email);
            else
                getRawData("http://picasaweb.google.com/data/entry/api/user/" + Email);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

    }

    @SuppressLint("StaticFieldLeak")
    public void getRawData(final String url) throws IOException {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    return response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null && result.contains("atom"))
                    parseRAW(result);
                else {
                    Log.e(this.getClass().getName(), "No network connection");
                    ((TextView) findViewById(R.id.usr_email)).setText(R.string.header_usr_noNetwork);
                }
            }

        }.execute();
    }

    public void parseRAW(String rawData) {
        setNavHeaderInfo((NavigationView) findViewById(R.id.nav_view),
                rawData.substring(rawData.indexOf("<name>") + 6, rawData.indexOf("</name>")),
                rawData.substring(rawData.indexOf("<gphoto:thumbnail>") + 18, rawData.lastIndexOf("</gphoto:thumbnail>")));
    }

    public void setNavHeaderInfo(NavigationView navigationView, String Name, String URL) {
        View header = navigationView.getHeaderView(0);
        ((TextView) header.findViewById(R.id.usr_name)).setText(Name);
        ((TextView) header.findViewById(R.id.usr_email)).setText(Email);

        Picasso.with(this)
                .load(URL)
                .placeholder(R.drawable.ic_user_placeholder)
                .transform(new CircleTransform())
                .into(((ImageView) header.findViewById(R.id.usr_img)));
    }

    @Override
    protected void onResume() {
        Log.v(getClass().getSimpleName(), "Resuming activity");
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            invalidateOptionsMenu();
        }
        if (logout) {
            finish();
        }
    }

    @Override
    protected void onPause() {
        Log.v(getClass().getSimpleName(), "Pausing activity");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.v(getClass().getSimpleName(), "Stopping activity");
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        new FilterMenu(this).onCreateOptionsMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter_system_apps:
            case R.id.filter_apps_with_ads:
            case R.id.filter_paid_apps:
            case R.id.filter_gsf_dependent_apps:
            case R.id.filter_category:
            case R.id.filter_rating:
            case R.id.filter_downloads:
                new FilterMenu(this).onOptionsItemSelected(item);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        if (null == receiver) {
            return;
        }
        try {
            super.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // Ignoring
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void addQueryTextListener(MenuItem searchItem) {
        SearchView searchView = (SearchView) searchItem.getActionView();
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (null != searchManager) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setQueryHint(getString(R.string.search_title));
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                Intent i = new Intent(GalaxyActivity.this, SearchActivity.class);
                i.setAction(Intent.ACTION_SEARCH);
                i.putExtra(SearchManager.QUERY, query);
                startActivity(i);
                return false;
            }
        });
    }

    AlertDialog showLogOutDialog() {
        return new AlertDialog.Builder(this)
                .setMessage(R.string.dialog_message_logout)
                .setTitle(R.string.dialog_title_logout)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new PlayStoreApiAuthenticator(getApplicationContext()).logout();
                        dialogInterface.dismiss();
                        finishAll();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    AlertDialog showFallbackSearchDialog() {
        final EditText textView = new EditText(this);
        return new AlertDialog.Builder(this)
                .setView(textView)
                .setPositiveButton(android.R.string.search_go, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(getApplicationContext(), SearchActivity.class);
                        i.setAction(Intent.ACTION_SEARCH);
                        i.putExtra(SearchManager.QUERY, textView.getText().toString());
                        startActivity(i);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    protected void finishAll() {
        logout = true;
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_myapps:
                startActivity(new Intent(this, InstalledAppsActivity.class));
                break;
            case R.id.action_updates:
                startActivity(new Intent(this, UpdatableAppsActivity.class));
                break;
            case R.id.action_categories:
                startActivity(new Intent(this, CategoryListActivity.class));
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, PreferenceActivity.class));
                break;
            case R.id.action_logout:
                showLogOutDialog();
                break;
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
