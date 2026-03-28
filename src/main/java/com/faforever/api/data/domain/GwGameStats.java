package com.faforever.api.data.domain;

import com.yahoo.elide.annotation.Include;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "gw_game_stats")
@Include(rootLevel = true, type = "gwGameStats")
@Immutable
@Setter
public class GwGameStats {

  private int gameId;
  private String galaxy;
  private int iteration;
  private int planetId;
  private Game game;

  @Id
  @Column(name = "game_id")
  public int getGameId() {
    return gameId;
  }

  @Column(name = "galaxy")
  public String getGalaxy() {
    return galaxy;
  }

  @Column(name = "iteration")
  public int getIteration() {
    return iteration;
  }

  @Column(name = "planet_id")
  public int getPlanetId() {
    return planetId;
  }

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id", insertable = false, updatable = false)
  public Game getGame() {
    return game;
  }
}
