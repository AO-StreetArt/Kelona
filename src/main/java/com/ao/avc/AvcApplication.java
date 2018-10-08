/*
Apache2 License Notice
Copyright 2018 Alex Barry
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.ao.avc;

import com.ao.avc.AvcMongoConfiguration;

import com.ao.avc.auth.AvcBasicAuthEntryPoint;
import com.ao.avc.auth.BasicCredentials;
import com.ao.avc.auth.PasswordCredentials;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableAutoConfiguration
@EnableDiscoveryClient
@EnableRetry
@EnableWebSecurity
@Import(AvcMongoConfiguration.class)
@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
public class AvcApplication extends WebSecurityConfigurerAdapter {

  // Is Authentication required for accessing our HTTP Server
  @Value("${server.auth.active:false}")
  private boolean httpAuthActive;

  // Username for the HTTP Server Authentication
  @Value("${server.auth.username:kelona}")
  private String httpUsername;

  // Password for the HTTP Server Authentication
  @Value("${server.auth.password:kelona}")
  private String httpPassword;

  // -------- Security Configuration ---------

  // Security Realm
  private static String REALM="AVC_REALM";

  // Configure Basic Auth
  @Autowired
  public void configureGlobalSecurity(AuthenticationManagerBuilder auth) throws Exception {
    if (httpAuthActive) {
      auth.inMemoryAuthentication().withUser(httpUsername).password(httpPassword).roles("USER");
    }
  }

  // Set up security filters
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    if (httpAuthActive) {
      http.csrf().disable()
          .authorizeRequests()
          .and().httpBasic().realmName(REALM).authenticationEntryPoint(getBasicAuthEntryPoint())
          .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    } else {
      // Security Disabled
      http.csrf().disable()
          .authorizeRequests().anyRequest().permitAll();
    }
  }

  // Set entrypoint for Authentication Failures
  @Bean
  public AvcBasicAuthEntryPoint getBasicAuthEntryPoint(){
      return new AvcBasicAuthEntryPoint();
  }

  // Allow Pre-flight [OPTIONS] request from browser
  @Override
  public void configure(WebSecurity web) throws Exception {
    if (httpAuthActive) {
      web.ignoring().antMatchers(HttpMethod.OPTIONS, "/**");
    }
  }

  // ---------- Main App -------------

  // Run the main application
  public static void main(String[] args) {
    SpringApplication.run(AvcApplication.class, args);
  }
}
