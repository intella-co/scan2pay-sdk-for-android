package co.intella.scan2pay;

import java.io.InputStream;
import java.security.PublicKey;
import java.util.Map;

import javax.crypto.SecretKey;

import co.intella.crypto.AesCryptoUtil;
import co.intella.crypto.KeyReader;
import co.intella.net.Constant;
import co.intella.net.HttpRequestUtil;

public class Utility {

    public static String doRequest(String url, InputStream rsaPubKeyStream, Map<String, String> requestMap) {

        try {
            PublicKey rsaPubKey = KeyReader.loadPublicKeyFromDER(rsaPubKeyStream);
            SecretKey aesKey = AesCryptoUtil.generateSecreteKey();
            return doPost(url, rsaPubKey, aesKey, requestMap);

        } catch (Exception e) {

            e.printStackTrace();
            return String.format("{\"Header\": {\"StatusCode\": \"9000\",\"StatusDesc\": \"%s\"}}", e.getMessage());
        }
    }

    private static String doPost(String url, PublicKey rsaPubKey, SecretKey aesKey, Map<String, String> requestMap) {

        try {
            String _response = HttpRequestUtil.httpsPost(url, requestMap, rsaPubKey, aesKey);
            return AesCryptoUtil.decryptResponse(aesKey, Constant.IV, _response);

        } catch (Exception e) {
            e.printStackTrace();
            return String.format("{\"Header\": {\"StatusCode\": \"9000\",\"StatusDesc\": \"%s\"}}", e.getMessage());
        }
    }
}
