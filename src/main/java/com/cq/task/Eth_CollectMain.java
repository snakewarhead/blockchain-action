package com.cq.task;

import com.cq.service.EthWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by lin on 2019/8/20.
 */
@Slf4j
@RequiredArgsConstructor
public class Eth_CollectMain implements Runnable {

    private final EthWalletService ethWalletService;
    private final Integer type;

    @Override
    public void run() {
        log.info("------------------------------------------------------------------------ begin");

        ethWalletService.collectMain(type);

        log.info("------------------------------------------------------------------------ end");
    }
}
