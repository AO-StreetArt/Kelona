package com.ao.avc;

import com.ao.avc.auth.AvcBasicAuthEntryPoint;
import com.ao.avc.auth.BasicCredentials;
import com.ao.avc.auth.PasswordCredentials;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.vault.config.EnvironmentVaultConfiguration;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultResponseSupport;

@Configuration
public class AvcMongoConfiguration extends AbstractMongoConfiguration {

  // Hostname of Mongo Connection
  @Value("${mongo.hosts:localhost}")
  private String mongoHosts;

  // Port of Mongo Connection
  @Value("${mongo.port:27017}")
  private int mongoPort;

  // Is Authentication Active in the Mongo Connection
  @Value("${mongo.auth.active:false}")
  private boolean mongoAuthActive;

  // Username of the Mongo Connection
  @Value("${mongo.auth.username:mongo}")
  private String mongoUsername;

  // Password of the Mongo Connection
  @Value("${mongo.auth.password:mongo}")
  private String mongoPassword;

  // Is Mongo SSL Connectivity Enabled
  @Value("${mongo.ssl.enabled:false}")
  private boolean mongoSslEnabled;

  // Trust Store Path
  @Value("${ssl.trustStore.path:}")
  private String mongoTrustStore;

  // Trust Store Password
  @Value("${ssl.trustStore.password:}")
  private String mongoTrustStorePw;

  // Keystore Path
  @Value("${ssl.keyStore.path:}")
  private String mongoKeyStore;

  // Keystore Password
  @Value("${ssl.keyStore.password:}")
  private String mongoKeyStorePw;

  // Is Vault Authentication Loading Active
  // If true, we'll load Mongo Auth info from Vault prior to connecting
  @Value("${vault.active:false}")
  private boolean vaultActive;

  // Vault Connection
  @Autowired
  private VaultOperations operations;

  // -------- Mongo Configuration -----------

  // Connect to Mongo, potentially authenticated
  @Override
  public MongoClient mongoClient() {
    // Build Mongo Connection Ops
    // See if we need to pull SSL Certificate credentials from Vault
    if (vaultActive) {
      PasswordCredentials sslKeyCreds;
      PasswordCredentials sslTrustCreds;
      VaultResponseSupport<PasswordCredentials> keyResponse =
          operations.read("AVC_SSL_KEY_CREDENTIALS", PasswordCredentials.class);
      sslKeyCreds = keyResponse.getData();
      mongoKeyStorePw = sslKeyCreds.getPassword();
      VaultResponseSupport<PasswordCredentials> trustResponse =
          operations.read("AVC_SSL_TRUST_CREDENTIALS", PasswordCredentials.class);
      sslTrustCreds = trustResponse.getData();
      mongoTrustStorePw = sslTrustCreds.getPassword();
    }

    // Build out the Mongo SSL Options
    MongoClientOptions options;
    MongoClientOptions.Builder builder = MongoClientOptions.builder();
    if (mongoSslEnabled) {
      if (!(mongoTrustStore.isEmpty())) {
        System.setProperty("javax.net.ssl.trustStore", mongoTrustStore);
      }
      if (!(mongoTrustStorePw.isEmpty())) {
        System.setProperty("javax.net.ssl.trustStorePassword", mongoTrustStorePw);
      }
      if (!(mongoKeyStore.isEmpty())) {
        System.setProperty("javax.net.ssl.keyStore", mongoKeyStore);
      }
      if (!(mongoKeyStorePw.isEmpty())) {
        System.setProperty("javax.net.ssl.keyStorePassword", mongoKeyStorePw);
      }
      options=builder.sslEnabled(true).build();
    } else {
      options=builder.build();
    }

    // Setup the list of Mongo Addresses
    List<ServerAddress> mongoAdressList = new ArrayList<ServerAddress>();
    String[] addressArray = mongoHosts.split(",");
    for (String address : addressArray) {
      mongoAdressList.add(new ServerAddress(address, mongoPort));
    }

    // Pull authentication information
    if (mongoAuthActive) {
      BasicCredentials mongoCreds;
      if (vaultActive) {
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
      return new MongoClient(mongoAdressList, mongoCredsList, options);
    }

    // Return a DB Client without Authentication
    return new MongoClient(mongoAdressList, options);
  }

  // Define Mongo Database name
  @Override
  protected String getDatabaseName() {
    return "_avc";
  }

  // Definitions for GridFS
  @Bean
  public GridFsTemplate gridFsTemplate() throws Exception {
    return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter());
  }

  @Bean public GridFSBucket getGridFSBuckets() {
    MongoDatabase db = mongoDbFactory().getDb();
    return GridFSBuckets.create(db);
  }
}
