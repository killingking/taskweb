//package com.sd.task.config;
//
//import org.mybatis.spring.boot.autoconfigure.MybatisLanguageDriverAutoConfiguration;
//import org.springframework.boot.autoconfigure.AutoConfigureAfter;
//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//
//@EnableConfigurationProperties(MybatisProperties.class) ： MyBatis 配置项绑定类。
//@AutoConfigureAfter({ DataSourceAutoConfiguration.class, MybatisLanguageDriverAutoConfiguration.class })
//public class MybatisAutoConfiguration {}
//
//@ConfigurationProperties(prefix = "mybatis")
//public class MybatisProperties