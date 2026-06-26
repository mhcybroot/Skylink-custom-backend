#!/bin/bash
curl -c cookies.txt -d "username=mhcybroot&password=MhR@2025" http://localhost:8083/login
curl -L -v -b cookies.txt -X POST -H "Content-Type: application/json" -d '{"woNumber":"375848", "field":"analyst", "value":"Panir"}' http://localhost:8083/processing-sheet/api/update-wo
