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
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.cleriotsimon.webtools.WebRequest;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class MainActivity extends SherlockActivity {
	//String api_url = "http://192.168.0.3:5000";
	private String api_url = "http://qqdroid.mobi:5000";
	
	private String token=null;
	private JSONObject currentTrack=null;
	private List<JSONObject> songs = new ArrayList<JSONObject>();
	private ListView l;
	private ArrayAdapter<String> adapter;
	private int songpos;
	
	int id=0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		l = (ListView) findViewById(R.id.listview);
		l.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				try {
					currentTrack = songs.get(arg2);
					
					int visiblePosition = l.getFirstVisiblePosition();
				    View v = l.getChildAt(arg2 - visiblePosition);
					
				    ProgressBar b = (ProgressBar) v.findViewById(currentTrack.getInt("progressid"));
				    b.setVisibility(View.VISIBLE);
				    
					HashMap<String,String> params = new HashMap<String, String>();
					params.put("token", token);
					params.put("songid", currentTrack.getString("id"));
					WebRequest.post(api_url+"/download_song", params,
							WebRequest.NEVER, new DownloadListener());
					
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
		
		
		adapter = new ArrayAdapter<String>(
				MainActivity.this, android.R.layout.simple_list_item_1) {
			@Override
			public View getView(int position, View convertView,
					ViewGroup parent) {
				Log.d("MUSIC", "TEST");
				LinearLayout li = new LinearLayout(MainActivity.this);
				li.setOrientation(LinearLayout.VERTICAL);
				
				TextView txt = new TextView(this.getContext());

				//txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float
					//	.parseFloat(pref.getString("policesize", "18")));

				ProgressBar b = new ProgressBar(MainActivity.this, null, android.R.attr.progressBarStyleHorizontal);
				b.setProgress(0);
				id++;
				b.setId(id);
				b.setVisibility(View.GONE);
				b.setIndeterminate(true);
				
				
				try {
					txt.setText(Html.fromHtml((String)songs.get(position).get("title")));
					songs.get(position).put("progressid", id);
				} catch (JSONException e) {
					e.printStackTrace();
				}

				/*if (pref.getString("design", "blackonwhite").equals(
						"blackonwhite")) {
					txt.setTextColor(Color.BLACK);
					txt.setBackgroundColor(Color.WHITE);
				} else if (pref.getString("design", "blackonwhite").equals(
						"whiteonblack")) {
					txt.setTextColor(Color.WHITE);
					txt.setBackgroundColor(Color.BLACK);
				}
				*/
				txt.setPadding(20, 20, 20, 20);
				Log.d("MUSIC", txt.getText().toString());
				li.addView(txt);
				li.addView(b);
				
				return li;
			}
		};
		l.setAdapter(adapter);
		
		
		Intent googlePicker =AccountPicker.newChooseAccountIntent(null,null,
	           new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},true,null,null,null,null) ;
	    startActivityForResult(googlePicker,1);
		    
		//new TestTask().execute();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.activity_main, menu);
	    return true;
	}
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		item.setEnabled(false);
		songpos=0;
		downloadSong(songpos);
		
		return true;
	}
		
	@Override
    protected void onActivityResult(final int requestCode, 
                                    final int resultCode, final Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            Toast.makeText(this,"Account Name="+accountName, 3000).show();
            new GoogleAuthTask(this).execute(accountName);
        }
    }
	
	public class LoginListener extends AsyncHttpResponseHandler {
		@Override
		public void onFailure(Throwable e, String response) {
			e.printStackTrace();
		}

		@Override
		public void onSuccess(String result) {
			Log.d("MUSIC", result);
			token=result;
			
			HashMap<String,String> params = new HashMap<String, String>();
			params.put("token", token);
			WebRequest.post(api_url+"/list", params,
					WebRequest.NEVER, new SongListener());
		}
	}
	
	public class SongListener extends AsyncHttpResponseHandler {
		@Override
		public void onFailure(Throwable e, String response) {
			e.printStackTrace();
		}

		@Override
		public void onSuccess(String result) {			
			adapter.clear();
			
			JSONArray array;
			try {
				array = new JSONArray(result);
								
				for(int i=0;i<10;i++){
					JSONObject obj;
					try {
						obj = (JSONObject) array.get(i);
						songs.add(obj);
						adapter.add((String) obj.get("title"));
					} catch (JSONException e) {
						e.printStackTrace();
					}	
				}
			} catch (JSONException e1) {
				e1.printStackTrace();
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
	
	private void downloadSong(int position){
		currentTrack = songs.get(position);
    	
    	int visiblePosition = l.getFirstVisiblePosition();
	    View v = l.getChildAt(position - visiblePosition);
		
	    ProgressBar b;
		try {
			b = (ProgressBar) v.findViewById(currentTrack.getInt("progressid"));
		    b.setVisibility(View.VISIBLE);
		    
			HashMap<String,String> params = new HashMap<String, String>();
			params.put("token", token);
			params.put("songid", currentTrack.getString("id"));
			WebRequest.post(api_url+"/download_song", params,
					WebRequest.NEVER, new DownloadListener());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	class DownloadTask extends AsyncTask<String, Void, Void> {
		File f=null;
        protected Void doInBackground(String... urls) {			
			String stream=urls[0];
			Log.d("MUSIC", stream);
			
			try {
	            URL url = new URL(stream);
	            URLConnection connection = url.openConnection();
	            connection.connect();
	            // this will be useful so that you can show a typical 0-100% progress bar
	            int fileLength = connection.getContentLength();

	            f=new File(Environment.getExternalStorageDirectory().getPath()+"/syncgmusic/"+currentTrack.getString("artistNorm")+"/"+currentTrack.getString("albumNorm")+"/"+currentTrack.getString("titleNorm")+".mp3");
	            File directory = new File(f.getParentFile().getAbsolutePath());
	            directory.mkdirs();
	            
	            // download the file
	            InputStream input = new BufferedInputStream(url.openStream());
	            OutputStream output = new FileOutputStream(f);

	            
	            
	            byte data[] = new byte[1024];
	            long total = 0;
	            int count;
	            while ((count = input.read(data)) != -1) {
	                total += count;
	                // publishing the progress....
	                //publishProgress((int) );
	                ProgressBar b = (ProgressBar) findViewById(currentTrack.getInt("progressid"));
	                b.setIndeterminate(false);
	                b.setProgress((int) (total * 100 / fileLength));
	                output.write(data, 0, count);
	            }

	            output.flush();
	            output.close();
	            input.close();
	            
	            
	        }
			catch (Exception e) {
	        	
	        }

			return null;
        }
        
        protected void onPostExecute(Void result) {
        	try {
				populateFileWithTuneTags(f, currentTrack);
			} catch (Exception e) {
				e.printStackTrace();
			}
	     }
	}
	
	private void populateFileWithTuneTags(File file, JSONObject track) throws IOException
	{
		try
		{
			AudioFile f = AudioFileIO.read(file);
			Tag tag = f.getTag();
			if(tag == null)
			{
				tag = new ID3v24Tag();
			}
			tag.setField(FieldKey.ALBUM, (String)track.get("album"));
			tag.setField(FieldKey.ALBUM_ARTIST, (String)track.get("albumArtist"));
			tag.setField(FieldKey.ARTIST, (String)track.get("artist"));
			tag.setField(FieldKey.COMPOSER, (String)track.get("composer"));
			//tag.setField(FieldKey.DISC_NO, String.valueOf(track.getDiscNumber()));
			//tag.setField(FieldKey.DISC_TOTAL, String.valueOf(track.getTotalDiscCount()));
			tag.setField(FieldKey.GENRE, (String)track.get("genre"));
			tag.setField(FieldKey.TITLE, (String)track.get("title"));
			//tag.setField(FieldKey.TRACK, String.valueOf(track.getTrackNumber()));
			//tag.setField(FieldKey.TRACK_TOTAL, String.valueOf(track.getTotalTrackCount()));
			tag.setField(FieldKey.YEAR, String.valueOf((Integer)track.get("year")));

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
		
			songpos++;
			downloadSong(songpos);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			//throw new IOException(e);
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
	            String authToken = GoogleAuthUtil.getToken(mActivity, params[0],
	                    "sj");

	            if (!TextUtils.isEmpty(authToken)) {
	            	return authToken;
	               //TODO: if (!success)
	                   // GoogleAuthUtil.invalidateToken(mActivity, authToken); 
	            }
	            else
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
	    	HashMap<String,String> param = new HashMap<String, String>();
			param.put("token", result);
        	WebRequest.post(api_url+"/login", param,
    				WebRequest.NEVER, new LoginListener());
	     }
	}
}
