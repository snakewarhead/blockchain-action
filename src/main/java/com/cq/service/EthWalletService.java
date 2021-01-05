package com.cq.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cq.config.ConfigParam;
import com.cq.util.BTCMessage;
import com.cq.util.ETHUtils;
import com.cq.util.HttpUtils;
import com.cq.util.MathUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by lin on 2019/8/26.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EthWalletService {

    private final ConfigParam configParam;

    public void obfuscateFlow() {
        final BigDecimal MIN_FEE = new BigDecimal("0.1");
        final BigDecimal MIN_BALANCE = new BigDecimal("100");
        final BigDecimal GAS_LIMIT_IN_MAIN = new BigDecimal("22000");
        final BigDecimal GAS_LIMIT_IN_SUB = new BigDecimal("120000.00000000");

        BTCMessage msg = new BTCMessage();
        msg.setIP(configParam.walletip);
        msg.setPORT(configParam.walletport);
        msg.setACCESS_KEY("");
        msg.setSECRET_KEY("");
        msg.setPASSWORD(configParam.walletpassword);
        ETHUtils client = new ETHUtils(msg);

        String addrMain = configParam.walletmainaddr;
        List<String> addrCreatedAll = new ArrayList<>();
        addrCreatedAll.add(addrMain);

        Random random = new Random();
        int time = 0;
        while (true) {
            try {
                time++;
                if (time > configParam.obfuscate_max_loops) {
                    log.info("over loop times:{} > {}", time, configParam.obfuscate_max_loops);
                    break;
                }

                // 1. dispatch tokens
                List<String> addrCreated = new ArrayList<>();

                // balance of main
                BigDecimal ethMain = client.getbalanceValueDecimal(addrMain);
                BigDecimal tokenMain = client.getBalanceToken(configParam.contract, addrMain);

                // check whether fee is enough
                if (ethMain.compareTo(MIN_FEE) < 0) {
                    log.error("fee blow {}", MIN_FEE.toPlainString());
                    break;
                }

                if (tokenMain.compareTo(MIN_BALANCE) < 0) {
                    log.error("blow MIN_BALANCE: {}", MIN_BALANCE.toPlainString());
                    break;
                }

                final int max = random.nextInt(configParam.obfuscate_interval) + 1;
                BigDecimal tokenDivided = tokenMain.divide(new BigDecimal(max));

                log.info("times:{}, max_addr:{}, eth:{}, token:{}, tokenDivided:{}", time, max,
                        ethMain.toPlainString(), tokenMain.toPlainString(), tokenDivided.toPlainString());

                int idx = 0;
                while (idx < max) {
                    try {
                        log.info("0 -- send begin idx:{}", idx);
                        // create new address
                        String addr = client.getNewaddressValue();
                        addrCreated.add(addr);
                        log.info("1 -- create address:{}", addr);

                        // token to be sent by float ratio [-0.1, 0.1]
                        double ratio = random.nextDouble() / 10.0;
                        ratio = random.nextBoolean() ? ratio : -ratio;
                        BigDecimal tokenDividedReal = tokenDivided
                                .multiply(BigDecimal.ONE.add(BigDecimal.valueOf(ratio)));
                        tokenDividedReal = tokenDividedReal.setScale(4, RoundingMode.HALF_UP);

                        boolean needBreak = false;
                        BigDecimal tokenMainCurr = client.getBalanceToken(configParam.contract, addrMain);
                        if (tokenDividedReal.compareTo(tokenMainCurr) > 0) {
                            tokenDividedReal = tokenMainCurr;
                            needBreak = true;
                        }
                        if (idx + 1 == max) {
                            tokenDividedReal = tokenMainCurr;
                        }
                        log.info("2 -- tokenDividedReal:{}", tokenDividedReal.toPlainString());

                        // send
                        String txid = client.sendtoaddressValueToken(configParam.contract, addrMain, addr,
                                tokenDividedReal, GAS_LIMIT_IN_SUB.longValue());
                        log.info("3 -- from {} to {} send txid:{}", addrMain, addr, txid);

                        int interval = random.nextInt(configParam.obfuscate_interval) + 1;
                        Thread.sleep(interval * 60 * 1000);

                        if (needBreak) {
                            log.warn("need break is true");
                            ++idx;
                            break;
                        }

                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        continue;
                    }

                    ++idx;
                }
                log.info("send end. idx:{}, max:{}", idx, max);

                addrCreatedAll.addAll(addrCreated);

                // 2. collect to another main address
                String addrMainAnother = client.getNewaddressValue();
                addrCreatedAll.add(addrMainAnother);

                int timesTry = 5;
                while (timesTry-- > 0) {
                    try {
                        // collect all
                        // fee
                        BigDecimal feeSub = client.calculateGasFee(GAS_LIMIT_IN_SUB);
                        for (String addr : addrCreated) {
                            BigDecimal token = client.getBalanceToken(configParam.contract, addr);
                            if (token.compareTo(BigDecimal.ONE) < 0) {
                                log.info("this address has no token 1. addr:{}, balance:{}", addr,
                                        token.toPlainString());
                                continue;
                            }

                            BigDecimal eth = client.getbalanceValueDecimal(addr);
                            if (eth.compareTo(feeSub) >= 0) {
                                log.info("this address has enough eth. addr:{}, balance:{}", addr,
                                        eth.toPlainString());
                                continue;
                            }

                            String txid = client.sendtoaddressValue(addrMain, addr, feeSub,
                                    GAS_LIMIT_IN_MAIN.longValue());
                            if (StringUtils.isEmpty(txid)) {
                                throw new Exception("eth send txid is null");
                            }

                            log.info("collect 1 ------ send fee ---- main:{}, total:{}, sub:{}, fee:{}, txid:{}",
                                    addrMain, eth.toPlainString(), addr, feeSub.toPlainString(), txid);
                        }

                        Thread.sleep(2 * 60 * 1000);

                        // token
                        for (String addr : addrCreated) {
                            BigDecimal token = client.getBalanceToken(configParam.contract, addr);
                            if (token.compareTo(BigDecimal.ONE) < 0) {
                                log.info("this address has no token 2. addr:{}, balance:{}", addr,
                                        token.toPlainString());
                                continue;
                            }

                            String txid = client.sendtoaddressValueToken(configParam.contract, addr, addrMainAnother,
                                    token, GAS_LIMIT_IN_SUB.longValue());
                            if (StringUtils.isEmpty(txid)) {
                                throw new Exception("token send txid is null");
                            }

                            log.info("collect 2 ------ send token ---- sub:{}, token:{}, another:{}, txid:{}", addr,
                                    token.toPlainString(), addrMainAnother, txid);
                        }

                        // transmit rest eth to another main address
                        BigDecimal ethRest = client.getbalanceValueDecimal(addrMain);
                        if (ethRest.compareTo(MIN_FEE) < 0) {
                            log.error("transmit rest, fee blow {}", MIN_FEE.toPlainString());
                            break;
                        }
                        BigDecimal feeMain = client.calculateGasFee(GAS_LIMIT_IN_MAIN);
                        ethRest = ethRest.subtract(feeMain);

                        String txid = client.sendtoaddressValue(addrMain, addrMainAnother, ethRest,
                                GAS_LIMIT_IN_MAIN.longValue());
                        if (StringUtils.isEmpty(txid)) {
                            throw new Exception("eth send txid is null");
                        }

                        log.info(
                                "collect 3 ------ send rest eth from main to another ---- main:{}, total:{}, another:{}, txid:{}",
                                addrMain, ethRest.toPlainString(), addrMainAnother, txid);

                        break;
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }

                    Thread.sleep(2 * 60 * 1000);
                }

                // change addrMain
                addrMain = addrMainAnother;

            } catch (Exception e) {
                log.error(e.getMessage(), e);
                break;
            }

            try {
                Thread.sleep(7 * 60 * 1000);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }

        // log all addresses
        log.info("------------- addrCreatedAll start");
        sumWalletTokenByAddress(addrCreatedAll);
        log.info("------------- addrCreatedAll end");

        log.info("------------- addresses in node start");
        sumWalletTokenInNodeEthWithConfig();
        log.info("------------- addresses in node end");

    }

    public void obfuscate() {
        final BigDecimal MIN_BALANCE = new BigDecimal("100");
        final BigDecimal GAS_LIMIT_IN_MAIN = new BigDecimal("22000");
        final BigDecimal GAS_LIMIT_IN_SUB = new BigDecimal("120000.00000000");

        Random random = new Random();

        BTCMessage msg = new BTCMessage();
        msg.setIP(configParam.walletip);
        msg.setPORT(configParam.walletport);
        msg.setACCESS_KEY("");
        msg.setSECRET_KEY("");
        msg.setPASSWORD(configParam.walletpassword);
        ETHUtils client = new ETHUtils(msg);

        int time = 0;
        while (true) {
            try {
                time++;
                final int max = random.nextInt(configParam.obfuscate_interval) + 1;

                List<String> addrCreated = new ArrayList<>();

                // balance of main
                BigDecimal ethMain = client.getbalanceValueDecimal(configParam.walletmainaddr);
                BigDecimal tokenMain = client.getBalanceToken(configParam.contract, configParam.walletmainaddr);

                if (ethMain.compareTo(new BigDecimal(0.1)) < 0) {
                    log.error("fee blow 0.1");
                    break;
                }

                if (tokenMain.compareTo(MIN_BALANCE) < 0) {
                    log.error("blow MIN_BALANCE: {}", MIN_BALANCE.toPlainString());
                    break;
                }

                BigDecimal tokenDivided = tokenMain.divide(new BigDecimal(max));

                log.info("times:{}, max_addr:{}, eth:{}, token:{}, tokenDivided:{}", time, max,
                        ethMain.toPlainString(), tokenMain.toPlainString(), tokenDivided.toPlainString());

                int idx = 0;
                while (idx < max) {
                    try {
                        log.info("0 -- send begin idx:{}", idx);
                        // create new address
                        String addr = client.getNewaddressValue();
                        addrCreated.add(addr);
                        log.info("1 -- create address:{}", addr);

                        // token to be sent by float ratio [-0.1, 0.1]
                        double ratio = random.nextDouble() / 10.0;
                        ratio = random.nextBoolean() ? ratio : -ratio;
                        BigDecimal tokenDividedReal = tokenDivided
                                .multiply(BigDecimal.ONE.add(BigDecimal.valueOf(ratio)));

                        boolean needBreak = false;
                        BigDecimal tokenMainCurr = client.getBalanceToken(configParam.contract,
                                configParam.walletmainaddr);
                        if (tokenDividedReal.compareTo(tokenMainCurr) > 0) {
                            tokenDividedReal = tokenMainCurr;
                            needBreak = true;
                        }

                        tokenDividedReal = tokenDividedReal.setScale(4, RoundingMode.HALF_UP);

                        log.info("2 -- tokenDividedReal:{}", tokenDividedReal.toPlainString());

                        // send
                        String txid = client.sendtoaddressValueToken(configParam.contract, configParam.walletmainaddr,
                                addr, tokenDividedReal, GAS_LIMIT_IN_SUB.longValue());
                        log.info("3 -- send txid:{}", txid);

                        int interval = random.nextInt(configParam.obfuscate_interval) + 1;
                        Thread.sleep(interval * 60 * 1000);

                        if (needBreak) {
                            log.warn("need break is true");
                            ++idx;
                            break;
                        }

                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        continue;
                    }

                    ++idx;
                }
                log.info("send end. idx:{}, max:{}", idx, max);

                int timesTry = 5;
                while (timesTry-- > 0) {
                    try {
                        // collect all
                        // fee
                        BigDecimal feeSub = client.calculateGasFee(GAS_LIMIT_IN_SUB);
                        for (String addr : addrCreated) {
                            BigDecimal token = client.getBalanceToken(configParam.contract, addr);
                            if (token.compareTo(BigDecimal.ONE) < 0) {
                                log.info("this address has no token 1. addr:{}, balance:{}", addr,
                                        token.toPlainString());
                                continue;
                            }

                            BigDecimal eth = client.getbalanceValueDecimal(addr);
                            if (eth.compareTo(feeSub) >= 0) {
                                log.info("this address has enough eth. addr:{}, balance:{}", addr,
                                        eth.toPlainString());
                                continue;
                            }

                            String txid = client.sendtoaddressValue(configParam.walletmainaddr, addr, feeSub,
                                    GAS_LIMIT_IN_MAIN.longValue());
                            if (StringUtils.isEmpty(txid)) {
                                throw new Exception("eth send txid is null");
                            }
                        }

                        Thread.sleep(2 * 60 * 1000);

                        // token
                        for (String addr : addrCreated) {
                            BigDecimal token = client.getBalanceToken(configParam.contract, addr);
                            if (token.compareTo(BigDecimal.ONE) < 0) {
                                log.info("this address has no token 2. addr:{}, balance:{}", addr,
                                        token.toPlainString());
                                continue;
                            }

                            String txid = client.sendtoaddressValueToken(configParam.contract, addr,
                                    configParam.walletmainaddr, token, GAS_LIMIT_IN_SUB.longValue());
                            if (StringUtils.isEmpty(txid)) {
                                throw new Exception("token send txid is null");
                            }
                        }

                        break;
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }

                    Thread.sleep(2 * 60 * 1000);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            try {
                Thread.sleep(7 * 60 * 1000);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }

    }

    public BigDecimal sumWalletTokenInNodeEth() {
        BigDecimal sumToken = BigDecimal.ZERO;

        try {
            final ETHUtils ethUtils = getETHclient();

            List<String> ls = ethUtils.eth_accountsValue();

            for (String a : ls) {

                StringBuffer buffer = new StringBuffer();

                buffer.append(a);
                if (a.equalsIgnoreCase(configParam.walletmainaddr)) {
                    buffer.append(" * ");
                }
                buffer.append("\t");

                try {
                    BigDecimal r = ethUtils.getbalanceValueDecimal(a);
                    buffer.append("eth: ").append(r.toPlainString()).append("\t");
                } catch (Exception e) {
                    buffer.append("eth: error\t");
                }

                try {
                    BigDecimal r = ethUtils.getBalanceToken(configParam.contract, a);
                    buffer.append("token: ").append(r.toPlainString()).append("\t");

                    sumToken = MathUtil.add(sumToken, r);
                } catch (Exception e) {
                    buffer.append("token: error\t");
                }

                log.info(buffer.toString());
            }

            log.info("count: " + ls.size() + "\tsum: " + sumToken.toPlainString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return sumToken;
    }

    private BigDecimal extractTokenAmountOnEtherscan(String address) throws Exception {
        BigDecimal amount = BigDecimal.ZERO;

        StringBuffer buffer = new StringBuffer();
        buffer.append(address).append("\t");

        // try {
        // String url =
        // String.format("https://api.etherscan.io/api?module=account&action=balance&address=%s&tag=latest&apikey=%s",
        // a.getFaddress(), configParam.apikey);
        // String resp = HttpUtils.doGet(url, 10000);
        // JSONObject jo = JSON.parseObject(resp);
        // if ("1".equals(jo.getString("status"))) {
        // BigDecimal r = MathUtil.div(MathUtil.getBigDecimal(jo.getString("result")),
        // BigDecimal.TEN.pow(18));
        // buffer.append("eth: ").append(r.toPlainString()).append("\t");
        // }
        // } catch (Exception e) {
        // buffer.append("eth: error\t");
        // }

        try {
            String url = String.format(
                    "https://api.etherscan.io/api?module=account&action=tokenbalance&contractaddress=%s&address=%s&tag=latest&apikey=%s",
                    configParam.contract, address, configParam.apikey);
            String resp = HttpUtils.doGet(url, 10000);
            JSONObject jo = JSON.parseObject(resp);
            if ("1".equals(jo.getString("status"))) {
                BigDecimal r = MathUtil.div(MathUtil.getBigDecimal(jo.getString("result")),
                        BigDecimal.TEN.pow(configParam.decimals));
                buffer.append("token: ").append(r.toPlainString()).append("\t");

                amount = r;
            }
        } catch (Exception e) {
            buffer.append("token: error\t");
            throw new Exception(String.format("token: error - %s", address));
        }

        log.info(buffer.toString());
        return amount;
    }

    public BigDecimal sumWalletTokenByAddress(List<String> lsAddresses) {
        BigDecimal sumEth = BigDecimal.ZERO;
        BigDecimal sumToken = BigDecimal.ZERO;

        try {
            ETHUtils ethUtils = getETHclient();

            for (String a : lsAddresses) {

                StringBuffer buffer = new StringBuffer();

                buffer.append(a);
                buffer.append("\t");

                try {
                    BigDecimal r = ethUtils.getbalanceValueDecimal(a);
                    buffer.append("eth: ").append(r.toPlainString()).append("\t");

                    sumEth = MathUtil.add(sumEth, r);
                } catch (Exception e) {
                    buffer.append("eth: error\t");
                }

                try {
                    BigDecimal r = ethUtils.getBalanceToken(configParam.contract, a);
                    buffer.append("token: ").append(r.toPlainString()).append("\t");

                    sumToken = MathUtil.add(sumToken, r);
                } catch (Exception e) {
                    buffer.append("token: error\t");
                }

                log.info(buffer.toString());
            }

            log.info("count: " + lsAddresses.size() + "\tsumEth: " + sumEth.toPlainString() + "\tsumToken: "
                    + sumToken.toPlainString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return sumToken;
    }

    public void sumWalletTokenInNodeEthWithConfig() {
        ETHUtils client = getETHclient();

        try {
            List<String> ls = client.eth_accountsValue();
            sumWalletTokenByAddress(ls);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private ETHUtils getETHclient() {
        BTCMessage msg = new BTCMessage();
        msg.setIP(configParam.walletip);
        msg.setPORT(configParam.walletport);
        msg.setACCESS_KEY("");
        msg.setSECRET_KEY("");
        msg.setPASSWORD(configParam.walletpassword);
        return new ETHUtils(msg);
    }

    public String send(String from, String to, BigDecimal amount, boolean isContract) throws Exception {
        log.info("-------------------------- send 1");

        String txid;
        final ETHUtils ethUtils = getETHclient();

        if (!isContract) {
            BigDecimal total = ethUtils.getbalanceValueDecimal(from);
            if (total.compareTo(amount) < 0) {
                log.error(String.format("send 2 余额不足 - total:%s, amount:%s", total.toPlainString(),
                        amount.toPlainString()));
                throw new RuntimeException("balance is not enough 1");
            }

            long gasLimit = configParam.eth_gas_limit;
            txid = ethUtils.sendtoaddressValue(from, to, amount, gasLimit);
        } else {
            // 余额够不够
            BigDecimal total = ethUtils.getBalanceToken(configParam.contract, from);
            if (total.compareTo(amount) < 0) {
                log.error(String.format("send 3 余额不足 - total:%s, amount:%s", total.toPlainString(),
                        amount.toPlainString()));
                throw new RuntimeException("balance is not enough 2");
            }

            long gasLimit = configParam.contract_gas_limit;
            txid = ethUtils.sendtoaddressValueToken(configParam.contract, from, to, amount, gasLimit);
        }

        if (StringUtils.isEmpty(txid)) {
            log.error("send 4 哪点出问题了 ");
            throw new RuntimeException("txid is null");
        }

        log.info("-------------------------- send 5 txid:{}", txid);

        return txid;
    }

    public void sendAllTokenFromMainto(String to) throws Exception {
        log.info("sendAllTokenFromMainto 1");

        if (StringUtils.isEmpty(configParam.contract)) {
            throw new RuntimeException("this coid must be a contract");
        }

        final ETHUtils ethUtils = getETHclient();
        String mainAddr = configParam.walletmainaddr;

        BigDecimal totalEth = ethUtils.getbalanceValueDecimal(mainAddr);
        final BigDecimal GAS_LIMIT_IN_SUB = new BigDecimal(configParam.eth_gas_limit);
        final BigDecimal FEE_IN_SUB = ethUtils.calculateGasFee(GAS_LIMIT_IN_SUB);
        if (totalEth.compareTo(FEE_IN_SUB) < 0) {
            throw new RuntimeException("fee balance is not enough");
        }

        BigDecimal totalToken = ethUtils.getBalanceToken(configParam.contract, mainAddr);
        String txid = ethUtils.sendtoaddressValueToken(configParam.contract, mainAddr, to, totalToken,
                GAS_LIMIT_IN_SUB.longValue());
        if (StringUtils.isEmpty(txid)) {
            throw new RuntimeException("txid is null");
        }

        log.info(String.format("sendAllTokenFromMainto 2 - txid:%s, toten:%s, eth:%s, fee:%s", txid, totalToken.toPlainString(), totalEth.toPlainString(), GAS_LIMIT_IN_SUB.toPlainString()));
    }

    public boolean sendAllFromMainto(String to) throws Exception {
        log.info("sendAllFromMainto 1");

        final ETHUtils ethUtils = getETHclient();
        String mainAddr = configParam.walletmainaddr;

        BigDecimal totalEth = ethUtils.getbalanceValueDecimal(mainAddr);
        BigDecimal fee = ethUtils.calculateGasFee(new BigDecimal(configParam.eth_gas_limit));
        if (totalEth.compareTo(fee) < 0) {
            log.warn("fee balance is not enough, eth is empty, so needn't transfer");
            return true;
        }

        BigDecimal gasLimit = new BigDecimal(configParam.eth_gas_limit);
        BigDecimal transferAmount = totalEth.subtract(fee);

        String txid = ethUtils.sendtoaddressValue(mainAddr, to, transferAmount, gasLimit.longValue());
        if (StringUtils.isEmpty(txid)) {
            log.error("txid is null");
            return false;
        }

        log.info(String.format("sendAllFromMainto 2 - txid:%s, eth:%s, fee:%s", txid, totalEth.toPlainString(), gasLimit.toPlainString()));
        return true;
    }

    public String getNewAddress() throws Exception {
        final ETHUtils ethUtils = getETHclient();
        return ethUtils.getNewaddressValue();
    }

    public void createNewAddressInConfig() throws Exception {
        final ETHUtils ethUtils = getETHclient();

        for (int i = 0; i < configParam.scanner_count_new_addresses; ++i) {
            String addr = ethUtils.getNewaddressValue();
            log.info(addr);
        }
    }

    public String changeMainAddress() throws Exception {
        log.info("-------------------------- changeMainAddress 1");

        // 生成一个新的主地址
        String addrMainNew = getNewAddress();
        if (StringUtils.isEmpty(addrMainNew)) {
            throw new RuntimeException("new main address create failed");
        }
        String addrMainOld = configParam.walletmainaddr;
        if (StringUtils.isEmpty(addrMainOld)) {
            throw new RuntimeException("old main address is empty");
        }
        log.info("-------------------------- changeMainAddress 2, old:{}, new:{}", addrMainOld, addrMainNew);

        // 转账usdt, eth，从旧的地址到新的去
        sendAllTokenFromMainto(addrMainNew);

        log.info("-------------------------- changeMainAddress 3 sleeping");
        Thread.sleep(5 * 60 * 1000);

        // 如果token转账顺利，则后续可以删除以前的Keystore；否则，不能删除，至少给下次操作留一点机会，可以通过汇总的方式的再转过来
        boolean sendAll = sendAllFromMainto(addrMainNew);
        log.info("-------------------------- changeMainAddress 4 sendAll:{}", sendAll);

        log.info("-------------------------- changeMainAddress 5 sleeping");
        Thread.sleep(5 * 60 * 1000);

        return sendAll ? addrMainNew : null;
    }

    public void collectMain(Integer type) {
        try {
            log.info("------------- addresses in node start");
            sumWalletTokenInNodeEthWithConfig();
            log.info("------------- addresses in node end");

            log.info("------------- collection start {}", type);

            ETHUtils client = getETHclient();

            List<String> ls = client.eth_accountsValue();
            if (type == 0) {
                collectMainEth(configParam.walletmainaddr, ls);
            } else if (type == 1) {
                collectMainToken(configParam.walletmainaddr, ls);
            }

            log.info("------------- collection end");

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    public void collectMainEth(String addrMain, List<String> lsAddr) {
        final BigDecimal MIN_NUM = configParam.collect_min_eth;
        final BigDecimal GAS_LIMIT_IN_MAIN = new BigDecimal("22000");

        log.info("sendMain=============================================================");
        log.info("sendMain - eth - 正在汇总到主地址 - " + addrMain);

        BTCMessage msg = new BTCMessage();
        msg.setIP(configParam.walletip);
        msg.setPORT(configParam.walletport);
        msg.setACCESS_KEY("");
        msg.setSECRET_KEY("");
        msg.setPASSWORD(configParam.walletpassword);
        ETHUtils client = new ETHUtils(msg);

        int count = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (String addr : lsAddr) {
            if (addr.equalsIgnoreCase(addrMain)) {
                continue;
            }
            try {
                BigDecimal balance = client.getbalanceValueDecimal(addr);

                // 应该大于最小汇总数量，也要大于手续费
                BigDecimal fee = client.calculateGasFee(GAS_LIMIT_IN_MAIN);
                BigDecimal minNum = MIN_NUM;
                if (balance.compareTo(minNum) > 0 && balance.compareTo(fee) > 0) {
                    BigDecimal gasLimit = GAS_LIMIT_IN_MAIN;
                    BigDecimal transferAmount = balance.subtract(fee);

                    String tx = client.sendtoaddressValue(addr, addrMain, transferAmount, gasLimit.longValue());

                    if (!StringUtils.isEmpty(tx)) {
                        ++count;
                        totalAmount = totalAmount.add(transferAmount);
                    }
                    log.info("sendMain - eth - trasfer from {} to {}, tx:{} amount:{} gasLimit:{}", addr, addrMain,
                            tx, transferAmount.toPlainString(), gasLimit.toPlainString());

                    Thread.sleep(2 * 1000);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        log.info("sendMain - eth - address count: {}, already count: {} , amount: {}", lsAddr.size(), count,
                totalAmount.toPlainString());
        log.info("sendMain=============================================================");
    }

    public void collectMainToken(String addrMain, List<String> lsAddr) throws Exception {
        final BigDecimal MIN_BALANCE = configParam.collect_min_token;
        final BigDecimal GAS_LIMIT_IN_MAIN = new BigDecimal("22000");
        final BigDecimal GAS_LIMIT_IN_SUB = new BigDecimal("120000.00000000");

        log.info("sendMainToken=============================================================");
        log.info("sendMainToken 1 - 正在汇总到主地址 - " + addrMain);

        BTCMessage msg = new BTCMessage();
        msg.setIP(configParam.walletip);
        msg.setPORT(configParam.walletport);
        msg.setACCESS_KEY("");
        msg.setSECRET_KEY("");
        msg.setPASSWORD(configParam.walletpassword);
        ETHUtils client = new ETHUtils(msg);

        // 从主链的主账户转eth到token的子账户的gasLimit
        final BigDecimal FEE_IN_MAIN = client.calculateGasFee(GAS_LIMIT_IN_MAIN);
        log.info(String.format("sendMainToken 2 - fee summary - GAS_LIMIT_IN_MAIN:%s, FEE_IN_MAIN:%s",
                GAS_LIMIT_IN_MAIN.toPlainString(), FEE_IN_MAIN.toPlainString()));

        // 从子账户转token到主账户的gasLimit
        final BigDecimal FEE_IN_SUB = client.calculateGasFee(GAS_LIMIT_IN_SUB);
        log.info(String.format("sendMainToken 3 - fee summary - GAS_LIMIT_IN_SUB:%s, FEE_IN_SUB:%s",
                GAS_LIMIT_IN_SUB.toPlainString(), FEE_IN_SUB.toPlainString()));

        // 不够手续费的，需要从主账户中转账过来，可以多转点,暂时定为够转4次的吧
        final BigDecimal GAS_FEE_NEED_BY_SUB = FEE_IN_SUB;
        log.info(String.format("sendMainToken 4 - fee summary - GAS_FEE_NEED_BY_SUB:%s",
                GAS_FEE_NEED_BY_SUB.toPlainString()));

        List<String> addressesFeeNotEnough = new ArrayList<>();

        int count = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (String addr : lsAddr) {
            if (addrMain.equalsIgnoreCase(addr)) {
                continue;
            }
            try {
                // 1. 查看账户中的代币是否 大于 某个设定值，才有必要汇总
                BigDecimal tokenNum = client.getBalanceToken(configParam.contract, addr);
                if (tokenNum.compareTo(MIN_BALANCE) <= 0) {
                    continue;
                }

                // 2. *当前账户*中的eth是否够支付手续费
                BigDecimal ethSub = client.getbalanceValueDecimal(addr);

                log.info(String.format("sendMainToken 5 - current balance - addr:%s, eth:%s, token:%s", addr,
                        ethSub.toPlainString(), tokenNum.toPlainString()));

                if (ethSub.compareTo(FEE_IN_SUB) >= 0) {
                    // 3. 直接转账token到主地址
                    String txFromSub = client.sendtoaddressValueToken(configParam.contract, addr, addrMain, tokenNum,
                            GAS_LIMIT_IN_SUB.longValue());
                    if (!StringUtils.isEmpty(txFromSub)) {
                        ++count;
                        totalAmount = totalAmount.add(tokenNum);
                    }
                    log.info(String.format("sendMainToken 6 - Sub send to Main --- %s, %s, %s", txFromSub,
                            tokenNum.toPlainString(), GAS_LIMIT_IN_SUB.toPlainString()));
                } else {
                    // 手续费不够的情况，需要从主链的主账户转点eth手续费过来
                    // 4. *关联主链的主账户*中的eth是否够支付手续费
                    BigDecimal ethMain = client.getbalanceValueDecimal(addrMain).subtract(FEE_IN_MAIN); // 应该要扣除转账费
                    if (ethMain.compareTo(GAS_FEE_NEED_BY_SUB) < 0) {
                        // 主账户已经空了，所以应该退出去
                        log.warn(String.format("sendMainToken 6.5 - ethMain is not enough"));
                        break;
                    }

                    // 5. 转账eth，从关联的主链的主账户中转到token的子账户
                    String txFromMain = client.sendtoaddressValue(addrMain, addr, GAS_FEE_NEED_BY_SUB,
                            GAS_LIMIT_IN_MAIN.longValue());
                    if (!StringUtils.isEmpty(txFromMain)) {
                        // 说明手续费转成功了，将该地址加入待转list中，后面将取出list，转token
                        addressesFeeNotEnough.add(addr);
                    }

                    log.info(String.format("sendMainToken 7 - Main send fee to Sub --- %s, %s, %s", txFromMain,
                            GAS_FEE_NEED_BY_SUB.toPlainString(), GAS_LIMIT_IN_MAIN.toPlainString()));
                }

                Thread.sleep(2 * 1000);

            } catch (Exception e) {
                log.error(e.getMessage(), e);
                // 报错的话，可能是手续费不够了，或则网络拥堵了，这里还是 break 出去，等哈哈再来，免得耽搁时间
                break;
            }

        }

        // 这些都是没有手续费的，主链主账户转账手续费需要先被打包成功之后，才能汇总token
        // 这是因为transaction在本地客户端发送的时候，客户端需要验证当前账户的余额是否够支付手续费
        if (addressesFeeNotEnough.size() > 0) {
            log.info("sendMainToken 8 - 正在等待手续费转账到子账户...");

            // 这里等待一会儿 5min
            Thread.sleep(5 * 60 * 1000);

            for (String addr : addressesFeeNotEnough) {
                BigDecimal tokenNum = client.getBalanceToken(configParam.contract, addr);
                String txFromSub = client.sendtoaddressValueToken(configParam.contract, addr, addrMain, tokenNum,
                        GAS_LIMIT_IN_SUB.longValue());
                if (!StringUtils.isEmpty(txFromSub)) {
                    ++count;
                    totalAmount = totalAmount.add(tokenNum);
                }
                log.info(String.format("sendMainToken 9 - Sub send to Main --- addr:%s, txid:%s, token:%s, gas:%s",
                        addr, txFromSub, tokenNum.toPlainString(), GAS_LIMIT_IN_SUB.toPlainString()));

                Thread.sleep(2 * 1000);

            }

        }

        log.info("sendMainToken 10 - address count: {}, already count: {} , amount: {}", lsAddr.size(), count,
                totalAmount.toPlainString());
        log.info("sendMainToken=============================================================");
    }
}
