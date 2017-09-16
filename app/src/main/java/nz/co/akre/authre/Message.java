package nz.co.akre.authre;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by Richard on 04-Sep-16.
 */
class MessageParser {
    private Context context;

    public MessageParser(Context context) {
        this.context = context;
    }

    /**
     * Check if this message is an auth message.
     */
    public boolean isAuthMessage(String message) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String authString = prefs.getString("auth_sig", context.getResources().getString(R.string.pref_default_auth_sig));
        if (message != null) {
            Log.i("AUTH", "checking if message contains '" + authString + "'");
            return message.toLowerCase().contains(authString.toLowerCase());
        } else {
            Log.i("AUTH", "a null string was passed to isAuthMessage");
            return false;
        }
    }
}

class MessageSender {
    public static String requestUrl(final String agent, final String message, final Context context) throws Exception {
        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("auth_agent", agent);
        paramMap.put("auth_message", message);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String authUrl = prefs.getString("auth_url", context.getResources().getString(R.string.pref_default_auth_url));
        final String apiKey = prefs.getString("api_key", context.getResources().getString(R.string.pref_default_api_key));
        if (!apiKey.isEmpty()) {
            paramMap.put("api_key", apiKey);
        }
        final String postParameters = createQueryStringForParameters(paramMap);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i("THREAD", authUrl);
                    requestUrlTask(authUrl, postParameters);
                } catch (Exception e) {
                    Log.e("THREAD", e.getMessage());
                }
            }
        });
        thread.start();
        return "OK";
    }

    private static String requestUrlTask(final String url, String postParameters) throws Exception {
        if (Log.isLoggable("SEND", Log.INFO)) {
            Log.i("SEND", "Requesting service: " + url);
        }
        disableConnectionReuseIfNecessary();
        HttpURLConnection urlConnection = null;
        try {
            // create connection
            URL urlToRequest = new URL(url);
            urlConnection = (HttpURLConnection) urlToRequest.openConnection();
            urlConnection.setConnectTimeout(99999);
            urlConnection.setReadTimeout(99999);
            // handle POST parameters
            if (postParameters != null) {
                if (Log.isLoggable("SEND", Log.INFO)) {
                    Log.i("SEND", "POST parameters: " + postParameters);
                }
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setFixedLengthStreamingMode(postParameters.getBytes().length);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                //send the POST out
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(postParameters);
                out.close();
            }
            // handle issues
            int statusCode = urlConnection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                Log.e("SEND", "Post status not ok");
            }
            // read output (only for GET)
            if (postParameters != null) {
                return null;
            } else {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                return getResponseText(in);
            }
        } catch (MalformedURLException e) {
            Log.e("SEND", "MalformedURLException", e); // handle invalid URL
        } catch (SocketTimeoutException e) {
            Log.e("SEND", "SocketTimeoutException", e); // handle timeout
        } catch (IOException e) {
            Log.e("SEND", "IOException", e); // handle I/0
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }

    /**
     * required in order to prevent issues in earlier Android version.
     */
    private static void disableConnectionReuseIfNecessary() {
        // see HttpURLConnection API doc
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    private static String getResponseText(InputStream inStream) {
        // very nice trick from
        // http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
        return new Scanner(inStream).useDelimiter("\\A").next();
    }

    private static final char PARAMETER_DELIMITER = '&';
    private static final char PARAMETER_EQUALS_CHAR = '=';
    public static String createQueryStringForParameters(Map<String, String> parameters) {
        StringBuilder parametersAsQueryString = new StringBuilder();
        if (parameters != null) {
            boolean firstParameter = true;
            for (String parameterName : parameters.keySet()) {
                if (!firstParameter) {
                    parametersAsQueryString.append(PARAMETER_DELIMITER);
                }
                try {
                    parametersAsQueryString.append(parameterName)
                            .append(PARAMETER_EQUALS_CHAR)
                            .append(URLEncoder.encode(parameters.get(parameterName), StandardCharsets.UTF_8.toString()));
                } catch (UnsupportedEncodingException e) {
                    Log.e("SEND", "Couldn't encode parameters");
                }
                firstParameter = false;
            }
        }
        return parametersAsQueryString.toString();
    }
}

