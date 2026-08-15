-- Contact details a completed application must carry: phone, address, city and
-- pincode join the profile, and applying now requires all of them (plus full
-- name, location and an uploaded resume — columns that already existed).
--
-- Nullable on purpose. Existing profiles cannot have values for columns that did
-- not exist, and completeness is an apply-time rule, not a storage rule: a
-- half-finished profile is allowed to exist, it just cannot apply yet.

ALTER TABLE `developer_profiles`
  ADD COLUMN `phone` varchar(20) DEFAULT NULL,
  ADD COLUMN `address` varchar(255) DEFAULT NULL,
  ADD COLUMN `city` varchar(120) DEFAULT NULL,
  ADD COLUMN `pincode` varchar(12) DEFAULT NULL;
