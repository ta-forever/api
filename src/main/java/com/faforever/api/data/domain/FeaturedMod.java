package com.faforever.api.data.domain;

import com.yahoo.elide.annotation.Include;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_featuredMods")
@Include(rootLevel = true, type = FeaturedMod.TYPE_NAME)
@Setter
public class FeaturedMod {
  public static final String TYPE_NAME = "featuredMod";

  private int id;
  private String technicalName;
  private String description;
  private String displayName;
  private boolean visible;
  private int order;
  private String gitUrl;
  private String gitBranch;
  private Boolean allowOverride;
  private String fileExtension;
  private String deploymentWebhook;
  private List<FeaturedModVersion> versions = new ArrayList<>();

  @Id
  @Column(name = "id")
  public int getId() {
    return id;
  }

  @Column(name = "gamemod")
  public String getTechnicalName() {
    return technicalName;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  @Column(name = "name")
  public String getDisplayName() {
    return displayName;
  }

  @Column(name = "publish")
  public boolean isVisible() {
    return visible;
  }

  @Column(name = "\"order\"")
  public int getOrder() {
    return order;
  }

  @Column(name = "git_url")
  public String getGitUrl() {
    return gitUrl;
  }

  @Column(name = "git_branch")
  public String getGitBranch() {
    return gitBranch;
  }

  @Column(name = "allow_override")
  public Boolean isAllowOverride() {
    return allowOverride;
  }

  @Column(name = "file_extension")
  public String getFileExtension() {
    return fileExtension;
  }

  @Column(name = "deployment_webhook ")
  public String getDeploymentWebhook() {
    return deploymentWebhook;
  }

  @OneToMany(mappedBy = "featuredMod", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @NotEmpty
  @Valid
  @BatchSize(size = 1000)
  public List<FeaturedModVersion> getVersions() {
    return versions;
  }
}
