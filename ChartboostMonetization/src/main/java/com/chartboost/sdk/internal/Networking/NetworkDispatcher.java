package com.chartboost.sdk.internal.Networking;

import static com.chartboost.sdk.internal.Model.CBError.Internal.INTERNET_UNAVAILABLE;
import static com.chartboost.sdk.internal.Model.CBError.Internal.NETWORK_FAILURE;

import android.os.Build;

import com.chartboost.sdk.BuildConfig;
import com.chartboost.sdk.internal.Libraries.CommonsIO;
import com.chartboost.sdk.internal.Libraries.TimeSource;
import com.chartboost.sdk.internal.Model.CBError;
import com.chartboost.sdk.internal.Networking.requests.VideoRequest;
import com.chartboost.sdk.internal.UiPoster;
import com.chartboost.sdk.internal.logging.Logger;
import com.chartboost.sdk.tracking.CriticalEvent;
import com.chartboost.sdk.tracking.EventTracker;
import com.chartboost.sdk.tracking.TrackingEventName;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

import kotlin.Unit;

public class NetworkDispatcher<T> implements Runnable, Comparable<NetworkDispatcher<T>> {
    private static final int INITIAL_TIMEOUT_MS = 10000;
    private static final int MAX_RETRIES = 1;
    private static final int TIMEOUT_RETRY_MULTIPLIER = 2;
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private final Executor backgroundExecutor;
    private final NetworkFactory factory;
    private final CBReachability reachability;
    private final TimeSource timeSource;
    private final UiPoster uiPoster;
    // Public :(
    public final CBNetworkRequest<T> request;
    private final EventTracker eventTracker;

    private CBNetworkRequestResult<T> result;
    private CBNetworkServerResponse serverResponse;
    //this won't be affect by multiple requests because new object is created for each requests
    private boolean shouldNotifyTempFileReady = true;


    NetworkDispatcher(
            Executor backgroundExecutor,
            NetworkFactory factory,
            CBReachability reachability,
            TimeSource timeSource,
            UiPoster uiPoster,
            CBNetworkRequest<T> request,
            EventTracker eventTracker
    ) {
        this.backgroundExecutor = backgroundExecutor;
        this.factory = factory;
        this.reachability = reachability;
        this.timeSource = timeSource;
        this.uiPoster = uiPoster;
        this.request = request;
        this.eventTracker = eventTracker;
    }

    @Override
    public void run() {

        // Second pass: Deliver the result
        if (result != null) {
            try {
                if (result.error == null) {
                    request.deliverResponse(result.value, serverResponse);
                } else {
                    request.deliverError(result.error, serverResponse);
                }
            } catch (Exception e) {
                Logger.e("deliver result: ", e);
            }
            return;
        }

        // First pass: Connect and get result
        if (!request.status.compareAndSet(CBNetworkRequest.Status.QUEUED, CBNetworkRequest.Status.PROCESSING))
            return;

        final long startProcessingNanoTime = timeSource.nanoTime();

        try {
            if (reachability.isNetworkAvailable()) {
                serverResponse = performRequestWithRetries(request);
                if (serverResponse.isStatusOk()) {
                    result = request.parseServerResponse(serverResponse);
                } else {
                    result = getHTTPStatusError(serverResponse.getStatusCode());
                }
            } else {
                result = getInternetUnavailableError();
            }
        } catch (UnknownHostException | InterruptedIOException | SSLException | SocketException ex) {
            // Needed to add network checks to properly return error. For example brief connection
            // interruption during video file download could return SSL error or I/O error and
            // then never recover proper state when connection was back but once network state
            // reports missing connection upon error we can throw proper error and recover ad download
            if (reachability.isNetworkAvailable()) {
                result = getNetworkFailureError(ex);
            } else {
                result = getInternetUnavailableError();
            }
            removePrecacheTempFileAndTrackError(TrackingEventName.Network.DISPATCHER_EXCEPTION, ex.toString());
        } catch (Throwable ex) {
            if (reachability.isNetworkAvailable()) {
                result = getMiscError(ex);
            } else {
                result = getInternetUnavailableError();
            }
            removePrecacheTempFileAndTrackError(TrackingEventName.Network.REQUEST_JSON_SERIALIZATION_ERROR, ex.toString());
        } finally {
            request.processingNs = timeSource.nanoTime() - startProcessingNanoTime;
            switch (request.dispatch) {
                case UI:
                    uiPoster.invoke(() -> {
                        run();
                        return Unit.INSTANCE;
                    });
                    break;
                case ASYNC:
                    backgroundExecutor.execute(this);
                    break;
            }
        }
    }

    private CBNetworkRequestResult<T> getHTTPStatusError(int statusCode) {
        return CBNetworkRequestResult.failure(
                new CBError(NETWORK_FAILURE, "Failure due to HTTP status code " + statusCode)
        );
    }

    private CBNetworkRequestResult<T> getInternetUnavailableError() {
        return CBNetworkRequestResult.failure(
                new CBError(INTERNET_UNAVAILABLE, "Internet Unavailable")
        );
    }

    private CBNetworkRequestResult<T> getNetworkFailureError(IOException ex) {
        return CBNetworkRequestResult.failure(
                new CBError(NETWORK_FAILURE, ex.toString())
        );
    }

    private CBNetworkRequestResult<T> getMiscError(Throwable t) {
        return CBNetworkRequestResult.failure(
                new CBError(CBError.Internal.MISCELLANEOUS, t.toString())
        );
    }

    private void removePrecacheTempFileAndTrackError(TrackingEventName errorName, String errorMsg) {
        try {
            removePrecacheTempFileOnError();
            eventTracker.track(
                    CriticalEvent.instance(errorName, errorMsg)
            );
        } catch (Exception e) {
            // handle possible crash - MO-4905
        }
    }

    private void removePrecacheTempFileOnError() {
        if (request != null && request.outputFile != null && request instanceof VideoRequest) {
            File tmpFile = new File(request.outputFile.getParentFile(), request.outputFile.getName() + ".tmp");
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }

    private CBNetworkServerResponse performRequestWithRetries(CBNetworkRequest<T> request) throws IOException {
        int timeoutMs = INITIAL_TIMEOUT_MS;
        int retryCount = 0;
        while (true) {
            try {
                return performRequest(request, timeoutMs);
            } catch (SocketTimeoutException ex) {
                if (retryCount < MAX_RETRIES) {
                    timeoutMs *= TIMEOUT_RETRY_MULTIPLIER;
                    retryCount++;
                } else {
                    throw ex;
                }
            }
        }
    }

    private CBNetworkServerResponse performRequest(CBNetworkRequest<T> request, int timeoutMs) throws IOException {
        shouldNotifyTempFileReady = true;
        CBNetworkRequestInfo info = request.buildRequestInfo();
        Map<String, String> headers = info.headers;
        HttpsURLConnection connection = factory.openConnection(request);
        connection.setSSLSocketFactory(CBSSLSocketFactory.getSSLSocketFactory());
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        try {
            addHeaders(headers, connection);
            connection.setRequestMethod(request.getMethod().name());
            tryPerformPostRequest(info, connection);

            long getResponseCodeStart = timeSource.nanoTime();
            long readDataStart;
            int responseCode;

            try {
                responseCode = connection.getResponseCode();
            } finally {
                //this is run regardless of the exception
                readDataStart = timeSource.nanoTime();
                request.getResponseCodeNs = readDataStart - getResponseCodeStart;
            }

            if (responseCode == -1) {
                // -1 is returned by getResponseCode() if the response code could not be retrieved.
                // Signal to the caller that something was wrong with the connection.
                throw new IOException("Could not retrieve response code from HttpsURLConnection.");
            }

            byte[] data = parseData(connection, responseCode, readDataStart);
            return new CBNetworkServerResponse(responseCode, data);
        } finally {
            connection.disconnect();
        }
    }

    private void addHeaders(Map<String, String> headers, HttpsURLConnection connection) {
        if (headers != null) {
            for (String headerName : headers.keySet()) {
                connection.addRequestProperty(headerName, headers.get(headerName));
            }
        }
    }

    private void tryPerformPostRequest(CBNetworkRequestInfo info, HttpsURLConnection connection) throws IOException {
        if (CBNetworkRequest.Method.POST.equals(request.getMethod())) {
            if (info.body != null) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(info.body.length);
                if (info.contentType != null)
                    connection.addRequestProperty(HEADER_CONTENT_TYPE, info.contentType);

                try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                    out.write(info.body);
                }
            }
        }
    }

    private byte[] parseData(HttpsURLConnection connection, int responseCode, long readDataStart) throws IOException {
        byte[] data = new byte[0];

        try {
            if (hasResponseBody(responseCode)) {
                if (request.outputFile != null) {
                    prepareTempFile(connection);
                } else {
                    data = parseConnectionData(connection);
                }
            } else {
                data = new byte[0];
            }
        } finally {
            request.readDataNs = timeSource.nanoTime() - readDataStart;
        }
        return data;
    }

    private void prepareTempFile(HttpsURLConnection connection) throws IOException {
        File tmpFile = new File(request.outputFile.getParentFile(), request.outputFile.getName() + ".tmp");
        boolean fileAlreadyExists = false;
        if (request instanceof VideoRequest) {
            // Notify right away temp file is created for precache
            if (!tmpFile.exists()) {
                if (!tmpFile.createNewFile()) {
                    throw new IOException("Video temp file was not created and doesn't exist");
                }
            } else {
                fileAlreadyExists = true;
            }
        }

        if (fileAlreadyExists) {
            // Video file is already downloading, starting new download would break the flow
            return;
        }

        if (request instanceof VideoRequest) {
            notifyTempFileIsReadyForVideoRequest(request.getUri(), getContentSize(connection));
        }

        try (InputStream is = connection.getInputStream(); FileOutputStream fos = new FileOutputStream(tmpFile)) {
            if (request instanceof VideoRequest) {
                //On each interval check if file exists, otherwise throw an error to close the download stream
                byte[] buffer = new byte[8 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    if (!tmpFile.exists()) {
                        throw new IOException("Temp file was deleted during download");
                    }
                    fos.write(buffer, 0, read);
                }
            } else {
                CommonsIO.INSTANCE.copy(is, fos);
            }
        }

        // Copy temp file to actual file in the cache folder
        if (!tmpFile.renameTo(request.outputFile)) {
            if (!tmpFile.delete()) {
                String errorMsg = "Unable to delete " + tmpFile.getAbsolutePath() + " after failing to rename to " + request.outputFile.getAbsolutePath();
                trackResponseDataWriteError(errorMsg);
                throw new IOException(errorMsg);
            }
            String errorMsg = "Unable to move " + tmpFile.getAbsolutePath() + " to " + request.outputFile.getAbsolutePath();
            trackResponseDataWriteError(errorMsg);
            throw new IOException(errorMsg);
        }
    }

    private void trackResponseDataWriteError(String errorMsg) {
        eventTracker.track(
                CriticalEvent.instance(
                        TrackingEventName.Network.RESPONSE_DATA_WRITE_ERROR,
                        errorMsg
                )
        );
    }

    private byte[] parseConnectionData(HttpsURLConnection connection) throws IOException {
        byte[] data;
        InputStream is = null;
        try {
            try {
                is = connection.getInputStream();
            } catch (IOException ioe) {
                is = connection.getErrorStream();
            }

            if (is != null) {
                data = CommonsIO.INSTANCE.toByteArray(new BufferedInputStream(is));
            } else {
                data = new byte[0];
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ignore) { /*DO NOTHING*/ }
        }
        return data;
    }

    /**
     * Notify request that temp file has been created, only once per request
     * Added boolean check due to successful path, we would call callback twice since code in
     * last finally block is always run
     */
    private void notifyTempFileIsReadyForVideoRequest(String uri, long contentSize) {
        if (shouldNotifyTempFileReady) {
            shouldNotifyTempFileReady = false;
            request.notifyTempFileIsReady(uri, contentSize);
        }
    }

    /**
     * Checks if a response message contains a body.
     *
     * @param responseCode response status code
     * @return whether the response has a body
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3">RFC 7230 section 3.3</a>
     * no param requestMethod: request method (we only support GET and POST, and only
     * HEAD never has a response body)
     */

    private static boolean hasResponseBody(int responseCode) {
        boolean is1xxInformational = 100 <= responseCode && responseCode < HttpsURLConnection.HTTP_OK;
        return !(is1xxInformational
                || responseCode == HttpsURLConnection.HTTP_NO_CONTENT
                || responseCode == HttpsURLConnection.HTTP_NOT_MODIFIED);
    }

    @Override
    public int compareTo(NetworkDispatcher another) {
        // process higher-priority requests first.
        return (this.request.getPriority().getValue() - (another.request.getPriority().getValue()));
    }

    private long getContentSize(HttpsURLConnection connection) {
        // wiremock is not able to send Content-Length hence I send Test-Length instead.
        // This should only be available in the automated test Debug context
        if (BuildConfig.DEBUG) {
            String fileSizeFromWiremock = connection.getHeaderField("Test-Length");
            try {
                long size = Long.parseLong(fileSizeFromWiremock);
                if (size > 0) {
                    return size;
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return connection.getContentLength();
        } else {
            return connection.getContentLengthLong();
        }
    }
}
