/**
 * 
 */
package com.ilivedata.nlp.samples;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LiveAudioCheckResult {

    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final Charset UTF8 = Charset.forName(DEFAULT_ENCODING);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private String pid = "YOUR_PID_GOES_HERE";
    private String secretKey = "YOUR_SECRETKEY_GOES_HERE";
    private String endpointHost = "asafe.ilivedata.com";
    private String endpointPath = "/api/v1/liveaudio/check/result";
    private String endpointURL = "https://asafe.ilivedata.com/api/v1/liveaudio/check/result";

    public String result(String taskId) throws IOException {
        String now = ZonedDateTime.now(ZoneId.of("UTC")).format(FORMATTER);
        
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("taskId", taskId);

        ObjectMapper mapper = new ObjectMapper();
        
        String queryBody = mapper.writeValueAsString(parameters);
        System.out.println(queryBody);
        
        // Prepare stringToSign
        StringBuilder stringToSign = new StringBuilder();
        stringToSign.append("POST").append("\n");
        stringToSign.append(endpointHost).append("\n");
        stringToSign.append(endpointPath).append("\n");
        stringToSign.append(sha256AndHexEncode(queryBody)).append("\n");
        stringToSign.append("X-AppId:").append(pid).append("\n");
        stringToSign.append("X-TimeStamp:").append(now);
        System.out.println(stringToSign);
        // Sign the request
        String signature = signAndBase64Encode(stringToSign.toString(), secretKey);
        System.out.println(signature);
        return request(queryBody, signature, now);
    }
    
    private String request(String body, String signature, String timeStamp) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(endpointURL)
                .post(RequestBody.create(body, MediaType.get("application/json; charset=utf-8")))
                .addHeader("X-AppId", pid)
                .addHeader("X-TimeStamp", timeStamp)
                .addHeader("authorization", signature)
                .addHeader("Content-type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }
    
    private String sha256AndHexEncode(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to calculate sha256 hash: " + e.getMessage(), e);
        }
        
    }
    
    private static String bytesToHex(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
        String hex = Integer.toHexString(0xff & hash[i]);
        if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String signAndBase64Encode(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(UTF8), "HmacSHA256"));
            byte[] signature = mac.doFinal(data.getBytes(UTF8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Unable to calculate a request signature: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws IOException {
        String taskId = "THE_TASK_ID_FROM_SUBMIT_API";
        LiveAudioCheckResult audioCheckResult = new LiveAudioCheckResult();
        String result = audioCheckResult.result(taskId);
        System.out.println(result);
    }

}   
