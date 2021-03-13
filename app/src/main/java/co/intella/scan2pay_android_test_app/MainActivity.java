package co.intella.scan2pay_android_test_app;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import co.intella.scan2pay.Utility;

public class MainActivity extends AppCompatActivity {

    TextView mTextView;
    ImageView mImageView;

    /*
    // for Production Server
    final static int publicKeyResId = R.raw.pub;
    final static String API_URL = "https://a.intella.co/allpaypass/api/general";   // for production server
    final static String MchId = "scan2payxxx"; // your MchId (login account)
    final static String TradeKey = "ffd9a5dbef83d54c4d59e329991dfb7a060656394f40a633fb7a03c0ed4babcd";     // SHA256 encoded
    final static String RefundKey = "9af15b336e6a9619928537df30b2e6a2376569fcf9d7e773eccede656065abcd";    // SHA256 encoded
    final static String DeviceId = "01300000"; // for EasyCard API
    */

    // for Stage Server
    final static int publicKeyResId = R.raw.stage_pub;
    final static String API_URL = "https://s.intella.co/allpaypass/api/general";   // for test server
    final static String MchId = "S2PT90001";   // your MchId (login account)
    final static String TradeKey = "c4fe6b6dbe94790f232013154cb80fc5dd3ec9106d433492f20f038b1ce25656";     // SHA256 encoded
    final static String RefundKey = "13b7994fae9387c2e1b598524ba1204ae404d02fa67016ed86c74183ab1aabcd";    // SHA256 encoded
    final static String DeviceId = "01300123"; // for EasyCard API


    private TestOLPay mTestOLPayTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = MainActivity.this.findViewById(R.id.textview);
        Button btnTestOLPay = MainActivity.this.findViewById(R.id.btnTestOLPay);
        btnTestOLPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mTextView.setText("Waiting for response...");
                mImageView.setImageBitmap(null);
                if (null != mTestOLPayTask) {
                    mTestOLPayTask.cancel(true);
                }

                // OLPay
                mTestOLPayTask = new TestOLPay(getPubKeyStream(), new TestOLPay.IOLPayCallback() {
                    @Override
                    public void onQRCodeStringGenerated(final String qrString) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                                try {
                                    BitMatrix bitMatrix = multiFormatWriter.encode(qrString, BarcodeFormat.QR_CODE,200,200);
                                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                                    Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                                    mImageView.setImageBitmap(bitmap);
                                } catch (WriterException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    @Override
                    public void onMessage(String s) {
                        if (null != mTextView) {
                            mTextView.setText(null == s ? "" : s);
                        }
                    }
                });
                mTestOLPayTask.execute();

            }
        });

        Button btnTestEZCSignOn = findViewById(R.id.btnTestEZCSignOn);
        btnTestEZCSignOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mTextView.setText("Waiting for response...");
                mImageView.setImageBitmap(null);
                if (null != mTestOLPayTask) {
                    mTestOLPayTask.cancel(true);
                }

                // EZC SignOn
                new TestEZCSignOn(getPubKeyStream(), new IApiCompletionCallback() {
                    @Override
                    public void onSuccess(String response) {
                        if (null != mTextView) {
                            mTextView.setText(response);
                        }
                    }

                    @Override
                    public void onFailure() {
                        if (null != mTextView) {
                            mTextView.setText("EZC SignOn Error!");
                        }
                    }
                }).execute();
            }
        });

        Button btnTestEZCPayment = findViewById(R.id.btnTestEZCPayment);
        btnTestEZCPayment.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mTextView.setText("Waiting for response...");
                mImageView.setImageBitmap(null);
                if (null != mTestOLPayTask) {
                    mTestOLPayTask.cancel(true);
                }

                // EZC Payment
                new TestEZCPayment(getPubKeyStream(), new IApiCompletionCallback() {
                    @Override
                    public void onSuccess(String response) {
                        mTextView.setText(response);
                    }

                    @Override
                    public void onFailure() {
                        mTextView.setText("EZC Payment Error!");
                    }
                }).execute();
            }
        });

        mImageView = findViewById(R.id.imageview);
    }

    private InputStream getPubKeyStream() {
        return MainActivity.this.getResources().openRawResource(publicKeyResId);
    }

    /**
     * Test OLPay, using polling to query the trade result.
     */
    private static class TestOLPay extends AsyncTask<Void, String, String> {

        // the expire time after the QR-code has been generated. (After this period, the QR-code can not be processed by Scan2Pay)
        private final int QRCODE_EXPIRE_TIME = 90;

        // timeout (seconds) before the user finish the payment process on his phone.
        private final int TIMEOUT_OLPAY_WAITING = 120;

        private InputStream mRsaPubKeyStream;
        private IOLPayCallback mCallback;

        public TestOLPay(InputStream rsaInputStream, IOLPayCallback callback) {
            mRsaPubKeyStream = rsaInputStream;
            mCallback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {

            Map<String, String> requestMap = new HashMap<>();

            Calendar calendar = Calendar.getInstance();

            String dateString = DateFormat.format("yyyyMMddHHmmss", calendar.getTime()).toString();

            requestMap.put("Method", "00000");
            requestMap.put("ServiceType", "OLPay");

            // fill your test account and password.
            // the password must be SHA256 encoded. Use online tool such as http://www.xorbin.com/tools/sha256-hash-calculator
            requestMap.put("MchId", MchId);
            requestMap.put("TradeKey", TradeKey);

            requestMap.put("DeviceInfo", "skb0001");
            requestMap.put("CreateTime", dateString);

            // you need to generate a unique order id according to your own order coding rules
            // here we just use date/time with a fixed prefix code.
            String storeOrderNo = String.format("ITLTEST%s", DateFormat.format("yyMMddhhmmss", new Date()).toString());
            requestMap.put("StoreOrderNo", storeOrderNo);

            requestMap.put("Body", "some-stuff");
            requestMap.put("TotalFee", "1");

            calendar.add(Calendar.SECOND, QRCODE_EXPIRE_TIME);
            String expireDateString = DateFormat.format("yyyyMMddHHmmss", calendar.getTime()).toString();
            requestMap.put("TimeExpire", expireDateString);

            // use R.raw.pub for production server public key
            try {

                String response = Utility.doRequest(API_URL, mRsaPubKeyStream, requestMap);

                // parse the OLPay response
                Gson gson = new GsonBuilder().create();
                OLPayResponse olpayRsp = gson.fromJson(response, OLPayResponse.class);

                if (!olpayRsp.Header.StatusCode.equals("0000"))
                {
                    return response;
                }

                // extract the urlToken and display it
                ShowQRCode(olpayRsp.Data.urlToken);

                Thread.sleep(5000);

                // check order status
                int timeoutCounter = 0;
                int resultCode;
                while (-1 == (resultCode = GetOrderStatus(storeOrderNo)))
                {
                    if (isCancelled()) {
                        break;
                    }

                    Thread.sleep(1000);
                    if (++timeoutCounter > TIMEOUT_OLPAY_WAITING)
                    {
                        break;
                    }

                    String remindingSecs = String.format(Locale.getDefault(), "Countdown: %d secs", TIMEOUT_OLPAY_WAITING - timeoutCounter);
                    publishProgress(remindingSecs);
                }

                return resultCode == 0 ? "Trade Succeeded" : resultCode == 1 ? "Trade Failed" : "Trade TimeOut";

            } catch (Exception e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (null != mCallback) {
                mCallback.onMessage(s);
            }
        }

        @Override
        protected void onPreExecute() {
            if (null != mCallback){
                mCallback.onMessage("Please scan the QR code above.");
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (null != mCallback) {
                mCallback.onMessage(values[0]);
            }
        }

        private int GetOrderStatus(String storeOrderNo) {

            Map<String, String> requestMap = new HashMap<>();
            String dateString = DateFormat.format("yyyyMMddhhmmss", new Date()).toString();

            requestMap.put("Method", "00000");
            requestMap.put("ServiceType", "SingleOrderQuery");

            // fill your test account and password.
            // the password must be SHA256 encoded. Use online tool such as http://www.xorbin.com/tools/sha256-hash-calculator
            requestMap.put("MchId", MchId);
            requestMap.put("TradeKey", TradeKey);

            requestMap.put("DeviceInfo", "skb0001");
            requestMap.put("CreateTime", dateString);

            requestMap.put("StoreOrderNo", storeOrderNo);

            // use R.raw.pub for production server public key
            try {

                String response = Utility.doRequest(API_URL, mRsaPubKeyStream, requestMap);

                // parse the OLPay response
                Gson gson = new GsonBuilder().create();
                SingleOrderQueryResponse queryRsp = gson.fromJson(response, SingleOrderQueryResponse.class);
                int retCode = Integer.parseInt(queryRsp.Data.OrderStatus);
                return (retCode == 1 ? 0 : retCode == 2 ? 1 : retCode == 3 ? 0 : -1);

            } catch (Exception e) {
                return -1;
            }
        }

        private void ShowQRCode(final String url)
        {
            if (null != mCallback) {
                mCallback.onQRCodeStringGenerated(url);
            }
        }


        static class Scan2PayResponseHeader {
            String StatusCode;
            String StatusDesc;
            String ServiceType;
            String MchId;
            String ResponseTime;
        }

        static class OLPayData {
            String urlToken;
        }

        static class OLPayResponse {
            Scan2PayResponseHeader Header;
            OLPayData Data;
        }

        static class SingleOrderQueryData {
            String SysOrderNo;
            String StoreOrderNo;
            String FeeType;
            String DeviceInfo;
            String Body;
            String OrderStatus;
            String Detail;
            String StoreInfo;
        }

        static class SingleOrderQueryResponse {
            Scan2PayResponseHeader Header;
            SingleOrderQueryData Data;
        }

        interface IOLPayCallback {
            void onQRCodeStringGenerated(String qrString);
            void onMessage(String s);
        }
    }

    private class TestEZCSignOn extends AsyncTask<Void, Void, String> {

        private final InputStream mRsaPubKeyStream;
        private final IApiCompletionCallback mCallback;

        public TestEZCSignOn(InputStream rsaPubKeyStream, IApiCompletionCallback callback) {
            mRsaPubKeyStream = rsaPubKeyStream;
            mCallback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            Map<String, String> requestMap = new HashMap<>();
            String dateString = DateFormat.format("yyyyMMddhhmmss", new Date()).toString();

            requestMap.put("Method", "31800");
            requestMap.put("ServiceType", "SignOn");

            // fill your test account and password.
            // the password must be SHA256 encoded. Use online tool such as http://www.xorbin.com/tools/sha256-hash-calculator
            requestMap.put("MchId", MchId);
            requestMap.put("TradeKey", TradeKey);

            requestMap.put("CreateTime", dateString);

            requestMap.put("DeviceId", DeviceId);
            requestMap.put("Retry", "0");

            try {
                return Utility.doRequest(API_URL, mRsaPubKeyStream, requestMap);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (null != mCallback) {
                if (null != s) {
                    mCallback.onSuccess(s);
                } else {
                    mCallback.onFailure();
                }
            }
        }
    }

    private static class TestEZCPayment extends AsyncTask<Void, Void, String> {

        private final InputStream mRsaPubKeyStream;
        private final IApiCompletionCallback mCallback;

        public TestEZCPayment(InputStream rsaPubKeyStream, IApiCompletionCallback callback) {
            mRsaPubKeyStream = rsaPubKeyStream;
            mCallback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            Map<String, String> requestMap = new HashMap<>();
            String dateString = DateFormat.format("yyyyMMddhhmmss", new Date()).toString();

            requestMap.put("Method", "31800");
            requestMap.put("ServiceType", "Payment");

            // fill your test account and password.
            // the password must be SHA256 encoded. Use online tool such as http://www.xorbin.com/tools/sha256-hash-calculator
            requestMap.put("MchId", MchId);
            requestMap.put("TradeKey", TradeKey);

            requestMap.put("CreateTime", dateString);

            requestMap.put("DeviceId", DeviceId);
            requestMap.put("Retry", "0");

            requestMap.put("StoreOrderNo", "PO" + dateString);
            requestMap.put("Amount", "1");
            requestMap.put("Body", "Milk");

            try {
                return Utility.doRequest(API_URL, mRsaPubKeyStream, requestMap);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (null != mCallback) {
                if (null != s) {
                    mCallback.onSuccess(s);
                } else {
                    mCallback.onFailure();
                }
            }
        }


    }

    public interface IApiCompletionCallback {
        void onSuccess(String response);
        void onFailure();
    }

}
