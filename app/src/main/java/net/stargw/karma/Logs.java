package net.stargw.karma;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Intent;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;

import androidx.core.content.FileProvider;

public class Logs {

	static final String FILE_LOG = "Karma-FW-log.txt";
	static final String FILE_LOGCAT = "Karma-FW-logcat.txt";
	
	private static final String TAG = "KarmaLog";
	private static FileOutputStream logFile = null;
	
	public Logs() {
		// TODO Auto-generated constructor stub

	}

	public static File getLogFile()
	{
		File file = new File(Global.getContext().getFilesDir(), FILE_LOG);
		
		return file;
	}
	
	public static ArrayList<String> getLogBufferList()
	{
		ArrayList<String> logBuffer = new ArrayList<String>();
		
		File file = new File(Global.getContext().getFilesDir(), FILE_LOG);
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
	
			while ((line = br.readLine()) != null) {
				logBuffer.add(line);
			}
			Logs.myLog("Loaded log file: " + file, 2);
			br.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.w(TAG, "Error opening log file: " + e);
		}

		// Do not reverse sort
		// Collections.reverse(logBuffer);
		return logBuffer;

	}
	
	public static String getLogBuffer()
	{
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		
		try {
			logFile.close();
		} catch (Exception e) {
			Log.w(TAG, time.format("%H:%M:%S") + ": Error closing log file");
		}
		
		File file = new File(Global.getContext().getFilesDir(), FILE_LOG);
		
		byte[] buffer = null;
		FileInputStream is;
		
		try {
			is = new FileInputStream(file);
			int size = is.available();
			
			buffer = new byte[size];
			is.read(buffer);
			is.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.w(TAG, time.format("%H:%M:%S") + ": Error opening log file: " + e);
		}
		
		try {
			logFile = new FileOutputStream(file, true);
		} catch (Exception e) {
			Log.w(TAG, time.format("%H:%M:%S") + ": Error appending to log file: " + file.getAbsolutePath() + " - " + e);
		}
		
		return new String(buffer);
	}


	public static void setLoggingLevel(int i)
	{
		Global.settingsLoggingLevel = i;

		Global.saveSetings();

	}

	
	public static int getLoggingLevel()
	{

		Global.getSettings();

		if (logFile == null)
		{
			Time time = new Time(Time.getCurrentTimezone());
			time.setToNow();

			File file = new File(Global.getContext().getFilesDir(), FILE_LOG);

			try {
				// file.createNewFile();
				if (file.length() > 100000)
				{
					logFile = new FileOutputStream(file, false);
				} else {
					logFile = new FileOutputStream(file, true);
				}
				// logBuffer = "\n[" + time.format("%A %d %b %Y at %H:%M:%S") + "] App Started.\n";
				// byte[] data = logBuffer.getBytes();
				// logFile.write(data);
			} catch (Exception e) {
				Log.w(TAG, time.format("%H:%M:%S") + ": Error creating log file: " + file.getAbsolutePath() + " : " + e);
				// e.printStackTrace();
			}
		}
		return Global.settingsLoggingLevel;
	}
	


	public static void checkLogSize()
	{
		File file = new File(Global.getContext().getFilesDir(), FILE_LOG);

		if (file.length() > 5000000)
		{
			clearLog();
			Logs.myLog("Log file too big. Reset.", 1);
		} else {
			Logs.myLog("Log size = " + file.length(), 3);
		}
	}

	public static void clearLog()
	{
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();
		
		File file = new File(Global.getContext().getFilesDir(), FILE_LOG);
		
		try {
			logFile.close();
		} catch (Exception e) {
			Log.w(TAG, time.format("%H:%M:%S") + ": Error closing log file: " + e);
		}
		
		try {
			file.createNewFile();
			logFile = new FileOutputStream(file);

			myLog(Global.getContext().getString(R.string.cleared) ,1);
			/*
			String logBuffer = "[" + time.format("%A %d %b %Y at %H:%M:%S") + "] " + Global.getContext().getString(R.string.cleared) + ".\n";
			byte[] data = logBuffer.getBytes(); 
			logFile.write(data);
			*/
		} catch (Exception e) {
			Log.w(TAG, time.format("%H:%M:%S") + ": Error creating log file: " + file.getAbsolutePath()+ " - " + e);
			// e.printStackTrace();
		}
	}

	// share log
	public static void shareLog()
	{
		File f = new File(Global.getContext().getFilesDir(),FILE_LOG);

		myLog("Log Path = " + f.toString(),3);

		// This provides a read only content:// for other apps
		Uri uri2 = FileProvider.getUriForFile(Global.getContext(),"net.stargw.karma.fileprovider",f);

		myLog("URI Path = " + uri2.toString(),3);

		Intent intent2 = new Intent(Intent.ACTION_SEND);
		intent2.putExtra(Intent.EXTRA_STREAM, uri2);
		intent2.setType("text/plain");
		intent2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Global.getContext().startActivity(intent2);

	}

	public static void shareLogcat()
	{
		Time time = new Time(Time.getCurrentTimezone());
		time.setToNow();

		File myDirPath = new File(Global.getContext().getCacheDir(), "temp");
		myDirPath.mkdirs();

		File file = new File(myDirPath, FILE_LOGCAT);

		File log = new File(Global.getContext().getFilesDir(),FILE_LOG);


		try {
			copy(log, file);
		} catch (IOException e) {
			Log.w(TAG, time.format("%H:%M:%S") + ": Error coping log file: " + e );
			// e.printStackTrace();
			Logs.myLog("Cannot share log file: " + e,1);
		}

		myLog("Logcat Path = " + file.toString(),3);

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file, true); // append
			fos.write("\n\n=======RAW ANDROID LOGCAT BELOW======\n\n".getBytes() );
		} catch (IOException e) {
			// Toast.makeText(Global.getContext(), "Error Exporting to file!", Toast.LENGTH_SHORT).show();
			Logs.myLog("Error appending to logcat: " + e,2);
			Logs.myLog("Cannot share log file: " + e,1);
			return;
		}

		try {
			Process process = Runtime.getRuntime().exec("logcat -v time -d");
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));

			String line = "";
			while ((line = bufferedReader.readLine()) != null) {
				// write line to file
				line = line + "\n";
				fos.write(line.getBytes() );
			}
			fos.close();
		} catch (Exception e) {
			Logs.myLog("Error running exec to get logcat: " + e,2);
			try {
				fos.write(("Error running exec to get logcat: " + e).getBytes());
			} catch (Exception e2) {

			}
			// return;
			// continue
		}

		// This provides a read only content:// for other apps
		Uri uri2 = FileProvider.getUriForFile(Global.getContext(),"net.stargw.karma.fileprovider",file);

		Intent intent2 = new Intent(Intent.ACTION_SEND);
		intent2.putExtra(Intent.EXTRA_STREAM, uri2);
		intent2.setType("text/plain"); // default!
		intent2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Global.getContext().startActivity(intent2);

	}

	static final int CHUNK = (1024*50);

	public static void copy(File src, File dst) throws IOException {
		try (InputStream in = new FileInputStream(src)) {
			try (OutputStream out = new FileOutputStream(dst)) {
				// Transfer bytes from in to out
				byte[] buf = new byte[CHUNK];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			}
		}
	}

	//
	// Logs are very important!
	//
	public static void myLog(String buf,int level)
	{
		// Log.w(TAG, "LOG = " + level + " : " + Global.settingsLoggingLevel);


		if (level <= Global.settingsLoggingLevel)
		{
			// Log.w(TAG, "LOG = " + level + " : " + Global.settingsLoggingLevel + " " + buf);

			Calendar today = new GregorianCalendar();
			SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss EEE dd/MM/yyyy");
			String humanDate = format.format(today.getTime());

			if (logFile != null)
			{
				String logBuffer = humanDate + " @SW@ " + buf + "\n";

				// Log.w(TAG, time.format("%H:%M:%S") + buf);
				byte[] data = logBuffer.getBytes(); 
				try {
					logFile.write(data);
					if (Global.settingsEnableLogcat == true) {
						Log.w(TAG, buf);
					}
				} catch (Exception e) {
					Log.w(TAG, humanDate + ": Error writing to log file.");
				}
			} else {
				Log.w(TAG, humanDate + ": Error writing to log file.");
			}
		}

	}


}
