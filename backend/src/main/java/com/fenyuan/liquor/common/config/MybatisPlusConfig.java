package com.fenyuan.liquor.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        DbType dbType = "prod".equalsIgnoreCase(activeProfile) ? DbType.MYSQL : DbType.H2;
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        return interceptor;
    }
}
