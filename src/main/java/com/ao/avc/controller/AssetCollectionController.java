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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import org.springframework.web.bind.annotation.RequestBody;
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

  /**
  * Get an Asset Collection.
  */
  @GetMapping("/v1/collection/{id}")
  @ResponseBody
  public ResponseEntity<AssetCollection> getCollection(@PathVariable String id) {
    logger.info("Responding to Asset Collection Get Request");
    HttpStatus returnCode = HttpStatus.OK;
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");
    AssetCollection returnCollection = null;
    Optional<AssetCollection> existingCollection = assetCollections.findById(id);
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
      @RequestParam(value = "page", defaultValue = "0") int pageNum) {
    logger.info("Responding to Asset Collection Get Request");
    HttpStatus returnCode = HttpStatus.OK;
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");
    List<AssetCollection> returnCollections = null;
    Pageable pageable = new PageRequest(pageNum, recordsInPage);
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
  public ResponseEntity<AssetCollection> updateCollection(
      @RequestBody AssetCollection inpCollection) {
    logger.info("Responding to Asset Collection Create Request");
    HttpStatus returnCode = HttpStatus.OK;
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");
    AssetCollection responseCollection = assetCollections.insert(inpCollection);
    return new ResponseEntity<AssetCollection>(responseCollection, responseHeaders, returnCode);
  }

  /**
   * Update an existing Asset Collection.
   */
  @PostMapping("/v1/collection/{id}")
  @ResponseBody
  public ResponseEntity<AssetCollection> createCollection(
      @PathVariable String id,
      @RequestBody AssetCollection inpCollection) {
    logger.info("Responding to Asset Collection Create Request");
    HttpStatus returnCode = HttpStatus.OK;
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");
    inpCollection.setId(id);
    AssetCollection responseCollection = assetCollections.save(inpCollection);
    return new ResponseEntity<AssetCollection>(responseCollection, responseHeaders, returnCode);
  }

}
