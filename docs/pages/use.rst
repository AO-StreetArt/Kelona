.. _use:

Using Kelona
============

Kelona's primary function is storing assets.  These assets can take any format,
binary or text-based, and are generally graphics-based in nature (although this
is not a requirement).

Kelona supports a number of different workflows in regards to these assets.
Assets can be created, queried, or destroyed.  When an Asset is updated, a new
Asset is created, and updates are made to both Relationships and Asset Histories.

Asset Relationships
-------------------

Relationships allow assets to be associated with other pieces of data, including
other assets.  This may include other assets (for example, one asset can be the
thumbnail of another).

Relationships are one of the primary means of discovering Assets, and should
always be the means of connecting assets to larger systems.

Asset Histories
---------------

Histories provide a list of all the previous versions of an asset.  Rather than
updating an asset, a new asset is created and all of it's relationships are
updated.  The history provides a listing of all previous assets, in chronological
order.

:ref:`Go Home <index>`
