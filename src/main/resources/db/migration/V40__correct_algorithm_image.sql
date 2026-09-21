-- BE-002: replace the unrelated food photo assigned to vocabulary id 14.
UPDATE vocabulary
SET image_url = 'https://images.unsplash.com/photo-1518770660439-4636190af475'
WHERE id = 14
  AND word = 'Algorithm';
