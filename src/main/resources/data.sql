INSERT INTO team (id, created_at, updated_at, name, page_key) VALUES
  (1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Limbo', 'limbo'),
  (2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '11:11', 'eleven-eleven'),
  (3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Tiny Lodge', 'tiny-lodge'),
  (4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '(Un)skinned', 'unskinned'),
  (5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'RE:I', 'rei'),
  (6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Dreamscape', 'dreamscape'),
  (7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Thorn Bloom', 'thorn-bloom'),
  (8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Official Goods', 'official-goods');

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
  (1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ALTER EGO REVERSIBLE ECHO BAG', 35000, 'Official Goods', 'assets/store/official-goods-thumb.webp', 'assets/store/official-goods-detail.webp', 8),
  (2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Removable STICKER', 4000, 'Thorn Bloom', 'assets/store/thorn-bloom-sticker-thumb.webp', 'assets/store/thorn-bloom-sticker-detail.webp', 7),
  (3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ASCII T-SHIRT', 25000, 'Thorn Bloom', 'assets/store/thorn-bloom-tshirt-thumb.webp', 'assets/store/thorn-bloom-tshirt-detail.webp', 7),
  (4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Acrylic KEYRING', 9000, 'Thorn Bloom', 'assets/store/thorn-bloom-keyring-thumb.webp', 'assets/store/thorn-bloom-keyring-detail.webp', 7);
