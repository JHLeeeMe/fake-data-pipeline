#!/usr/bin/env bash

# for file path
DIR="$(cd "$(dirname "$0")" && pwd -P)"

psql -U postgres -d postgres -a -f $DIR/../resources/sql/create_database.sql
