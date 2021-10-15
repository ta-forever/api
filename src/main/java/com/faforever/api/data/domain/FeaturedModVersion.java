package com.faforever.api.data.domain;

import com.yahoo.elide.annotation.Include;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;

@Entity
@Table(name = "game_featuredMods_version")
@Include(rootLevel = true, type = FeaturedModVersion.TYPE_NAME)
@Setter
public class FeaturedModVersion {
  public static final String TYPE_NAME = "featuredModVersion";

  private int id;
  private FeaturedMod featuredMod;
  private String version;
  private String taHash;  // md5
  private Boolean confirmed;  // set when mod/version/hash combination is a verified fact
  private Integer observationCount; // number of games seen with this mod/version/hash combination

  @Id
  @Column(name = "id")
  public int getId() {
    return id;
  }

  @Column(name = "version")
  public String getVersion() {
    return version;
  }

  @Column(name = "ta_hash")
  public String getTaHash() {
    return taHash;
  }

  @Column(name = "confirmed")
  public Boolean getConfirmed() { return confirmed; }

  @Column(name = "observation_count")
  public Integer getObservationCount() { return observationCount; }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_featuredMods_id")
  @NotNull
  @BatchSize(size = 1000)
  public FeaturedMod getFeaturedMod() {
    return this.featuredMod;
  }
}
