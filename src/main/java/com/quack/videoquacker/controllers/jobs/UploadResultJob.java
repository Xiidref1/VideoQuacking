package com.quack.videoquacker.controllers.jobs;

import com.quack.videoquacker.controllers.JobPaneController;
import com.quack.videoquacker.exceptions.JobFailedException;
import com.quack.videoquacker.models.JobParameters;
import com.quack.videoquacker.utils.DataManager;
import com.quack.videoquacker.utils.PropertiesManager;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UploadResultJob extends BasicJobStep {

    private static String LOGIN_URL = PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.pocisalie_endpoint_login);
    private static String LOGOUT_URL = PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.pocisalie_endpoint_login);
    private static String UPLOAD_URL = PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.pocisalie_endpoint_upload);
    private static String UPLOAD_BOUNDARY = UUID.randomUUID().toString();

    private static CookieManager COOKIE_MANAGER = new CookieManager();
    private static HttpCookie SESSION_COOKIE = null;

    private ArrayList<String> jobErrors = new ArrayList<>();
    private UPLOAD_STATUS uploadStatus = UPLOAD_STATUS.NOT_STARTED;


    public UploadResultJob(JobPaneController controller, JobParameters jobParameters) {
        super(controller, jobParameters);
    }

    @Override
    protected List<Label> getLabels() {
        return List.of(this.controller.lblUploadResFinalLink, this.controller.lblUploadResSessionConnect);
    }

    @Override
    protected Label getLblTitle() {
        return this.controller.lblUploadRes;
    }

    @Override
    protected Circle getCircle() {
        return this.controller.cirUploadRes;
    }

    @Override
    protected void run() throws JobFailedException {
        if (this.jobParameters.getUploadId() != null) {
            try {
                Platform.runLater(() -> {
                   this.controller.lblUploadResSessionConnect.setText("Connecting to session");
                });
                this.doLogin();
                Platform.runLater(() -> {
                    this.controller.lblUploadResSessionConnect.setText("Connected");
                });

                this.doUpload(new File(this.jobParameters.getProbeResult().getFileName()), this.jobParameters.getUploadId());

            } catch (IOException | URISyntaxException e) {
                JobFailedException jobFailedException = new JobFailedException(e.getMessage());
                jobFailedException.initCause(e);
                throw jobFailedException;
            }
        } else {
            Platform.runLater(() -> {
                this.controller.lblUploadRes.setText("Upload Skipped no folder id set");
                this.controller.lblUploadResProgress.setText("Skipped no folder id configured");
                this.controller.lblUploadResFinalLink.setText("N/A");
                this.controller.lblUploadResSessionConnect.setText("N/A");

                this.controller.lblUploadRes.setStyle("-fx-accent: " + DataManager.getRGBAString(JobPaneController.JobStepStatusEnum.STATUS_STAGING.lblColor));
                this.controller.lblUploadResProgress.setStyle("-fx-accent: " + DataManager.getRGBAString(JobPaneController.JobStepStatusEnum.STATUS_STAGING.lblColor));
                this.controller.lblUploadResFinalLink.setStyle("-fx-accent: " + DataManager.getRGBAString(JobPaneController.JobStepStatusEnum.STATUS_STAGING.lblColor));
                this.controller.lblUploadResSessionConnect.setStyle("-fx-accent: " + DataManager.getRGBAString(JobPaneController.JobStepStatusEnum.STATUS_STAGING.lblColor));
            });
        }
    }

    @Override
    public void stop() {

    }

    @Override
    public JobPaneController.JobStepsEnum getStep() {
        return null;
    }


    public static void main(String[] args) throws JobFailedException {
        new UploadResultJob(null, null).run();
    }


    private void doLogin() throws IOException {
        if (UploadResultJob.SESSION_COOKIE == null) {
            String login = PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.pocisalie_cred_login);
            String password = PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.pocisalie_cred_password);

            CookieHandler.setDefault(UploadResultJob.COOKIE_MANAGER);
            HttpURLConnection connection = (HttpURLConnection) new URL(UploadResultJob.LOGIN_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            try (DataOutputStream formWriter =  new DataOutputStream(connection.getOutputStream())) {
                formWriter.write(("pseudo=" + URLEncoder.encode(login, StandardCharsets.UTF_8) + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8));
            }

            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                for (HttpCookie cookie : UploadResultJob.COOKIE_MANAGER.getCookieStore().getCookies()) {
                    if ("PHPSESSID".equals(cookie.getName())) {
                        UploadResultJob.SESSION_COOKIE = cookie;
                        connection.disconnect();
                        break;
                    }
                }
            }
        }
    }

    private void doUpload(File file, long folderId) throws IOException, URISyntaxException, JobFailedException {
        if (UploadResultJob.SESSION_COOKIE != null) {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                this.uploadStatus = UPLOAD_STATUS.PENDING;

                //Opening the connection with multipart/form-data for file upload


                //Use for debug with a proxy like burpsuite
//                HttpsTrustManager.allowAllSSL();
//                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8080));
//                HttpURLConnection connection = (HttpURLConnection) new URI(UploadResultJob.UPLOAD_URL).toURL().openConnection(proxy);

                // If not debugging
                HttpURLConnection connection = (HttpURLConnection) new URI(UploadResultJob.UPLOAD_URL).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + UploadResultJob.UPLOAD_BOUNDARY);

                OutputStream outputStream = connection.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "utf-8"), true);

                String newLine = "\r\n";
                //Folder id parameter
                writer.append("--").append(UploadResultJob.UPLOAD_BOUNDARY).append(newLine)
                        .append("Content-Disposition: form-data; name=\"idFolder\"").append(newLine)
                        .append(newLine)
                        .append(String.valueOf(folderId)).append(newLine);
                //File data parameter
                writer.append("--").append(UploadResultJob.UPLOAD_BOUNDARY).append(newLine)
                        .append("Content-Disposition: form-data; name=\"files[]\"; filename=\"").append(file.getName()).append("\"").append(newLine).append("Content-Type: ").append(URLConnection.guessContentTypeFromName(file.getName())).append(newLine)
                        .append(newLine)
                        .flush();

                byte[] buffer = new byte[4096];
                int bytesRead = -1;
                long totalBytes = file.length();
                long bytesTransferred = 0;

                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    bytesTransferred += bytesRead;
                    int progress = (int) ((bytesTransferred * 100) / totalBytes);
                    Platform.runLater(()->{
                        this.controller.lblUploadResProgress.setText(progress + " %");
                        this.controller.pbUploadResProgress.setProgress(progress);
                        if (progress == 100) {
                            this.controller.pbUploadResProgress.setStyle("-fx-accent: " + DataManager.getRGBAString(JobPaneController.JobStepStatusEnum.STATUS_DONE.cirColor));
                        } else {
                            this.controller.pbUploadResProgress.setStyle("-fx-accent: " + DataManager.getRGBAString(this.status.lblColor));
                        }
                    });
                }
                outputStream.flush();
                writer.append(newLine)
                        .append("--").append(UploadResultJob.UPLOAD_BOUNDARY).append("--").append(newLine)
                        .flush();

                writer.close();

                // Request closing
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    ByteArrayOutputStream result = new ByteArrayOutputStream();
                    byte[] resBuffer = new byte[1024];
                    int length;
                    while ((length = connection.getInputStream().read(resBuffer)) != -1) {
                        result.write(resBuffer, 0, length);
                    }
                    String response = result.toString(StandardCharsets.UTF_8);
                    connection.disconnect();
                    try {
                        JSONObject obj = new JSONObject(response);
                        JSONArray errors = obj.getJSONObject("data").getJSONArray("errors");
                        if (errors.isEmpty()) {
                            this.uploadStatus = UPLOAD_STATUS.OK;
                            return;
                        }

                        JobFailedException jobFailedException = new JobFailedException("Upload failed response contain error(s). Response is " + response);
                        for (Object error:errors.toList()) {
                            this.jobErrors.add(error.toString());
                        }
                        this.uploadStatus = UPLOAD_STATUS.ISSUE;
                        throw jobFailedException;

                    } catch (JSONException exception) {
                        JobFailedException jobFailedException = new JobFailedException("Upload failed cannot read response. Response is " + response);
                        jobFailedException.initCause(exception);
                        throw jobFailedException;
                    }
                }
            }
        } else {
            this.uploadStatus = UPLOAD_STATUS.NOT_CONNECTED;
        }
    }
    private enum UPLOAD_STATUS {
        NOT_STARTED,
        PENDING,
        OK,
        NOT_CONNECTED,
        ISSUE
    }


    /**
     * Class used for debugging with a proxy like burp. Allow to bypass SSL checks since proxy broke SSL
      */
    public static class HttpsTrustManager implements X509TrustManager {
        private static TrustManager[] trustManagers;
        private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[]{};

        @Override
        public void checkClientTrusted(
                X509Certificate[] x509Certificates, String s)
                throws java.security.cert.CertificateException {

        }

        @Override
        public void checkServerTrusted(
                X509Certificate[] x509Certificates, String s)
                throws java.security.cert.CertificateException {

        }

        public boolean isClientTrusted(X509Certificate[] chain) {
            return true;
        }

        public boolean isServerTrusted(X509Certificate[] chain) {
            return true;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return _AcceptedIssuers;
        }

        public static void allowAllSSL() {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }

            });

            SSLContext context = null;
            if (trustManagers == null) {
                trustManagers = new TrustManager[]{new HttpsTrustManager()};
            }

            try {
                context = SSLContext.getInstance("TLS");
                context.init(null, trustManagers, new SecureRandom());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
            }

            HttpsURLConnection.setDefaultSSLSocketFactory(context != null ? context.getSocketFactory() : null);
        }
    }
}
