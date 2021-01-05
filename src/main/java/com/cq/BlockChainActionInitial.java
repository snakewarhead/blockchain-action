package com.cq;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.cq.service.EthWalletService;
import com.cq.task.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Future;

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
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.initialize();

        String actionName = args.getOptionValues("name").get(0);
        if (StrUtil.isEmpty(actionName)) {
            throw new RuntimeException("must Set cmd line name");
        }

        List<String> params = args.getOptionValues("params");

        if (actionName.equals(Eth_CreateAddress.class.getSimpleName())) {
            scheduler.submit(new Eth_CreateAddress(ethWalletService));
        } else if (actionName.equals(Eth_AnalyzeBalance.class.getSimpleName())) {
            scheduler.submit(new Eth_AnalyzeBalance(ethWalletService));
        } else if (actionName.equals(Eth_Transfer.class.getSimpleName())) {
            if (CollectionUtil.isEmpty(params)) {
                throw new RuntimeException("Eth_Transfer params is empty. --name=Eth_Transfer --params=\"[from, to, amount, isContract]\" (0 - eth, 1 - contract)");
            }
            JSONArray ja = JSONUtil.parseArray(params.get(0));
            scheduler.submit(new Eth_Transfer(ethWalletService, ja.toArray()));
        } else if (actionName.equals(Eth_CollectMain.class.getSimpleName())) {
            if (CollectionUtil.isEmpty(params)) {
                throw new RuntimeException("Eth_CollectMain params is empty. 0 - eth, 1 - token");
            }
            Integer type = Integer.parseInt(params.get(0));
            scheduler.submit(new Eth_CollectMain(ethWalletService, type));
        } else if (actionName.equals(Eth_Obfuscate.class.getSimpleName())) {
            scheduler.submit(new Eth_Obfuscate(ethWalletService));
        }

    }
}
