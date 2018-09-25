.. _history_api:

Asset History API
=================

An Asset History is a record of all the versions of a particular asset.

Asset History Retrieval
~~~~~~~~~~~~~~~~~~~~~~~

.. http:get:: /v1/asset/(asset_key)

   Get a list of Asset Histories associated to a particular Asset.

   :statuscode 200: Success

.. include:: _examples/asset/asset_history_get.rst
