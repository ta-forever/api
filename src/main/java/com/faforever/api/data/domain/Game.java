package com.faforever.api.data.domain;

import com.faforever.api.data.checks.*;
import com.faforever.api.data.converter.VictoryConditionConverter;
import com.faforever.api.data.listeners.GameEnricher;
import com.faforever.api.security.elide.permission.AdminMapCheck;
import com.yahoo.elide.annotation.*;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.time.OffsetDateTime;
import java.util.Set;

@Entity
@Table(name = "game_stats")
@Include(rootLevel = true, type = "game")
@Setter
@EntityListeners(GameEnricher.class)
@ReadPermission(expression = "not " + IsGameHidden.EXPRESSION + " or " + IsGameParticipatedByCaller.EXPRESSION)
public class Game {

  private int id;
  private OffsetDateTime startTime;
  private OffsetDateTime endTime;
  private Integer replayTicks;
  private VictoryCondition victoryCondition;
  private FeaturedMod featuredMod;
  private Player host;
  private MapVersion mapVersion;
  private String name;
  private Validity validity;
  private Set<GamePlayerStats> playerStats;
  private String replayUrl;
  private Set<GameReview> reviews;
  private GameReviewsSummary reviewsSummary;
  private Boolean replayAvailable;
  private Boolean tadaAvailable;
  private String replayMeta;
  private Boolean replayHidden;

  @Id
  @Column(name = "id")
  public int getId() {
    return id;
  }

  @Column(name = "startTime")
  public OffsetDateTime getStartTime() {
    return startTime;
  }

  @Column(name = "replay_ticks")
  public Integer getReplayTicks() {
    return replayTicks;
  }

  @Column(name = "gameType")
  @Convert(converter = VictoryConditionConverter.class)
  public VictoryCondition getVictoryCondition() {
    return victoryCondition;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "gameMod")
  public FeaturedMod getFeaturedMod() {
    return featuredMod;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "host")
  public Player getHost() {
    return host;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mapId")
  public MapVersion getMapVersion() {
    return mapVersion;
  }

  @Column(name = "gameName")
  public String getName() {
    return name;
  }

  @Column(name = "validity")
  @Enumerated(EnumType.ORDINAL)
  public Validity getValidity() {
    return validity;
  }

  @OneToMany(mappedBy = "game")
  @BatchSize(size = 1000)
  public Set<GamePlayerStats> getPlayerStats() {
    return playerStats;
  }

  @Column(name = "endTime")
  @Nullable
  public OffsetDateTime getEndTime() {
    return endTime;
  }

  @Transient
  @ComputedAttribute
  public String getReplayUrl() {
    return replayUrl;
  }

  @OneToMany(mappedBy = "game")
  @UpdatePermission(expression = Prefab.ALL)
  @BatchSize(size = 1000)
  public Set<GameReview> getReviews() {
    return reviews;
  }

  @OneToOne(fetch = FetchType.LAZY)
  @PrimaryKeyJoinColumn
  @UpdatePermission(expression = Prefab.ALL)
  @BatchSize(size = 1000)
  public GameReviewsSummary getReviewsSummary() {
    return reviewsSummary;
  }

  @Column(name = "replay_available")
  public Boolean isReplayAvailable() {
    return replayAvailable;
  }

  @Column(name = "tada_available")
  public Boolean isTadaAvailable() {
    return tadaAvailable;
  }

  @Column(name = "replay_meta")
  public String getReplayMeta() { return replayMeta; }

  @UpdatePermission(expression = AdminMapCheck.EXPRESSION + " or (" + IsGameHostedByCaller.EXPRESSION + " and " + BooleanChange.TO_FALSE_EXPRESSION + ")")
  @Audit(action = Audit.Action.UPDATE, logStatement = "Updated game_stats for `{0}` attribute replay_hidden to: {1}", logExpressions = {"${game.id}", "${game.replayHidden}"})
  @Column(name = "replay_hidden")
  public Boolean getReplayHidden() { return replayHidden; }

  /**
   * This ManyToOne relationship leads to a double left outer join through Elide causing an additional full table
   * scan on the matchmaker_queue table. Even though it has only 3 records, it causes MySql 5.7 and MySQL to run
   * a list of all games > 1 min on prod where it was ~1 second before.
   *
   * This can be fixed by migrating to MariaDB.
   */
//  private Integer matchmakerQueueId;
//
//  @JoinTable(name = "matchmaker_queue_game",
//    joinColumns = @JoinColumn(name = "game_stats_id"),
//    inverseJoinColumns = @JoinColumn(name = "matchmaker_queue_id")
//  )
//  @ManyToOne(fetch = FetchType.LAZY)
//  @Nullable
//  public Integer getMatchmakerQueueId() {
//    return matchmakerQueueId;
//  }
}
