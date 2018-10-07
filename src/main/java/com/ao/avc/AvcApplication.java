package com.ao.avc;

import com.ao.avc.auth.BasicCredentials;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.vault.config.EnvironmentVaultConfiguration;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultResponseSupport;

@EnableDiscoveryClient
@Configuration
@EnableRetry
@EnableAutoConfiguration
@Import(EnvironmentVaultConfiguration.class)
@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
public class AvcApplication extends AbstractMongoConfiguration {

  // Hostname of Mongo Connection
  @Value("${server.mongo.hosts:localhost}")
  private String mongoHosts;

  // Port of Mongo Connection
  @Value("${server.mongo.port:27017}")
  private int mongoPort;

  // Is Authentication Active in the Mongo Connection
  @Value("${server.mongo.auth.active:false}")
  private boolean mongoAuthActive;

  // Is Vault Authentication Loading Active
  // If true, we'll load Mongo Auth info from Vault prior to connecting
  @Value("${server.mongo.auth.vault.active:false}")
  private boolean mongoVaultAuthActive;

  // Username of the Mongo Connection
  @Value("${server.mongo.auth.username:mongo}")
  private String mongoUsername;

  // Password of the Mongo Connection
  @Value("${server.mongo.auth.password:mongo}")
  private String mongoPassword;

  @Autowired
  private VaultOperations operations;

  @Bean
  public GridFsTemplate gridFsTemplate() throws Exception {
    return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter());
  }

  @Override
  public MongoClient mongoClient() {
    // Setup the list of Mongo Addresses
    List<ServerAddress> mongoAdressList = new ArrayList<ServerAddress>();
    String[] addressArray = mongoHosts.split(",");
    for (String address : addressArray) {
      mongoAdressList.add(new ServerAddress(address, mongoPort));
    }

    // Pull authentication information
    if (mongoAuthActive) {
      BasicCredentials mongoCreds;
      if (mongoVaultAuthActive) {
        VaultResponseSupport<BasicCredentials> response =
            operations.read("AVC_MONGO_CREDENTIALS", BasicCredentials.class);
        mongoCreds = response.getData();
      } else {
        mongoCreds = new BasicCredentials();
        mongoCreds.setUsername(mongoUsername);
        mongoCreds.setPassword(mongoPassword);
      }

      List<MongoCredential> mongoCredsList = new ArrayList<MongoCredential>();
      mongoCredsList.add(MongoCredential.createCredential(mongoCreds.getUsername(), "_avc", mongoCreds.getPassword().toCharArray()));

      // Return a DB Client with Authentication
      return new MongoClient(mongoAdressList, mongoCredsList);
    }

    // Return a DB Client without Authentication
    return new MongoClient(mongoAdressList);
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
