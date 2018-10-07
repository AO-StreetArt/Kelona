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
import com.ao.avc.model.AssetHistory;
import com.ao.avc.model.AssetMetadata;

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

  private void updateAssetHistory(String sceneName, String objectName,
      String assetId, String oldAssetId) {
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
      newHistory.setScene(sceneName);
      newHistory.setObject(objectName);
      newHistory.setAsset(assetId);
      newHistory.setAssetIds(historyList);
      assetHistories.save(newHistory);
    }
  }

  private ResponseEntity<Resource> getAsset(String id, boolean isThumbnail,
      boolean isSceneThumbnail) throws MalformedURLException {
    logger.info("Responding to Asset Get Request");
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "text/plain");
    // Load the file from Mongo
    GridFSFile gridFsdbFile;
    try {
      Query query = new Query();
      if (isThumbnail) {
        query.addCriteria(Criteria.where("metadata.asset-type").is("thumbnail"));
        if (isSceneThumbnail) {
          query.addCriteria(Criteria.where("metadata.scene").is(id));
        } else {
          query.addCriteria(Criteria.where("metadata.parent").is(id));
        }
      } else {
        query.addCriteria(Criteria.where("_id").is(id));
      }
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
  * Find Assets by metadata.
  */
  @GetMapping("/v1/asset")
  @ResponseBody
  public ResponseEntity<List<AssetMetadata>> findAssets(
      @RequestParam(value = "scene", defaultValue = "") String sceneId,
      @RequestParam(value = "object", defaultValue = "") String objectId,
      @RequestParam(value = "parent", defaultValue = "") String parentId,
      @RequestParam(value = "content-type", defaultValue = "") String contentType,
      @RequestParam(value = "file-type", defaultValue = "") String fileType,
      @RequestParam(value = "asset-type", defaultValue = "standard") String assetType)
      throws MalformedURLException, IOException {
    logger.info("Responding to Asset Find Request");
    HttpHeaders responseHeaders = new HttpHeaders();
    // Load the file from Mongo
    GridFSFindIterable gridFsdbFiles;
    try {
      Query query = new Query();
      if (!(sceneId.isEmpty())) {
        query.addCriteria(Criteria.where("metadata.scene").is(sceneId));
      } else if (!(objectId.isEmpty())) {
        query.addCriteria(Criteria.where("metadata.object").is(objectId));
      } else if (!(parentId.isEmpty())) {
        query.addCriteria(Criteria.where("metadata.parent").is(parentId));
      } else if (!(contentType.isEmpty())) {
        query.addCriteria(Criteria.where("metadata.content-type").is(contentType));
      } else if (!(fileType.isEmpty())) {
        query.addCriteria(Criteria.where("metadata.file-type").is(fileType));
      } else if (!(assetType.isEmpty())) {
        query.addCriteria(Criteria.where("metadata.asset-type").is(assetType));
      }
      gridFsdbFiles = gridFsTemplate.find(query);
    } catch (Exception e) {
      logger.error("Error Retrieving Asset from Mongo: ", e);
      return new ResponseEntity<List<AssetMetadata>>(
          new ArrayList<AssetMetadata>(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (gridFsdbFiles == null) {
      logger.error("Null Asset Retrieved from Mongo");
      return new ResponseEntity<List<AssetMetadata>>(
          new ArrayList<AssetMetadata>(), responseHeaders, HttpStatus.NO_CONTENT);
    }
    List<AssetMetadata> returnList = new ArrayList<AssetMetadata>();
    for (GridFSFile mongoFile : gridFsdbFiles) {
      Document metaDoc = mongoFile.getMetadata();
      AssetMetadata returnDoc = new AssetMetadata();
      logger.info(metaDoc.toString());
      returnDoc.setKey(mongoFile.getId().toString());
      returnDoc.setScene(metaDoc.getString("scene"));
      returnDoc.setObject(metaDoc.getString("object"));
      returnDoc.setParent(metaDoc.getString("parent"));
      returnDoc.setContentType(metaDoc.getString("content-type"));
      returnDoc.setFileType(metaDoc.getString("file-type"));
      returnDoc.setAssetType(metaDoc.getString("asset-type"));
      returnDoc.setCreatedTimestamp(metaDoc.getString("created-dttm"));
      returnList.add(returnDoc);
    }
    responseHeaders.set("Content-Type", "application/json");
    return new ResponseEntity<List<AssetMetadata>>(returnList, responseHeaders, HttpStatus.OK);
  }

  /**
  * Retrieve an Asset.
  */
  @GetMapping("/v1/asset/{key}")
  @ResponseBody
  public ResponseEntity<Resource> serveFile(@PathVariable String key)
      throws MalformedURLException, IOException {
    return getAsset(key, false, false);
  }

  /**
  * Retrieve a Thumbnail by Parent ID.
  */
  @GetMapping("/v1/asset-thumbnail/{parent}")
  @ResponseBody
  public ResponseEntity<Resource> serveThumbnail(@PathVariable String parent)
      throws MalformedURLException, IOException {
    return getAsset(parent, true, false);
  }

  /**
  * Retrieve a Thumbnail by Scene ID.
  */
  @GetMapping("/v1/scene-thumbnail/{scene}")
  @ResponseBody
  public ResponseEntity<Resource> serveSceneThumbnail(@PathVariable String scene)
      throws MalformedURLException, IOException {
    return getAsset(scene, true, true);
  }

  /**
  * Create an Asset.
  * Uses Multi-part form data to accept the file
  */
  @RequestMapping(path = "/v1/asset",
      headers = ("content-type=multipart/*"),
      method = RequestMethod.POST)
  public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file,
      @RequestParam(value = "scene", defaultValue = "") String sceneId,
      @RequestParam(value = "object", defaultValue = "") String objectId,
      @RequestParam(value = "parent", defaultValue = "") String parentId,
      @RequestParam(value = "content-type", defaultValue = "text/plain") String contentType,
      @RequestParam(value = "file-type", defaultValue = "txt") String fileType,
      @RequestParam(value = "asset-type", defaultValue = "standard") String assetType) {
    logger.info("Responding to Asset Save Request");
    DBObject metaData = new BasicDBObject();
    metaData.put("scene", sceneId);
    metaData.put("object", objectId);
    metaData.put("parent", parentId);
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
    // TO-DO: Add Asset to scene and/or object in CLyman and Crazy Ivan
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
      @RequestParam(value = "scene", defaultValue = "") String sceneId,
      @RequestParam(value = "object", defaultValue = "") String objectId,
      @RequestParam(value = "parent", defaultValue = "") String parentId,
      @RequestParam(value = "content-type", defaultValue = "text/plain") String contentType,
      @RequestParam(value = "file-type", defaultValue = "txt") String fileType,
      @RequestParam(value = "asset-type", defaultValue = "standard") String assetType) {
    // TO-DO: Only accept updates for non-thumbnail assets
    DBObject metaData = new BasicDBObject();
    metaData.put("scene", sceneId);
    metaData.put("object", objectId);
    metaData.put("parent", parentId);
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
    // TO-DO: Update Scene and/or Object in CLyman/CrazyIvan
    updateAssetHistory(sceneId, objectId, newId, key);
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
    return new ResponseEntity<String>("Success", responseHeaders, HttpStatus.OK);
  }

}
