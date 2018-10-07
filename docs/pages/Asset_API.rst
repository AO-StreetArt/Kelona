.. _asset_api:

Asset API
=========

An Asset, at it's core, is simply a file.  The general expectation, however, is
that this file is quite large, and may be in a binary format.  An Asset has some
associated metadata, but at it's core it is nothing but a file.

Asset Creation
~~~~~~~~~~~~~~

.. http:post:: /v1/asset/

   Create a new asset.

   :query string content-type: Optional.  The content type of the asset (ie. application/json).
   :query string file-type: Optional.  The file type of the asset (ie. json).
   :query string asset-type: Optional.  Valid options are 'standard' (for normal assets), and 'thumbnail' for thumbnail assets.
   :query string scene: Optional.  Used to associate a thumbnail to a scene.
   :query string object: Optional.  Used to associate a thumbnail to an object.
   :query string parent: Optional.  Used to associate a thumbnail to an asset.
   :reqheader Content-Type: multipart/*
   :statuscode 200: Success

.. include:: _examples/asset/asset_create.rst

Asset Update
~~~~~~~~~~~~

.. http:post:: /v1/asset/{asset_key}

   Update an existing Asset.  This returns a new key for the asset, and adds
   an entry to the associated Asset History.

   :query string content-type: Optional.  The content type of the asset (ie. application/json).
   :query string file-type: Optional.  The file type of the asset (ie. json).
   :query string asset-type: Optional.  Valid options are 'standard' (for normal assets), and 'thumbnail' for thumbnail assets.
   :query string scene: Optional.  Used to associate a thumbnail to a scene.
   :query string object: Optional.  Used to associate a thumbnail to an object.
   :query string parent: Optional.  Used to associate a thumbnail to an asset.
   :reqheader Content-Type: multipart/*
   :statuscode 200: Success

.. include:: _examples/asset/asset_update.rst

Asset Retrieval
~~~~~~~~~~~~~~~

.. http:get:: /v1/asset/(asset_key)

   Retrieve an asset by ID.

   :statuscode 200: Success

.. include:: _examples/asset/asset_get.rst

Asset Metadata Query
~~~~~~~~~~~~~~~~~~~~

.. http:get:: /v1/asset

   Query Asset Metadata based on various attributes.

   :query string content-type: Optional.  The content type of the asset (ie. application/json).
   :query string file-type: Optional.  The file type of the asset (ie. json).
   :query string asset-type: Optional.  Valid options are 'standard' (for normal assets), and 'thumbnail' for thumbnail assets.
   :query string scene: Optional.  Used to associate a thumbnail to a scene.
   :query string object: Optional.  Used to associate a thumbnail to an object.
   :query string parent: Optional.  Used to associate a thumbnail to an asset.
   :statuscode 200: Success

.. include:: _examples/asset/asset_query.rst

Asset Deletion
~~~~~~~~~~~~~~

.. http:delete:: /v1/asset/(asset_key)

   Delete an asset.

   :statuscode 200: Success

.. include:: _examples/asset/asset_delete.rst
