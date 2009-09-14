
DROP TABLE IF EXISTS subject;
CREATE TABLE subject (
       id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
       label VARCHAR(31),
       strain VARCHAR(31),
       pod TINYINT,
       li VARCHAR(63),
       ri VARCHAR(63)
);

-- ------------------------------------------------------------------------------
-- STATIC TABLE DATA
-- ------------------------------------------------------------------------------

INSERT INTO label (name, description) VALUES ('major', 'arteria gracilis major');
INSERT INTO label (name, description) VALUES ('minor', 'arteria gracilis minor');
INSERT INTO label (name, description) VALUES ('minor-superior', 'arteria gracilis minor superior');
INSERT INTO label (name, description) VALUES ('minor-inferior', 'arteria gracilis minor inferior');

INSERT INTO subject (label, strain, pod, li, ri) VALUES ('gc1', 'c57bl6', 21, 'gracilis-C57-1L.png', 'gracilis-balbc-1R.png');

