ALTER TABLE ACCESS ADD COLUMN UnderConstruction CHAR(1) NOT NULL DEFAULT 'N' CONSTRAINT ACCESS_UnderConstruction_CN CHECK (UnderConstruction IN ('Y','N'))
<<>>