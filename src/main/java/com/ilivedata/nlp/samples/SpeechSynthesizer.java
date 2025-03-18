package com.ilivedata.nlp.samples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 2025/3/18
 * Pengyang.li
 */
public class SpeechSynthesizer {

    private static final ObjectMapper mapper =
            JsonMapper
                    .builder()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .build();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final String endpointHost = "tts.ilivedata.com";
    private final String endpointPath = "/api/v1/speech/synthesis";
    private final String endpointURL = "https://tts.ilivedata.com/api/v1/speech/synthesis";

    private String pid = "YOUR_PID_GOES_HERE";
    private String secretKey = "YOUR_SECRETKEY_GOES_HERE";

    /**
     * Convert text to voice
     * @param text which to be convent to voice
     * @param voiceName voice name in voice library
     * @return  response detail info
     * @throws IOException Network error
     */
    public String speechSynthesis(String text, String voiceName) throws IOException {
        String now = ZonedDateTime.now(ZoneId.of("UTC")).format(FORMATTER);

        Map<String, Object> voice = new HashMap<>();
        voice.put("name", voiceName);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("text", text);
        parameters.put("voice", voice);

        String queryBody = mapper.writeValueAsString(parameters);
        System.out.println("Speech synthesis request body:");
        System.out.println(queryBody);

        return request(queryBody, now);
    }

    /**
     * Convert text to voice with given audio voice timbre
     * @param text which to be convent to voice
     * @param audioUrl from which to extract voice timbre
     * @return  response detail info
     * @throws IOException Network error
     */
    public String voiceClone(String text, String audioUrl) throws IOException {
        String now = ZonedDateTime.now(ZoneId.of("UTC")).format(FORMATTER);

        Map<String, Object> voice = new HashMap<>();
        voice.put("audio", audioUrl);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("text", text);
        parameters.put("voice", voice);

        String queryBody = mapper.writeValueAsString(parameters);
        System.out.println("Voice clone request body:");
        System.out.println(queryBody);

        return request(queryBody, now);
    }

    private String request(String body, String timeStamp) throws IOException {

        // Prepare stringToSign
        StringBuilder stringToSign = new StringBuilder();
        stringToSign.append("POST").append("\n");
        stringToSign.append(endpointHost).append("\n");
        stringToSign.append(endpointPath).append("\n");
        stringToSign.append(sha256AndHexEncode(body)).append("\n");
        stringToSign.append("X-AppId:").append(pid).append("\n");
        stringToSign.append("X-TimeStamp:").append(timeStamp);

        // Sign the request
        String signature = signAndBase64Encode(stringToSign.toString(), secretKey);

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
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String signAndBase64Encode(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Unable to calculate a request signature: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws IOException {

        SpeechSynthesizer synthesizer = new SpeechSynthesizer();
        String text = "云上曲率的语音合成/克隆服务利用大模型，深度融合文本理解和语音生成，精准解析并诠释各类文本内容，转化为宛如真人般的自然语音。";

        // speech synthesis
        String voiceName = "ZhangWei";
        String synthesisResult = synthesizer.speechSynthesis(text, voiceName);
        Object obj = mapper.readValue(synthesisResult, Object.class);
        System.out.println("Speech synthesis response body:");
        System.out.println(mapper.writeValueAsString(obj));

        System.out.println("========================================================================");
        System.out.println("========================================================================");
        System.out.println("============Speech synthesis complete and ready for cloning ============");
        System.out.println("========================================================================");
        System.out.println("========================================================================");

        // voice clone
        String voiceUrl = "https://vtai-off-sts-ap-1306922583.cos.accelerate.myqcloud.com//timbre_library/common/费尔南多·雷伊/vocal_fc53a7f4-a30a-468f-944c-89a5925e18e8.wav";
        String cloneResult = synthesizer.voiceClone(text, voiceUrl);
        obj = mapper.readValue(cloneResult, Object.class);
        System.out.println("Voice clone response body:");
        System.out.println(mapper.writeValueAsString(obj));

    }

}
