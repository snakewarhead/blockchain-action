package com.cq.task;

import com.cq.service.EthWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by lin on 2020-12-31.
 */
@Slf4j
@RequiredArgsConstructor
public class Eth_Obfuscate implements Runnable {

    private final EthWalletService ethWalletService;

    @Override
    public void run() {
        log.info("------------------------------------------------------------------------ begin");

        ethWalletService.obfuscateFlow();

        log.info("------------------------------------------------------------------------ end");
    }
}
