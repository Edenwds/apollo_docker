package com.wds.apollo;

import com.ctrip.framework.apollo.core.enums.Env;

/**
 * @Author: wds
 * @Description:
 * @Date: created in 14:35 2019/11/21
 */
public class Test {
    public static void main(String[] args) {
        CustomMetaServerProvider serverProvider = new CustomMetaServerProvider();
        System.out.println(serverProvider.getMetaServerAddress(Env.LOCAL));
        System.out.println(serverProvider.getMetaServerAddress(Env.DEV));
        System.out.println(serverProvider.getMetaServerAddress(Env.FAT));
        System.out.println(serverProvider.getMetaServerAddress(Env.UAT));
        System.out.println(serverProvider.getMetaServerAddress(Env.PRO));
    }
}
