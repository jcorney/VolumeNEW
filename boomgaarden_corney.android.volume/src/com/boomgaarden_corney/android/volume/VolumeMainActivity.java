package com.boomgaarden_corney.android.volume;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class VolumeMainActivity extends Activity {

	private final static String DEBUG_TAG = "DEBUG_VOLUME";
	private final static String SERVER_URL = "http://54.86.68.241/audio/test.php";

	private static TextView txtResults;

	private static String errorMsg;
	private static String volumeAdjustment;

	private static int currentRingVolume;
	private static int maxRingVolume;
	private static int currentMediaVolume;
	private static int maxMediaVolume;
	private static int currentAlarmVolume;
	private static int maxAlarmVolume;
	private static int currentDTMFVolume;
	private static int maxDTMFVolume;
	private static int volumeRingUpCounter = 0;
	private static int volumeRingDownCounter = 0;
	private static int volumeMediaUpCounter = 0;
	private static int volumeMediaDownCounter = 0;
	private static int volumeAlarmUpCounter = 0;
	private static int volumeAlarmDownCounter = 0;
	private static int newRingVolume;
	private static int newMaxRingVolume;
	private static int newMediaVolume;
	private static int newMaxMediaVolume;
	private static int newAlarmVolume;
	private static int newMaxAlarmVolume;
	private static int newDTMFVolume;
	private static int newMaxDTMFVolume;

	static AudioManager mAudioManager;
	static KeyEvent keyEvent;

	private static List<NameValuePair> paramsDevice = new ArrayList<NameValuePair>();
	private static List<NameValuePair> paramsErrorMsg = new ArrayList<NameValuePair>();
	private static List<NameValuePair> paramsVolume = new ArrayList<NameValuePair>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_volume_main);

		txtResults = (TextView) this.findViewById(R.id.txtResults);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		setDeviceData();
		showDeviceData();
		sendDeviceData();

		if (mAudioManager == null) {
			setErrorMsg("No Volume Detected");
			showErrorMsg();
			sendErrorMsg();
		} else {
			setVolumeData();
			showVolumeData();
			sendVolumeData();
		}
		
		//Forcing Volume Changes
		mAudioManager.setStreamVolume(AudioManager.STREAM_RING,
				currentRingVolume + 1, 0);
		mAudioManager.setStreamVolume(AudioManager.STREAM_RING,
				currentRingVolume - 1, 0);
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
				currentMediaVolume + 1, 0);
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
				currentMediaVolume - 1, 0);
		mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM,
				currentAlarmVolume + 1, 0);
		mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM,
				currentAlarmVolume - 1, 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.volume_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private static String buildPostRequest(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String sendHttpRequest(String myURL, String postParameters)
			throws IOException {

		URL url = new URL(myURL);

		// Setup Connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000); /* in milliseconds */
		conn.setConnectTimeout(15000); /* in milliseconds */
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Setup POST query params and write to stream
		OutputStream ostream = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				ostream, "UTF-8"));

		if (postParameters.equals("DEVICE")) {
			writer.write(buildPostRequest(paramsDevice));
		} else if (postParameters.equals("VOLUME")) {
			writer.write(buildPostRequest(paramsVolume));
			paramsVolume = new ArrayList<NameValuePair>();
		} else if (postParameters.equals("ERROR_MSG")) {
			writer.write(buildPostRequest(paramsErrorMsg));
			paramsErrorMsg = new ArrayList<NameValuePair>();
		}

		writer.flush();
		writer.close();
		ostream.close();

		// Connect and Log response
		conn.connect();
		int response = conn.getResponseCode();
		Log.d(DEBUG_TAG, "The response is: " + response);

		conn.disconnect();

		return String.valueOf(response);

	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		// @params come from SendHttpRequestTask.execute() call
		@Override
		protected String doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url,
			// params[1] is type POST
			// request to send - i.e. whether to send Device or Accelerometer
			// parameters.
			try {
				return sendHttpRequest(params[0], params[1]);
			} catch (IOException e) {
				setErrorMsg("Unable to retrieve web page. URL may be invalid.");
				showErrorMsg();
				return errorMsg;
			}
		}
	}

	private void setDeviceData() {
		paramsDevice.add(new BasicNameValuePair("Device", Build.DEVICE));
		paramsDevice.add(new BasicNameValuePair("Brand", Build.BRAND));
		paramsDevice.add(new BasicNameValuePair("Manufacturer",
				Build.MANUFACTURER));
		paramsDevice.add(new BasicNameValuePair("Model", Build.MODEL));
		paramsDevice.add(new BasicNameValuePair("Product", Build.PRODUCT));
		paramsDevice.add(new BasicNameValuePair("Board", Build.BOARD));
		paramsDevice.add(new BasicNameValuePair("Android API", String
				.valueOf(Build.VERSION.SDK_INT)));
	}

	private static void setErrorMsg(String error) {
		errorMsg = error;
		paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
	}

	private static void setVolumeData() {
		currentRingVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_RING);
		maxRingVolume = mAudioManager
				.getStreamMaxVolume(AudioManager.STREAM_RING);
		currentMediaVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		maxMediaVolume = mAudioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		currentAlarmVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_ALARM);
		maxAlarmVolume = mAudioManager
				.getStreamMaxVolume(AudioManager.STREAM_ALARM);
		currentDTMFVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_DTMF);
		maxDTMFVolume = mAudioManager
				.getStreamMaxVolume(AudioManager.STREAM_DTMF);

		paramsVolume.add(new BasicNameValuePair("INITIAL VOLUME VALUES:", " "));
		paramsVolume.add(new BasicNameValuePair("Ring Volume", String
				.valueOf(currentRingVolume)));
		paramsVolume.add(new BasicNameValuePair("Ring Max Volume", String
				.valueOf(maxRingVolume)));
		paramsVolume.add(new BasicNameValuePair("Media Volume", String
				.valueOf(currentMediaVolume)));
		paramsVolume.add(new BasicNameValuePair("Media Max Volume", String
				.valueOf(maxMediaVolume)));
		paramsVolume.add(new BasicNameValuePair("Alarm Volume", String
				.valueOf(currentAlarmVolume)));
		paramsVolume.add(new BasicNameValuePair("Alarm Max Volume", String
				.valueOf(maxAlarmVolume)));
		paramsVolume.add(new BasicNameValuePair("DTMF Volume", String
				.valueOf(currentDTMFVolume)));
		paramsVolume.add(new BasicNameValuePair("DTMF Max Volume", String
				.valueOf(maxDTMFVolume)));
	}

	private void showDeviceData() {
		// Display and store (for sending via HTTP POST query) device
		// information
		txtResults.append("Device: " + Build.DEVICE + "\n");
		txtResults.append("Brand: " + Build.BRAND + "\n");
		txtResults.append("Manufacturer: " + Build.MANUFACTURER + "\n");
		txtResults.append("Model: " + Build.MODEL + "\n");
		txtResults.append("Product: " + Build.PRODUCT + "\n");
		txtResults.append("Board: " + Build.BOARD + "\n");
		txtResults.append("Android API: "
				+ String.valueOf(Build.VERSION.SDK_INT) + "\n");

		txtResults.append("\n");

	}

	private static void showErrorMsg() {
		Log.d(DEBUG_TAG, errorMsg);
		txtResults.append(errorMsg + "\n");
	}

	private void showVolumeData() {
		StringBuilder results = new StringBuilder();

		results.append("Initial Volume Values: \n");
		results.append("Ring Volume: " + currentRingVolume + "\n");
		results.append("Ring Max Volume: " + maxRingVolume + "\n");
		results.append("Media Volume: " + currentMediaVolume + "\n");
		results.append("Media Max Volume: " + maxMediaVolume + "\n");
		results.append("Alarm Volume: " + currentAlarmVolume + "\n");
		results.append("Alarm Max Volume: " + maxAlarmVolume + "\n");
		results.append("DTMF Volume: " + currentDTMFVolume + "\n");
		results.append("DTMF Max Volume: " + maxDTMFVolume + "\n");

		txtResults.append(new String(results));
		txtResults.append("\n");
	}

	private void sendDeviceData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "DEVICE");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendErrorMsg() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "ERROR_MSG");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendVolumeData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "VOLUME");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	public static class VolumeBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			newRingVolume = mAudioManager
					.getStreamVolume(AudioManager.STREAM_RING);
			newMaxRingVolume = mAudioManager
					.getStreamMaxVolume(AudioManager.STREAM_RING);
			newMediaVolume = mAudioManager
					.getStreamVolume(AudioManager.STREAM_MUSIC);
			newMaxMediaVolume = mAudioManager
					.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			newAlarmVolume = mAudioManager
					.getStreamVolume(AudioManager.STREAM_ALARM);
			newMaxAlarmVolume = mAudioManager
					.getStreamMaxVolume(AudioManager.STREAM_ALARM);
			newDTMFVolume = mAudioManager
					.getStreamVolume(AudioManager.STREAM_DTMF);
			newMaxDTMFVolume = mAudioManager
					.getStreamMaxVolume(AudioManager.STREAM_DTMF);

			if (newRingVolume > currentRingVolume) {
				volumeRingUpCounter++;
				volumeAdjustment = "Increasing Ring Volume";
				paramsVolume.add(new BasicNameValuePair("Volume Adjustment"
						+ volumeRingUpCounter, volumeAdjustment));
				currentRingVolume = newRingVolume;
				showNEWVolumeData(volumeRingUpCounter);
			} else if (newRingVolume < currentRingVolume) {
				volumeRingDownCounter++;
				volumeAdjustment = "Decreasing Ring Volume";
				paramsVolume.add(new BasicNameValuePair("Volume Adjustment"
						+ volumeRingDownCounter, volumeAdjustment));
				currentRingVolume = newRingVolume;
				showNEWVolumeData(volumeRingDownCounter);
			} else if (newMediaVolume > currentMediaVolume) {
				volumeMediaUpCounter++;
				volumeAdjustment = "Increasing Media Volume";
				paramsVolume.add(new BasicNameValuePair("Volume Adjustment"
						+ volumeMediaUpCounter, volumeAdjustment));
				currentMediaVolume = newMediaVolume;
				showNEWVolumeData(volumeMediaUpCounter);
			} else if (newMediaVolume < currentMediaVolume) {
				volumeMediaDownCounter++;
				volumeAdjustment = "Decreasing Media Volume";
				paramsVolume.add(new BasicNameValuePair("Volume Adjustment"
						+ volumeMediaDownCounter, volumeAdjustment));
				currentMediaVolume = newMediaVolume;
				showNEWVolumeData(volumeMediaDownCounter);
			} else if (newAlarmVolume > currentAlarmVolume) {
				volumeAlarmUpCounter++;
				volumeAdjustment = "Increasing Alarm Volume";
				paramsVolume.add(new BasicNameValuePair("Volume Adjustment"
						+ volumeAlarmUpCounter, volumeAdjustment));
				currentAlarmVolume = newAlarmVolume;
				showNEWVolumeData(volumeMediaUpCounter);
			} else if (newAlarmVolume < currentAlarmVolume) {
				volumeAlarmDownCounter++;
				volumeAdjustment = "Decreasing Alarm Volume";
				paramsVolume.add(new BasicNameValuePair("Volume Adjustment"
						+ volumeAlarmDownCounter, volumeAdjustment));
				currentAlarmVolume = newAlarmVolume;
				setNEWVolumeData();
				showNEWVolumeData(volumeAlarmDownCounter);
				
			}
			sendNEWVolumeData(context);

		}

		private void setNEWVolumeData() {
			paramsVolume.add(new BasicNameValuePair("Ring Volume", String
					.valueOf(newRingVolume)));
			paramsVolume.add(new BasicNameValuePair("Ring Max Volume", String
					.valueOf(newMaxRingVolume)));
			paramsVolume.add(new BasicNameValuePair("Media Volume", String
					.valueOf(newMediaVolume)));
			paramsVolume.add(new BasicNameValuePair("Media Max Volume", String
					.valueOf(newMaxMediaVolume)));
			paramsVolume.add(new BasicNameValuePair("Alarm Volume", String
					.valueOf(newAlarmVolume)));
			paramsVolume.add(new BasicNameValuePair("Alarm Max Volume", String
					.valueOf(newMaxAlarmVolume)));
			paramsVolume.add(new BasicNameValuePair("DTMF Volume", String
					.valueOf(newDTMFVolume)));
			paramsVolume.add(new BasicNameValuePair("DTMF Max Volume", String
					.valueOf(newMaxDTMFVolume)));

		}

		private void showNEWVolumeData(int counter) {
			StringBuilder results = new StringBuilder();

			results.append("VOLUME EVENT OCCURED: \n");
			results.append("Event:" + volumeAdjustment + "\n");
			results.append("Total " + volumeAdjustment + " Occurances: "
					+ counter + "\n");
			results.append("Ring Volume: " + newRingVolume + "\n");
			results.append("Ring Max Volume: " + newMaxRingVolume + "\n");
			results.append("Media Volume: " + newMediaVolume + "\n");
			results.append("Media Max Volume: " + newMaxMediaVolume + "\n");
			results.append("Alarm Volume: " + newAlarmVolume + "\n");
			results.append("Alarm Max Volume: " + newMaxAlarmVolume + "\n");
			results.append("DTMF Volume: " + newDTMFVolume + "\n");
			results.append("DTMF Max Volume: " + newMaxDTMFVolume + "\n");

			txtResults.append(new String(results));
			txtResults.append("\n");
		}

		private void sendNEWVolumeData(Context context) {
			ConnectivityManager connectMgr = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

			// Verify network connectivity is working; if not add note to
			// TextView
			// and Logcat file
			if (networkInfo != null && networkInfo.isConnected()) {
				// Send HTTP POST request to server which will include POST
				// parameters with Telephony info
				new SendHttpRequestTask().execute(SERVER_URL, "VOLUME");
			} else {
				setErrorMsg("No Network Connectivity");
				showErrorMsg();
			}
		}

		private String sendHttpRequest(String myURL, String postParameters)
				throws IOException {

			URL url = new URL(myURL);

			// Setup Connection
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000); /* in milliseconds */
			conn.setConnectTimeout(15000); /* in milliseconds */
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);

			// Setup POST query params and write to stream
			OutputStream ostream = conn.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					ostream, "UTF-8"));

			if (postParameters.equals("DEVICE")) {
				writer.write(buildPostRequest(paramsDevice));
			} else if (postParameters.equals("VOLUME")) {
				writer.write(buildPostRequest(paramsVolume));
				paramsVolume = new ArrayList<NameValuePair>();
			} else if (postParameters.equals("ERROR_MSG")) {
				writer.write(buildPostRequest(paramsErrorMsg));
				paramsErrorMsg = new ArrayList<NameValuePair>();
			}

			writer.flush();
			writer.close();
			ostream.close();

			// Connect and Log response
			conn.connect();
			int response = conn.getResponseCode();
			Log.d(DEBUG_TAG, "The response is: " + response);

			conn.disconnect();

			return String.valueOf(response);

		}

		private class SendHttpRequestTask extends
				AsyncTask<String, Void, String> {

			// @params come from SendHttpRequestTask.execute() call
			@Override
			protected String doInBackground(String... params) {
				// params comes from the execute() call: params[0] is the url,
				// params[1] is type POST
				// request to send - i.e. whether to send Device or
				// Accelerometer
				// parameters.
				try {
					return sendHttpRequest(params[0], params[1]);
				} catch (IOException e) {
					setErrorMsg("Unable to retrieve web page. URL may be invalid.");
					showErrorMsg();
					return errorMsg;
				}
			}
		}

	}
}
