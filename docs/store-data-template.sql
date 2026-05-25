-- Replace the sample rows with the final 2026 store data before production launch.
-- Image paths should match what the frontend expects, for example /store/keyring.webp.

INSERT INTO team (id, created_at, updated_at, name, page_key) VALUES
  (1, NOW(), NOW(), 'Limbo', 'limbo'),
  (2, NOW(), NOW(), '11:11', 'eleven-eleven'),
  (3, NOW(), NOW(), 'Tiny Lodge', 'tiny-lodge'),
  (4, NOW(), NOW(), '(Un)skinned', 'unskinned'),
  (5, NOW(), NOW(), 'RE:I', 'rei'),
  (6, NOW(), NOW(), 'Dreamscape', 'dreamscape'),
  (7, NOW(), NOW(), 'Thorn Bloom', 'thorn-bloom');

INSERT INTO item (
  id,
  created_at,
  updated_at,
  name,
  price,
  creator,
  item_image_path,
  description_image_path,
  team_id
) VALUES
  (1, NOW(), NOW(), 'Sample Product', 10000, 'Limbo', '/store/sample.webp', '/store/sample-detail.webp', 1);
