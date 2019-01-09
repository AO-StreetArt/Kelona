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
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface AssetCollectionRepository extends MongoRepository<AssetCollection, String> {

  @Query("{ '$or': [ {'isPublic': true}, {'user': ?0} ] }")
  public List<AssetCollection> findPublicOrPrivate(String user, Pageable pageable);

  @Query("{ '$and': [ {'$or': [ {'isPublic': true}, {'user': ?1} ]}, {'id': ?0} ]}")
  public Optional<AssetCollection> findPublicOrPrivateById(String id, String user);

  public List<AssetCollection> findByName(String name, Pageable pageable);

  @Query("{ '$and': [ {'$or': [ {'isPublic': true}, {'user': ?1} ]}, {'name': ?0} ]}")
  public List<AssetCollection> findPublicOrPrivateByName(String name, String user, Pageable pageable);

  public List<AssetCollection> findByCategory(String category, Pageable pageable);

  @Query("{ '$and': [ {'$or': [ {'isPublic': true}, {'user': ?1} ]}, {'category': ?0} ]}")
  public List<AssetCollection> findPublicOrPrivateByCategory(String category, String user, Pageable pageable);

  public List<AssetCollection> findByTagsIn(Set<String> tags, Pageable pageable);

  @Query("{ '$and': [ {'$or': [ {'isPublic': true}, {'user': ?1} ]}, {'tags': {'$in': ?0}} ]}")
  public List<AssetCollection> findPublicOrPrivateByTagsIn(Set<String> tags, String user, Pageable pageable);

  public List<AssetCollection> findByCategoryAndTagsIn(
      String category,
      Set<String> tags,
      Pageable pageable);

  @Query("{ '$and': [ {'$or': [ {'isPublic': true}, {'user': ?2} ]}, {'$and': [{'category': ?0}, {'tags': {'$in': ?1}}] } ]}")
  public List<AssetCollection> findPublicOrPrivateByCategoryAndTagsIn(String category,
                                                              Set<String> tags,
                                                              String user,
                                                              Pageable pageable);

}
