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

package com.ao.avc.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "assetrelationship")
public class AssetRelationship {

  @Id
  public String id;
  // Id of the Asset referred to by the relationship
  public String assetId;
  // The path or ID within the asset which needs to be imported,
  // (ie. the name of the object to import within a .blend file)
  public String assetSubId;
  // The type of relationship (ie. Scene, Object, Thumbnail, etc.)
  public String relationshipType;
  // The subtype of the relationship (ie. full, mesh, material, texture, etc)
  public String relationshipSubtype;
  // The ID of the related data element (ie. object or scene)
  public String relatedId;

}
