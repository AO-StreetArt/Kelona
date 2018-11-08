.. _asset_api:

Asset API
=========

An Asset, at it's core, is simply a file.  The general expectation, however, is
that this file is quite large, and may be in a binary format.  An Asset has some
associated metadata.

Asset Creation
~~~~~~~~~~~~~~

.. http:post:: /v1/asset/

   Create a new asset from the File Data in the body of the request.  If the
   'related-id' and 'related-type' are also populated, then an Asset Relationship
   is created as well.

   :query string content-type: Optional.  The content type of the asset (ie. application/json).
   :query string file-type: Optional.  The file type of the asset (ie. json).
   :query string related-id: Optional.  Must appear with 'related-type'.  Used to create a relationship to the specified object.
   :query string related-type: Optional.  Must appear with 'related-id'.  Used the create a relationship of the specified type.
   :query string asset-type: Optional.  Populated into the query-able Asset Metadata.
   :reqheader Content-Type: multipart/*
   :statuscode 200: Success

.. include:: _examples/asset/asset_create.rst

Asset Update
~~~~~~~~~~~~

.. http:post:: /v1/asset/{asset_key}

   Update an existing Asset.  This returns a new key for the asset, and adds
   an entry to the associated Asset History.  This will also update all relationships
   which were associated to the old Asset, and associate them to the new Asset.

   :query string content-type: Optional.  The content type of the asset (ie. application/json).
   :query string file-type: Optional.  The file type of the asset (ie. json).
   :query string asset-type: Optional.  Populated into the query-able Asset Metadata.
   :reqheader Content-Type: multipart/*
   :statuscode 200: Success

.. include:: _examples/asset/asset_update.rst

Asset Retrieval
~~~~~~~~~~~~~~~

.. http:get:: /v1/asset/(asset_key)

   Retrieve an asset by ID.

   :statuscode 200: Success

.. include:: _examples/asset/asset_get.rst

Asset Count
~~~~~~~~~~~

.. http:get:: /v1/asset/count

   Count the total number of assets matching the given query.

   :query string content-type: Optional.  The content type of the asset (ie. application/json).
   :query string file-type: Optional.  The file type of the asset (ie. json).
   :query string asset-type: Optional.  Valid options are 'standard' (for normal assets), and 'thumbnail' for thumbnail assets.
   :statuscode 200: Success

.. include:: _examples/asset/asset_count.rst

Asset Metadata Query
~~~~~~~~~~~~~~~~~~~~

.. http:get:: /v1/asset

   Query Asset Metadata based on various attributes.

   :query string content-type: Optional.  The content type of the asset (ie. application/json).
   :query string file-type: Optional.  The file type of the asset (ie. json).
   :query string asset-type: Optional.  Valid options are 'standard' (for normal assets), and 'thumbnail' for thumbnail assets.
   :query limit: Optional.  The maximum number of records to return.
   :query offset: Optional.  The number of records to skip, enabling pagination with the 'limit' parameter.
   :statuscode 200: Success

.. include:: _examples/asset/asset_query.rst

Asset Deletion
~~~~~~~~~~~~~~

.. http:delete:: /v1/asset/(asset_key)

   Delete an asset.

   :statuscode 200: Success

.. include:: _examples/asset/asset_delete.rst
