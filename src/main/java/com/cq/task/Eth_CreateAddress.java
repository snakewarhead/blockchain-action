package com.cq.task;

import com.cq.service.EthWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Created by lin on 2019/8/20.
 */
@Slf4j
@RequiredArgsConstructor
public class Eth_CreateAddress implements Runnable {

    private final EthWalletService ethWalletService;

    @Override
    public void run() {
        log.info("------------------------------------------------------------------------ begin");

        try {
            ethWalletService.createNewAddressInConfig();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        log.info("------------------------------------------------------------------------ end");
    }
}
