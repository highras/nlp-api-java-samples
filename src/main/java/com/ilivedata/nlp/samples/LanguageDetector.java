package com.ilivedata.nlp.samples;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LanguageDetector {

    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final Charset UTF8 = Charset.forName(DEFAULT_ENCODING);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * Regex which matches any of the sequences that we need to fix up after
     * URLEncoder.encode().
     */
    private static final Pattern ENCODED_CHARACTERS_PATTERN;
    static {
        StringBuilder pattern = new StringBuilder();
        pattern
                .append(Pattern.quote("+"))
                .append("|")
                .append(Pattern.quote("*"))
                .append("|")
                .append(Pattern.quote("%7E"))
                .append("|")
                .append(Pattern.quote("%2F"));

        ENCODED_CHARACTERS_PATTERN = Pattern.compile(pattern.toString());
    }

    private final String pid = "YOUR_PID_GOES_HERE";
    private final String secretKey = "YOUR_SECRETKEY_GOES_HERE";
    private final String endpointHost = "translate.ilivedata.com";
    private final String endpointPath = "/api/v1/detect";
    private final String endpointURL = "https://translate.ilivedata.com/api/v1/detect";

    public String detect(String sentence) throws IOException {
        String now = ZonedDateTime.now(ZoneId.of("UTC")).format(FORMATTER);
        SortedMap<String, String> parameters = new TreeMap<>();
        parameters.put("q", sentence);
        parameters.put("timeStamp", now);
        parameters.put("appId", pid);

        StringBuilder builder = new StringBuilder();
        Iterator<Map.Entry<String, String>> sortedPairs = parameters.entrySet().iterator();
        while (sortedPairs.hasNext()) {
            Map.Entry<String, String> pair = sortedPairs.next();
            builder.append(urlEncode(pair.getKey(), false));
            builder.append("=");
            builder.append(urlEncode(pair.getValue(), false));
            if (sortedPairs.hasNext()) {
                builder.append("&");
            }
        }

        String data = "POST" + "\n" +
                endpointHost + "\n" +
                endpointPath + "\n" +
                builder;

        String signature = signAndBase64Encode(data, secretKey);

        return request(parameters, signature);
    }

    private String request(Map<String, String> parameters, String signature) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        FormBody.Builder builder = new FormBody.Builder();

        for (String key : parameters.keySet()) {
            // 此处builder会自动对value进行url encoding, 如果使用其他Http Client需要注意手动对value编码
            builder.add(key, parameters.get(key));
        }
        Request request = new Request.Builder()
                .url(endpointURL)
                .post(builder.build())
                .addHeader("authorization", signature)
                .addHeader("Accept", "application/json;charset=UTF-8")
                .addHeader("Content-type", "application/x-www-form-urlencoded")
                .build();
        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            return response.body().string();
        }
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

    /**
     * Encode a string for use in the path of a URL; uses URLEncoder.encode,
     * (which encodes a string for use in the query portion of a URL), then
     * applies some postfilters to fix things up per the RFC. Can optionally
     * handle strings which are meant to encode a path (ie include '/'es
     * which should NOT be escaped).
     *
     * @param value the value to encode
     * @param path true if the value is intended to represent a path
     * @return the encoded value
     */
    private String urlEncode(final String value, final boolean path) {
        if (value == null) {
            return "";
        }

        try {
            String encoded = URLEncoder.encode(value, DEFAULT_ENCODING);

            Matcher matcher = ENCODED_CHARACTERS_PATTERN.matcher(encoded);
            StringBuffer buffer = new StringBuffer(encoded.length());

            while (matcher.find()) {
                String replacement = matcher.group(0);

                if ("+".equals(replacement)) {
                    replacement = "%20";
                } else if ("*".equals(replacement)) {
                    replacement = "%2A";
                } else if ("%7E".equals(replacement)) {
                    replacement = "~";
                } else if (path && "%2F".equals(replacement)) {
                    replacement = "/";
                }

                matcher.appendReplacement(buffer, replacement);
            }

            matcher.appendTail(buffer);
            return buffer.toString();

        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) throws IOException {
        LanguageDetector detector = new LanguageDetector();
        String result = detector.detect("Hello world!");
        System.out.println(result);
    }

}
