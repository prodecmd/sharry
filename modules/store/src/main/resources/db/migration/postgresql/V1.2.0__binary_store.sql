ALTER TABLE "filemeta" DROP COLUMN IF EXISTS "chunksize";
ALTER TABLE "filemeta" DROP COLUMN IF EXISTS "chunks";

ALTER TABLE "filemeta"
RENAME COLUMN "id" TO "file_id";

ALTER TABLE "filechunk"
RENAME COLUMN "fileid" TO "file_id";

ALTER TABLE "filechunk"
RENAME COLUMN "chunknr" TO "chunk_nr";

ALTER TABLE "filechunk"
RENAME COLUMN "chunklength" TO "chunk_len";

ALTER TABLE "filechunk"
RENAME COLUMN "chunkdata" TO "chunk_data";

-- change timestamp format, bitpeace used a string
ALTER TABLE "filemeta"
ADD COLUMN "created" timestamp;

UPDATE "filemeta" SET "created" = CAST("timestamp" as timestamp);

ALTER TABLE "filemeta"
ALTER COLUMN "created" SET NOT NULL;

ALTER TABLE "filemeta"
DROP COLUMN "timestamp";