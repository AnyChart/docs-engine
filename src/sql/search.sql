-- name: sql-search-for
select * from docs_stg_index where match('@full_name :query @content :query') and version_id=:version limit 100;
