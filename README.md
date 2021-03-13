# scan2pay-sdk-for-android

### Library Release
release/scan2pay-lib.aar

### Import Lib into Android Studio
File -> New -> New Module -> Import .JAR/.AAR Package

### Running Sample App
You need to fill the Scan2Pay associated parameters. You need to contact intella for applying an account before you can run the demo app.



    // for Production Server
    final static int publicKeyResId = R.raw.pub;
    final static String API_URL = "https://a.intella.co/allpaypass/api/general";   // for production server
    final static String MchId = "scan2payxxx"; // your MchId (login account)
    final static String TradeKey = "ffd9a5dbef83d54c4d59e329991dfb7a060656394f40a633fb7a03c0ed4babcd";     // SHA256 encoded
    final static String RefundKey = "9af15b336e6a9619928537df30b2e6a2376569fcf9d7e773eccede656065abcd";    // SHA256 encoded
    final static String DeviceId = "01300000"; // for EasyCard API
  

    // for Stage Server
    final static int publicKeyResId = R.raw.stage_pub;
    final static String API_URL = "https://s.intella.co/allpaypass/api/general";   // for test server
    final static String MchId = "S2PT90001";   // your MchId (login account)
    final static String TradeKey = "c4fe6b6dbe94790f232013154cb80fc5dd3ec9106d433492f20f038b1ce2abcd";     // SHA256 encoded
    final static String RefundKey = "13b7994fae9387c2e1b598524ba1204ae404d02fa67016ed86c74183ab1aabcd";    // SHA256 encoded
    final static String DeviceId = "01300123"; // for EasyCard API


### About Scan2Pay API
Check [https://intella.gitbook.io/scan2pay/](https://intella.gitbook.io/scan2pay/)

### About intella
[https://intella.co/](https://intella.co/)

