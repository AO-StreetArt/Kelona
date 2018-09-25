.. _quickstart:

Getting Started with Clyman
==============================

:ref:`Go Home <index>`

Docker
------

An official Docker Image of Kelona is provided, and to get you up and running
quickly, a Docker Compose file is provided as well.  To start up a Mongo
instance, a Consul instance, and a Kelona instance, simply run the following
from the 'compose/min' folder:

.. code-block:: bash

   docker-compose up

Once the services have started, test them by hitting Clyman's healthcheck endpoint:

.. code-block:: bash

   curl http://localhost:5635/health

Keep in mind that this is not a secure deployment,
but is suitable for exploring the :ref:`Kelona API <api_index>`.

Building from Source
--------------------

Once you've got the required backend services started, build and execute the tests
for the repository.  Please note that integration tests will fail unless you
have instances of the required backend services running:

``./gradlew check``

And, finally, start Kelona:

``./gradlew bootRun``

Using the Latest Release
------------------------

Kelona can also be downloaded as a runnable JAR for the latest release from `here <https://github.com/AO-StreetArt/Kelona/releases>`__.

When using a JAR, unzip the downloaded package, move to the main directory from a terminal, and run:

``java -jar build/libs/kelona-0.0.1.jar``
