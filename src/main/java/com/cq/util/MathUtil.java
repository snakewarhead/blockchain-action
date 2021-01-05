package com.cq.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtil {

    public final static int RESULT_SCALE = 8;

    private final static int CAL_SCALE = 12;

    private final static BigDecimal NEG_ONE = new BigDecimal("-1");

    public final static BigDecimal MIN_VAL = new BigDecimal("0.00000001");
    public final static BigDecimal NEG_MIN_VAL = new BigDecimal("-0.00000001");
    public static final BigDecimal TWO = new BigDecimal("2");
    public static final BigDecimal ONEHUNDRED = new BigDecimal("100");

    public static BigDecimal getBigDecimal(int val) {
        return BigDecimal.valueOf(val).setScale(CAL_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal getBigDecimal(double val) {
        return BigDecimal.valueOf(val).setScale(CAL_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal getBigDecimal(String val) {
        return new BigDecimal(val).setScale(CAL_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal add(BigDecimal one, BigDecimal two) {
        if (one == null) {
            one = BigDecimal.ZERO;
        }
        if (two == null) {
            two = BigDecimal.ZERO;
        }
        BigDecimal result = one.add(two);
        result = result.setScale(CAL_SCALE, RoundingMode.HALF_UP);
        return result;
    }

    public static BigDecimal add(BigDecimal one, BigDecimal two, BigDecimal three) {
        return add(add(one, two), three);
    }

    public static BigDecimal neg(BigDecimal one) {
        return mul(one, NEG_ONE);
    }

    public static String getBigDecimalString(BigDecimal one) {
        return getBigDecimalString(one, RESULT_SCALE);
    }

    public static String getBigDecimalString(BigDecimal one, int scale) {
        if (one == null) {
            return "";
        }
        one = one.setScale(scale, RoundingMode.FLOOR);
        return one.stripTrailingZeros().toPlainString();
    }

    public static String stripTrailingZeros(BigDecimal one, int scale) {
        return one.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    public static String stripTrailingZeros(BigDecimal one) {
        return one.stripTrailingZeros().toPlainString();
    }

    public static BigDecimal getBigDecimal(BigDecimal one, int scale) {
        if (one == null) {
            return BigDecimal.ZERO;
        }
        one = one.setScale(scale, RoundingMode.HALF_UP);
        return one;
    }

    public static BigDecimal getBigDecimal(BigDecimal one) {
        return getBigDecimal(one, RESULT_SCALE);
    }

    public static BigDecimal sub(BigDecimal one, BigDecimal two) {
        if (one == null) {
            one = BigDecimal.ZERO;
        }
        if (two == null) {
            two = BigDecimal.ZERO;
        }
        BigDecimal result = one.subtract(two);
        result = result.setScale(CAL_SCALE, RoundingMode.HALF_UP);
        return result;
    }

    public static BigDecimal sub(BigDecimal one, BigDecimal two, BigDecimal three) {
        return sub(sub(one, two), three);
    }

    public static BigDecimal mul(BigDecimal one, BigDecimal two) {
        BigDecimal result = one.multiply(two);
        result = result.setScale(CAL_SCALE, RoundingMode.HALF_UP);
        return result;
    }

    public static BigDecimal mul(BigDecimal one, BigDecimal two, BigDecimal three) {
        return mul(mul(one, two), three);
    }

    public static BigDecimal div(BigDecimal one, BigDecimal two) {
        BigDecimal result = one.divide(two, CAL_SCALE, RoundingMode.HALF_UP);
        return result;
    }



    public static MathUtil of(BigDecimal val) {
        return new MathUtil(val);
    }

    private BigDecimal val;

    private MathUtil(BigDecimal val) {
        this.val = val;
    }

    public MathUtil add(BigDecimal opVal) {
        val = add(val, opVal);
        return this;
    }

    public MathUtil sub(BigDecimal opVal) {
        val = sub(val, opVal);
        return this;
    }

    public MathUtil mul(BigDecimal opVal) {
        val = mul(val, opVal);
        return this;
    }

    public MathUtil div(BigDecimal opVal) {
        val = div(val, opVal);
        return this;
    }

    public MathUtil abs() {
        val = val.abs();
        return this;
    }

    public BigDecimal to() {
        return val;
    }
}
