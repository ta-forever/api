package com.faforever.api.data.listeners;

import com.faforever.api.config.FafApiProperties;
import com.faforever.api.data.domain.CoopMap;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.persistence.PostLoad;

@Component
public class CoopMapEnricher {

  private static FafApiProperties fafApiProperties;

  @Inject
  public void init(FafApiProperties fafApiProperties) {
    CoopMapEnricher.fafApiProperties = fafApiProperties;
  }

  @PostLoad
  public void enhance(CoopMap coopMap) {
    String filename = coopMap.getFilename();
    coopMap.setFolderName(filename.substring(filename.indexOf('/') + 1, filename.indexOf(".zip")));
    coopMap.setDownloadUrl(String.format(fafApiProperties.getMap().getDownloadUrlFormat(), filename.replace("maps/", "")));
    coopMap.setThumbnailUrl(String.format(fafApiProperties.getMap().getPreviewsUrlFormat(), filename.replace("maps/", "").replace(".zip", ".png")));
  }
}
