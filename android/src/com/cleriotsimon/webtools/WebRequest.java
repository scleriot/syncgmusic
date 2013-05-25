package com.cleriotsimon.webtools;

import java.util.HashMap;

import android.util.Log;

import com.cleriotsimon.syncgmusiclibrary.MyApplication;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class WebRequest {
	public static long NEVER = 0;
	public static long ONE_SECOND = 1000;
	public static long ONE_MINUTE = 60 * ONE_SECOND;
	public static long ONE_HOUR = 60 * ONE_MINUTE;
	public static long ONE_DAY = 24 * ONE_HOUR;
	public static long ONE_WEEK = 7 * ONE_DAY;

	private static AsyncHttpClient client = new AsyncHttpClient();
	private static DBHandler db = new DBHandler(MyApplication.getAppContext());

	public static void get(String url, long maxTime,
			AsyncHttpResponseHandler responseHandler) {
		client.setTimeout(0);
		String content = db.getContent(url);
		if (content == null) {
			DefaultRequestListener listener = new DefaultRequestListener(
					responseHandler, maxTime, url);
			client.get(url, null, listener);
		} else {
			Log.d("QQDROID", "LOLILOL");
			responseHandler.onSuccess(content);
		}
	}

	public static void post(String url, HashMap<String, String> params, long maxTime,
			AsyncHttpResponseHandler responseHandler) {
		client.setTimeout(0);
		String content = db.getContent(url);
		if (content == null) {
			DefaultRequestListener listener = new DefaultRequestListener(
					responseHandler, maxTime, url);
			
			RequestParams requestParams = new RequestParams(params);
			
			client.post(url, requestParams, listener);
		} else {
			Log.d("QQDROID", "LOLILOL");
			responseHandler.onSuccess(content);
		}
	}
}
