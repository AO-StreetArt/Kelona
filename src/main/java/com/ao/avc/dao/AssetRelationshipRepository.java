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

package com.ao.avc.dao;

import com.ao.avc.model.AssetRelationship;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface AssetRelationshipRepository extends MongoRepository<AssetRelationship, String> {

  // Find Asset Relationships by Asset
  public List<AssetRelationship> findByAssetId(String asset);

  // Find Asset Relationships by Relationship Type
  public List<AssetRelationship> findByRelationshipType(String type);

  // Find Asset Relationships by Related Object
  public List<AssetRelationship> findByRelatedId(String related);

  // Find Asset Relationships by Relationship Type & Related Object
  public List<AssetRelationship> findByRelationshipTypeAndRelatedId(String type, String related);

  // Find Asset Relationships by Asset & Relationship Type
  public List<AssetRelationship> findByAssetIdAndRelationshipType(String asset, String type);

  // Find Asset Relationships by Asset & Relationship Type & Related Object
  public List<AssetRelationship> findByAssetIdAndRelationshipTypeAndRelatedId(String asset,
                                                                              String type,
                                                                              String related);

  @Query("{ 'assetId': { '$in': ?0 } }")
  public List<AssetRelationship> findByAssetIds(List<String> assetIds);

  @Query("{ 'relatedId': { '$in': ?0 } }")
  public List<AssetRelationship> findByRelatedIds(List<String> relatedIds);
}
