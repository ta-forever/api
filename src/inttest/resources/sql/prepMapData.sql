INSERT INTO map (id, display_name, map_type, battle_type, author) VALUES
  (1, 'SHERWOOD', 'FFA', 'skirmish', 1),
  (2, 'Gods of War', 'FFA', 'skirmish', 1);

INSERT INTO map_version (id, description, max_players, width, height, version, filename, hidden, map_id) VALUES
  (1, '6 x 6  fighting around 3 large hills', 8, 5, 5, 1, 'totala2.hpi/SHERWOOD/ead82fc5', 0, 1),
  (2, '9 x 8  asymmetric water map.', 8, 5, 5, 1, 'totala2.hpi/Gods of War/5a282a18', 0, 2);

INSERT INTO ladder_map (id, idmap) VALUES
  (1, 1);

INSERT INTO map_pool (id, name) VALUES
  (1, 'Ladder 1v1 <300'),
  (2, 'Ladder 1v1 300-800'),
  (3, 'Ladder 1v1 800-1300'),
  (4, 'Ladder 1v1 1300-1800'),
  (5, 'Ladder 1v1 1800+');

INSERT INTO matchmaker_queue_map_pool (matchmaker_queue_id, map_pool_id, min_rating, max_rating) VALUES
  (1, 1, null, 300),
  (1, 2, 300, 800),
  (1, 3, 800, 1300),
  (1, 4, 1300, 1800),
  (1, 5, 1800, null);

INSERT INTO map_pool_map_version (id, map_pool_id, map_version_id, map_params, weight) VALUES
  (1, 1, 1, null, 1),
  (2, 1, null, '{"type": "neroxis", "size": 512, "spawns": 2, "version": "1.4.3"}', 1),
  (3, 2, 2, null, 1);
