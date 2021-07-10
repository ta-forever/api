package com.faforever.api.game;

import com.faforever.api.config.FafApiProperties;
import com.faforever.api.data.domain.Player;
import com.faforever.api.error.ApiException;
import com.faforever.api.error.Error;
import com.faforever.api.error.ErrorCode;
import com.faforever.api.player.PlayerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@RestController
@RequestMapping(path = "/game")
@Slf4j
@RequiredArgsConstructor
public class GameController {
  private final GameService gameService;
  private final PlayerService playerService;
  private final FafApiProperties fafApiProperties;
  private final ObjectMapper objectMapper;

  @GetMapping("/{id}/replay")
  public void downloadReplay(HttpServletResponse httpServletResponse,
                             @PathVariable("id") int replayId) {
    httpServletResponse.setHeader(HttpHeaders.LOCATION, gameService.getReplayDownloadUrl(replayId));
    httpServletResponse.setStatus(HttpStatus.FOUND.value());
  }

  @ApiOperation("Upload game logs")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Success"),
    @ApiResponse(code = 401, message = "Unauthorized"),
    @ApiResponse(code = 500, message = "Failure")})
  @RequestMapping(path = "/logs_upload", method = RequestMethod.POST, produces = APPLICATION_JSON_UTF8_VALUE)
  public void uploadLogs(@RequestParam("file") MultipartFile file,
                        @RequestParam("metadata") String jsonString,
                        Authentication authentication) throws IOException {
    if (file == null) {
      throw new ApiException(new Error(ErrorCode.UPLOAD_FILE_MISSING));
    }

    String extension = Files.getFileExtension(file.getOriginalFilename());
    if (!fafApiProperties.getGameLogs().getAllowedExtensions().contains(extension)) {
      throw new ApiException(new Error(ErrorCode.UPLOAD_INVALID_FILE_EXTENSIONS, fafApiProperties.getGameLogs().getAllowedExtensions()));
    }

    String context;
    int id;
    try {
      JsonNode node = objectMapper.readTree(jsonString);
      context = node.path("context").asText("adhoc");
      id = node.path("id").asInt(0);
    } catch (IOException e) {
      log.debug("Could not parse metadata", e);
      throw new ApiException(new Error(ErrorCode.INVALID_METADATA, e.getMessage()));
    }

    Player player = playerService.getPlayer(authentication);
    gameService.uploadGameLogs(file.getInputStream(), player, context, id);
  }
}
