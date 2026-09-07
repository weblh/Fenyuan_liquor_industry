package com.fenyuan.liquor.modules.kingdee.service;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * 金蝶云星空 HTTPS 客户端。部分环境证书链 Issuer DN 为空，JDK/Hutool 校验会抛
 * {@code SSLProtocolException: Empty issuer DN not allowed in X509Certificates}，
 * 此处对金蝶域名统一信任证书（仅用于对接金蝶 API）。
 */
public final class KingdeeHttpSupport {

    private static final Logger log = LoggerFactory.getLogger(KingdeeHttpSupport.class);

    private KingdeeHttpSupport() {
    }

    public static OkHttpClient newClient(long connectSec, long readSec, long writeSec) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(connectSec, TimeUnit.SECONDS)
                .readTimeout(readSec, TimeUnit.SECONDS)
                .writeTimeout(writeSec, TimeUnit.SECONDS);
        try {
            X509TrustManager trustManager = trustAllManager();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            HostnameVerifier hostnameVerifier = (hostname, session) -> true;
            builder.sslSocketFactory(sslSocketFactory, trustManager)
                    .hostnameVerifier(hostnameVerifier);
        } catch (Exception e) {
            log.warn("初始化金蝶信任证书 SSL 失败，回退默认校验：{}", e.getMessage());
        }
        return builder.build();
    }

    private static X509TrustManager trustAllManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // trust all for Kingdee API
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // trust all for Kingdee API
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
}
