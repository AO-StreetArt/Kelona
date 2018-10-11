Kelona
======

.. figure:: https://travis-ci.org/AO-StreetArt/Kelona.svg?branch=master
   :alt:

Overview
--------

Kelona is a Version Control service for large files, binary or text based.  It is
primarily targeted at storing files for graphics applications, such as:

- .obj files
- .glsl files
- .fbx files
- Proprietary data formats (ie. for Blender or Maya)

Kelona does not have access to file contents, instead tracking metadata and dropping
files into a large-scale datastore.  The only currently supported backend is Mongo GridFS.

Kelona is a part of the AO Aesel Project, along
with `CLyman <https://github.com/AO-StreetArt/CLyman>`__,
`Crazy Ivan <https://github.com/AO-StreetArt/CrazyIvan>`__,
and `Adrestia <https://github.com/AO-StreetArt/Adrestia>`__.

Features
--------

- Storage of large-scale Assets (files), and associated metadata.
- Storage of relationships between assets and other data elements.
- Provide a History of updates on each Asset.

Stuck and need help?  Have general questions about the application?  We encourage you to publish your question
on `Stack Overflow <https://stackoverflow.com>`__.  We regularly monitor for the tag 'aesel' in questions.

We encourage the use of Stack Overflow for a few reasons:

* Once the question is answered, it is searchable and viewable by everyone else.
* The forum format offers an easy method to get a larger community involved with a tougher question.
