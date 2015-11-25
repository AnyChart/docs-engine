Setup:


    CREATE USER docs_user WITH PASSWORD 'pass';
    CREATE DATABASE docs_db;
    GRANT ALL PRIVILEGES ON DATABASE docs_db TO docs_user;
    psql -p5432 -d docs_db -U docs_user -W

