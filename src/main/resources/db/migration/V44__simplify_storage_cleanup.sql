-- Keep existing V43 checksum intact; automatic schema-wide reconciliation is no longer used.
DROP FUNCTION IF EXISTS storage_url_is_referenced(TEXT);
