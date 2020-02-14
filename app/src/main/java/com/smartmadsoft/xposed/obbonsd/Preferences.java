package com.smartmadsoft.xposed.obbonsd;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.smartmadsoft.xposed.obbonsd.utils.Prefs;

import java.io.File;
import java.util.List;

public class Preferences extends AppCompatPreferenceActivity {
    Preference prefAlternative;
    Preference prefLabelPathInternal;

    static boolean showWarning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.preferences);

        if (Deluxe.showBottomAd(getApplicationContext(), this)) {
            setContentView(R.layout.main);

            // Load an ad into the AdMob banner view.
            AdView adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        prefAlternative = findPreference("enable_alternative");
        prefLabelPathInternal = findPreference("label-path_internal");

        if (Build.VERSION.SDK_INT < 21)
            prefAlternative.setEnabled(false);

        prefAlternative.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                printAltSummary(preference, (boolean) o);
                return true;
            }
        });
        printAltSummary(prefAlternative, ((SwitchPreference) prefAlternative).isChecked());

        printPathInternalSummary(prefLabelPathInternal);

        // Do not show xposed warn: may be edxposed
        // showWarning = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (showWarning)
            detectAndShowXposedDialog();
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences settings = getPreferenceManager().getDefaultSharedPreferences(this);
        Prefs.setBool(this, "enable_dataonsd", settings.getBoolean("enable_dataonsd", false));
        Prefs.setBool(this, "enable_playstorehooks", settings.getBoolean("enable_playstorehooks", false));
        Prefs.setBool(this, "enable_alternative", settings.getBoolean("enable_alternative", false));
        Prefs.setString(this, "label-path_internal", settings.getString("label-path_internal", null));
    }

    void detectAndShowXposedDialog() {
        if (isXposedPresent())
            return;

        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setTitle("Xposed framework not found");
        builder.setMessage("This app is a Xposed module and requires Xposed framework to work");
        builder.setPositiveButton("Ok, quit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setNeutralButton("Find out more", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String url = "http://repo.xposed.info/module/de.robv.android.xposed.installer";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    boolean isXposedPresent() {
        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals("de.robv.android.xposed.installer"))
                return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_ads:
                Deluxe.openPlayStore(getApplicationContext());
                return true;
            case R.id.hide_icon:
                toggleIcon();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (!Deluxe.showUpgradeMenu(getApplicationContext()))
            menu.findItem(R.id.action_ads).setVisible(false);
        if (!isXposedPresent())
            menu.findItem(R.id.hide_icon).setVisible(false);

        return true;
    }

    void printAltSummary(Preference preference, boolean enabled) {
        String path = getAltSummary(enabled);
        if (path == null)
            preference.setSummary("No path found");
        else
            preference.setSummary("Detected: " + path);
    }

    void printPathInternalSummary(Preference preference) {
        String path = getRealInternal();
        if (path == null || path.equals(""))
            preference.setSummary("No path found");
        else {
            preference.setSummary("Detected: " + path);
            savePath(null, path);
        }
    }

    String getAltSummary(boolean enabled) {
        if (enabled)
            return getRealExternal2();
        else
            return getRealExternal1();
    }

    String getRealExternal1() {
        String secondaryStorage = System.getenv("SECONDARY_STORAGE");
        if (secondaryStorage == null)
            return null;
        return secondaryStorage.split(":")[0];
    }

    String getRealInternal() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    String getRealExternal2() {
        if (Build.VERSION.SDK_INT >= 21) {
            File[] dirs = this.getExternalMediaDirs();
            for (File dir : dirs) {
                if (Environment.isExternalStorageRemovable(dir)) {
                    String absolutePath = dir.getAbsolutePath();
                    int c = absolutePath.indexOf("/Android/");
                    String path = absolutePath.substring(0, c);
                    savePath(path, null);
                    return path;
                }
            }
        }
        return null;
    }

    void savePath(String path, String pathInternal) {
        Prefs.setString(this, "path", path);
        Prefs.setString(this, "path_internal", pathInternal);

        SharedPreferences settings = getPreferenceManager().getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        if (path != null)
            editor.putString("path", path);
        if (pathInternal != null)
            editor.putString("path_internal", pathInternal);
        editor.commit();
    }

    void toggleIcon() {
        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, "com.smartmadsoft.xposed.obbonsd.Launch");
        int newComponentState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        String text = "Launcher icon has been hidden";
        if (packageManager.getComponentEnabledSetting(componentName) == newComponentState) {
            newComponentState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            text = "Launcher icon has been restored";
        }
        packageManager.setComponentEnabledSetting(componentName, newComponentState, PackageManager.DONT_KILL_APP);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

}
