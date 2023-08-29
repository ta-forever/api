package com.faforever.api.game;

import com.faforever.api.config.FafApiProperties;
import com.faforever.api.data.domain.Player;
import com.faforever.api.error.ApiException;
import com.faforever.api.error.Error;
import com.faforever.api.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameService {
  private final FafApiProperties fafApiProperties;

  public String getReplayDownloadUrl(int replayId) {
    Assert.state(replayId > 0, "Replay ID must be positive");

    String leadingZeroReplayId = String.format("%010d", replayId);

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < 4; i++) {
      String subfolderId = leadingZeroReplayId.substring(i * 2, i * 2 + 2);
      sb.append(Integer.parseInt(subfolderId));
      sb.append("/");
    }

    sb.append(replayId);
    sb.append(".zip");

    return String.format(fafApiProperties.getReplay().getDownloadUrlFormat(), sb.toString());
  }

  @Transactional
  @SneakyThrows
  public void uploadGameLogs(InputStream logDataInputStream, Player player, String context, int id)
  {
    Assert.notNull(player, "'player' must not be null");
    Assert.isTrue(logDataInputStream.available() > 0, "'logDataInputStream' must not be empty");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    Path destination = fafApiProperties.getGameLogs().getTargetDirectory()
      .resolve(String.format("%s(%d)", context, id))
      .resolve(String.format("%s(%d)-%s-%s(%d).zip", context, id, formatter.format(Instant.now()), player.getLogin(), player.getId()));

    long freeSpace = destination.getParent().getParent().toFile().getFreeSpace();
    long logFileSize = logDataInputStream.available();
    log.info("[uploadGameLogs] destination={}", destination);
    log.info("[uploadGameLogs] free space={} GB; logs size={} kB", (int)(freeSpace/1e9), (int)(logFileSize/1e3));

    final long MAX_LOG_FILE_SIZE = 1000000;
    if (logFileSize*10 >= freeSpace) {
      throw ApiException.of(ErrorCode.SERVER_DISK_FULL);
    }
    else if (logFileSize > MAX_LOG_FILE_SIZE) {
      throw new ApiException(new Error(ErrorCode.FILE_SIZE_EXCEEDED, MAX_LOG_FILE_SIZE, logFileSize));
    }
    else {
      log.info("[uploadGameLogs] saving game logs");
      Files.createDirectories(destination.getParent());
      Files.copy(logDataInputStream, destination);
    }
  }
}
