package com.lvtong.LvTongTransportDept;

import com.lvtong.LvTongTransportDept.service.impl.HikPlayBackServiceImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.lvtong.LvTongTransportDept.mapper")
@EnableConfigurationProperties({HikPlayBackServiceImpl.HikNvrConfig.class, HikPlayBackServiceImpl.HikFfmpegConfig.class})
public class LvTongTransportDeptApplication {

	public static void main(String[] args) {
		SpringApplication.run(LvTongTransportDeptApplication.class, args);
	}
}
