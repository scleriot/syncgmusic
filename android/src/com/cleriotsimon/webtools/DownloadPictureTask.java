package com.cleriotsimon.webtools;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

public class DownloadPictureTask extends AsyncTask<ImageView, Object, Object> {
    Bitmap bitmap;
    WeakReference<ImageView> iv = null;

    protected Object doInBackground(ImageView... imageview) {
	this.iv = new WeakReference<ImageView>(imageview[0]);

	if (iv.get() != null) {
	    return download_Image((String) iv.get().getTag());
	} else
	    return null;
    }

    protected void onPostExecute(Object result) {
	ImageView imageView = iv.get();
	if (imageView != null && result != null)
	    imageView.setImageBitmap((Bitmap) result);
    }

    private Bitmap download_Image(String url) {
	try {
	    bitmap = BitmapFactory.decodeStream((InputStream) new URL(url)
		    .getContent());
	} catch (MalformedURLException e) {
	    e.printStackTrace();
	    Log.d("WebTOOLS", "BAD URL : " + url);
	} catch (IOException e) {
	    e.printStackTrace();
	}

	return bitmap;
    }
}
