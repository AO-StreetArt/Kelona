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

import com.ao.avc.dao.AssetHistoryRepository;
import com.ao.avc.dao.AssetRelationshipRepository;
import com.ao.avc.model.AssetHistory;
import com.ao.avc.model.AssetMetadata;
import com.ao.avc.model.AssetRelationship;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Sorts;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Hex;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bson.Document;
import org.bson.types.ObjectId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.vault.core.VaultOperations;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
* Rest Controller defining the Asset Metadata API.
* Uses the base Mongo driver for Java to connect in order
* to provide full querying capacity against the underlying
* mongo collections.
*/
@Controller
public class AssetMetadataController {

  // Spring-Data Object allowing access to Mongo GridFS
  @Autowired
  GridFsTemplate gridFsTemplate;

  // Object Controller Logger
  private static final Logger logger =
      LogManager.getLogger("avc.AssetController");

  @Value(value = "${mongo.metadata.collection:fs.files}")
  private String mongoCollectionName;

  @Autowired
  MongoDatabase mongoDb;
  MongoCollection<Document> mongoCollection = null;

  /**
  * Use the Mongo Client to access the database and collection.
  */
  @PostConstruct
  public void init() {
    mongoCollection = mongoDb.getCollection(mongoCollectionName);
  }

  /**
   * Count assets
   */
  @GetMapping("/v1/asset/count")
  @ResponseBody
  public ResponseEntity<String> countAssets(
      @RequestParam(value = "content-type", defaultValue = "") String contentType,
      @RequestParam(value = "file-type", defaultValue = "") String fileType,
      @RequestParam(value = "asset-type", defaultValue = "") String assetType,
      @RequestParam(value = "name", defaultValue = "") String assetName,
      @RequestParam(value = "description", defaultValue = "") String assetDesc) {
    logger.info("Responding to Asset Count Request");
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");

    // Run the Mongo Query
    BasicDBObject query = new BasicDBObject();
    if (!(contentType.isEmpty())) {
      query.put("metadata.content-type", contentType);
    } else if (!(fileType.isEmpty())) {
      query.put("metadata.file-type", fileType);
    } else if (!(assetType.isEmpty())) {
      query.put("metadata.asset-type", assetType);
    } else if (!(assetName.isEmpty())) {
      query.put("metadata.name", assetName);
    } else if (!(assetDesc.isEmpty())) {
      query.put("metadata.description", assetDesc);
    }
    long assetCount = mongoCollection.count(query);

    // Setup the response
    String returnString = "{\"count\":" + String.valueOf(assetCount) + "}";
    return new ResponseEntity<String>(returnString, responseHeaders, HttpStatus.OK);
  }

  /**
  * Find Assets by metadata.
  */
  @GetMapping("/v1/asset")
  @ResponseBody
  public ResponseEntity<List<AssetMetadata>> findAssets(
      @RequestParam(value = "content-type", defaultValue = "") String contentType,
      @RequestParam(value = "file-type", defaultValue = "") String fileType,
      @RequestParam(value = "asset-type", defaultValue = "") String assetType,
      @RequestParam(value = "key", defaultValue = "") String assetKey,
      @RequestParam(value = "name", defaultValue = "") String assetName,
      @RequestParam(value = "limit", defaultValue = "100") int queryLimit,
      @RequestParam(value = "offset", defaultValue = "0") int queryOffset)
      throws MalformedURLException, IOException {
    logger.info("Responding to Asset Find Request");
    HttpHeaders responseHeaders = new HttpHeaders();
    GridFSFindIterable gridFsdbFiles;
    FindIterable<Document> resultDocs;
    try {

      // Run the Mongo Query
      BasicDBObject query = new BasicDBObject();
      if (!(contentType.isEmpty())) {
        query.put("metadata.content-type", contentType);
      } else if (!(fileType.isEmpty())) {
        query.put("metadata.file-type", fileType);
      } else if (!(assetType.isEmpty())) {
        query.put("metadata.asset-type", assetType);
      } else if (!(assetName.isEmpty())) {
        query.put("metadata.name", assetName);
      } else if (!(assetKey.isEmpty())) {
        query.put("_id", new ObjectId(assetKey));
      }
      resultDocs = mongoCollection.find(query)
                                  .sort(Sorts.ascending("_id"))
                                  .skip(queryOffset)
                                  .limit(queryLimit);

    // Error Handling
    } catch (Exception e) {
      logger.error("Error Retrieving Asset from Mongo: ", e);
      return new ResponseEntity<List<AssetMetadata>>(
          new ArrayList<AssetMetadata>(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (resultDocs == null) {
      logger.error("Null Asset Retrieved from Mongo");
      return new ResponseEntity<List<AssetMetadata>>(
          new ArrayList<AssetMetadata>(), responseHeaders, HttpStatus.NO_CONTENT);
    }

    // Load asset metadata into the return list
    List<AssetMetadata> returnList = new ArrayList<AssetMetadata>();
    MongoCursor<Document> returnCursor = resultDocs.iterator();
    while (returnCursor.hasNext()) {
      Document dbDoc = returnCursor.next();
      Document defaultDoc = new Document();
      Document metaDoc = dbDoc.get("metadata", defaultDoc);
      AssetMetadata returnDoc = new AssetMetadata();
      logger.debug("Metadata returned from Query: " + metaDoc.toString());
      returnDoc.setKey(Hex.encodeHexString(dbDoc.getObjectId("_id").toByteArray()));
      returnDoc.setContentType(metaDoc.getString("content-type"));
      returnDoc.setName(metaDoc.getString("name"));
      returnDoc.setDescription(metaDoc.getString("description"));
      returnDoc.setFileType(metaDoc.getString("file-type"));
      returnDoc.setAssetType(metaDoc.getString("asset-type"));
      returnDoc.setCreatedTimestamp(metaDoc.getString("created-dttm"));
      returnList.add(returnDoc);
    }
    responseHeaders.set("Content-Type", "application/json");
    return new ResponseEntity<List<AssetMetadata>>(returnList, responseHeaders, HttpStatus.OK);
  }

}
