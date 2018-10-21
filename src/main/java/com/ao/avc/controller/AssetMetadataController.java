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

import org.apache.commons.codec.binary.Hex;

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
public class AssetMetadataController {

  // Spring-Data Object allowing access to Mongo GridFS
  @Autowired
  GridFsTemplate gridFsTemplate;

  // Object Controller Logger
  private static final Logger logger =
      LogManager.getLogger("avc.AssetController");

  /**
  * Find Assets by metadata.
  */
  @GetMapping("/v1/asset")
  @ResponseBody
  public ResponseEntity<List<AssetMetadata>> findAssets(
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
      if (!(contentType.isEmpty())) {
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
      returnDoc.setKey(Hex.encodeHexString(mongoFile.getId().asObjectId().getValue().toByteArray()));
      returnDoc.setContentType(metaDoc.getString("content-type"));
      returnDoc.setFileType(metaDoc.getString("file-type"));
      returnDoc.setAssetType(metaDoc.getString("asset-type"));
      returnDoc.setCreatedTimestamp(metaDoc.getString("created-dttm"));
      returnList.add(returnDoc);
    }
    responseHeaders.set("Content-Type", "application/json");
    return new ResponseEntity<List<AssetMetadata>>(returnList, responseHeaders, HttpStatus.OK);
  }

}
