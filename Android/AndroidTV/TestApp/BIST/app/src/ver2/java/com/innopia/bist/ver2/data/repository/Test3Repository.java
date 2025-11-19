package com.innopia.bist.ver2.data.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.ver2.util.TestResult;
import com.innopia.bist.ver2.util.TestStatus;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Test3 - 네트워크 속도 측정 Repository
 */
public class Test3Repository implements Test {

    private static final String TAG = "Test3Repository";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TestStatus currentStatus = TestStatus.IDLE;
    private boolean isTestRunning = false;

    // 테스트용 공개 파일 URL
    private static final String DOWNLOAD_TEST_URL =
            "http://ipv4.download.thinkbroadband.com/5MB.zip";

    // 업로드 테스트용 엔드포인트
    private static final String UPLOAD_TEST_URL =
            "http://httpbin.org/post";

    // Ping 테스트용 호스트
    private static final String PING_TEST_HOST =
            "http://www.google.com";

    public Test3Repository(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 다운로드 속도 테스트
     */
    public void testDownloadSpeed(Test3Callback callback) {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;

        if (!isNetworkAvailable()) {
            handler.post(() -> callback.onError("No network connection available"));
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream input = null;

            try {
                handler.post(() -> callback.onTestProgress(10, "Connecting to server..."));

                URL url = new URL(DOWNLOAD_TEST_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setInstanceFollowRedirects(true);
                connection.connect();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Download response code: " + responseCode);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Server returned code: " + responseCode);
                }

                long fileSize = connection.getContentLength();
                if (fileSize <= 0) {
                    fileSize = 5 * 1024 * 1024; // 5MB
                }
                Log.d(TAG, "File size: " + fileSize + " bytes");

                input = connection.getInputStream();
                handler.post(() -> callback.onTestProgress(20, "Downloading..."));

                byte[] buffer = new byte[8192];
                long totalBytesRead = 0;
                int bytesRead;
                long startTime = System.currentTimeMillis();
                long lastUpdateTime = startTime;

                List<Float> speedData = new ArrayList<>();

                while ((bytesRead = input.read(buffer)) != -1 && isTestRunning) {
                    totalBytesRead += bytesRead;

                    final long currentBytes = totalBytesRead;
                    int progress = 20 + (int)((currentBytes * 60) / fileSize);
                    long currentTime = System.currentTimeMillis();
                    double elapsedSeconds = (currentTime - startTime) / 1000.0;

                    if (currentTime - lastUpdateTime >= 500 && elapsedSeconds > 0) {
                        double currentSpeed = (currentBytes / 1024.0 / 1024.0) / elapsedSeconds;
                        speedData.add((float) currentSpeed);

                        handler.post(() -> {
                            callback.onTestProgress(progress,
                                    String.format("Download: %.2f MB/s", currentSpeed));
                            callback.onChartDataUpdate(new ArrayList<>(speedData));
                        });
                        lastUpdateTime = currentTime;
                    }
                }

                long endTime = System.currentTimeMillis();
                double totalTimeSeconds = (endTime - startTime) / 1000.0;
                double downloadSpeedMbps = (totalBytesRead * 8.0 / 1024.0 / 1024.0) / totalTimeSeconds;

                Log.d(TAG, "Download completed: " + totalBytesRead + " bytes in " + totalTimeSeconds + "s");

                DownloadTestResult result = new DownloadTestResult();
                result.downloadSpeed = downloadSpeedMbps;
                result.totalBytes = totalBytesRead;
                result.totalTimeSeconds = totalTimeSeconds;
                result.connectionType = getConnectionType();
                result.speedData = speedData;

                currentStatus = TestStatus.COMPLETED;
                handler.post(() -> callback.onDownloadTestCompleted(result));

            } catch (Exception e) {
                Log.e(TAG, "Download test error", e);
                currentStatus = TestStatus.ERROR;
                String errorMsg = getErrorMessage(e);
                handler.post(() -> callback.onError(errorMsg));
            } finally {
                closeQuietly(input);
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    /**
     * 업로드 속도 테스트
     */
    public void testUploadSpeed(Test3Callback callback) {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;

        if (!isNetworkAvailable()) {
            handler.post(() -> callback.onError("No network connection available"));
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            OutputStream output = null;

            try {
                handler.post(() -> callback.onTestProgress(10, "Preparing upload..."));

                // 512KB 테스트 데이터 생성 (크기 축소)
                int dataSize = 512 * 1024;
                byte[] testData = new byte[dataSize];
                for (int i = 0; i < dataSize; i++) {
                    testData[i] = (byte)(i % 256);
                }

                URL url = new URL(UPLOAD_TEST_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Content-Type", "application/octet-stream");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                handler.post(() -> callback.onTestProgress(20, "Uploading..."));

                long startTime = System.currentTimeMillis();
                output = connection.getOutputStream();

                int chunkSize = 8192;
                int totalChunks = dataSize / chunkSize;
                long lastUpdateTime = startTime;

                List<Float> speedData = new ArrayList<>();

                for (int i = 0; i < dataSize && isTestRunning; i += chunkSize) {
                    int length = Math.min(chunkSize, dataSize - i);
                    output.write(testData, i, length);
                    output.flush();

                    final int currentChunk = i / chunkSize;
                    int progress = 20 + (currentChunk * 60 / totalChunks);
                    long currentTime = System.currentTimeMillis();

                    if (currentTime - lastUpdateTime >= 500) {
                        double elapsedSeconds = (currentTime - startTime) / 1000.0;
                        if (elapsedSeconds > 0) {
                            double currentSpeed = (i * 8.0 / 1024.0 / 1024.0) / elapsedSeconds;
                            speedData.add((float) currentSpeed);

                            handler.post(() -> {
                                callback.onTestProgress(progress,
                                        String.format("Upload: %.2f Mbps", currentSpeed));
                                callback.onChartDataUpdate(new ArrayList<>(speedData));
                            });
                        }
                        lastUpdateTime = currentTime;
                    }
                }

                output.close();

                int responseCode = connection.getResponseCode();
                long endTime = System.currentTimeMillis();

                Log.d(TAG, "Upload response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    double totalTimeSeconds = (endTime - startTime) / 1000.0;
                    double uploadSpeedMbps = (dataSize * 8.0 / 1024.0 / 1024.0) / totalTimeSeconds;

                    UploadTestResult result = new UploadTestResult();
                    result.uploadSpeed = uploadSpeedMbps;
                    result.totalBytes = dataSize;
                    result.totalTimeSeconds = totalTimeSeconds;
                    result.connectionType = getConnectionType();
                    result.speedData = speedData;

                    currentStatus = TestStatus.COMPLETED;
                    handler.post(() -> callback.onUploadTestCompleted(result));
                } else {
                    throw new Exception("Upload failed with code: " + responseCode);
                }

            } catch (Exception e) {
                Log.e(TAG, "Upload test error", e);
                currentStatus = TestStatus.ERROR;
                String errorMsg = getErrorMessage(e);
                handler.post(() -> callback.onError(errorMsg));
            } finally {
                closeQuietly(output);
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    /**
     * Ping 테스트
     */
    public void testPing(Test3Callback callback) {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;

        if (!isNetworkAvailable()) {
            handler.post(() -> callback.onError("No network connection available"));
            return;
        }

        new Thread(() -> {
            try {
                handler.post(() -> callback.onTestProgress(10, "Testing ping..."));

                int pingCount = 10;
                long totalPingTime = 0;
                int successCount = 0;

                List<Float> pingData = new ArrayList<>();

                for (int i = 0; i < pingCount && isTestRunning; i++) {
                    long startTime = System.currentTimeMillis();

                    try {
                        URL url = new URL(PING_TEST_HOST);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("HEAD");
                        connection.setConnectTimeout(5000);
                        connection.setReadTimeout(5000);
                        connection.setInstanceFollowRedirects(true);
                        connection.connect();

                        int responseCode = connection.getResponseCode();
                        long endTime = System.currentTimeMillis();

                        connection.disconnect();

                        if (responseCode >= 200 && responseCode < 400) {
                            long pingTime = endTime - startTime;
                            totalPingTime += pingTime;
                            successCount++;
                            pingData.add((float) pingTime);

                            Log.d(TAG, "Ping " + (i+1) + ": " + pingTime + "ms");
                        } else {
                            pingData.add(0f);
                        }

                        final int currentPing = i + 1;
                        final long currentPingTime = endTime - startTime;
                        int progress = 10 + (currentPing * 80 / pingCount);
                        handler.post(() -> {
                            callback.onTestProgress(progress,
                                    "Ping " + currentPing + "/" + pingCount + ": " + currentPingTime + "ms");
                            callback.onChartDataUpdate(new ArrayList<>(pingData));
                        });

                    } catch (Exception e) {
                        Log.w(TAG, "Ping attempt " + (i + 1) + " failed", e);
                        pingData.add(0f);
                    }

                    if (i < pingCount - 1) {
                        Thread.sleep(200);
                    }
                }

                if (successCount > 0) {
                    long averagePing = totalPingTime / successCount;

                    PingTestResult result = new PingTestResult();
                    result.averagePing = averagePing;
                    result.successCount = successCount;
                    result.totalAttempts = pingCount;
                    result.connectionType = getConnectionType();
                    result.pingData = pingData;

                    currentStatus = TestStatus.COMPLETED;
                    handler.post(() -> callback.onPingTestCompleted(result));
                } else {
                    throw new Exception("All ping attempts failed");
                }

            } catch (Exception e) {
                Log.e(TAG, "Ping test error", e);
                currentStatus = TestStatus.ERROR;
                String errorMsg = getErrorMessage(e);
                handler.post(() -> callback.onError(errorMsg));
            }
        }).start();
    }

    /**
     * 에러 메시지 생성
     */
    private String getErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (e instanceof java.net.UnknownHostException) {
            return "Cannot reach server: " + msg;
        } else if (e instanceof java.net.SocketTimeoutException) {
            return "Connection timeout";
        } else if (e instanceof java.io.IOException) {
            if (msg != null && msg.contains("Cleartext")) {
                return "HTTP not allowed. Check network_security_config.xml";
            }
            return "Network error: " + msg;
        }
        return "Test failed: " + (msg != null ? msg : e.getClass().getSimpleName());
    }

    /**
     * 안전하게 스트림 닫기
     */
    private void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }

    private String getConnectionType() {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                int type = networkInfo.getType();
                switch (type) {
                    case ConnectivityManager.TYPE_WIFI:
                        return "WiFi";
                    case ConnectivityManager.TYPE_MOBILE:
                        return "Mobile";
                    case ConnectivityManager.TYPE_ETHERNET:
                        return "Ethernet";
                    default:
                        return "Unknown";
                }
            }
        }
        return "Not Connected";
    }

    @Override
    public void startTest(TestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        callback.onTestStarted();
    }

    @Override
    public void stopTest() {
        currentStatus = TestStatus.IDLE;
        isTestRunning = false;
    }

    @Override
    public void pauseTest() {
        currentStatus = TestStatus.PAUSED;
        isTestRunning = false;
    }

    @Override
    public void resumeTest() {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;
    }

    @Override
    public TestStatus getTestStatus() {
        return currentStatus;
    }

    // 콜백 및 결과 클래스는 동일
    public interface Test3Callback {
        void onTestProgress(int progress, String message);
        void onChartDataUpdate(List<Float> data);
        void onDownloadTestCompleted(DownloadTestResult result);
        void onUploadTestCompleted(UploadTestResult result);
        void onPingTestCompleted(PingTestResult result);
        void onError(String error);
    }

    public static class DownloadTestResult {
        public double downloadSpeed;
        public long totalBytes;
        public double totalTimeSeconds;
        public String connectionType;
        public List<Float> speedData;
    }

    public static class UploadTestResult {
        public double uploadSpeed;
        public long totalBytes;
        public double totalTimeSeconds;
        public String connectionType;
        public List<Float> speedData;
    }

    public static class PingTestResult {
        public long averagePing;
        public int successCount;
        public int totalAttempts;
        public String connectionType;
        public List<Float> pingData;
    }
}
