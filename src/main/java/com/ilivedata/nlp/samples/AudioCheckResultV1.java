/**
 *
 */
package com.ilivedata.nlp.samples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AudioCheckResultV1 {

    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final Charset UTF8 = Charset.forName(DEFAULT_ENCODING);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private String pid = "YOUR_PID_GOES_HERE";
    private String secretKey = "YOUR_SECRETKEY_GOES_HERE";
    private String endpointHost = "asafe.ilivedata.com";
    private String endpointPath = "/api/v1/audio/check/result";
    private String endpointURL = "https://asafe.ilivedata.com/api/v1/audio/check/result";

    private static Gson gson = new Gson();

    public AudioCheckResultDto result(String taskId) throws IOException {
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
        String result = request(queryBody, signature, now);
        return gson.fromJson(result, AudioCheckResultDto.class);
    }

    private String request(String body, String signature, String timeStamp) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
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
            if (hex.length() == 1) hexString.append('0');
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
        AudioCheckResultV1 audioCheckResult = new AudioCheckResultV1();
        AudioCheckResultDto result = audioCheckResult.result(taskId);
        System.out.println(result);
    }

}


class AudioCheckResultDto {
    private Integer errorCode;
    private Integer code;
    private Integer result;
    private String taskId;
    private List<AudioSpam> audioSpams;
}

class AudioSpam {
    private Float startTime;
    private Float endTime;
    private Boolean vpr;
    private List<Tag> tags;
    private Float score;

}

class Tag {
    private Integer tag;
    private Integer level;
    private List<SubTag> subTags;
}

class SubTag {
    private Integer subTag;
}