#!/bin/bash

psql -U postgres -d orbitmessengerdb -f sql/dropTables.sql
psql -U postgres -d orbitmessengerdb -f sql/createTables.sql
echo "done!"
