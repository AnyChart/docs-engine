-- name: sql-search-for
select url, snippet(content, :query) from docs_stg_index where match(:query) and version_id=:version limit 100;
