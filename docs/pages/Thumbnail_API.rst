.. _thumbnail_api:

Thumbnail API
=============

A Thumbnail is an Asset with asset-type = 'thumbnail'.  In general, these are
2D images which can be displayed in a UI for an asset or scene.

In general, thumbnails are not expected to be updated or versions like other
assets.  Because of this, users should delete an old thumbnail and add a new
one, rather than calling the Asset Update API.

Asset Thumbnail Retrieval
~~~~~~~~~~~~~~~~~~~~~~~~~

.. http:post:: /v1/asset-thumbnail/{parent}

   Get an asset thumbnail by the Id of it's parent asset.

   :statuscode 200: Success

.. include:: _examples/asset/get_asset_thumbnail.rst

Scene Thumbnail Retrieval
~~~~~~~~~~~~~~~~~~~~~~~~~

.. http:post:: /v1/scene-thumbnail/{scene}

   Get a scene thumbnail by the Id of it's scene.

   :statuscode 200: Success

.. include:: _examples/asset/get_scene_thumbnail.rst
