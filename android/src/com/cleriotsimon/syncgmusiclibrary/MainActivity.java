package com.cleriotsimon.syncgmusiclibrary;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.cleriotsimon.webtools.DownloadPictureTask;
import com.cleriotsimon.webtools.WebRequest;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class MainActivity extends SherlockActivity {
    private String api_url = "http://192.168.0.3:5000";
    // private String api_url = "http://gmusic.cleriotsimon.com";

    private String token = null;
    private JSONObject currentTrack = null;
    private JSONObject currentPlaylist = null;
    private List<JSONObject> songs = new ArrayList<JSONObject>();
    private List<JSONObject> playlists = new ArrayList<JSONObject>();
    private ListView l;
    private ArrayAdapter<String> adapter;
    private int songpos;
    private int playlistposition;
    private Menu menu;

    int id = 0;

    private boolean downloading = false;
    private boolean canceling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);

	l = (ListView) findViewById(R.id.listview);
	l.setOnItemClickListener(new OnItemClickListener() {
	    @Override
	    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
		    long arg3) {
		if (!downloading) {
		    EasyTracker.getTracker().sendEvent("ui_action", "list",
			    "download_playlist", null);

		    menu.getItem(0).setVisible(true);
		    downloading = true;
		    try {
			currentPlaylist = playlists.get(arg2);

			int visiblePosition = l.getFirstVisiblePosition();
			View v = l.getChildAt(arg2 - visiblePosition);

			ProgressBar b = (ProgressBar) v
				.findViewById(currentPlaylist
					.getInt("progressid"));
			b.setVisibility(View.VISIBLE);

			songs.clear();
			JSONArray songsArray = currentPlaylist
				.getJSONArray("songs");
			for (int i = 0; i < songsArray.length(); i++) {
			    songs.add(songsArray.getJSONObject(i));
			}

			playlistposition = arg2;
			songpos = 0;
			downloadSong(0);

		    } catch (JSONException e) {
			e.printStackTrace();
		    }
		}
	    }
	});

	adapter = new ArrayAdapter<String>(MainActivity.this,
		android.R.layout.simple_list_item_1) {
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout li2 = new RelativeLayout(MainActivity.this);
		// li2.setOrientation(LinearLayout.HORIZONTAL);

		LinearLayout li = new LinearLayout(MainActivity.this);
		li.setOrientation(LinearLayout.VERTICAL);

		TextView txt = new TextView(this.getContext());
		AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
		fadeIn.setDuration(500);
		fadeIn.setFillAfter(true);
		txt.setPadding(20, 20, 20, 20);
		txt.setAnimation(fadeIn);
		li.addView(txt);

		if (!this.getItem(position).equals(
			getResources().getString(R.string.loading))) {

		    ProgressBar b = new ProgressBar(MainActivity.this, null,
			    android.R.attr.progressBarStyleHorizontal);
		    b.setProgress(0);
		    id++;
		    b.setId(id);
		    b.setVisibility(View.GONE);
		    b.setIndeterminate(true);

		    try {
			DisplayMetrics metrics = MainActivity.this
				.getResources().getDisplayMetrics();
			int width = metrics.widthPixels;

			int max = width / 150 + 1;

			LinearLayout li_tmp = new LinearLayout(
				MainActivity.this);
			li_tmp.setOrientation(LinearLayout.HORIZONTAL);
			li_tmp.setTag("covers");

			li_tmp.setOnTouchListener(new OnTouchListener() {
			    @Override
			    public boolean onTouch(View v, MotionEvent event) {
				AlphaAnimation alpha;
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				    alpha = new AlphaAnimation(1F, 0.5F);
				    alpha.setDuration(0);
				    alpha.setFillAfter(true);
				    v.startAnimation(alpha);
				    break;
				case MotionEvent.ACTION_MOVE:
				case MotionEvent.ACTION_UP:
				    alpha = new AlphaAnimation(0.5F, 1F);
				    alpha.setDuration(500);
				    alpha.setFillAfter(true);
				    v.startAnimation(alpha);
				    break;
				}
				return true;
			    }
			});

			for (int i = 0; i < max
				&& i < playlists.get(position)
					.getJSONArray("songs").length(); i++) {

			    if (playlists.get(position).getJSONArray("songs")
				    .getJSONObject(i).has("albumArtUrl")) {
				ImageView iv = new ImageView(MainActivity.this);
				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					150, 150);
				iv.setLayoutParams(layoutParams);
				iv.setTag("http:"
					+ playlists.get(position)
						.getJSONArray("songs")
						.getJSONObject(i)
						.getString("albumArtUrl"));
				DownloadPictureTask dlpicture = new DownloadPictureTask();
				dlpicture.execute(iv);

				li_tmp.addView(iv);
			    } else {
				do {
				    i++;
				    max++;
				} while (i < playlists.get(position)
					.getJSONArray("songs").length()
					&& !playlists.get(position)
						.getJSONArray("songs")
						.getJSONObject(i)
						.has("albumArtUrl"));
				i--;
			    }
			}

			/*LinearLayout li_touched = new LinearLayout(
				MainActivity.this);
			li_touched.setOrientation(LinearLayout.HORIZONTAL);
			li_touched.setTag("touch");
			li_touched
				.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT));

			li2.addView(li_touched);*/
			li2.addView(li_tmp);

			playlists.get(position).put("progressid", id);
		    } catch (JSONException e) {
			e.printStackTrace();
		    }

		    li.addView(b);
		}

		RelativeLayout.LayoutParams relativeparams = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.WRAP_CONTENT,
			RelativeLayout.LayoutParams.WRAP_CONTENT);
		relativeparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
			RelativeLayout.TRUE);
		li.setLayoutParams(relativeparams);

		li.setBackgroundColor(getResources().getColor(
			R.color.playlist_title_background));
		txt.setText(this.getItem(position));
		txt.setTextColor(getResources().getColor(
			R.color.playlist_title_color));

		li2.addView(li);

		return li2;
	    }
	};
	l.setAdapter(adapter);

	showGooglePicker();
	// new TestTask().execute();
    }

    public void showGooglePicker() {
	Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null,
		new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE }, true,
		null, null, null, null);
	startActivityForResult(googlePicker, 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getSupportMenuInflater();
	inflater.inflate(R.menu.activity_main, menu);
	this.menu = menu;
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case R.id.menu_cancel:
	    canceling = true;
	    EasyTracker.getTracker().sendEvent("ui_action", "menu_press",
		    "cancel_menu", null);
	    item.setVisible(false);
	    break;
	case R.id.menu_about:
	    EasyTracker.getTracker().sendEvent("ui_action", "menu_press",
		    "about_menu", null);
	    Intent i = new Intent(this, AboutActivity.class);
	    startActivity(i);
	    break;
	case R.id.menu_account:
	    EasyTracker.getTracker().sendEvent("ui_action", "menu_press",
		    "change_menu", null);
	    showGooglePicker();
	    break;
	}

	return true;
    }

    @Override
    protected void onActivityResult(final int requestCode,
	    final int resultCode, final Intent data) {
	if (requestCode == 1) {
	    if (resultCode == RESULT_OK) {
		adapter.clear();

		adapter.add(getResources().getString(R.string.loading));
		l.setAdapter(adapter);

		String accountName = data
			.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		Toast.makeText(this, "Account Name=" + accountName,
			Toast.LENGTH_LONG).show();
		new GoogleAuthTask(this).execute(accountName);
	    } else {
		menu.getItem(0).setVisible(false);
	    }
	}
    }

    public class LoginListener extends AsyncHttpResponseHandler {
	@Override
	public void onFailure(Throwable e, String response) {
	    e.printStackTrace();
	}

	@Override
	public void onSuccess(String result) {
	    token = result;

	    HashMap<String, String> params = new HashMap<String, String>();
	    params.put("token", token);
	    WebRequest.post(api_url + "/playlists", params, WebRequest.NEVER,
		    new SongListener());
	}
    }

    public class SongListener extends AsyncHttpResponseHandler {
	@Override
	public void onFailure(Throwable e, String response) {
	    e.printStackTrace();
	}

	@Override
	public void onSuccess(String result) {
	    new SaveSongsTask().execute(result);
	}
    }

    class SaveSongsTask extends AsyncTask<String, Void, Void> {
	protected Void doInBackground(String... results) {
	    String result = results[0];

	    playlists.clear();

	    JSONArray object = null;
	    try {
		object = new JSONArray(result);
	    } catch (JSONException e1) {
		e1.printStackTrace();
		return null;
	    }
	    if (object != null) {
		for (int i = 0; i < object.length(); i++) {
		    try {
			JSONObject o = object.getJSONObject(i);
			playlists.add(o);
		    } catch (JSONException e1) {
			e1.printStackTrace();
		    }
		}
	    }

	    return null;
	}

	protected void onPostExecute(Void result) {
	    adapter.clear();

	    for (int i = 0; i < playlists.size(); i++) {
		try {
		    adapter.add(playlists.get(i).getString("title"));
		} catch (JSONException e) {
		    e.printStackTrace();
		}
	    }

	    l.setAdapter(adapter);
	}
    }

    public class DownloadListener extends AsyncHttpResponseHandler {
	@Override
	public void onFailure(Throwable e, String response) {
	    e.printStackTrace();
	}

	@Override
	public void onSuccess(String result) {
	    Log.d("MUSIC", result);

	    new DownloadTask().execute(result);
	}
    }

    private void downloadSong(int position) {
	if (canceling || position == songs.size()) {
	    int visiblePosition = l.getFirstVisiblePosition();
	    View v = l.getChildAt(playlistposition - visiblePosition);

	    try {
		ProgressBar b = (ProgressBar) v.findViewById(currentPlaylist
			.getInt("progressid"));
		b.setVisibility(View.GONE);
		b.setIndeterminate(true);
		b.setProgress(0);
	    } catch (JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    canceling = false;
	    downloading = false;
	} else {
	    currentTrack = songs.get(position);

	    try {
		Log.d("SYNCGMUSIC",
			"DOWNLOADING : " + currentTrack.getString("title"));
	    } catch (JSONException e1) {
		e1.printStackTrace();
	    }

	    int visiblePosition = l.getFirstVisiblePosition();
	    View v = l.getChildAt(playlistposition - visiblePosition);

	    ProgressBar b;
	    try {
		b = (ProgressBar) v.findViewById(currentPlaylist
			.getInt("progressid"));
		b.setVisibility(View.VISIBLE);
		b.setMax(songs.size());
		b.setProgress(position);
		b.setIndeterminate(false);

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("token", token);
		params.put("songid", currentTrack.getString("id"));
		WebRequest.post(api_url + "/download_song", params,
			WebRequest.NEVER, new DownloadListener());
	    } catch (JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    }

    class DownloadTask extends AsyncTask<String, Void, Void> {
	File f = null;

	protected Void doInBackground(String... urls) {
	    String stream = urls[0];
	    Log.d("MUSIC", stream);

	    try {
		URL url = new URL(stream);
		URLConnection connection = url.openConnection();
		connection.connect();
		// this will be useful so that you can show a typical 0-100%
		// progress bar
		int fileLength = connection.getContentLength();

		f = new File(Environment.getExternalStorageDirectory()
			.getPath()
			+ "/syncgmusic/"
			+ currentTrack.getString("artistNorm")
			+ "/"
			+ currentTrack.getString("albumNorm")
			+ "/"
			+ currentTrack.getString("titleNorm") + ".mp3");
		File directory = new File(f.getParentFile().getAbsolutePath());
		directory.mkdirs();

		// download the file
		InputStream input = new BufferedInputStream(url.openStream());
		OutputStream output = new FileOutputStream(f);

		byte data[] = new byte[1024];
		long total = 0;
		int count;
		while (!canceling && (count = input.read(data)) != -1) {
		    total += count;
		    ProgressBar b = (ProgressBar) findViewById(currentPlaylist
			    .getInt("progressid"));
		    b.setIndeterminate(false);
		    // b.setProgress((int) (total * 100 / fileLength));
		    output.write(data, 0, count);
		}

		output.flush();
		output.close();
		input.close();

	    } catch (Exception e) {

	    }

	    return null;
	}

	protected void onPostExecute(Void result) {
	    new PopulateTuneTask().execute(f);
	}
    }

    class PopulateTuneTask extends AsyncTask<Object, Void, Void> {
	protected Void doInBackground(Object... urls) {
	    if (!canceling) {
		try {
		    populateFileWithTuneTags((File) urls[0], currentTrack);
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }

	    return null;
	}

	private void populateFileWithTuneTags(File file, JSONObject track)
		throws IOException {
	    try {
		AudioFile f = AudioFileIO.read(file);
		Tag tag = f.getTag();
		if (tag == null) {
		    tag = new ID3v24Tag();
		}
		tag.setField(FieldKey.ALBUM, (String) track.get("album"));
		tag.setField(FieldKey.ALBUM_ARTIST,
			(String) track.get("albumArtist"));
		tag.setField(FieldKey.ARTIST, (String) track.get("artist"));
		tag.setField(FieldKey.COMPOSER, (String) track.get("composer"));
		// tag.setField(FieldKey.DISC_NO,
		// String.valueOf(track.getDiscNumber()));
		// tag.setField(FieldKey.DISC_TOTAL,
		// String.valueOf(track.getTotalDiscCount()));
		tag.setField(FieldKey.GENRE, (String) track.get("genre"));
		tag.setField(FieldKey.TITLE, (String) track.get("title"));
		// tag.setField(FieldKey.TRACK,
		// String.valueOf(track.getTrackNumber()));
		// tag.setField(FieldKey.TRACK_TOTAL,
		// String.valueOf(track.getTotalTrackCount()));
		tag.setField(FieldKey.YEAR,
			String.valueOf((Integer) track.get("year")));

		/*if(track.getAlbumArtRef() != null && !track.getAlbumArtRef().isEmpty())
		{
			AlbumArtRef[] array = track.getAlbumArtRef().toArray(new AlbumArtRef[track.getAlbumArtRef().size()]);
			for(int i = 0; i < array.length; i++)
			{
				Artwork artwork = new Artwork();
				File imageFile = new File(storageDirectory + System.getProperty("path.separator") + track.getId() + ".im" + i);
				FileUtils.copyURLToFile(array[i].getUrlAsURI().toURL(), imageFile);
				artwork.setFromFile(imageFile);
				tag.addField(artwork);
			}
		}*/

		f.setTag(tag);
		AudioFileIO.write(f);
	    } catch (Exception e) {
		e.printStackTrace();
		// throw new IOException(e);
	    }
	}

	protected void onPostExecute(Void result) {
	    songpos++;
	    downloadSong(songpos);
	}
    }

    public class GoogleAuthTask extends AsyncTask<String, Void, String> {
	private Activity mActivity;

	public GoogleAuthTask(Activity activity) {
	    mActivity = activity;
	}

	@Override
	protected String doInBackground(String... params) {
	    try {
		String authToken = GoogleAuthUtil.getToken(mActivity,
			params[0], "sj");

		Log.d("MUSIC", authToken);

		if (!TextUtils.isEmpty(authToken)) {
		    return authToken;
		    // TODO: if (!success)
		    // GoogleAuthUtil.invalidateToken(mActivity, authToken);
		} else
		    return "";
	    } catch (UserRecoverableAuthException e) {
		mActivity.startActivityForResult(e.getIntent(), 1001);
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    } catch (GoogleAuthException e) {
		e.printStackTrace();
	    }

	    return "";
	}

	protected void onPostExecute(String result) {
	    HashMap<String, String> param = new HashMap<String, String>();
	    param.put("token", result);
	    WebRequest.post(api_url + "/login", param, WebRequest.NEVER,
		    new LoginListener());
	}
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
