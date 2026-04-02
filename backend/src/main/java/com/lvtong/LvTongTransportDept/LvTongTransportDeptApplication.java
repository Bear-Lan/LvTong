package com.lvtong.LvTongTransportDept;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lvtong.LvTongTransportDept.mapper")
public class LvTongTransportDeptApplication {

	public static void main(String[] args) {
		SpringApplication.run(LvTongTransportDeptApplication.class, args);
	}
}
