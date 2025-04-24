-- Drop and recreate raffle table
DROP TABLE IF EXISTS `participant`;
DROP TABLE IF EXISTS `raffle`;

CREATE TABLE IF NOT EXISTS `raffle` (
  `id` int NOT NULL AUTO_INCREMENT,
  `artwork_id` int NOT NULL,
  `creator_id` int NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `winner_id` int DEFAULT NULL,
  `creator_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `raffle_description` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_raffle_artwork` (`artwork_id`),
  KEY `FK_raffle_creator` (`creator_id`),
  KEY `FK_raffle_winner` (`winner_id`),
  CONSTRAINT `FK_raffle_artwork` FOREIGN KEY (`artwork_id`) REFERENCES `artwork` (`id`),
  CONSTRAINT `FK_raffle_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_raffle_winner` FOREIGN KEY (`winner_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `participant` (
  `id` int NOT NULL AUTO_INCREMENT,
  `raffle_id` int NOT NULL,
  `user_id` int NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `joined_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_participant_raffle` (`raffle_id`),
  KEY `FK_participant_user` (`user_id`),
  CONSTRAINT `FK_participant_raffle` FOREIGN KEY (`raffle_id`) REFERENCES `raffle` (`id`),
  CONSTRAINT `FK_participant_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;