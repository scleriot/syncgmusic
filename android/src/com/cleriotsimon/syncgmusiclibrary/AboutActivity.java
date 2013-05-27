package com.cleriotsimon.syncgmusiclibrary;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;

public class AboutActivity extends SherlockPreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	addPreferencesFromResource(R.xml.about);

	getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case android.R.id.home:
	    finish();
	    return (true);

	}
	return true;
    }

    @Override
    public void onStart() {
	super.onStart();
	EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
	super.onStop();
	EasyTracker.getInstance().activityStop(this);
    }
}
