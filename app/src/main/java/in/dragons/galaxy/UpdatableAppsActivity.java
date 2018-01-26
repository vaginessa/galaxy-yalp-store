package in.dragons.galaxy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import in.dragons.galaxy.model.App;
import in.dragons.galaxy.task.AppListValidityCheckTask;
import in.dragons.galaxy.task.playstore.ForegroundUpdatableAppsTask;
import in.dragons.galaxy.view.ListItem;
import in.dragons.galaxy.view.UpdatableAppBadge;

public class UpdatableAppsActivity extends AppListActivity {

    private UpdateAllReceiver updateAllReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bottomsUp(1);
        setTitle(getString(R.string.activity_title_updates_only));
        onNewIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAllReceiver = new UpdateAllReceiver(this);
        AppListValidityCheckTask task = new AppListValidityCheckTask(this);
        task.setRespectUpdateBlacklist(true);
        task.setIncludeSystemApps(true);
        task.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(updateAllReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        loadApps();
    }

    @Override
    public void loadApps() {
        getTask().execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (YalpStorePermissionManager.isGranted(requestCode, permissions, grantResults)) {
            Log.i(getClass().getSimpleName(), "User granted the write permission");
            launchUpdateAll();
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == R.id.action_ignore || item.getItemId() == R.id.action_unwhitelist) {
            String packageName = getAppByListPosition(info.position).getPackageName();
            BlackWhiteListManager manager = new BlackWhiteListManager(this);
            if (item.getItemId() == R.id.action_ignore) {
                manager.add(packageName);
            } else {
                manager.remove(packageName);
            }
            removeApp(packageName);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected ListItem getListItem(App app) {
        UpdatableAppBadge appBadge = new UpdatableAppBadge();
        appBadge.setApp(app);
        return appBadge;
    }

    @Override
    public void removeApp(String packageName) {
        super.removeApp(packageName);
        if (listItems.isEmpty()) {
            ((TextView) getListView().getEmptyView()).setText(R.string.list_empty_updates);
            findViewById(R.id.main_button).setVisibility(View.GONE);
        }
    }

    private ForegroundUpdatableAppsTask getTask() {
        ForegroundUpdatableAppsTask task = new ForegroundUpdatableAppsTask(this);
        task.setErrorView((TextView) getListView().getEmptyView());
        task.setProgressIndicator(findViewById(R.id.progress));
        return task;
    }

    public void launchUpdateAll() {
        ((YalpStoreApplication) getApplicationContext()).setBackgroundUpdating(true);
        new UpdateChecker().onReceive(UpdatableAppsActivity.this, getIntent());
        Button button = (Button) findViewById(R.id.main_button);
        button.setEnabled(false);
        button.setText(R.string.list_updating);
    }
}

