package com.cq.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Uint128;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;

public class ETHUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ETHUtils.class);

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


    public static boolean validateaddress(String address) {
        return address != null && address.toLowerCase().matches("^(0x)?[0-9a-f]{40}$");
    }

    public ETHUtils(BTCMessage btcMessage) {
        this.ACCESS_KEY = btcMessage.getACCESS_KEY();
        this.SECRET_KEY = btcMessage.getSECRET_KEY();
        this.IP = btcMessage.getIP();
        this.PORT = btcMessage.getPORT();
        this.PASSWORD = btcMessage.getPASSWORD();
    }

    public JSONObject getbalance(String address) throws Exception {
        String s = main("eth_getBalance", "[\"" + address + "\", \"latest\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public JSONObject eth_accounts() throws Exception {
        String s = main("eth_accounts", "[]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public List<String> eth_accountsValue() throws Exception {
        JSONObject s = eth_accounts();
        List<String> list = new ArrayList<String>();
        if (s.containsKey("result")) {
            JSONArray arr = s.getJSONArray("result");
            for (int i = 0; i < arr.size(); i++) {
                list.add(arr.getString(i));
            }
        }
        return list;
    }

    public BigDecimal getbalanceValue(String address) throws Exception {
        BigDecimal result = BigDecimal.ZERO;
        JSONObject s = getbalance(address);
        if (s.containsKey("result")) {
            result = parseEthAmount(s.getString("result"));
        }
        return result;
    }

    public BigDecimal getbalanceValueDecimal(String address) throws Exception {
        return getbalanceValue(address);
    }


    public BigDecimal getbalanceValue() throws Exception {
        BigDecimal result = BigDecimal.ZERO;
        List<String> address = this.eth_accountsValue();
        for (String addr : address) {
            BigDecimal bal = getbalanceValue(addr);
            result = MathUtil.add(result, bal);
            if (bal.compareTo(BigDecimal.ZERO) > 0) {
                LOGGER.info(String.format("getbalanceValue all - addr:%s, balance:%s", addr, bal));
            }
        }
        LOGGER.info(String.format("getbalanceValue all - allBalance:%s", result));
        return MathUtil.getBigDecimal(result);

    }

    public BigDecimal getBalanceToken(String contract, String address) throws Exception {
        BigDecimal result = BigDecimal.ZERO;

        Function function = new Function(
                "balanceOf",
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(address)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint128>() {
                }));
        String data = FunctionEncoder.encode(function);

        String s = main("eth_call", "[{" +
                "\"to\": \"" + contract + "\"," +
                " \"data\": \"" + data + "\" " +
                "}, \"latest\"]");
        JSONObject json = JSONObject.fromObject(s);

        if (json.containsKey("result")) {
            String r = json.getString("result");
            if ("0x".equals(r)) {
                return BigDecimal.ZERO;
            }

            BigDecimal balanceWei = new BigDecimal(Numeric.toBigInt(r));

            int decimals = getDecimalsToken(contract);
            result = balanceWei.divide(BigDecimal.TEN.pow(decimals));
        }
        return result;
    }

    public BigDecimal getAllBalanceToken(String contract) throws Exception {
        BigDecimal result = BigDecimal.ZERO;
        List<String> addresses = this.eth_accountsValue();
        for (String addr : addresses) {
            BigDecimal bal = getBalanceToken(contract, addr);
            result = result.add(bal);

            if (bal.compareTo(BigDecimal.ZERO) > 0) {
                LOGGER.info(String.format("getAllBalanceToken all - addr:%s, balance:%s", addr, bal.toPlainString()));
            }
        }
        LOGGER.info(String.format("getAllBalanceToken all - allBalance:%s", result.toPlainString()));
        return result;
    }

    public JSONObject getNewaddress() throws Exception {
        String s = main("personal_newAccount", "[\"" + PASSWORD + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public String getNewaddressValue() throws Exception {
        String result = null;
        JSONObject s = getNewaddress();
        if (s.containsKey("result")) {
            result = s.get("result").toString();
            if (result.equals("null")) {
                result = null;
            }
        }
        return result;
    }

    public JSONObject eth_getTransactionByHash(String hash) throws Exception {
        String s = main("eth_getTransactionByHash", "[\"" + hash + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public BTCInfo eth_getTransactionByHashValue(String hash) throws Exception {
        JSONObject jsonObject = eth_getTransactionByHash(hash);
        if (jsonObject.containsKey("result")) {
            JSONObject item = jsonObject.getJSONObject("result");
            if (item.containsKey("hash") == false) {
                return null;
            }
            String from = item.getString("from");
            String to = item.getString("to");
            String value = item.getString("value");

            BTCInfo info = new BTCInfo();
            info.setToAddress(to);
            info.setAmount(parseEthAmount(value));
            info.setConfirmations(0);
            info.setTime(new Timestamp(new Date().getTime()));
            info.setTxid(item.getString("hash"));
            info.setBlockNumber(Long.parseLong(item.getString("blockNumber").substring(2), 16));
            info.setConfirmations((int) (eth_blockNumberValue() - Long.parseLong(item.getString("blockNumber").substring(2), 16)));
            return info;
        }
        return null;
    }

    //区块高度
    public JSONObject eth_blockNumber() throws Exception {
        String s = main("eth_blockNumber", "[]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public long eth_blockNumberValue() throws Exception {
        JSONObject jsonObject = eth_blockNumber();
        if (jsonObject.containsKey("result")) {
            return Numeric.toBigInt(jsonObject.getString("result")).longValue();
        } else {
            return 0;
        }
    }

    private static class EtherscanFetcher {

        private static final String FETCH_URL = "https://etherscan.io/";
        private static final String FETCH_URL_NONCE = FETCH_URL + "block/";

        static int fetchBlockNum(int timeout) {
            final String PARENT_TAG = "ContentPlaceHolder1_LitLastBlock";
            final String PRE_TAG = "<span id='lastblock'>";
            final String AFT_TAG = "</span>";

            int curblock = 0;
            try {
                long pre = System.currentTimeMillis();
                String resp = HttpUtils.doGet(FETCH_URL, timeout);
                long aft = System.currentTimeMillis();
//				System.out.println("fetchBlockNum - 1 " + (aft - pre));

                // 简单粗暴
                int idxParent = resp.indexOf(PARENT_TAG);
                if (idxParent < 0) {
                    return 0;
                }

                int idxPre = resp.indexOf(PRE_TAG, idxParent);
                if (idxPre < 0) {
                    return 0;
                }
                int idxPreLast = idxPre + PRE_TAG.length();

                int idxAft = resp.indexOf(AFT_TAG, idxPre);
                if (idxAft < 0) {
                    return 0;
                }

                String sub = resp.substring(idxPreLast, idxAft).trim();
                curblock = Integer.parseInt(sub);

                long aft2 = System.currentTimeMillis();
//				System.out.println("fetchBlockNum - 2 " + (aft2 - aft));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return curblock;
        }

        static String fetchBlockNonce(int blocknum, int timeout) {
            final String PARENT_TAG = "Nonce:";
            final String PRE_TAG = "<div class=\"col-sm-9 cbs\" style=\"word-wrap: break-word;\">";
            final String AFT_TAG = "</div>";

            String nonce = null;
            try {
                long pre = System.currentTimeMillis();
                String resp = HttpUtils.doGet(FETCH_URL_NONCE + blocknum, timeout);
                long aft = System.currentTimeMillis();
//				System.out.println("fetchBlockNonce - 1 " + (aft - pre));

                // 简单粗暴
                int idxParent = resp.indexOf(PARENT_TAG);
                if (idxParent < 0) {
                    return null;
                }

                int idxPre = resp.indexOf(PRE_TAG, idxParent);
                if (idxPre < 0) {
                    return null;
                }
                int idxPreLast = idxPre + PRE_TAG.length();

                int idxAft = resp.indexOf(AFT_TAG, idxPre);
                if (idxAft < 0) {
                    return null;
                }

                nonce = resp.substring(idxPreLast, idxAft).trim();

                long aft2 = System.currentTimeMillis();
//				System.out.println("fetchBlockNonce - 2 " + (aft2 - aft));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return nonce;
        }

    }

    public long eth_blockNumberValueFromEtherscan() {
        long highest = 0L;

        // etherscan api 获取
//		String url = "https://api.etherscan.io/api?module=proxy&action=eth_blockNumber&apikey=YourApiKeyToken";
//		try {
//			// 超时时间为5s
//			String json = HttpUtils.doGet(url, 5000);
//			JSONObject jo = JSONObject.fromObject(json);
//			if(jo.containsKey("result")){
//				highest = Numeric.toBigInt(jo.getString("result")).longValue();
//			}
//		} catch (Exception e) {
//		    LOGGER.error(e.getMessage(), e);
//		}

        // etherscan 首页抓取
        highest = EtherscanFetcher.fetchBlockNum(5000);

        LOGGER.debug("eth_blockNumberValueFromEtherscan - 1 - " + highest);

        if (highest == 0) {
            // 获取失败，则从本地获取
            LOGGER.warn("eth_blockNumberValueFromEtherscan - 2 - get from wallet");
            try {
                highest = eth_blockNumberValue();
            } catch (Exception e1) {
                LOGGER.error(e1.getMessage(), e1);
            }
            LOGGER.warn("eth_blockNumberValueFromEtherscan - 3 - get from wallet - " + highest);
        }

        LOGGER.debug("eth_blockNumberValueFromEtherscan - 2 - " + highest);

        return highest;
    }

    public Date eth_getBlockByNumberTimestemp(long blockNum) throws Exception {
        String hexBlockNum = Numeric.toHexStringWithPrefix(BigInteger.valueOf(blockNum));
        String json = main("eth_getBlockByNumber", "[\"" + hexBlockNum + "\", false]");
        JSONObject jo = JSONObject.fromObject(json);
        if (!jo.containsKey("result")) {
            throw new Exception("eth_getBlockByNumberTimestemp no result");
        }

        String tms = jo.getJSONObject("result").getString("timestamp");
        long tm = Numeric.toBigInt(tms).longValue() * 1000;
        return new Date(tm);
    }

    public String eth_getBlockNonceByNumberFromEtherscan(int blockNum) {
        String nonce = "";

        // etherscan 获取
        String hexBlockNum = Numeric.toHexStringWithPrefix(BigInteger.valueOf(blockNum));
//		String url = "https://api.etherscan.io/api?module=proxy&action=eth_getBlockByNumber&tag=" + hexBlockNum + "&boolean=false&apikey=YourApiKeyToken";
//		try {
//			// 超时时间为5s
//			String json = HttpUtils.doGet(url, 5000);
//			nonce = parseBlockNonce(json);
//		} catch (Exception e) {
//			LOGGER.error(e.getMessage(), e);
//		}

        nonce = EtherscanFetcher.fetchBlockNonce(blockNum, 5000);

        LOGGER.debug("eth_getBlockNonceByNumberFromEtherscan - 1 - " + nonce);

        if (StringUtils.isEmpty(nonce)) {
            // 获取失败，则从本地获取
            LOGGER.warn("eth_getBlockNonceByNumberFromEtherscan - 2 - get from wallet");
            try {
                String json = main("eth_getBlockByNumber", "[\"" + hexBlockNum + "\", false]");
                nonce = parseBlockNonce(json);
            } catch (Exception e1) {
                LOGGER.error(e1.getMessage(), e1);
            }
            LOGGER.warn("eth_getBlockNonceByNumberFromEtherscan - 3 - get from wallet - " + nonce);
        }

        LOGGER.debug("eth_getBlockNonceByNumberFromEtherscan - 4 - " + nonce);

        return nonce;
    }

    private String parseBlockNonce(String json) {
        String nonce = "";
        JSONObject jo = JSONObject.fromObject(json);
        if (jo.containsKey("result")) {
            nonce = jo.getJSONObject("result").getString("nonce");
        }
        return nonce;
    }

    //区块交易数量
    public JSONObject eth_getBlockTransactionCountByNumber(long id) throws Exception {
        String s = main("eth_getBlockTransactionCountByNumber", "[\"0x" + Long.toHexString(id) + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public long eth_getBlockTransactionCountByNumberValue(long id) throws Exception {
        JSONObject jsonObject = eth_getBlockTransactionCountByNumber(id);
        long count = 0;
        if (jsonObject.containsKey("result")) {
            count = Long.parseLong(jsonObject.getString("result").substring(2), 16);
        }
        return count;
    }


    //区块交易记录收据
    public JSONObject eth_getTransactionReceipt(String hash) throws Exception {
        String s = main("eth_getTransactionReceipt", "[\"" + hash + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    /**
     * 从receipt中取token transfer的logs数据，即为转账信息
     * <p/>
     * 一个token转账的receipt大概是这个样子:<br/>
     * <pre>
     * {
     * "jsonrpc": "2.0",
     * "id": 1,
     * "result": {
     * "blockHash": "0x3ca944b1bf201ffb898617993155e8ec5b23a4b693ec18195a9e0041ff53afc4",
     * "blockNumber": "0x69fb",
     * "contractAddress": null,
     * "cumulativeGasUsed": "0x927c",
     * "from": "0xef678007d18427e6022059dbc264f27507cd1ffc",
     * "gasUsed": "0x927c",
     * "logs": [{
     * "address": "0xec0d1e2d07d3c81bfd22eeac06115385577696f4",
     * "topics": ["0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef", "0x000000000000000000000000ef678007d18427e6022059dbc264f27507cd1ffc", "0x000000000000000000000000421c5f1bb27e48cf66f7d25d354a423525b2c3fd"],
     * "data": "0x0000000000000000000000000000000000000000000000000000000000000064",
     * "blockNumber": "0x69fb",
     * "transactionHash": "0x16e68f223c7f7f3642303158c7e26d934d67ceee936ff83e5c289c0b64b081c2",
     * "transactionIndex": "0x0",
     * "blockHash": "0x3ca944b1bf201ffb898617993155e8ec5b23a4b693ec18195a9e0041ff53afc4",
     * "logIndex": "0x0",
     * "removed": false
     * }],
     * "logsBloom": "0x00000000000000000000000000000000000000000000000000000001000000000000000000000000000001000000000000000000000000080000000000000000000000000000000000000008004000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000002000000008000020000000000000000000000000000000000000000000100000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000400000",
     * "root": "0x7efe1c9ca670ca4e9ea85284bff56f09179ada90696bf275b5cae549d0893743",
     * "to": "0xec0d1e2d07d3c81bfd22eeac06115385577696f4",
     * "transactionHash": "0x16e68f223c7f7f3642303158c7e26d934d67ceee936ff83e5c289c0b64b081c2",
     * "transactionIndex": "0x0"
     * }
     * }
     * </pre>
     * <p>
     * 需要注意的是，logs可能包含多个转账信息<br/>
     * <p>
     * 转账方法的hash:<br/>
     * Transfer(address,address,uint256):0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef
     *
     * @param contractAddress
     * @param hashTransaction
     * @param hashTransferSignature erc20中transfer的signature的hash值，它是个定值吗？很多代币都是这个值，还是作为参数传进来吧
     * @return
     * @throws Exception
     */
    public List<BTCInfo> eth_getTokenTransactionReceiptValue(String contractAddress, String hashTransaction, String hashTransferSignature) throws Exception {
        try {
            JSONObject json = eth_getTransactionReceipt(hashTransaction);

            LOGGER.debug(String.format("eth_getTokenTransactionReceiptValue 1 - %s", json.toString()));

            JSONObject result = json.getJSONObject("result");
            if (result == null || result.size() == 0) {
                return null;
            }

            JSONArray logs = result.getJSONArray("logs");
            if (logs == null || logs.size() == 0) {
                return null;
            }

            List<BTCInfo> btcInfos = new ArrayList<>();
            for (int i = 0; i < logs.size(); ++i) {
                JSONObject l = logs.getJSONObject(i);

                // 只能是我们平台有的合约
                String contractAddressTheir = l.getString("address");
                if (!contractAddress.equalsIgnoreCase(contractAddressTheir)) {
                    continue;
                }

                // topics
                JSONArray topics = l.getJSONArray("topics");
                if (topics == null || topics.size() != 3) {
                    continue;
                }
                // topics[0]就是 hash of signature of event，这个值必须等于transfer的，才是转账事件
                String hashEventSignature = topics.getString(0);
                if (!hashTransferSignature.equalsIgnoreCase(hashEventSignature)) {
                    continue;
                }
                String fromAddress = topics.getString(1);
                // to address需要在上一层进行判断，是否为我们的平台的钱包地址
                String toAddress = topics.getString(2);

                // data就是转账的金额，uint256
                String amount = l.getString("data");

                BTCInfo info = new BTCInfo();
                info.setFromAddress(parseAddressInTopic(fromAddress));
                info.setToAddress(parseAddressInTopic(toAddress));
                info.setAmount(parseTokenAmountInLog(contractAddress, amount));
                info.setConfirmations(0);
                info.setTime(new Timestamp(new Date().getTime()));
                info.setTxid(hashTransaction);

                btcInfos.add(info);

                LOGGER.debug(String.format("eth_getTokenTransactionReceiptValue 2 - %s", info.toString()));
            }

            if (btcInfos.size() == 0) {
                return null;
            }
            return btcInfos;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public int getDecimalsToken(String contract) throws Exception {
        Function function = new Function("decimals",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint>() {
                }));
        String data = FunctionEncoder.encode(function);

        String s = main("eth_call", "[{" +
                "\"to\": \"" + contract + "\"," +
                " \"data\": \"" + data + "\" " +
                "}, \"latest\"]");
        JSONObject json = JSONObject.fromObject(s);

        return Numeric.toBigInt(json.getString("result")).intValue();
    }


    //区块交易记录
    public JSONObject getTransactionByBlockNumberAndIndex(long number, int index) throws Exception {
        String s = main("eth_getTransactionByBlockNumberAndIndex", "[\"0x" + Long.toHexString(number) + "\",\"0x" + Integer.toHexString(index) + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public BTCInfo getTransactionByBlockNumberAndIndexValue(long number, int index) throws Exception {
        BTCInfo btcInfo = null;
        JSONObject jsonObject = getTransactionByBlockNumberAndIndex(number, index);
        if (jsonObject.containsKey("result")) {
            JSONObject item = jsonObject.getJSONObject("result");
            String from = item.getString("from");
            String to = item.getString("to");
            String value = item.getString("value");

            BTCInfo info = new BTCInfo();
            info.setBlockNumber(number);
            info.setFromAddress(from);
            info.setToAddress(to);
            info.setAmount(parseEthAmount(value));
            info.setConfirmations(0);
            info.setTime(new Timestamp(new Date().getTime()));
            info.setTxid(item.getString("hash"));
            return info;

        }
        return btcInfo;
    }

    public List<BTCInfo> listtransactionsValue(long number) throws Exception {
        long count = eth_getBlockTransactionCountByNumberValue(number);
        List<BTCInfo> all = new ArrayList();
        for (int i = 0; i < count; i++) {
            BTCInfo info = getTransactionByBlockNumberAndIndexValue(number, i);
            all.add(info);
        }
        return all;
    }

    public boolean lockAccount(String account) throws Exception {

        try {
            String s = main("personal_lockAccount", "[" +
                    "\"" + account + "\"" +
                    "]");
            JSONObject json = JSONObject.fromObject(s);
            return json.getBoolean("result");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;

    }

    public boolean walletpassphrase(String account) throws Exception {

        try {
            String s = main("personal_unlockAccount", "[" +
                    "\"" + account + "\"," + "\"" + PASSWORD + "\"" +
                    "]");
            JSONObject json = JSONObject.fromObject(s);
            return json.getBoolean("result");
        } catch (Exception e) {
        }
        return false;

    }

//	private String eth_gasPrice() throws Exception {
//		String s = main("eth_gasPrice", "[]");
//		JSONObject json = JSONObject.fromObject(s);
//		return json.getString("result");
//	}

    private String eth_gasPrice() throws Exception {
        String s = main("eth_gasPrice", "[]");
        JSONObject json = JSONObject.fromObject(s);
        String strprice = json.getString("result");

        BigDecimal price = new BigDecimal(Numeric.toBigInt(strprice));
        BigDecimal priceHigher = price.multiply(new BigDecimal(1.05));
        return Numeric.toHexStringWithPrefix(priceHigher.toBigInteger());
    }

    /**
     * 返回gas price
     *
     * @return 单位ETH
     * @throws Exception
     */
    public BigDecimal getGasPrice() throws Exception {
        String p = eth_gasPrice();
        if (StringUtils.isEmpty(p)) {
            throw new NumberFormatException("eth_gasPrice 获取失败");
        }
        BigDecimal priceInWei = new BigDecimal(Numeric.toBigInt(p));
        return Convert.fromWei(priceInWei, Convert.Unit.ETHER);
    }

    private String getNonce(String fromAddress) throws Exception {
        String s = main("eth_getTransactionCount", "[\"" + fromAddress + "\", \"pending\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json.getString("result");
    }

    private String eth_sign(String address, String data) throws Exception {
        String s = main("eth_sign", "[\"" + address + "\", \"" + data + "\"]");
        JSONObject json = JSONObject.fromObject(s);
        return json.getString("result");
    }

    public JSONObject eth_sendTransaction(String from, String to, BigDecimal amount, long gas) throws Exception {
        walletpassphrase(from);

        String gasprice = eth_gasPrice();
        String condition = "[{" +
                " \"from\": \"" + from + "\"," +
                "\"to\": \"" + to + "\"," +
                " \"gas\": \"0x" + Long.toHexString(gas) + "\"," +
                "\"gasPrice\": \"" + gasprice + "\"," +
                " \"value\": \"" + parseEthAmountHex(amount) + "\" " +
                "}]";

        LOGGER.info("eth_sendTransaction 1 - " + condition);
        String s = main("eth_sendTransaction", condition);
        LOGGER.info("eth_sendTransaction 2 - " + s);

        lockAccount(from);
        JSONObject json = JSONObject.fromObject(s);
        return json;
    }

    public JSONObject eth_sendRawTransaction(String contract, String from, String to, BigDecimal amount, long gasLimit) throws Exception {
        walletpassphrase(from);

        JSONObject json = null;
        try {
            // 转账金额换算也使用contract对应的decimals来计算
            int decimals = getDecimalsToken(contract);
            BigInteger nAmount = amount.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger();

            Function function = new Function(
                    "transfer",
                    Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(to),
                            new org.web3j.abi.datatypes.generated.Uint256(nAmount)),
                    Collections.emptyList());
            String data = FunctionEncoder.encode(function);

//			String nonce = getNonce(from);
            String gasPrice = eth_gasPrice();
            String hexGasLimit = Numeric.toHexStringWithPrefix(BigInteger.valueOf(gasLimit));

//			String condition = "[{"+
//					" \"from\": \""+from+"\","+
//					"\"to\": \""+contract+"\","+
//					" \"gas\": \""+hexGasLimit+"\","+
//					"\"gasPrice\": \""+gasPrice+"\","+
//					" \"value\": \"0x0\", "+
//					" \"data\": \""+data+"\", "+
//					" \"nonce\": \""+nonce+"\" "+
//					"}]";
            String condition = "[{" +
                    " \"from\": \"" + from + "\"," +
                    "\"to\": \"" + contract + "\"," +
                    " \"gas\": \"" + hexGasLimit + "\"," +
                    "\"gasPrice\": \"" + gasPrice + "\"," +
                    " \"value\": \"0x0\", " +
                    " \"data\": \"" + data + "\" " +
                    "}]";

            LOGGER.info(String.format("eth_sendRawTransaction 0 - contract:%s,from:%s,to:%s,amount:%s,gasLimit:%s",
                    contract,
                    from,
                    to,
                    amount.toPlainString(),
                    Long.toString(gasLimit)));
            LOGGER.info("eth_sendRawTransaction 1 - " + condition);
            String s = main("eth_sendTransaction", condition);
            LOGGER.info("eth_sendRawTransaction 2 - " + s);

            json = JSONObject.fromObject(s);
        } catch (Throwable e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage(), e);

            // TODO: 还应该标记一个错误的状态，要么重试，要么就等到客户联系
        }

        lockAccount(from);

        return json;

    }

    public String sendtoaddressValue(String from, String to, BigDecimal amount, long gas) throws Exception {
        String result = "";
        try {
            JSONObject s = eth_sendTransaction(from, to, amount, gas);
            if (s.containsKey("result")) {
                if (!s.get("result").toString().equals("null")) {
                    result = s.get("result").toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public String sendtoaddressValueToken(String contract, String from, String to, BigDecimal amount, long gasLimit) throws Exception {
        String result = "";
        try {
            JSONObject s = eth_sendRawTransaction(contract, from, to, amount, gasLimit);
            if (s != null && s.containsKey("result")) {
                if (!s.get("result").toString().equals("null")) {
                    result = s.get("result").toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //	'{"jsonrpc":"2.0","method":"eth_getBalance","params":["0x407d73d8a49eeb85d32cf465507dd71d507100c1", "latest"],"id":1}'
    private String main(String method, String condition) throws Exception {
        String result = "";
        String tonce = "" + (System.currentTimeMillis() * 1000);
        String params = "tonce=" + tonce.toString() + "&accesskey="
                + ACCESS_KEY
                + "&requestmethod=post&id=1&method=" + method + "&params=" + condition;

        String url = "http://" + IP + ":" + PORT;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setConnectTimeout(20000);
        con.setReadTimeout(60000);

        // add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("Json-Rpc-Tonce", tonce.toString());
        con.setRequestProperty("content-type", "application/json");

        String postdata = "{\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\", \"params\":" + condition + ", \"id\": 1}";
        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(postdata);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        if (responseCode != 200) {
            return "{\"result\":null,\"error\":" + responseCode + ",\"id\":1}";
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

    public static BigDecimal parseHexval(String hexval) {
        return new BigDecimal(Numeric.toBigInt(hexval));
    }

    public String parseEthAmountHex(BigDecimal amount) {
        BigInteger nAmount = Convert.toWei(amount.toString(), Convert.Unit.ETHER).toBigInteger();
        return Numeric.toHexStringWithPrefix(nAmount);
    }

    public BigDecimal parseEthAmount(String hexval) {
        BigDecimal nAmount = Convert.fromWei(new BigDecimal(Numeric.toBigInt(hexval)), Convert.Unit.ETHER);
        return nAmount;
    }

    public BigDecimal parseTokenAmountInLog(String contract, String hexval) throws Exception {
        BigInteger bigval = (BigInteger) FunctionReturnDecoder.decode(hexval,
                org.web3j.abi.Utils.convert(Arrays.asList(new TypeReference<Uint256>() {
                })))
                .get(0)
                .getValue();

        int decimals = getDecimalsToken(contract);

        return new BigDecimal(bigval).divide(BigDecimal.TEN.pow(decimals));
    }

    public String parseAddressInTopic(String address) {
        return (String) FunctionReturnDecoder.decodeIndexedValue(address, new TypeReference<Address>() {
        })
                .getValue();
    }

//	/**
//	 * @deprecated 因为gasPrice是变动的，所以这样就算不合适，feeEth改为了gas limit，直接拿来用
//	 *
//	 * @param feeEth
//	 * @return
//	 * @throws Exception
//	 */
//	public BigDecimal extractGasLimit(BigDecimal feeEth) throws Exception {
//		BigDecimal price = new BigDecimal(Numeric.toBigInt(eth_gasPrice()));
//		BigDecimal fee = Convert.toWei(feeEth, Convert.Unit.ETHER);
//		return fee.divide(price, 0, BigDecimal.ROUND_DOWN);
//	}

    public BigDecimal calculateGasFee(BigDecimal gasLimit) throws Exception {
        BigDecimal price = getGasPrice();
        return price.multiply(gasLimit);
    }

}