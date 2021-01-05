package com.cq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;


@Component
public class ConfigParam {

    @Value("${scanner.contract}")
    public String contract;

    @Value("${scanner.decimals}")
    public int decimals;

    @Value("${scanner.apikey}")
    public String apikey;

    @Value("${scanner.wallet.ip}")
    public String walletip;

    @Value("${scanner.wallet.port}")
    public String walletport;

    @Value("${scanner.wallet.password}")
    public String walletpassword;

    @Value("${scanner.wallet.mainaddr}")
    public String walletmainaddr;

    @Value("${scanner.startheight}")
    public long startheight;

    @Value("${scanner.contract_event_hash}")
    public String contract_event_hash;

    @Value("${scanner.geth_path}")
    public String geth_path;

    @Value("${scanner.keystore_path}")
    public String keystore_path;

    @Value("${scanner.keystore_backup_path}")
    public String keystore_backup_path;

    @Value("${scanner.keystore_check_path}")
    public String keystore_check_path;

    @Value("${obfuscate.max_loops}")
    public int obfuscate_max_loops;

    @Value("${obfuscate.interval}")
    public int obfuscate_interval;

    @Value("${obfuscate.max_addrass}")
    public int obfuscate_max_addrass;

    @Value("${scanner.omni_id}")
    public int scanner_omni_id;

    @Value("${scanner.omni_currency_id}")
    public String scanner_omni_currency_id;

    @Value("${scanner.count_new_addresses}")
    public int scanner_count_new_addresses;

    @Value("${collect.min_eth}")
    public BigDecimal collect_min_eth;

    @Value("${collect.min_token}")
    public BigDecimal collect_min_token;

    @Value("${scanner.eth.gaslimit}")
    public long eth_gas_limit;

    @Value("${scanner.contract.gaslimit}")
    public long contract_gas_limit;
}