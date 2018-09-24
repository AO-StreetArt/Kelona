package com.ao.avc;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSBucket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

@EnableDiscoveryClient
@Configuration
@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
public class AvcApplication extends AbstractMongoConfiguration {

  // Hostname of Mongo Connection
  @Value("${server.mongo.host}")
  private String mongoHost;

  // Hostname of Mongo Port
  @Value("${server.mongo.port}")
  private int mongoPort;

  @Bean
  public GridFsTemplate gridFsTemplate() throws Exception {
    return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter());
  }

  @Override
  public MongoClient mongoClient() {
    return new MongoClient(mongoHost, mongoPort);
  }

  @Override
  protected String getDatabaseName() {
    return "_avc";
  }

  @Bean public GridFSBucket getGridFSBuckets() {
    MongoDatabase db = mongoDbFactory().getDb();
    return GridFSBuckets.create(db);
  }

  public static void main(String[] args) {
    SpringApplication.run(AvcApplication.class, args);
  }
}
