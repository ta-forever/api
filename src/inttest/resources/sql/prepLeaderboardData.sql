insert into ladder1v1_rating_rank_view (id, ranking, mean, deviation, num_games, win_games, rating, streak, recent_scores, recent_mod)
VALUES (1, 1, 1500, 120, 5, 1, 1500 - 3 * 120, 1, '2', 'tacc'),
       (2, 2, 1200, 90, 5, 2, 1200 - 3 * 90, -2, '00', 'tacc'),
       (3, 3, 1000, 100, 5, 3, 1000 - 3 * 100, 0, '111', 'tacc');

insert into global_rating_rank_view (id, ranking, mean, deviation, num_games, rating, streak, recent_scores, recent_mod)
VALUES (1, 1, 1500, 120, 5, 1500 - 3 * 120, 3, '22200', 'tacc'),
       (2, 2, 1200, 90, 5, 1200 - 3 * 90, 1, '202020', 'tacc'),
       (3, 3, 1000, 100, 5, 1000 - 3 * 100, -1, '00202020', 'tacc');


commit;
