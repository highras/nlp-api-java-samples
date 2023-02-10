
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MergeCheck {

    private String companyID = "123456";
    private String secretKey = "xxxxxxxxxxxxxxxxxxxxxxx";
    private String endpointURL = "https://media-safe.ilivedata.com/service/check";

    private String textAppID = "80700001";
    private String imageAppID = "81000001";

    public String check(String text, List<String> imageList, String userID) throws IOException {
        long time = System.currentTimeMillis() / 1000;

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("companyID", companyID);
        parameters.put("timestamp", time);
        parameters.put("signature", md5(companyID + ":" + time + ":" + secretKey));
        parameters.put("textAppID", textAppID);
        parameters.put("imageAppID", imageAppID);
        parameters.put("userID", userID);
        parameters.put("text", text);
        parameters.put("imageList", imageList);

        ObjectMapper mapper = new ObjectMapper();

        String queryBody = mapper.writeValueAsString(parameters);
        return request(queryBody);
    }

    public static String md5(String plainText) {
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance("md5").digest(plainText.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No Method");
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);
        int len = md5code.length();
        for (int i = 0; i < 32 - len; i++) {
            md5code = "0" + md5code;
        }
        return md5code.toUpperCase();
    }

    private String request(String body) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(endpointURL)
                .post(RequestBody.create(MediaType.get("application/json; charset=utf-8"), body))
                .addHeader("Content-type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public static void main(String[] args) throws IOException {
        MergeCheck mergeCheck = new MergeCheck();
        List<String> imageList = new ArrayList<>();
        imageList.add("https://test.com/1.jpg");
        imageList.add("https://test.com/2.jpg");

        String result = mergeCheck.check("啊啊啊哦哦fuck哦哈哈哈", imageList, "12345678");
        System.out.println(result);
    }
}

/*
返回json格式：
{
    "errorCode":0,
    "errorMessage":"",
    "result":2,
    "textContent":"啊啊啊哦哦****哦哈哈哈",
    "textSpamTagCode":{
        "160":[
            "160001"
        ]
    },
    "textSpamTagName":{
        "辱骂":[
            "谩骂人身攻击"
        ]
    },
    "textSpamTagNameEn":{
        "insults":[
            "insults and personal attacks"
        ]
    },
    "textWordList":[
        "fuck"
    ],
    "textTaskID":"8cd280de-9bf0-4f91-890b-fd37fbfa0301",
    "textResult":2,
    "imageTaskID":{
        "ap_d020399e99074ecca1e787ae5299a08c_1676013327412":"https:\/\/test.com\/1.jpg",
        "ap_d3658d2858bc48b29ea7bc0ed63a8592_1676013327203":"https:\/\/test.com\/2.jpg"
    },
    "imageResult":{
        "ap_d020399e99074ecca1e787ae5299a08c_1676013327412":0,
        "ap_d3658d2858bc48b29ea7bc0ed63a8592_1676013327203":2
    },
    "imageSpamTagCode":{
        "ap_d020399e99074ecca1e787ae5299a08c_1676013327412":{

        },
        "ap_d3658d2858bc48b29ea7bc0ed63a8592_1676013327203":{
            "140":[
                "140002",
                "140003",
                "140006",
                "140007"
            ]
        }
    },
    "imageSpamTagName":{
        "ap_d020399e99074ecca1e787ae5299a08c_1676013327412":{

        },
        "ap_d3658d2858bc48b29ea7bc0ed63a8592_1676013327203":{
            "性感":[
                "内衣裤",
                "女性胸部-露沟",
                "腿部特写",
                "轻度性感-露锁骨"
            ]
        }
    },
    "imageSpamTagNameEn":{
        "ap_d020399e99074ecca1e787ae5299a08c_1676013327412":{

        },
        "ap_d3658d2858bc48b29ea7bc0ed63a8592_1676013327203":{
            "sexy":[
                "cleavage",
                "legs",
                "off shoulder",
                "underwear"
            ]
        }
    }
}
 */
