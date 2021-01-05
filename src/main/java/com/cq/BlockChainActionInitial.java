package com.cq;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.cq.service.EthWalletService;
import com.cq.task.Eth_CreateAddress;
import com.cq.task.Eth_CollectMain;
import com.cq.task.Eth_Obfuscate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by lin on 2020-11-06.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class BlockChainActionInitial implements ApplicationRunner {

    private final EthWalletService ethWalletService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int poolSize = 8;
        try {
            poolSize = Integer.parseInt(args.getOptionValues("threadPoolSize").get(0));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(poolSize);
        threadPoolTaskScheduler.initialize();

        String actionName = args.getOptionValues("name").get(0);
        if (StrUtil.isEmpty(actionName)) {
            throw new RuntimeException("must Set cmd line name");
        }

        List<String> params = args.getOptionValues("params");

        if (actionName.equals(Eth_CreateAddress.class.getSimpleName())) {
            threadPoolTaskScheduler.submit(new Eth_CreateAddress(ethWalletService));
        } else if (actionName.equals(Eth_CollectMain.class.getSimpleName())) {
            if (CollectionUtil.isEmpty(params)) {
                throw new RuntimeException("Eth_CollectMain params is empty. 0 - eth, 1 - token");
            }
            Integer type = Integer.parseInt(params.get(0));
            threadPoolTaskScheduler.submit(new Eth_CollectMain(ethWalletService, type));
        } else if (actionName.equals(Eth_Obfuscate.class.getSimpleName())) {
            threadPoolTaskScheduler.submit(new Eth_Obfuscate(ethWalletService));
        }

        while (true) {
            Thread.sleep(1000);
        }
    }
}
