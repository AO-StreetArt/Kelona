..  http:example:: curl wget httpie python-requests
    :response: asset_relationship_save_response.rst

    PUT /v1/relationship HTTP/1.1
    Host: localhost:5635
    Content-Type: application/json

    {
      "assetId": "asset123",
      "relationshipType": "scene",
      "relatedId": "scene123"
    }
