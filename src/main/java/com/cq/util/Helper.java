package com.cq.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Helper {

    private static final String SEPARATOR = "\\|\\|";

    /**
     * check param whether is null or ''
     *
     * @param params
     * @return boolean
     */
    public static Boolean isEmptyOrNull(Object... params) {
        if (params != null) {
            for (Object val : params) {
                if ("".equals(val) || val == null) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * merge byte[] head and byte[] tail ->byte[head+tail] rs
     *
     * @param head
     * @param tail
     * @return byte[]
     */
    public static byte[] byteMerrage(byte[] head, byte[] tail) {
        byte[] temp = new byte[head.length + tail.length];
        System.arraycopy(head, 0, temp, 0, head.length);
        System.arraycopy(tail, 0, temp, head.length, tail.length);
        return temp;
    }


    public static String currentMethod() {
        return new Exception("").getStackTrace()[1].getMethodName();
    }

    public static String md5(String text, String key) throws Exception {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        return DigestUtils.md5Hex(text + key);
    }

    public static boolean verifyMD5(String text, String key, String md5) throws Exception {
        if (StringUtils.isEmpty(md5)) {
            return true;
        }
        return md5.equalsIgnoreCase(md5(text, key));
    }

    public static Timestamp getTimestamp() {
        return new Timestamp(new Date().getTime());
    }

    public static int getTimestampSec() {
        return (int) (new Date().getTime() / 1000);
    }

    public static Date getDate() {
        return new Date();
    }

    public static boolean isAddressValid(String address) {
        return StringUtils.isNotBlank(address) && address.length() == 34;
    }

    public static boolean isTXIDValid(String txid) {
        return StringUtils.isNotBlank(txid) && txid.length() == 64;
    }

    public static boolean isNameValid(String name) {
        return StringUtils.isNotBlank(name) && !StringUtils.containsWhitespace(name)
                && name.length() >= 4
                && name.length() <= 32;
    }

    public static boolean isPasswordValidIfExist(String password) {
        return StringUtils.isEmpty(password) ||
                (
                        StringUtils.isNotBlank(password) && !StringUtils.containsWhitespace(password)
                                && password.length() >= 4
                                && password.length() <= 32
                );
    }

    public static boolean isPageNumberValid(int pageSize, int pageNumber) {
        return pageSize > 0 && pageNumber >= 0 && pageSize < 200;
    }

    public static Map<String, String> convertMap(Map<String, Object> map) {
        Map<String, String> converted = new HashMap<>();
        for (String k : map.keySet()) {
            converted.put(k, map.get(k).toString());
        }
        return converted;
    }

    public static String getDateString(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(d);
    }

    public static String getTimestampString(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(d);
    }
}
