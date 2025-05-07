package com.faforever.api.data.domain;

import com.yahoo.elide.annotation.Include;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_launch_verifier")
@Include(rootLevel = true, type = GameLaunchVerification.TYPE_NAME)
@Setter
public class GameLaunchVerification {
  public static final String TYPE_NAME = "gameLaunchVerification";

  private int id;
  private int gameId;
  private int loginId;
  private String data;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public int getId() {
    return id;
  }

  @Column(name = "game_id")
  public int getGameId() {
    return gameId;
  }

  @Column(name = "login_id")
  public int getLoginId() {
    return loginId;
  }

  @Column(name = "data")
  public String getData() {
    return data;
  }
}
