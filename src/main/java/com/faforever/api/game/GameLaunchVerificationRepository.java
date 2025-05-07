package com.faforever.api.game;

import com.faforever.api.data.domain.GameLaunchVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameLaunchVerificationRepository extends JpaRepository<GameLaunchVerification, Integer> {
}
