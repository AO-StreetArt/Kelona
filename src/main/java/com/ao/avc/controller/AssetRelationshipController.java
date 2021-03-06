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

import com.ao.avc.dao.AssetRelationshipRepository;
import com.ao.avc.model.AssetRelationship;
import com.ao.avc.query.BulkRequest;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
* Rest Controller defining the Asset API.
* Responsible for handling and responding to all Asset API Requests.
*/
@Controller
public class AssetRelationshipController {

  // Spring Data Mongo Repository allowing access to standard Mongo operations
  @Autowired
  AssetRelationshipRepository assetRelationships;

  // Object Controller Logger
  private static final Logger logger =
      LogManager.getLogger("avc.AssetController");

  /**
  * Delete a Relationship.
  */
  @DeleteMapping("/v1/relationship")
  @ResponseBody
  public ResponseEntity<List<AssetRelationship>> deleteRelations(
      @RequestParam(value = "type", defaultValue = "") String relatedType,
      @RequestParam(value = "asset", defaultValue = "") String assetId,
      @RequestParam(value = "related", defaultValue = "") String relatedId) {
    logger.info("Responding to Asset Relationship Deletion Request");
    HttpStatus returnCode = HttpStatus.OK;
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");
    List<AssetRelationship> existingRelationships;
    if (!(relatedId.isEmpty()) && !(relatedType.isEmpty()) && !(assetId.isEmpty())) {
      existingRelationships =
          assetRelationships.findByAssetIdAndRelationshipTypeAndRelatedId(assetId,
                                                                          relatedType,
                                                                          relatedId);
    } else {
      existingRelationships = new ArrayList<AssetRelationship>();
      returnCode = HttpStatus.BAD_REQUEST;
      logger.warn("Incorrect Input: Invalid Query Parameter Set");
    }

    for (AssetRelationship relation : existingRelationships) {
      assetRelationships.delete(relation);
    }

    // Send the response
    return new ResponseEntity<List<AssetRelationship>>(existingRelationships,
                                                       responseHeaders,
                                                       returnCode);
  }

  /**
  * Add a new Relationship, or update an existing one.
  */
  @PutMapping("/v1/relationship")
  @ResponseBody
  public ResponseEntity<List<AssetRelationship>> addRelation(
      @RequestParam(value = "type", defaultValue = "") String relatedType,
      @RequestParam(value = "asset", defaultValue = "") String assetId,
      @RequestParam(value = "related", defaultValue = "") String relatedId,
      @RequestBody AssetRelationship inpRelationship) {
    logger.info("Responding to Asset Relationship Addition Request");
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");
    List<AssetRelationship> updatedRelationships;

    // See if we have any existing relationships
    if (!(relatedId.isEmpty()) && !(relatedType.isEmpty()) && assetId.isEmpty()) {
      updatedRelationships =
          assetRelationships.findByRelationshipTypeAndRelatedId(relatedType,
                                                                relatedId);
    } else if (relatedId.isEmpty() && !(relatedType.isEmpty()) && !(assetId.isEmpty())) {
      updatedRelationships =
          assetRelationships.findByAssetIdAndRelationshipType(assetId, relatedType);
    } else {
      updatedRelationships = new ArrayList<AssetRelationship>();
    }

    // Update existing relationships
    if (updatedRelationships.size() > 0) {
      for (AssetRelationship relation : updatedRelationships) {
        relation.setAssetId(inpRelationship.getAssetId());
        relation.setAssetSubId(inpRelationship.getAssetSubId());
        relation.setRelationshipType(inpRelationship.getRelationshipType());
        relation.setRelationshipSubtype(inpRelationship.getRelationshipSubtype());
        relation.setRelatedId(inpRelationship.getRelatedId());
        assetRelationships.save(relation);
      }

    // Create a new relationship
    } else {
      assetRelationships.save(inpRelationship);
      updatedRelationships.add(inpRelationship);
    }

    // Send the response
    HttpStatus returnCode = HttpStatus.OK;
    return new ResponseEntity<List<AssetRelationship>>(updatedRelationships,
                                                       responseHeaders,
                                                       returnCode);
  }

  /**
  * Find Assets By Relationship.
  */
  @GetMapping("/v1/relationship")
  @ResponseBody
  public ResponseEntity<List<AssetRelationship>> findRelations(
      @RequestParam(value = "type", defaultValue = "") String type,
      @RequestParam(value = "asset", defaultValue = "") String asset,
      @RequestParam(value = "related", defaultValue = "") String related,
      @RequestParam(value = "num_records", defaultValue = "10") int recordsInPage,
      @RequestParam(value = "page", defaultValue = "0") int pageNum) {
    logger.info("Responding to Asset Relationship Query");
    HttpStatus returnCode = HttpStatus.OK;
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");
    Pageable pageable = new PageRequest(pageNum, recordsInPage);

    List<AssetRelationship> existingRelationships;
    if (asset.isEmpty() && type.isEmpty() && !(related.isEmpty())) {
      existingRelationships = assetRelationships.findByRelatedId(related);
    } else if (type.isEmpty() && related.isEmpty() && !(asset.isEmpty())) {
      existingRelationships = assetRelationships.findByAssetId(asset);
    } else if (asset.isEmpty() && related.isEmpty() && !(type.isEmpty())) {
      existingRelationships = assetRelationships.findByRelationshipType(type);
    } else if (asset.isEmpty() && !(related.isEmpty()) && !(type.isEmpty())) {
      existingRelationships = assetRelationships.findByRelationshipTypeAndRelatedId(type, related);
    } else if (related.isEmpty() && !(asset.isEmpty()) && !(type.isEmpty())) {
      existingRelationships = assetRelationships.findByAssetIdAndRelationshipType(asset, type);
    } else if (related.isEmpty() && asset.isEmpty() && type.isEmpty()) {
      existingRelationships = assetRelationships.findAll(pageable).getContent();
    } else {
      existingRelationships = new ArrayList<AssetRelationship>();
      returnCode = HttpStatus.BAD_REQUEST;
      logger.warn("Incorrect Input: Invalid Query Parameter Set");
    }

    if (existingRelationships.size() == 0) {
      returnCode = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
      logger.debug("No Asset Relationships found");
    }

    // Create and return the new HTTP Response
    return new ResponseEntity<List<AssetRelationship>>(existingRelationships,
                                                       responseHeaders,
                                                       returnCode);
  }

  /**
  * Get relationships in bulk.
  */
  @PostMapping("/v1/bulk/relationship")
  @ResponseBody
  public ResponseEntity<List<AssetRelationship>> bulkGetRelations(
      @RequestBody BulkRequest bulkQuery) {
    logger.info("Executing Bulk Relationship Query");
    List<AssetRelationship> resultRelations = null;
    if (bulkQuery.getAssetIds() != null && bulkQuery.getAssetIds().size() > 0) {
      logger.debug("Bulk Asset ID Query detected: {}", bulkQuery.getAssetIds());
      resultRelations = assetRelationships.findByAssetIds(bulkQuery.getAssetIds());
    } else if (bulkQuery.getRelatedIds() != null && bulkQuery.getRelatedIds().size() > 0) {
      logger.debug("Bulk Related ID Query detected: {}", bulkQuery.getRelatedIds());
      resultRelations = assetRelationships.findByRelatedIds(bulkQuery.getRelatedIds());
    } else {
      logger.debug("Empty query detected");
      resultRelations = new ArrayList<AssetRelationship>();
    }
    logger.debug("Query results:");
    for (AssetRelationship rel : resultRelations) {
      logger.debug(rel);
    }

    // Send the response
    HttpStatus returnCode = HttpStatus.OK;
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");
    return new ResponseEntity<List<AssetRelationship>>(resultRelations,
                                                       responseHeaders,
                                                       returnCode);
  }

}
