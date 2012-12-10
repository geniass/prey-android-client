/*******************************************************************************
 * Created by Carlos Yaconi
 * Copyright 2012 Fork Ltd. All rights reserved.
 * License: GPLv3
 * Full license at "/LICENSE"
 ******************************************************************************/
package com.prey.net;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.util.Log;

import com.prey.PreyConfig;
import com.prey.PreyLogger;

/**
 * Implements a Rest API using android http client.
 * 
 * @author Carlos Yaconi H.
 * 
 */
public class PreyRestHttpClient {

	private static PreyRestHttpClient _instance = null;
	private DefaultHttpClient httpclient = null;
	private Context ctx = null;

	private PreyRestHttpClient(Context ctx) {
		this.ctx = ctx;
		//httpclient = new DefaultHttpClient();
		httpclient = (DefaultHttpClient) HttpUtils.getNewHttpClient();

		HttpParams params = new BasicHttpParams();
		
		// Set the timeout in milliseconds until a connection is established.
		int timeoutConnection = 30000;
		HttpConnectionParams.setConnectionTimeout(params, timeoutConnection);
		
		// Set the default socket timeout (SO_TIMEOUT) 
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 50000;
		HttpConnectionParams.setSoTimeout(params, timeoutSocket);
		
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF_8");
		HttpProtocolParams.setUseExpectContinue(params, false);
		HttpProtocolParams.setUserAgent(params, getUserAgent());

		httpclient.setParams(params);
	}

	public static PreyRestHttpClient getInstance(Context ctx) {

		_instance = new PreyRestHttpClient(ctx);
		return _instance;

	}

	private static List<NameValuePair> getHttpParamsFromMap(Map<String, String> params) {

		List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		for (Iterator<Map.Entry<String, String>> it = params.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, String> entry = it.next();
			String key = entry.getKey();
			String value = entry.getValue();
			// httpParams.setParameter(key, value);
			parameters.add(new BasicNameValuePair(key, value));
		}
		return parameters;
	}

	public PreyHttpResponse methodAsParameter(String url, String methodAsString, Map<String, String> params, PreyConfig preyConfig, String user, String pass)
			throws IOException {
		HttpPost method = new HttpPost(url);
		method.setHeader("Content-type", "text/plain");
		params.put("_method", methodAsString);
		method.setEntity(new UrlEncodedFormEntity(getHttpParamsFromMap(params), HTTP.UTF_8));
		// method.setQueryString(getHttpParamsFromMap(params));
		PreyLogger.d("Sending using " + methodAsString + "(using _method) - URI: " + url + " - parameters: " + params.toString());
		return sendUsingMethodUsingCredentials(method, preyConfig, user, pass);
	}

	public PreyHttpResponse methodAsParameter(String url, String methodAsString, Map<String, String> params, PreyConfig preyConfig) throws IOException {
		HttpPost method = new HttpPost(url);
		method.setHeader("Content-type", "text/plain");
		params.put("_method", methodAsString);
		method.setEntity(new UrlEncodedFormEntity(getHttpParamsFromMap(params), HTTP.UTF_8));
		// method.setQueryString(getHttpParamsFromMap(params));
		PreyLogger.d("Sending using " + methodAsString + "(using _method) - URI: " + url + " - parameters: " + params.toString());
		return sendUsingMethod(method, preyConfig);
	}

	public PreyHttpResponse put(String url, Map<String, String> params, PreyConfig preyConfig) throws IOException {
		HttpPut method = new HttpPut(url);
		method.setHeader("Accept", "application/xml,text/html,application/xhtml+xml;q=0.9,*/*;q=0.8");
		method.setHeader("Content-type", "text/plain");
		method.setEntity(new UrlEncodedFormEntity(getHttpParamsFromMap(params), HTTP.UTF_8));
		// method.setParams(getHttpParamsFromMap(params));
		PreyLogger.d("Sending using 'PUT' - URI: " + url + " - parameters: " + params.toString());
		HttpResponse httpResponse = httpclient.execute(method);
		PreyHttpResponse response = new PreyHttpResponse(httpResponse);
		PreyLogger.d("Response from server: " + response.toString());
		return response;
	}

	public PreyHttpResponse put(String url, Map<String, String> params, PreyConfig preyConfig, String user, String pass) throws IOException {
		HttpPut method = new HttpPut(url);
		method.setHeader("Accept", "application/xml,text/html,application/xhtml+xml;q=0.9,*/*;q=0.8");
		method.addHeader("Authorization", "Basic " + getCredentials(user, pass));
		method.setEntity(new UrlEncodedFormEntity(getHttpParamsFromMap(params), HTTP.UTF_8));
		// method.setParams(getHttpParamsFromMap(params));
		PreyLogger.d("Sending using 'PUT' (Basic Authentication) - URI: " + url + " - parameters: " + params.toString());
		HttpResponse httpResponse = httpclient.execute(method);
		PreyHttpResponse response = new PreyHttpResponse(httpResponse);
		PreyLogger.d("Response from server: " + response.toString());
		return response;
	}

	public PreyHttpResponse post(String url, Map<String, String> params, PreyConfig preyConfig) throws IOException {
		HttpPost method = new HttpPost(url);
		method.setHeader("Accept", "application/xml,text/html,application/xhtml+xml;q=0.9,*/*;q=0.8");
		//method.setHeader("Content-type", "text/plain");
		//method.setEntity(new UrlEncodedFormEntity(getHttpParamsFromMap(params), HTTP.UTF_8));
		String encoded = URLEncodedUtils.format(getHttpParamsFromMap(params), HTTP.UTF_8);
		encoded = encoded.replaceAll("\\+", "%20");
		Log.d("POST DATA", encoded);
		StringEntity entity = new StringEntity(encoded, HTTP.UTF_8);
		entity.setContentType("text/plain");
		method.setEntity(entity);

		//Log.d("xml RESPONSE", Integer.toString(getHttpParamsFromMap(params).size()));
		 //method.setParams(new BasicHttpParams().(params));
		/*BasicHttpParams basic = new BasicHttpParams();
		Iterator it = params.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        basic.setParameter((String)pairs.getKey(), pairs.getValue());
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    method.setParams(basic);*/
	    
		PreyLogger.d("Sending using 'POST' - URI: " + url + " - parameters: " + params.toString());
		httpclient.setRedirectHandler(new NotRedirectHandler());
		HttpResponse httpResponse = httpclient.execute(method);
		PreyHttpResponse response = new PreyHttpResponse(httpResponse);
		PreyLogger.d("Response from server: " + response.toString());
		return response;
	}

	public PreyHttpResponse get(String url, Map<String, String> params, PreyConfig preyConfig) throws IOException {
		HttpGet method = new HttpGet(url + URLEncodedUtils.format(getHttpParamsFromMap(params), "UTF-8"));
		method.setHeader("Accept", "application/xml,text/html,application/xhtml+xml;q=0.9,*/*;q=0.8");
		PreyLogger.d("Sending using 'GET' - URI: " + method.getURI());
		HttpResponse httpResponse = httpclient.execute(method);
		PreyHttpResponse response = new PreyHttpResponse(httpResponse);
		PreyLogger.d("Response from server: " + response.toString());
		return response;
	}

	public PreyHttpResponse get(String url, Map<String, String> params, PreyConfig preyConfig, String user, String pass) throws IOException {
		HttpGet method = new HttpGet(url + URLEncodedUtils.format(getHttpParamsFromMap(params), "UTF-8"));
		method.setHeader("Accept", "application/xml,text/html,application/xhtml+xml;q=0.9,*/*;q=0.8");
		method.addHeader("Authorization", "Basic " + getCredentials(user, pass));
		PreyLogger.d("Sending using 'GET' (Basic Authentication) - URI: " + method.getURI());
		HttpResponse httpResponse = httpclient.execute(method);
		PreyHttpResponse response = new PreyHttpResponse(httpResponse);
		PreyLogger.d("Response from server: " + response.toString());
		method.removeHeaders("Authorization");
		return response;
	}

	public PreyHttpResponse delete(String url, Map<String, String> params, PreyConfig preyConfig) throws IOException {
		HttpDelete method = new HttpDelete(url + URLEncodedUtils.format(getHttpParamsFromMap(params), "UTF-8"));
		method.setHeader("Accept", "application/xml,text/html,application/xhtml+xml;q=0.9,*/*;q=0.8");
		method.addHeader("Authorization", "Basic " + getCredentials(preyConfig.getApiKey(), "X"));
		PreyLogger.d("Sending using 'DELETE' (Basic Authentication) - URI: " + url + " - parameters: " + params.toString());
		HttpResponse httpResponse = httpclient.execute(method);
		PreyHttpResponse response = new PreyHttpResponse(httpResponse);
		PreyLogger.d("Response from server: " + response.toString());
		return response;
	}

	private PreyHttpResponse sendUsingMethodUsingCredentials(HttpPost method, PreyConfig preyConfig, String user, String pass) throws IOException {

		PreyHttpResponse response = null;
		try {
			// method.setDoAuthentication(true);
			method.addHeader("Authorization", "Basic " + getCredentials(user, pass));
			HttpResponse httpResponse = httpclient.execute(method);
			response = new PreyHttpResponse(httpResponse);
			PreyLogger.d("Response from server: " + response.toString());

		} catch (IOException e) {
			PreyLogger.e("Error connecting with server", e);
			throw e;
		}
		return response;
	}

	private String getCredentials(String user, String password) {

		return (Base64.encodeBytes((user + ":" + password).getBytes()));
	}

	private PreyHttpResponse sendUsingMethod(HttpRequestBase method, PreyConfig preyConfig) throws IOException {

		PreyHttpResponse response = null;
		try {

			HttpResponse httpResponse = httpclient.execute(method);
			response = new PreyHttpResponse(httpResponse);
			PreyLogger.d("Response from server: " + response.toString());

		} catch (IOException e) {
			throw e;
		}
		return response;
	}

	private String getUserAgent() {
		return "Prey/".concat(PreyConfig.getPreyConfig(ctx).getPreyVersion()).concat(" (Android) - v") + PreyConfig.getPreyConfig(ctx).getPreyMinorVersion();
	}

}

final class NotRedirectHandler implements RedirectHandler {

	public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
		return false;
	}

	public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
		return null;
	}
}
