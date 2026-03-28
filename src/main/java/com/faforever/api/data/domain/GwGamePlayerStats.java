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
@Table(name = "gw_game_player_stats")
@Include(rootLevel = true, type = "gwGamePlayerStats")
@Immutable
@Setter
public class GwGamePlayerStats {

  private long gamePlayerStatsId;
  private String gwFaction;
  private int gwRank;
  private GamePlayerStats gamePlayerStats;

  @Id
  @Column(name = "game_player_stats_id")
  public long getGamePlayerStatsId() {
    return gamePlayerStatsId;
  }

  @Column(name = "gw_faction")
  public String getGwFaction() {
    return gwFaction;
  }

  @Column(name = "gw_rank")
  public int getGwRank() {
    return gwRank;
  }

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_player_stats_id", insertable = false, updatable = false)
  public GamePlayerStats getGamePlayerStats() {
    return gamePlayerStats;
  }
}
