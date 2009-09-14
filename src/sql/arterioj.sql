
-- ------------------------------------------------------------------------------
-- ARTERIOJ DATA MODEL 
-- ------------------------------------------------------------------------------

DROP TABLE IF EXISTS subject;
CREATE TABLE subject (
       id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
       region VARCHAR(31),      
       strain VARCHAR(31), 
       pod TINYINT,
       li VARCHAR(63),
       ri VARCHAR(63)
);

DROP TABLE IF EXISTS label;
CREATE TABLE label (
       id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
       name VARCHAR(63) NOT NULL,
       description VARCHAR(255),
       CONSTRAINT unique_label_name UNIQUE (name)
);

DROP TABLE IF EXISTS image;
CREATE TABLE image (
       id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
       name VARCHAR(63) NOT NULL,
       path VARCHAR(63) NOT NULL,
       CONSTRAINT unique_image_name UNIQUE (name)
);

DROP TABLE IF EXISTS bif;
CREATE TABLE bif (
       id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
       item INT NOT NULL,
       x DOUBLE NOT NULL,
       y DOUBLE NOT NULL,
       image_id INT NOT NULL,
       trib_item INT,
       trunk_item INT,
       branch_item INT,
       label VARCHAR(63)
);

DROP TABLE IF EXISTS vessel;
CREATE TABLE vessel (
       id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
       item INT NOT NULL,
       x DOUBLE NOT NULL,
       y DOUBLE NOT NULL,
       image_id INT NOT NULL,
       src_item INT,
       dst_item INT,
       len DOUBLE,
       distance DOUBLE,
       ldr DOUBLE,
       diameter DOUBLE,
       tortuosity DOUBLE,
       collateral BOOLEAN, 
       label VARCHAR(63)
);

DROP TABLE IF EXISTS diameter;
CREATE TABLE diameter (
       id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
       item INT NOT NULL,
       x DOUBLE NOT NULL,
       y DOUBLE NOT NULL,
       image_id INT NOT NULL,
       vessel_item INT,
       x1 DOUBLE,
       y1 DOUBLE,
       x2 DOUBLE,
       y2 DOUBLE,
       label VARCHAR(63)
);

DROP TABLE IF EXISTS path;
CREATE TABLE path (
       id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
       vessel_id INT, -- reference to the collateral object
       len DOUBLE,
       distance DOUBLE,
       ldr DOUBLE,
       diameter DOUBLE,
       tortuosity DOUBLE,
       name VARCHAR(255)
);

DROP TABLE IF EXISTS pathentry;
CREATE TABLE pathentry (
       id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
       path_id INT,
       vessel_id INT,
       seq INT,
       x DOUBLE,
       distance DOUBLE,
       diameter DOUBLE,
       length DOUBLE,
       ldr DOUBLE,
       tortuosity DOUBLE,
       curvature DOUBLE,
       label VARCHAR(255),
       units VARCHAR(31)
);




