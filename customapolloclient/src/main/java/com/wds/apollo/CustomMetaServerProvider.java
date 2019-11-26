package com.wds.apollo;

import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.spi.MetaServerProvider;
import com.ctrip.framework.apollo.core.spi.Ordered;
import com.ctrip.framework.apollo.core.utils.ResourceUtils;
import com.google.common.base.Strings;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wds
 * @Description:
 * @Date: created in 11:55 2019/11/21
 */
public class CustomMetaServerProvider implements MetaServerProvider {

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE;
    private static final Map<Env, String> ENVMAP = new ConcurrentHashMap<>(16);

    public CustomMetaServerProvider() {
        initProperties();
    }

    public void initProperties() {
        Properties properties = ResourceUtils.readConfigFile("apollo-env.properties", null);
        properties.entrySet().forEach(entry -> {
            String envStr = (String) entry.getKey();
            String serverUrl = (String) entry.getValue();
            Env env = Env.fromString(envStr.substring(0, envStr.indexOf("_")));
            ENVMAP.put(env, serverUrl);
        });
    }

    @Override
    public String getMetaServerAddress(Env env) {
        String url = System.getProperty("apollo.meta");
        if (!Strings.isNullOrEmpty(url)) {
            return url;
        }
        url = getServerAddress(env);
        return url;
    }

    private String getServerAddress(Env env) {
        return ENVMAP.get(env);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
