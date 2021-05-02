package com.faforever.api.data.listeners;

import com.faforever.api.config.FafApiProperties;
import com.faforever.api.data.domain.Map;
import com.faforever.api.data.domain.MapVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.persistence.PostLoad;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

@Component
@Slf4j
public class MapVersionEnricher {

  private static FafApiProperties apiProperties;

  @Inject
  public void init(FafApiProperties apiProperties) {
    MapVersionEnricher.apiProperties = apiProperties;
  }

  @PostLoad
  public void enhance(MapVersion mapVersion) {
    String filename = mapVersion.getFilename();
    String [] filenameParts = filename.split("/");
    mapVersion.setDownloadUrl(String.format(apiProperties.getMap().getDownloadUrlFormat(), filenameParts[0]));
    mapVersion.setThumbnailUrl(String.format(apiProperties.getMap().getPreviewsUrlFormat(), filenameParts[1] + ".png"));
    mapVersion.setArchiveName(filenameParts[0]);
    mapVersion.setName(filenameParts[1]);
    mapVersion.setCrc(filenameParts[2]);
  }

  @CacheEvict(allEntries = true, cacheNames = {Map.TYPE_NAME, MapVersion.TYPE_NAME})
  @PostUpdate
  @PostRemove
  public void mapVersionChanged(MapVersion mapVersion) {
    log.debug("Map and MapVersion cache evicted, due to change on MapVersion with id: {}", mapVersion.getId());
  }
}
