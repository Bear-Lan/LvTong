package com.lvtong.LvTongTransportDept.config;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 创建分页拦截器
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);

        // 2. 设置最大单页限制（可选，防止恶意查询导致内存溢出）
        paginationInterceptor.setMaxLimit(500L);

        // 3. 将分页插件添加到主拦截器中
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}