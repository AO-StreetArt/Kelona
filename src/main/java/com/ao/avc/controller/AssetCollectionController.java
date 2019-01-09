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

package com.ao.avc.controller;

import com.ao.avc.dao.AssetCollectionRepository;
import com.ao.avc.model.AssetCollection;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bson.Document;
import org.bson.types.ObjectId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
* Rest Controller defining the Asset Collection API.
* Responsible for handling and responding to all Asset Collection API Requests.
*/
@Controller
public class AssetCollectionController {

  // Spring Data Mongo Repository allowing access to standard Mongo operations
  @Autowired
  AssetCollectionRepository assetCollections;

  // Asset Controller Logger
  private static final Logger logger =
      LogManager.getLogger("avc.AssetController");

  @Autowired
  MongoDatabase mongoDb;
  MongoCollection<Document> mongoCollection = null;
  private String mongoCollectionName = "assetcollections";

  /**
  * Use the Mongo Client to access the database and collection.
  */
  @PostConstruct
  public void init() {
    mongoCollection = mongoDb.getCollection(mongoCollectionName);
  }

  /**
  * Get an Asset Collection.
  */
  @GetMapping("/v1/collection/{id}")
  @ResponseBody
  public ResponseEntity<AssetCollection> getCollection(@PathVariable String id,
      @RequestHeader(name="X-Aesel-Principal", defaultValue="") String aeselPrincipal) {
    logger.info("Responding to Asset Collection Get Request");
    HttpStatus returnCode = HttpStatus.OK;
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");
    Optional<AssetCollection> existingCollection = null;
    if (aeselPrincipal.isEmpty()) {
      existingCollection = assetCollections.findById(id);
    } else {
      existingCollection = assetCollections.findPublicOrPrivateById(id, aeselPrincipal);
    }
    AssetCollection returnCollection = null;
    if (existingCollection.isPresent()) {
      returnCollection = existingCollection.get();
    } else {
      returnCode = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
      logger.debug("No Asset Collection found");
      returnCollection = new AssetCollection();
    }

    // Create and return the new HTTP Response
    return new ResponseEntity<AssetCollection>(returnCollection, responseHeaders, returnCode);
  }

  /**
  * Query for Asset Collections.
  */
  @GetMapping("/v1/collection")
  @ResponseBody
  public ResponseEntity<List<AssetCollection>> findCollections(
      @RequestParam(value = "name", defaultValue = "") String name,
      @RequestParam(value = "category", defaultValue = "") String category,
      @RequestParam(value = "tag", defaultValue = "") String tag,
      @RequestParam(value = "num_records", defaultValue = "10") int recordsInPage,
      @RequestParam(value = "page", defaultValue = "0") int pageNum,
      @RequestHeader(name="X-Aesel-Principal", defaultValue="") String aeselPrincipal) {
    logger.info("Responding to Asset Collection Get Request");
    HttpStatus returnCode = HttpStatus.OK;
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");
    List<AssetCollection> returnCollections = null;
    Pageable pageable = new PageRequest(pageNum, recordsInPage);

    // Construct non-user based queries
    if (aeselPrincipal.isEmpty()) {
      if (!name.equals("")) {
        returnCollections = assetCollections.findByName(name, pageable);
      } else if (!category.equals("") && !tag.equals("")) {
        returnCollections = assetCollections.findByCategoryAndTagsIn(category,
                                                                     new HashSet<String>(Arrays.asList(tag)),
                                                                     pageable);
      } else if (!category.equals("")) {
        returnCollections = assetCollections.findByCategory(category, pageable);
      } else if (!tag.equals("")) {
        returnCollections = assetCollections.findByTagsIn(new HashSet<String>(Arrays.asList(tag)),
                                                          pageable);
      } else {
        returnCollections = assetCollections.findAll(pageable).getContent();
      }

    // Construct user-based queries
    } else {
      logger.debug("Asset Collection Query User: {}", aeselPrincipal);
      if (!name.equals("")) {
        returnCollections = assetCollections.findPublicOrPrivateByName(name, aeselPrincipal, pageable);
      } else if (!category.equals("") && !tag.equals("")) {
        returnCollections = assetCollections.findPublicOrPrivateByCategoryAndTagsIn(category,
                                                                     new HashSet<String>(Arrays.asList(tag)),
                                                                     aeselPrincipal,
                                                                     pageable);
      } else if (!category.equals("")) {
        returnCollections = assetCollections.findPublicOrPrivateByCategory(category, aeselPrincipal, pageable);
      } else if (!tag.equals("")) {
        returnCollections = assetCollections.findPublicOrPrivateByTagsIn(new HashSet<String>(Arrays.asList(tag)),
                                                                         aeselPrincipal, pageable);
      } else {
        returnCollections = assetCollections.findPublicOrPrivate(aeselPrincipal, pageable);
      }
    }
    if (returnCollections.size() == 0 && returnCode == HttpStatus.OK) {
      returnCode = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
      logger.debug("No Asset Collection found");
      returnCollections = new ArrayList<AssetCollection>();
    }

    // Create and return the new HTTP Response
    return new ResponseEntity<List<AssetCollection>>(returnCollections,
                                                     responseHeaders,
                                                     returnCode);
  }

  /**
  * Delete an Asset Collection.
  */
  @DeleteMapping("/v1/collection/{id}")
  @ResponseBody public ResponseEntity<String> deleteCollection(
      @PathVariable String id) {
    logger.info("Responding to Asset Collection Delete Request");
    HttpStatus returnCode = HttpStatus.OK;
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");
    assetCollections.deleteById(id);
    return new ResponseEntity<String>(id, responseHeaders, returnCode);
  }

  /**
   * Create a new Asset Collection.
   */
  @PostMapping("/v1/collection")
  @ResponseBody
  public ResponseEntity<AssetCollection> createCollection(
      @RequestBody AssetCollection inpCollection,
      @RequestHeader(name="X-Aesel-Principal", defaultValue="") String aeselPrincipal) {
    logger.info("Responding to Asset Collection Create Request");
    HttpStatus returnCode = HttpStatus.OK;
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");
    inpCollection.setUser(aeselPrincipal);
    AssetCollection responseCollection = assetCollections.insert(inpCollection);
    return new ResponseEntity<AssetCollection>(responseCollection, responseHeaders, returnCode);
  }

  private BasicDBObject genIdQuery(String id, String aeselPrincipal) {
    // Create the ID section of the query
    BasicDBObject innerIdQuery = new BasicDBObject();
    innerIdQuery.put("_id", new ObjectId(id));

    // Start the array of query objects, and add the ID query to it
    ArrayList<BasicDBObject> queryObjectList = new ArrayList<BasicDBObject>();
    queryObjectList.add(innerIdQuery);

    // If X-Aesel-Principal header is present on request, then activate
    // public and private projects for individual users.
    if (!(aeselPrincipal.equals(""))) {
      BasicDBObject innerUserQuery = new BasicDBObject();
      innerUserQuery.put("user", aeselPrincipal);
      BasicDBObject innerPublicQuery = new BasicDBObject();
      innerPublicQuery.put("isPublic", true);
      ArrayList<BasicDBObject> innerQueryList = new ArrayList<BasicDBObject>();
      innerQueryList.add(innerUserQuery);
      innerQueryList.add(innerPublicQuery);
      BasicDBObject innerOrQuery = new BasicDBObject();
      innerOrQuery.put("$or", innerQueryList);
      queryObjectList.add(innerOrQuery);
    }

    BasicDBObject outerQuery = new BasicDBObject();
    outerQuery.put("$and", queryObjectList);
    return outerQuery;
  }

  /**
   * Update an existing Asset Collection.
   */
  @PostMapping("/v1/collection/{id}")
  @ResponseBody
  public ResponseEntity<String> updateCollection(
      @PathVariable String id,
      @RequestBody AssetCollection inpCollection,
      @RequestHeader(name="X-Aesel-Principal", defaultValue="") String aeselPrincipal) {
    logger.info("Responding to Asset Collection Update Request");
    BasicDBObject updateQuery = new BasicDBObject();
    if (inpCollection.getName() != null && !(inpCollection.getName().isEmpty())) {
      updateQuery.put("name", inpCollection.getName());
    }
    if (inpCollection.getDescription() != null && !(inpCollection.getDescription().isEmpty())) {
      updateQuery.put("description", inpCollection.getDescription());
    }
    if (inpCollection.getCategory() != null && !(inpCollection.getCategory().isEmpty())) {
      updateQuery.put("category", inpCollection.getCategory());
    }
    if (inpCollection.getThumbnail() != null && !(inpCollection.getThumbnail().isEmpty())) {
      updateQuery.put("thumbnail", inpCollection.getThumbnail());
    }

    UpdateResult result = mongoCollection.updateOne(genIdQuery(id, aeselPrincipal),
        new BasicDBObject("$set", updateQuery), new UpdateOptions());

    // Set the http response code
    HttpStatus returnCode = HttpStatus.OK;
    if (result.getModifiedCount() < 1) {
      returnCode = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
      logger.debug("No documents modified for array attribute update");
    }
    HttpHeaders responseHeaders = new HttpHeaders();
    return new ResponseEntity<String>("", responseHeaders, returnCode);
  }

  private BasicDBObject genUpdateQuery(String attrKey, String attrVal, String opType) {
    BasicDBObject update = new BasicDBObject();
    update.put(attrKey, attrVal);
    return new BasicDBObject(opType, update);
  }

  private ResponseEntity<String> updateArrayAttr(String projectKey,
      String attrKey, String attrVal, String updType, String aeselPrincipal) {
    BasicDBObject updateQuery = genUpdateQuery(attrKey, attrVal, updType);
    BasicDBObject query = genIdQuery(projectKey, aeselPrincipal);
    UpdateResult result = mongoCollection.updateOne(query, updateQuery, new UpdateOptions());
    // Set the http response code
    HttpStatus returnCode = HttpStatus.OK;
    if (result.getModifiedCount() < 1) {
      returnCode = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
      logger.debug("No documents modified for array attribute update");
    }
    // Set up a response header to return a valid HTTP Response
    HttpHeaders responseHeaders = new HttpHeaders();
    return new ResponseEntity<String>("", responseHeaders, returnCode);
  }

  /**
   * Add a tag to an existing Project.
   */
  @PutMapping("/v1/collection/{collKey}/tags/{tagValue}")
  @ResponseBody
  public ResponseEntity<String> addTagToCollection(
      @PathVariable String collKey,
      @PathVariable String tagValue,
      @RequestHeader(name="X-Aesel-Principal", defaultValue="") String aeselPrincipal) {
    logger.info("Adding tag to Collection");
    return updateArrayAttr(collKey, "tags", tagValue, "$push", aeselPrincipal);
  }

  /**
   * Remove a tag from an existing Project.
   */
  @DeleteMapping("/v1/collection/{collKey}/tags/{tagValue}")
  @ResponseBody
  public ResponseEntity<String> removeTagFromCollection(
      @PathVariable String collKey,
      @PathVariable String tagValue,
      @RequestHeader(name="X-Aesel-Principal", defaultValue="") String aeselPrincipal) {
    logger.info("Removing tag from Collection");
    return updateArrayAttr(collKey, "tags", tagValue, "$pull", aeselPrincipal);
  }

}
