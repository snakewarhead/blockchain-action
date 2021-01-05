package com.cq.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class BTCUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(BTCUtils.class);

    //用户名
    private String ACCESS_KEY = null;
    //密码
    private String SECRET_KEY = null;
    //钱包IP地址
    private String IP = null;
    //端口
    private String PORT = null;
    //比特币钱包密码
    private String PASSWORD = null;

    private String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    public BTCUtils(BTCMessage btcMessage) {
        this.ACCESS_KEY = btcMessage.getACCESS_KEY();
        this.SECRET_KEY = btcMessage.getSECRET_KEY();
        this.IP = btcMessage.getIP();
        this.PORT = btcMessage.getPORT();
        this.PASSWORD = btcMessage.getPASSWORD();
    }

    public String getSignature(String data, String key) throws Exception {
        // get an hmac_sha1 key from the raw key bytes
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(),
                HMAC_SHA1_ALGORITHM);

        // get an hmac_sha1 Mac instance and initialize with the signing key
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);

        // compute the hmac on input data bytes
        byte[] rawHmac = mac.doFinal(data.getBytes());
        return bytArrayToHex(rawHmac);
    }

    private String bytArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for (byte b : a) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    //The easiest way to tell Java to use HTTP Basic authentication is to set a default Authenticator:
    private void authenticator() {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(ACCESS_KEY, SECRET_KEY.toCharArray());
            }
        });
    }

    /***
     * 取得钱包相关信息
     * {"result":{"version":80300,"protocolversion":70001,"walletversion":60000,
     * "balance":0.00009500,"blocks":284795,"timeoffset":-1,"connections":6,
     * "proxy":"","difficulty":2621404453.06461525,"testnet":false,
     * "keypoololdest":1388131357,"keypoolsize":102,"paytxfee":0.00000000,
     * "errors":""},"error":null,"id":1}
     * 若获取失败，result为空，error信息为错误信息的编码
     * */
    public JSONObject getInfo() throws Exception {
        String s = main("getinfo", "[]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    /***
     * 取得钱包余额
     * {"result":9.5E-5,"error":null,"id":1}
     * {"result":0,"error":null,"id":1}
     * 若获取失败，result为空，error信息为错误信息的编码
     * */
    public JSONObject getbalance() throws Exception {
        String s = main("getbalance", "[]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public String getaccount(String address) throws Exception {
        String s = main("getaccount", "[\"" + address + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        String result = json.getString("result");
        return result;
//		return StringUtils.isBlank(result)|| result=="null"?null:result;
    }

    public JSONObject getaddressesbyaccount(String account) throws Exception {
        String s = main("getaddressesbyaccount", "[\"" + account + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public JSONObject isValidateaddress(String address) throws Exception {
        String s = main("validateaddress", "[\"" + address + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public BigDecimal getbalanceValue() throws Exception {
        BigDecimal result = BigDecimal.ZERO;
        JSONObject s = getbalance();
        if (s.containsKey("result")) {
            result = new BigDecimal(s.get("result").toString());
        }
        return result;
    }

//    /**
//     * @deprecated
//     */
//	public BigDecimal getbalanceValue(String account) throws Exception {
//		BigDecimal result = BigDecimal.ZERO;
//		String s = main("getbalance", "[\""+account+"\"]");
//		JSONObject json = JSONObject.fromObject(s);
//		if(json.containsKey("result")){
//			result =new BigDecimal(json.get("result").toString());
//		}
//		return result;
//	}

    //判断地址是否有效
    public boolean validateaddress(String address) {
        if (StringUtils.isEmpty(address)) {
            return false;
        }

        try {
            JSONObject s = isValidateaddress(address);
            if (s.containsKey("result")) {
                String xx = JSONObject.fromObject(s.get("result")).getString("isvalid");
                if (xx.equals("true")) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return false;
    }

    /***
     * 根据用户ID，生成交易地址
     * @throws Exception
     * {"result":"1MySAsi6bzPLY3HbCcrDd7qgiG7CYnQn3k","error":null,"id":1}
     * 若获取失败，result为空，error信息为错误信息的编码
     * */
    public JSONObject getNewaddress(int userId) throws Exception {
        String s = main("getnewaddress", "[\"" + userId + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public String getNewaddressValue(int userId) throws Exception {
        String result = null;
        if (PASSWORD != null && PASSWORD.trim().length() > 0) {
            walletpassphrase(30);
        }
        JSONObject s = getNewaddress(userId);
        if (PASSWORD != null && PASSWORD.trim().length() > 0) {
            walletlock();
        }
        if (s.containsKey("result")) {
            result = s.get("result").toString();
            if (result.equals("null")) {
                result = null;
            }
        }
        return result;
    }

    public JSONObject getNewaddressForAdmin(String userId) throws Exception {
        String s = main("getnewaddress", "[\"" + userId + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public String getNewaddressValueForAdmin(String userId) throws Exception {
        String result = null;
        if (!walletpassphrase(30)) {
            throw new IllegalArgumentException("password is incorrect");
        }

        JSONObject s = getNewaddressForAdmin(userId);

        walletlock();
        if (s.containsKey("result")) {
            result = s.get("result").toString();
            if (result.equals("null")) {
                result = null;
            }
            // bch address - bitcoincash:qznepa0jf9cl77wysn5qduc7yxykfp5us5895t4v8x
            if (result.contains(":")) {
                result = result.split(":")[1];
            }
        }

        return result;
    }

    /**
     * 根据用户ID，获取交易记录
     * {"result":[{"account":"test","address":"i6W5Ng4X49gJDAVnPafFm3rQwEAfU28SUo",
     * "category":"receive","amount":10,"confirmations":166,
     * "blockhash":"9b227a05cce30b33d991199af734cdd72171c6db608ec36aa71f48952ad1a639",
     * "blockindex":1,"txid":"4ed4de64672cb86cc3458a12a8a1d1c4a79478627a536ec62033367ba180ffc9",
     * "time":1391943409,"comment":"","from":"","message":"","to":""}],"error":null,"id":1}
     * 没有交易记录的结果：{"result":[],"error":null,"id":1}
     * 若获取失败，result为空，error信息为错误信息的编码
     */
    public JSONObject listtransactions(int userId) throws Exception {
        String s = main("listtransactions", "[\"" + userId + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    /**
     * 查所有
     **/
    public JSONObject listtransactions(int count, int from) throws Exception {
        String s = main("listtransactions", "[\"*\"," + count + "," + from + "]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    /**
     * 取得所有的收到的交易记录
     * <p/>
     * 注意，不是所有的交易记录，只有转入到我们钱包的地址的才有交易记录。
     * 比特币钱包已经帮我们过滤了那些我们不关心的交易记录
     *
     * @param count - 一次获取的数量
     * @param from  - 从哪个位置开始，0就是从最后一个条记录开始向前面查询
     * @return
     * @throws Exception
     */
    public List<BTCInfo> listtransactionsValue(int count, int from) throws Exception {
        JSONObject json = listtransactions(count, from);
        List<BTCInfo> all = new ArrayList();
        if (json.containsKey("result") && !json.get("result").toString().equals("null")) {
            List allResult = (List) json.get("result");
            Iterator it = allResult.iterator();
            while (it.hasNext()) {
                Map map = (Map) it.next();
                if (map.get("category").toString().equals("receive")) {
                    BTCInfo info = new BTCInfo();
                    info.setAccount(map.get("label") + "");
                    info.setToAddress(map.get("address") + "");
                    info.setAmount(new BigDecimal(map.get("amount").toString()));    // TODO: 暂时保留6位小数
                    info.setCategory(map.get("category") + "");
                    info.setComment(map.get("comment") + "");
                    try {
                        if (map.get("confirmations") != null
                                && map.get("confirmations").toString().trim().length() > 0) {
                            info.setConfirmations(Integer.parseInt(map.get("confirmations").toString()));
                        }
                    } catch (Exception e) {
                        info.setConfirmations(0);
                    }
                    try {
                        long time = Long.parseLong(map.get("time").toString());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date dt = new Date(time * 1000);
                        String sDateTime = sdf.format(dt);  //得到精确到秒的表示：08/31/2006 21:08:00
                        info.setTime(Timestamp.valueOf(sDateTime));
                    } catch (Exception e) {
                        info.setTime(new Date());
                    }
                    info.setTxid(map.get("txid") + "");
                    all.add(info);
                }
            }
        }
        Collections.reverse(all);
        return all;
    }

    /***
     * 以地址GROUP BY
     * 查出所有收到的币以地址
     * [
     {
     "address" : "YBjKaMR7XC3yLck2aaNmkJiy2cbr37jezD",
     "account" : "fdafdsa",
     "amount" : 0.40000000,
     "confirmations" : 210
     },
     ]
     * */
    public JSONObject listreceivedbyaddress() throws Exception {
        String s = main("listreceivedbyaddress", "[]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    /***
     *
     * 根据交易ID取得交易明细
     * */
    private JSONObject gettransaction(String xid) throws Exception {
        String s = main("gettransaction", "[\"" + xid + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    /**
     * 获取txid对应的交易详细信息，并解析确认数
     * <p>
     * category receive or send
     */
    public BTCInfo gettransactionValue(String xid, String address, String category) throws Exception {
        JSONObject json = gettransaction(xid);
        BTCInfo btcInfo = null;
        if (json.containsKey("result") && !json.get("result").toString().equals("null")) {
            btcInfo = new BTCInfo();

            Map map = (Map) json.get("result");
            List xList = (List) map.get("details");
            Iterator it = xList.iterator();
            while (it.hasNext()) {
                Map xMap = (Map) it.next();
                if (xMap.get("category").toString().equals(category)) {
                    String address2 = xMap.get("address") + "";

                    if (address.equals(address2)) {
                        btcInfo.setAccount(xMap.get("account") + "");
                        btcInfo.setToAddress(xMap.get("address") + "");
                        btcInfo.setAmount(new BigDecimal(xMap.get("amount").toString()));
                        break;
                    }

                }
            }
            btcInfo.setCategory(category);
            btcInfo.setComment(map.get("comment") + "");
            try {
                if (map.get("confirmations") != null
                        && map.get("confirmations").toString().trim().length() > 0) {
                    btcInfo.setConfirmations(Integer.parseInt(map.get("confirmations").toString()));
                }
            } catch (Exception e) {
                btcInfo.setConfirmations(0);
            }
            try {
                long time = Long.parseLong(map.get("time").toString());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date dt = new Date(time * 1000);
                String sDateTime = sdf.format(dt);  //得到精确到秒的表示：08/31/2006 21:08:00
                btcInfo.setTime(Timestamp.valueOf(sDateTime));
            } catch (Exception e) {
                btcInfo.setTime(new Date());
            }
            btcInfo.setTxid(map.get("txid") + "");
        }
        return btcInfo;
    }

    /***
     * 查出所有的币以标签
     *
     [
     {
     "account" : "dfasf",
     "amount" : 0.50100000,
     "confirmations" : 575
     },
     ]
     * */
    public JSONObject listreceivedbyaccount() throws Exception {
        String s = main("listreceivedbyaddress", "[]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }


    /***
     * 根据收款地址，提现比特币
     * Returns the transaction ID <txid> if successful.
     * {"result":"2278857ca4ab04b41b36fe0f5ec8415d8e72c66a4688c60d8d2eefe994d3042c","error":null,"id":1}
     * 若获取失败，result为空，error信息为错误信息的编码
     * {"result":null,"error":500,"id":1}
     * */
    public JSONObject sendtoaddress(String address, BigDecimal amount, String comment) throws Exception {
        if (PASSWORD != null && PASSWORD.trim().length() > 0) {
            walletpassphrase(30);
        }

        String condition = "[\"" + address + "\"," + amount + "," + "\"" + comment + "\"]";

        LOGGER.info("sendtoaddress 1 - " + condition);
        String s = main("sendtoaddress", condition);
        LOGGER.info("sendtoaddress 2 - " + s);

        if (PASSWORD != null && PASSWORD.trim().length() > 0) {
            walletlock();
        }
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    /**
     * 转账btc
     *
     * @param address
     * @param amount
     * @param ffees
     * @param comment
     * @return
     * @throws Exception
     */
    public String sendtoaddressValue(String address, BigDecimal amount, BigDecimal ffees, String comment) throws Exception {
        String result = "";
        settxfee(ffees);    // 实际这个方法没有起作用，btc钱包自己定义最佳手续费
        JSONObject s = sendtoaddress(address, amount, comment);
        if (s.containsKey("result")) {
            if (!s.get("result").toString().equals("null")) {
                result = s.get("result").toString();
            }
        }
        return result;
    }

    //设置手续费
    public void settxfee(BigDecimal ffee) throws Exception {
        JSONArray js = new JSONArray();
        js.add(ffee);
        main("settxfee", js.toString());
    }

    //解锁
    //解锁
    public boolean walletpassphrase(int times) throws Exception {
        boolean flag = false;
        try {
            String s = main("walletpassphrase", "[\"" + PASSWORD + "\"," + times + "]");
            JSONObject json = JSONObject.fromObject(s);
            if (json.containsKey("error")) {
                String error = json.get("error").toString();
                if (error.equals("null") || error == null || error == "" || error.trim().length() == 0) {
                    flag = true;
                }
            }
        } catch (Exception e) {
        }
        return flag;
    }

    //锁
    public void walletlock() throws Exception {
        main("walletlock", "[]");
    }

    private String main(String method, String condition) throws Exception {
        String result = "";
        String tonce = "" + (System.currentTimeMillis() * 1000);
        authenticator();

        String params = "tonce=" + tonce.toString() + "&accesskey="
                + ACCESS_KEY
                + "&requestmethod=post&id=1&method=" + method + "&params=" + condition;

        String hash = getSignature(params, SECRET_KEY);

        String url = "http://" + ACCESS_KEY + ":" + SECRET_KEY + "@" + IP + ":" + PORT;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        String userpass = ACCESS_KEY + ":" + hash;
        String basicAuth = "Basic "
                + DatatypeConverter.printBase64Binary(userpass.getBytes());

        con.setConnectTimeout(5000);
        con.setReadTimeout(60000);

        // add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("Json-Rpc-Tonce", tonce.toString());
        con.setRequestProperty("Authorization", basicAuth);

        String postdata = "{\"method\":\"" + method + "\", \"params\":" + condition + ", \"id\": 1}";

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(postdata);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        if (responseCode != 200) {
            return "{\"result\":null,\"error\":" + responseCode + ",\"id\":1,\"code\":-1,\"msg\":\"\"}";
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        inputLine = in.readLine();
        response.append(inputLine);
        in.close();
        result = response.toString();
        return result;
    }

    /**
     * ---------------------------------OmniCore(USDT)----------------------------------------
     */

    private String convertStreamToString(InputStream is) {
        Scanner s = (new Scanner(is, "UTF-8")).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public List<BTCInfo> omniListTransactionsValue(int count, int from, String propertyid) throws Exception {
        String s = main("omni_listtransactions", "[\"*\"," + count + "," + from + "]");
        JSONObject jo = JSONObject.fromObject(s);
        JSONArray result = jo.getJSONArray("result");

        List<BTCInfo> ls = new ArrayList<>(result.size());
        for (int i = 0; i < result.size(); ++i) {
            JSONObject info = result.getJSONObject(i);
            if (!info.containsKey("valid") || !info.getBoolean("valid")) {
                // 不是有效的交易
                LOGGER.warn("omniListTransactionsValue - invalid - {}", s);
                continue;
            }
            if (info.getInt("type_int") != 0) {
                // 不是转账操作
                LOGGER.warn("omniListTransactionsValue - type_int is not 0 - {}", s);
                continue;
            }
            if (!propertyid.equals(info.getString("propertyid"))) {
                // 不是指定的资产
                LOGGER.warn("omniListTransactionsValue - propertyid is not the same - {}", s);
                continue;
            }

            BTCInfo b = new BTCInfo();

            b.setTxid(info.getString("txid"));

            b.setFromAddress(info.getString("sendingaddress"));
            b.setToAddress(info.getString("referenceaddress"));

            b.setAmount(new BigDecimal(info.getString("amount")));

            b.setBlockNumber(info.getLong("block"));
            b.setConfirmations(info.getInt("confirmations"));
            b.setTime(new Date(info.getLong("blocktime") * 1000));

            ls.add(b);
        }

        return ls;
    }

    /**
     * 正确格式 {"result":{"txid":"34a91670eeebe2f0b010c4c77b5722e9e349fdcca356e9e34898fc3d5379d829","fee":"0.00005140","sendingaddress":"n3tx5VVp4MtvhveLYQWiX7q8VKtydjQsKL","referenceaddress":"n1Xjvy3BzDYwBvk3X7ptYEcgqVTuj6CcDW","ismine":true,"version":0,"type_int":0,"type":"Simple Send","propertyid":3,"divisible":true,"amount":"20.00000000","valid":true,"blockhash":"03e93736e3a56cdca67e226656b2cb93b8d13573a447ea4feb282854f80e87fc","blocktime":1526872895,"positioninblock":1,"block":240,"confirmations":4},"error":null,"id":1}
     * 失败格式 {"result":null,"error":500,"id":1}
     */
    public BTCInfo omniGettransaction(String txid, String propertyid) throws Exception {
        String s = main("omni_gettransaction", "[\"" + txid + "\"]");
        JSONObject jo = JSONObject.fromObject(s);
        JSONObject info = jo.getJSONObject("result");

        if (!info.containsKey("valid") || !info.getBoolean("valid")) {
            // 不是有效的交易
            LOGGER.warn("omniGettransaction - invalid - {}", s);
            return null;
        }
        if (info.getInt("type_int") != 0) {
            // 不是转账操作
            LOGGER.warn("omniGettransaction - type_int is not 0 - {}", s);
            return null;
        }
        if (!propertyid.equals(info.getString("propertyid"))) {
            // 不是指定的资产
            LOGGER.warn("omniGettransaction - propertyid is not the same - {}", s);
            return null;
        }

        BTCInfo b = new BTCInfo();

        b.setTxid(info.getString("txid"));

        b.setFromAddress(info.getString("sendingaddress"));
        b.setToAddress(info.getString("referenceaddress"));

        b.setAmount(new BigDecimal(info.getString("amount")));

        b.setBlockNumber(info.getLong("block"));
        b.setConfirmations(info.getInt("confirmations"));
        b.setTime(new Date(info.getLong("blocktime") * 1000));

        return b;
    }

    //查询余额
    public BigDecimal omniGetBalance(String address, String currency, Integer type) throws Exception {
        BigDecimal val;
        String s = main("omni_getbalance", "[\"" + address + "\"," + currency + "]");
        JSONObject json = JSONObject.fromObject(s);
        JSONObject data = json.getJSONObject("result");
        if (type == null || type == 1) {
            val = new BigDecimal(data.getString("balance"));
        } else {
            val = new BigDecimal(data.getString("reserved"));
        }
        return val;
    }

    public BigDecimal omniGetWalletBalance(String currencyid) throws Exception {
        String res = main("omni_getwalletbalances", "[]");
        JSONArray arr = JSONObject.fromObject(res).getJSONArray("result");

        for (int i = 0; i < arr.size(); ++i) {
            JSONObject o = arr.getJSONObject(i);
            if (currencyid.equals(o.getString("propertyid"))) {
                return new BigDecimal(o.getString("balance"));
            }
        }

        return new BigDecimal("-1");
    }

    /**
     * json: {"result":[{"address":"mfgegyhVXzYsqZjSRuW7G75w8CtEvvT7cG","balances":[{"propertyid":2147483651,"name":"TetherUS","balance":"42.00000000","reserved":"0.00000000","frozen":"0.00000000"}]},{"address":"mqVDcHtcdKC4wHcpPdP2DZJugofNYKxvSb","balances":[{"propertyid":2147483651,"name":"TetherUS","balance":"107.00000000","reserved":"0.00000000","frozen":"0.00000000"}]},{"address":"mtm36dWFZXKLUGEnqfyXCLbC5c9yFmoKtZ","balances":[{"propertyid":2147483651,"name":"TetherUS","balance":"2519999848.00000000","reserved":"0.00000000","frozen":"0.00000000"}]},{"address":"n1hxRxhKHGc3Z7MB9bJ9nfdsRKHkPzzXrN","balances":[{"propertyid":2147483651,"name":"TetherUS","balance":"3.00000000","reserved":"0.00000000","frozen":"0.00000000"}]}],"error":null,"id":1}
     * map: [{"address": xxx, "balance": xxx}, ...]
     *
     * @param currencyid
     * @return
     * @throws Exception
     */
    public List<Map<String, Object>> omniGetWalletAddressBalances(String currencyid) throws Exception {
        String res = main("omni_getwalletaddressbalances", "[]");
        JSONArray arr = JSONObject.fromObject(res).getJSONArray("result");

        List<Map<String, Object>> addresses = new ArrayList<>();
        for (int i = 0; i < arr.size(); ++i) {
            JSONObject o = arr.getJSONObject(i);

            String address = o.getString("address");

            BigDecimal balance = BigDecimal.ZERO;
            JSONArray balancesArr = o.getJSONArray("balances");
            for (int j = 0; j < balancesArr.size(); ++j) {
                JSONObject b = balancesArr.getJSONObject(j);
                if (currencyid.equals(b.getString("propertyid"))) {
                    balance = new BigDecimal(b.getString("balance"));
                    break;
                }
            }

            if (balance.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Map<String, Object> m = new HashMap<>();
            m.put("address", address);
            m.put("balance", balance);
            addresses.add(m);
        }

        return addresses;
    }

    /**
     * success: {"result":"4660aff244fb6eb4f51c8dbd918d4ad24f4fd49766197b2d803e568b9d3245f8","error":null,"id":1}
     *
     * @param fromAddress
     * @param toAddress
     * @param currency
     * @param amount
     * @return
     * @throws Exception
     */
    public String omniSend(String fromAddress, String toAddress, String currency, BigDecimal amount) throws Exception {
        if (!walletpassphrase(30)) {
            throw new IllegalArgumentException("password is incorrect");
        }

        String condition = "[\"" + fromAddress + "\",\"" + toAddress + "\"," + currency + ",\"" + amount + "\"]";

        LOGGER.info("omniSend 1 - " + condition);
        String s = main("omni_send", condition);
        JSONObject json = JSONObject.fromObject(s);
        LOGGER.info("omniSend 2 - " + json);

        walletlock();

        String result = json.getString("result");
        if ("null".equals(result)) {
            return null;
        }
        return result;
    }

    /**
     * 使用feeAddress提供fee，但是会将toAddress的btc也一起转走，所以要注意使用
     *
     * @param fromAddress
     * @param toAddress
     * @param currency
     * @param amount
     * @param feeAddress
     * @return
     * @throws Exception
     */
    public String omniFundedSend(String fromAddress, String toAddress, String currency, BigDecimal amount, String feeAddress) throws Exception {
        if (!walletpassphrase(30)) {
            throw new IllegalArgumentException("password is incorrect");
        }

        String condition = "[\"" + fromAddress + "\",\"" + toAddress + "\"," + currency + ",\"" + amount + "\",\"" + feeAddress + "\"]";

        LOGGER.info("omniSend 1 - " + condition);
        String s = main("omni_funded_send", condition);
        JSONObject json = JSONObject.fromObject(s);
        LOGGER.info("omniSend 2 - " + json);

        walletlock();

        String result = json.getString("result");
        if ("null".equals(result)) {
            return null;
        }
        return result;
    }

}