.. _security:

Security
========

Kelona has several built-in security measures, each of which is highly
configurable.

Vault Integration
-----------------

All configuration properties for Kelona can be loaded from Hashicorp Vault,
which is a highly secured, centralized secret store.  The Vault connection
is configurable in the bootstrap.properties file for Kelona.

In a Production environment, all sensitive values should be loaded exclusively
from Vault.

HTTPS Support
-------------

Kelona fully supports HTTPS communications, with configuration options in the
application.properties file to set the certificate and trust store information.

HTTPS should always be utilized in a Production Environment.

Authentication
--------------

Kelona supports Basic Auth for HTTP(S), with a single username and password
configured through Vault.  The configuration can also be set from
application.properties, however should always be set from Vault in Production.

:ref:`Go Home <index>`
