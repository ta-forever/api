package com.faforever.api.data.domain;

import com.faforever.api.data.checks.IsEntityOwner;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Setter
@Table(name = "matchmaker_queue")
@Include(rootLevel = true, type = MatchmakerQueue.TYPE_NAME)
public class MatchmakerQueue extends AbstractEntity {

  public static final String TYPE_NAME = "matchmakerQueue";

  private String technicalName;
  private String nameKey;
  private FeaturedMod featuredMod;
  private Leaderboard leaderboard;
  private int teamSize;
  private boolean enabled;

  @Column(name = "technical_name")
  @NotNull
  @UpdatePermission(expression = IsEntityOwner.EXPRESSION)
  public String getTechnicalName() {
    return technicalName;
  }

  @Column(name = "name_key")
  @NotNull
  @UpdatePermission(expression = IsEntityOwner.EXPRESSION)
  public String getNameKey() {
    return nameKey;
  }

  @Column(name = "team_size")
  @NotNull
  @UpdatePermission(expression = IsEntityOwner.EXPRESSION)
  public int getTeamSize() {
    return teamSize;
  }

  @Column(name = "enabled")
  @NotNull
  @UpdatePermission(expression = IsEntityOwner.EXPRESSION)
  public boolean getEnabled() {
    return enabled;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "featured_mod_id")
  public FeaturedMod getFeaturedMod() {
    return featuredMod;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "leaderboard_id")
  public Leaderboard getLeaderboard() {
    return leaderboard;
  }
}
