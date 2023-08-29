package com.faforever.api.data.domain;

import com.yahoo.elide.annotation.Include;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Setter
@Table(name = "leaderboard")
@Include(rootLevel = true, type = Leaderboard.TYPE_NAME)
public class Leaderboard extends AbstractEntity {

  public static final String TYPE_NAME = "leaderboard";

  private String technicalName;
  private String nameKey;
  private String descriptionKey;
  private Boolean leaderboardHidden;

  @Column(name = "technical_name")
  public String getTechnicalName() {
    return technicalName;
  }

  @Column(name = "name_key")
  public String getNameKey() {
    return nameKey;
  }

  @Column(name = "description_key")
  public String getDescriptionKey() {
    return descriptionKey;
  }

  @Column(name = "leaderboard_hidden")
  public Boolean getLeaderboardHidden() {
    return leaderboardHidden;
  }
}
