package com.cq.task;

import com.cq.service.EthWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * Created by lin on 2019/8/20.
 */
@Slf4j
@RequiredArgsConstructor
public class Eth_Transfer implements Runnable {

    private final EthWalletService ethWalletService;
    private final Object[] params;

    @Override
    public void run() {
        log.info("------------------------------------------------------------------------ begin");

        try {
            ethWalletService.send((String) params[0], (String) params[1], BigDecimal.valueOf((double) params[2]), (int) params[3] != 0);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        log.info("------------------------------------------------------------------------ end");
    }
}
