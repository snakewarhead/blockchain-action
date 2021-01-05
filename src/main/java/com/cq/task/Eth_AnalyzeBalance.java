package com.cq.task;

import cn.hutool.core.thread.ThreadUtil;
import com.cq.service.EthWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by lin on 2021-01-05.
 */
@Slf4j
@RequiredArgsConstructor
public class Eth_AnalyzeBalance implements Runnable {

    private final EthWalletService ethWalletService;

    @Override
    public void run() {
        log.info("------------------------------------------------------------------------ begin");

        while (true) {
            ethWalletService.sumWalletTokenInNodeEthWithConfig();
            ThreadUtil.sleep(60 * 10 * 1000);
        }
    }
}
