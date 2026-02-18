ALTER TABLE "${flyway:defaultSchema}".links
ADD COLUMN clicks_count INT NOT NULL DEFAULT 0;
