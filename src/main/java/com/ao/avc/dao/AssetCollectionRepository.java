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

import com.ao.avc.model.AssetCollection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetCollectionRepository extends MongoRepository<AssetCollection, String> {

  public List<AssetCollection> findByName(String name);
  public List<AssetCollection> findByCategory(String category);
  public List<AssetCollection> findByTagsIn(Set<String> tags);
  public List<AssetCollection> findByCategoryAndTagsIn(String category, Set<String> tags);

}