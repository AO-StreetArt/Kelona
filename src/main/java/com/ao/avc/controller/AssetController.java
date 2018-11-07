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
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bson.Document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
* Rest Controller defining the Asset API.
* Responsible for handling and responding to all Asset API Requests.
*/
@Controller
public class AssetController {

  // Spring-Data Object allowing access to Mongo GridFS
  @Autowired
  GridFsTemplate gridFsTemplate;

  // Spring Data Mongo Repository allowing access to standard Mongo operations
  @Autowired
  AssetHistoryRepository assetHistories;

  // Spring Data Mongo Repository allowing access to standard Mongo operations
  @Autowired
  AssetRelationshipRepository assetRelationships;

  @Autowired
  GridFSBucket gridFsBucket;

  // Object Controller Logger
  private static final Logger logger =
      LogManager.getLogger("avc.AssetController");

  private String saveAsset(MultipartFile file, DBObject metaData, String fileType) {
    String newId = "";
    if (file == null) {
      return newId;
    }
    try {
      newId =
        gridFsTemplate.store(file.getInputStream(), "asset." + fileType, metaData).toString();
    } catch (Exception e) {
      logger.error("Error Saving Asset to Mongo: ", e);
    }
    return newId;
  }

  private void updateAssetHistory(String assetId, String oldAssetId) {
    List<AssetHistory> existingHistoryList = assetHistories.findByAsset(oldAssetId);
    // If we have an existing history, update it.
    if (existingHistoryList.size() > 0) {
      AssetHistory existingHistory = existingHistoryList.get(0);
      existingHistory.getAssetIds().add(0, assetId);
      existingHistory.setAsset(assetId);
      assetHistories.save(existingHistory);
    } else {
      // If we don't have an existing history, create one
      List<String> historyList = new ArrayList<String>();
      historyList.add(oldAssetId);
      historyList.add(0, assetId);
      AssetHistory newHistory = new AssetHistory();
      newHistory.setAsset(assetId);
      newHistory.setAssetIds(historyList);
      assetHistories.save(newHistory);
    }
  }

  private ResponseEntity<Resource> getAsset(String id) throws MalformedURLException {
    logger.info("Responding to Asset Get Request");
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "text/plain");
    // Load the file from Mongo
    GridFSFile gridFsdbFile;
    try {
      Query query = new Query();
      query.addCriteria(Criteria.where("_id").is(id));
      gridFsdbFile = gridFsTemplate.findOne(query);
    } catch (Exception e) {
      logger.error("Error Retrieving Asset from Mongo: ", e);
      return new ResponseEntity<Resource>(new UrlResource("http://server.error"), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (gridFsdbFile == null) {
      logger.error("Null Asset Retrieved from Mongo");
      return new ResponseEntity<Resource>(new UrlResource("http://server.error"), responseHeaders, HttpStatus.NO_CONTENT);
    }

    // Send the file back in a response
    InputStreamResource fileResource;
    try {
      GridFSDownloadStream gridFsDownloadStream =
          gridFsBucket.openDownloadStream(gridFsdbFile.getObjectId());
      GridFsResource gridResource = new GridFsResource(gridFsdbFile,gridFsDownloadStream);
      fileResource = new InputStreamResource(gridResource.getInputStream());
    } catch (Exception e) {
      logger.error("Error Loading Asset from Mongo: ", e);
      return new ResponseEntity<Resource>(new UrlResource("http://server.error"), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return ResponseEntity.ok().contentLength(gridFsdbFile.getLength())
        .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"" + id + "\"")
        .body(fileResource);
  }

  /**
  * Retrieve an Asset by ID.
  */
  @GetMapping("/v1/asset/{key}")
  @ResponseBody
  public ResponseEntity<Resource> serveFile(@PathVariable String key)
      throws MalformedURLException, IOException {
    return getAsset(key);
  }

  /**
  * Retrieve an Asset by ID, using a filename extension.
  * This makes it easy to support browser-based asset loaders.
  * Note that we're note going to actually use the filename here,
  * it's just for convenience.
  */
  @GetMapping("/v1/asset/{key}/{filename}")
  @ResponseBody
  public ResponseEntity<Resource> serveFile(@PathVariable String key,
                                            @PathVariable String filename)
      throws MalformedURLException, IOException {
    return getAsset(key);
  }

  /**
  * Create an Asset.
  * Uses Multi-part form data to accept the file
  */
  @RequestMapping(path = "/v1/asset",
      headers = ("content-type=multipart/*"),
      method = RequestMethod.POST)
  public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file,
      @RequestParam(value = "related-id", defaultValue = "") String relatedId,
      @RequestParam(value = "related-type", defaultValue = "") String relatedType,
      @RequestParam(value = "content-type", defaultValue = "text/plain") String contentType,
      @RequestParam(value = "file-type", defaultValue = "txt") String fileType,
      @RequestParam(value = "asset-type", defaultValue = "standard") String assetType) {
    logger.info("Responding to Asset Save Request");

    // Persist the file
    DBObject metaData = new BasicDBObject();
    metaData.put("content-type", contentType);
    metaData.put("file-type", fileType);
    metaData.put("asset-type", assetType);
    String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
    metaData.put("created-dttm", timeStamp);
    String newId = saveAsset(file, metaData, fileType);
    HttpHeaders responseHeaders = new HttpHeaders();
    if (newId.isEmpty()) {
      return new ResponseEntity<String>("Failure", responseHeaders,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Persist a relationship, if provided
    if (!(relatedId.isEmpty()) && !(relatedType.isEmpty()) && !(newId.isEmpty())) {
      AssetRelationship newRelation = new AssetRelationship();
      newRelation.setAssetId(newId);
      newRelation.setRelationshipType(relatedType);
      newRelation.setRelatedId(relatedId);
      assetRelationships.save(newRelation);
    }

    HttpStatus returnCode = HttpStatus.OK;
    return new ResponseEntity<String>(newId, responseHeaders, returnCode);
  }

  /**
  * Update an Asset.
  * Uses Multi-part form data to accept the file
  */
  @RequestMapping(path = "/v1/asset/{key}",
      headers = ("content-type=multipart/*"),
      method = RequestMethod.POST)
  public ResponseEntity<String> handleFileUpdate(@PathVariable String key,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "content-type", defaultValue = "text/plain") String contentType,
      @RequestParam(value = "file-type", defaultValue = "txt") String fileType,
      @RequestParam(value = "asset-type", defaultValue = "standard") String assetType) {
    // Persist New Asset
    DBObject metaData = new BasicDBObject();
    metaData.put("content-type", contentType);
    metaData.put("file-type", fileType);
    metaData.put("asset-type", assetType);
    String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
    metaData.put("created-dttm", timeStamp);
    String newId = saveAsset(file, metaData, fileType);
    HttpHeaders responseHeaders = new HttpHeaders();
    if (newId.isEmpty()) {
      return new ResponseEntity<String>("Failure", responseHeaders,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Update Asset Relationships
    List<AssetRelationship> existingRelationships =
        assetRelationships.findByAssetId(key);
    for (AssetRelationship relation : existingRelationships) {
      relation.setAssetId(newId);
      assetRelationships.save(relation);
    }

    // Update Asset Histories
    updateAssetHistory(newId, key);

    // Response setup
    HttpStatus returnCode = HttpStatus.OK;
    return new ResponseEntity<String>(newId, responseHeaders, returnCode);
  }

  /**
  * Delete an Asset.
  */
  @RequestMapping(path = "/v1/asset/{key}",
      method = RequestMethod.DELETE)
  public ResponseEntity<String> deleteFile(@PathVariable String key) {
    logger.info("Responding to Asset Delete Request");
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "text/plain");
    // Delete the file from Mongo
    try {
      gridFsTemplate.delete(new Query(Criteria.where("_id").is(key)));
    } catch (Exception e) {
      logger.error("Error Deleting Asset from Mongo: ", e);
      return new ResponseEntity<String>("Failure", responseHeaders,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Remove Asset Relationships
    List<AssetRelationship> existingRelationships =
        assetRelationships.findByAssetId(key);
    for (AssetRelationship relation : existingRelationships) {
      assetRelationships.delete(relation);
    }

    // Send response
    return new ResponseEntity<String>("Success", responseHeaders, HttpStatus.OK);
  }

}
