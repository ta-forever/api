package com.faforever.api.data.domain;

import com.yahoo.elide.annotation.Include;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Setter
@Table(name = "leaderboard_rating")
@Include(rootLevel = true, type = LeaderboardRating.TYPE_NAME)
public class LeaderboardRating extends AbstractEntity implements OwnableEntity {

  public static final String TYPE_NAME = "leaderboardRating";

  private Double mean;
  private Double deviation;
  private double rating;
  private int totalGames;
  private int wonGames;
  private int lostGames;
  private int drawnGames;
  private Leaderboard leaderboard;
  private Player player;
  private int streak;
  private int bestStreak;
  private String recentScores;
  private String recentMod;

  @ManyToOne
  @JoinColumn(name = "login_id")
  public Player getPlayer() {
    return player;
  }

  @ManyToOne
  @JoinColumn(name = "leaderboard_id")
  public Leaderboard getLeaderboard() {
    return leaderboard;
  }

  @Column(name = "total_games")
  public int getTotalGames() {
    return totalGames;
  }

  @Column(name = "won_games")
  public int getWonGames() {
    return wonGames;
  }

  @Column(name = "lost_games")
  public int getLostGames() {
    return lostGames;
  }

  @Column(name = "drawn_games")
  public int getDrawnGames() {
    return drawnGames;
  }

  @Column(name = "mean")
  public Double getMean() {
    return mean;
  }

  @Column(name = "deviation")
  public Double getDeviation() {
    return deviation;
  }

  @Column(name = "rating")
  public double getRating() {
    return rating;
  }

  @Column(name = "streak")
  public int getStreak() { return streak; }

  @Column(name = "best_streak")
  public int getBestStreak() { return bestStreak; }

  @Column(name = "recent_scores")
  // 0=loss, 1=draw, 2=win.  most recent first
  public String getRecentScores() { return recentScores; }

  @Column(name = "recent_mod")
  public String getRecentMod() { return recentMod; }

  @Override
  @Transient
  public Login getEntityOwner() {
    return getPlayer();
  }
}
