package com.chartboost.sdk.test;

import com.chartboost.sdk.internal.Networking.CBNetworkServerResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;

public class ResponseDescriptor {
    public final Endpoint endpoint;

    public final int statusCode;
    public final byte[] data; // intended to be treated as immutable

    public final IOException getResponseCodeIOException;
    public final int getResponseCodeDelayMs;
    public final int readDataMs;
    public final IOException getOutputStreamIOException;

    private ResponseDescriptor(Endpoint endpoint,
                               int statusCode,
                               byte[] data,
                               IOException getResponseCodeIOException,
                               int getResponseCodeDelayMs,
                               int readDataMs,
                               IOException getOutputStreamIOException) {
        this.endpoint = endpoint;
        this.statusCode = statusCode;
        this.data = data;
        this.getResponseCodeIOException = getResponseCodeIOException;
        this.getResponseCodeDelayMs = getResponseCodeDelayMs;
        this.readDataMs = readDataMs;
        this.getOutputStreamIOException = getOutputStreamIOException;
    }

    ResponseDescriptor(Endpoint endpoint) {
        this(endpoint, 0, new byte[0], null, 0, 0, null);
    }

    public ResponseDescriptor(Endpoint endpoint, int statusCode, byte[] data) {
        this(endpoint, statusCode, data, null, 0, 0, null);
    }

    public ResponseDescriptor(AssetDescriptor assetDescriptor) {
        this(assetDescriptor.endpoint, HttpsURLConnection.HTTP_OK, assetDescriptor.copyContents());
    }

    private ResponseDescriptor withStatusCode(int statusCode) {
        return new ResponseDescriptor(endpoint,
                statusCode,
                data,
                getResponseCodeIOException,
                getResponseCodeDelayMs,
                readDataMs,
                getOutputStreamIOException);
    }

    private ResponseDescriptor withData(byte[] data) {
        return new ResponseDescriptor(endpoint,
                statusCode,
                data,
                getResponseCodeIOException,
                getResponseCodeDelayMs,
                readDataMs,
                getOutputStreamIOException);
    }

    public ResponseDescriptor withGetOutputStreamIOException(IOException getOutputStreamIOException) {
        return new ResponseDescriptor(endpoint,
                statusCode,
                data,
                getResponseCodeIOException,
                getResponseCodeDelayMs,
                readDataMs,
                getOutputStreamIOException);
    }

    public ResponseDescriptor withGetResponseCodeIOException(IOException getResponseCodeIOException) {
        return new ResponseDescriptor(endpoint,
                statusCode,
                data,
                getResponseCodeIOException,
                getResponseCodeDelayMs,
                readDataMs,
                getOutputStreamIOException);
    }

    public ResponseDescriptor withGetResponseCodeSocketException(SocketException getResponseCodeSocketException) {
        return new ResponseDescriptor(endpoint,
                statusCode,
                data,
                getResponseCodeIOException,
                getResponseCodeDelayMs,
                readDataMs,
                getOutputStreamIOException);
    }

    public ResponseDescriptor withGetResponseCodeDelayMs(int getResponseCodeDelayMs) {
        return new ResponseDescriptor(endpoint,
                statusCode,
                data,
                getResponseCodeIOException,
                getResponseCodeDelayMs,
                readDataMs,
                getOutputStreamIOException);
    }

    public ResponseDescriptor withReadDataMs(int readDataMs) {
        return new ResponseDescriptor(endpoint,
                statusCode,
                data,
                getResponseCodeIOException,
                getResponseCodeDelayMs,
                readDataMs,
                getOutputStreamIOException);
    }

    public ResponseDescriptor withInvalidResponseCode() {
        return withStatusCode(-1).withData(new byte[0]);
    }

    public ResponseDescriptor withInternalError() {
        return withStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
    }

    public ResponseDescriptor withNoData(int statusCode) {
        return withStatusCode(statusCode).withData(new byte[0]);
    }

    public ResponseDescriptor withGetResponseCodeIOException() {
        return withGetResponseCodeIOException(new IOException("simulated getResponseCode exception"));
    }

    public ResponseDescriptor withGetResponseCodeSocketException() {
        return withGetResponseCodeSocketException(new SocketException("simulated Socket getResponseCode exception"));
    }

    public byte[] copyBytes() {
        return Arrays.copyOf(data, data.length);
    }

    public JSONObject asJSONObject() {
        try {
            String json = new String(copyBytes());
            return new JSONObject(json);
        } catch (JSONException ex) {
            throw new Error(ex);
        }
    }

    public CBNetworkServerResponse asServerResponse() {
        return new CBNetworkServerResponse(statusCode, data);
    }

    public String getBodyString(String field) {
        try {
            return asJSONObject().getString(field);
        } catch (JSONException ex) {
            throw new Error(ex);
        }
    }
}
