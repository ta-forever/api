package com.faforever.api.map;

import com.faforever.api.config.FafApiProperties;
import com.faforever.api.content.ContentService;
import com.faforever.api.data.domain.BanDurationType;
import com.faforever.api.data.domain.BanLevel;
import com.faforever.api.data.domain.Map;
import com.faforever.api.data.domain.MapVersion;
import com.faforever.api.data.domain.Player;
import com.faforever.api.error.ApiException;
import com.faforever.api.error.Error;
import com.faforever.api.error.ErrorCode;
import com.faforever.api.map.MapNameValidationResponse.FileNames;
import com.faforever.api.utils.FilePermissionUtil;
import com.faforever.api.utils.NameUtil;
import com.faforever.commons.io.Unzipper;
import com.faforever.commons.io.Zipper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.faforever.api.map.MapService.ScenarioMapInfo.FILE_ENDING_MAP;
import static com.faforever.api.map.MapService.ScenarioMapInfo.FILE_ENDING_SAVE;
import static com.faforever.api.map.MapService.ScenarioMapInfo.FILE_ENDING_SCENARIO;
import static com.faforever.api.map.MapService.ScenarioMapInfo.FILE_ENDING_SCRIPT;
import static com.faforever.api.map.MapService.ScenarioMapInfo.MANDATORY_FILES;
import static com.faforever.api.map.MapService.MapDetailInfo.MANDATORY_MAP_DETAILS;
import static java.text.MessageFormat.format;

@Service
@Slf4j
@AllArgsConstructor
public class MapService {
  private static final Pattern MAP_NAME_INVALID_CHARACTER_PATTERN = Pattern.compile("[a-zA-Z0-9\\- ]+");
  private static final Pattern MAP_NAME_DOES_NOT_START_WITH_LETTER_PATTERN = Pattern.compile("^[^a-zA-Z]+");
  private static final Pattern MAP_SIZE_PATTERN = Pattern.compile("^([0-9]{1,3})[ xX]{1,3}([0-9]{1,3})");
  private static final int MAP_NAME_MINUS_MAX_OCCURENCE = 3;
  private static final int MAP_NAME_MIN_LENGTH = 4;
  private static final int MAP_NAME_MAX_LENGTH = 50;

  private final FafApiProperties fafApiProperties;
  private final MapRepository mapRepository;
  private final ContentService contentService;

  @VisibleForTesting
  private final Set<String> _officialMapArchives = ImmutableSet.of(
    "btdata.ccx", "btmaps.ccx", "ccdata.ccx", "ccmaps.ccx", "ccmiss.ccx", "cdmaps.ccx", "rev31.gp3",
    "tactics1.hpi", "tactics2.hpi", "tactics3.hpi", "tactics4.hpi", "tactics5.hpi", "tactics6.hpi", "tactics7.hpi", "tactics8.hpi",
    "totala1.hpi", "totala2.hpi", "totala3.hpi", "totala4.hpi", "worlds.hpi", "afark.ufo", "aflea.ufo", "ascarab.ufo",
    "cometctr.ufo", "cormabm.ufo", "cornecro.ufo", "corplas.ufo", "evadrivd.ufo", "example.ufo", "floggen.ufo", "mndsmars.ufo",
    "tademo.ufo");

  private final Set<String> officialMapArchives = new HashSet<String>(_officialMapArchives);

  public boolean isOfficialArchive(String archiveName) {
    return officialMapArchives.stream().anyMatch(name -> name.equalsIgnoreCase(archiveName));
  }

  public MapNameValidationResponse requestMapNameValidation(String mapName) {
    Assert.notNull(mapName, "The map name is mandatory.");

    validateMapName(mapName);
    MapNameBuilder mapNameBuilder = new MapNameBuilder(mapName);

    int nextVersion = mapRepository.findOneByDisplayName(mapNameBuilder.getDisplayName())
      .map(map -> map.getVersions().stream()
        .mapToInt(MapVersion::getVersion)
        .map(i -> i + 1)
        .max()
        .orElse(1))
      .orElse(1);

    return MapNameValidationResponse.builder()
      .displayName(mapNameBuilder.getDisplayName())
      .nextVersion(nextVersion)
      .folderName(mapNameBuilder.buildFolderName(nextVersion))
      .fileNames(
        FileNames.builder()
          .scmap(mapNameBuilder.buildFileName(FILE_ENDING_MAP))
          .scenarioLua(mapNameBuilder.buildFileName(FILE_ENDING_SCENARIO))
          .scriptLua(mapNameBuilder.buildFileName(FILE_ENDING_SCRIPT))
          .saveLua(mapNameBuilder.buildFileName(FILE_ENDING_SAVE))
          .build()
      )
      .build();
  }

  @VisibleForTesting
  void validateMapName(String mapName) {
    List<Error> errors = new ArrayList<>();

    if (!MAP_NAME_INVALID_CHARACTER_PATTERN.matcher(mapName).matches()) {
      errors.add(new Error(ErrorCode.MAP_NAME_INVALID_CHARACTER));
    }

    if (mapName.length() < MAP_NAME_MIN_LENGTH) {
      errors.add(new Error(ErrorCode.MAP_NAME_TOO_SHORT, MAP_NAME_MIN_LENGTH, mapName.length()));
    }

    if (mapName.length() > MAP_NAME_MAX_LENGTH) {
      errors.add(new Error(ErrorCode.MAP_NAME_TOO_LONG, MAP_NAME_MAX_LENGTH, mapName.length()));
    }

    if (StringUtils.countOccurrencesOf(mapName, "-") > MAP_NAME_MINUS_MAX_OCCURENCE) {
      errors.add(new Error(ErrorCode.MAP_NAME_INVALID_MINUS_OCCURENCE, MAP_NAME_MINUS_MAX_OCCURENCE));
    }

    if (MAP_NAME_DOES_NOT_START_WITH_LETTER_PATTERN.matcher(mapName).find()) {
      errors.add(new Error(ErrorCode.MAP_NAME_DOES_NOT_START_WITH_LETTER));
    }

    if (!errors.isEmpty()) {
      throw new ApiException(errors.toArray(new Error[0]));
    }
  }

  private String _getNewVersionedArchiveName(String archiveFileName) {
    String basename = FilenameUtils.getBaseName(archiveFileName);
    String extension = FilenameUtils.getExtension(archiveFileName);
    int version = 1;

    Path finalArchiveDestination = fafApiProperties.getMap().getTargetDirectory().resolve(archiveFileName);
    while (Files.exists(finalArchiveDestination)) {
      ++version;
      finalArchiveDestination = fafApiProperties.getMap().getTargetDirectory().resolve(
        String.format("%s.v%04d.%s", basename, version, extension));
    }

    if (version == 1) {
      return archiveFileName;
    }
    else {
      return String.format("%s.v%04d.%s", basename, version, extension);
    }
  }

  @Transactional
  @SneakyThrows
  @CacheEvict(value = {Map.TYPE_NAME, MapVersion.TYPE_NAME}, allEntries = true)
  public void uploadMap(InputStream mapDataInputStream, Player author, boolean isRanked, List<java.util.Map<String,String>> mapsDetails) {
    Assert.notNull(author, "'author' must not be null");
    Assert.isTrue(mapDataInputStream.available() > 0, "'mapData' must not be empty");

    checkAuthorVaultBan(author);

    Path rootTempFolder = contentService.createTempDir();
    long tempFreeSpace = rootTempFolder.toFile().getFreeSpace();
    long mapsFreeSpace = fafApiProperties.getMap().getTargetDirectory().toFile().getFreeSpace();
    log.info("[uploadMap] rootTempFolder='{}'. temp free space={}GB; maps directory free space={}GB; archive size={}kB",
      rootTempFolder, (int)(tempFreeSpace/1e9), (int)(mapsFreeSpace/1e9), (int)(mapDataInputStream.available()/1e3));
    if (tempFreeSpace < 10*mapDataInputStream.available() || mapsFreeSpace < 10*mapDataInputStream.available()) {
      throw ApiException.of(ErrorCode.SERVER_DISK_FULL);
    }

    try (mapDataInputStream) {
      Path unzippedFileFolder = unzipToTemporaryDirectory(mapDataInputStream, rootTempFolder);
      log.info("[uploadMap] unzippedFileFolder=''{}''", unzippedFileFolder);
      Path mapFolder = validateMapFolderStructure(unzippedFileFolder);
      log.info("[uploadMap] mapFolder=''{}''", mapFolder);
      String archiveFileName = mapFolder.getFileName().toString();  // the directory and the archive located within it are named the same
      if (isOfficialArchive(archiveFileName)) {
        throw ApiException.of(ErrorCode.MAP_ARCHIVE_OFFICIAL, archiveFileName);
      }
      log.info("[uploadMap] archiveFileName=''{}''", archiveFileName);
      validateRequiredFiles(mapFolder, MANDATORY_FILES);

      validateMapsDetails(mapsDetails);
      java.util.Map<String, Optional<Map>> existingMaps = new HashMap<>();
      for (java.util.Map<String,String> mapDetails: mapsDetails) {
        String mapName = mapDetails.get("name");
        validateMapDetails(mapDetails, MANDATORY_MAP_DETAILS);
        existingMaps.put(mapDetails.get("name"), mapRepository.findOneByDisplayName(mapName));
        if (!mapDetails.get("archive").equals(archiveFileName)) {
          throw ApiException.of(ErrorCode.MAP_DETAIL_ARCHIVE_NAME_MISMATCH, mapName, mapDetails.get("archive"), archiveFileName);
        }
        if (existingMaps.get(mapName).isPresent()) {
          validateAgainstExistingMap(existingMaps.get(mapName).get(), author, mapDetails.get("crc"));
        }
      }

      java.util.Set<Path> mapPreviewFileNameSet = mapsDetails.stream()
        .map(mapDetails -> Paths.get(mapDetails.get("name") + ".png"))
        .collect(Collectors.toSet());

      java.util.Set<Path> presentPreviewFileNameSet;
      try(Stream<Path> paths = Files.walk(Paths.get(mapFolder.resolve("mini").toString()))) {
        presentPreviewFileNameSet = paths
          .filter(p -> Files.isRegularFile(p))
          .map(p -> Paths.get(URLDecoder.decode(p.getFileName().toString(), StandardCharsets.ISO_8859_1)))
          .collect(Collectors.toSet());
      }
      java.util.Set<Path> missingPreviewFileNameSet = new java.util.HashSet<>(mapPreviewFileNameSet);
      missingPreviewFileNameSet.removeAll(presentPreviewFileNameSet);
      if (!missingPreviewFileNameSet.isEmpty()) {
        throw ApiException.of(ErrorCode.MAP_MISSING_PREVIEW, missingPreviewFileNameSet, missingPreviewFileNameSet.size()-1);
      }

      String versionedArchiveName = _getNewVersionedArchiveName(archiveFileName);
      log.info("[uploadMap] versionedArchiveName=''{}''", versionedArchiveName);

      for (java.util.Map<String,String> mapDetails: mapsDetails) {
        String mapName = mapDetails.get("name");
        updateHibernateMapEntities(versionedArchiveName, mapDetails, existingMaps.get(mapName), author, isRanked);
      }

      Path finalPath = fafApiProperties.getMap().getTargetDirectory().resolve(versionedArchiveName);
      log.info("[uploadMap] finalPath=''{}''", finalPath);
      copyMapArchive(mapFolder.resolve(archiveFileName), finalPath);

      Path previewPath = fafApiProperties.getMap().getDirectoryPreviewPath();
      Files.createDirectories(previewPath, FilePermissionUtil.directoryPermissionFileAttributes());
      try(Stream<Path> paths = Files.walk(Paths.get(mapFolder.resolve("mini").toString()))) {
        paths
          .map(p -> new Path[] {p, Paths.get(URLDecoder.decode(p.getFileName().toString(), StandardCharsets.ISO_8859_1))})
          .filter(p -> Files.isRegularFile(p[0]) && mapPreviewFileNameSet.contains(p[1]))
          .forEach(p -> {
            try {  Files.copy(p[0], previewPath.resolve(p[1])); }
            catch (IOException e) { log.warn("unable to copy '{0}' to '{1}'", p[0], p[1]); }
          });
      }

    } finally {
      FileSystemUtils.deleteRecursively(rootTempFolder);
    }
  }

  private void checkAuthorVaultBan(Player author) {
    author.getActiveBanOf(BanLevel.VAULT)
      .ifPresent((banInfo) -> {
        String message = banInfo.getDuration() == BanDurationType.PERMANENT ?
          "You are permanently banned from uploading maps to the vault." :
          format("You are banned from uploading maps to the vault until {0}.", banInfo.getExpiresAt());
        throw HttpClientErrorException.create(message, HttpStatus.FORBIDDEN, "Upload forbidden",
          HttpHeaders.EMPTY, null, null);
      });
  }

  private Path unzipToTemporaryDirectory(InputStream mapDataInputStream, Path rootTempFolder)
    throws IOException, ArchiveException {
    Path unzippedDirectory = Files.createDirectories(rootTempFolder.resolve("unzipped-content"));
    log.debug("Unzipping uploaded file ''{}'' to: {}", mapDataInputStream, unzippedDirectory);

    Unzipper.from(mapDataInputStream)
      .zipBombByteCountThreshold(5_000_000)
      .zipBombProtectionFactor(200)
      .to(unzippedDirectory)
      .unzip();

    return unzippedDirectory;
  }

  /**
   * @param zipContentFolder The folder containing the content of the zipped map file
   * @return the root folder of the map
   */
  private Path validateMapFolderStructure(Path zipContentFolder) throws IOException {
    Path mapFolder;

    try (Stream<Path> mapFolderStream = Files.list(zipContentFolder)) {
      mapFolder = mapFolderStream
        .filter(Files::isDirectory)
        .findFirst()
        .orElseThrow(() -> ApiException.of(ErrorCode.MAP_MISSING_MAP_FOLDER_INSIDE_ZIP));
    }

    try (Stream<Path> mapFolderStream = Files.list(zipContentFolder)) {
      if (mapFolderStream.count() != 1) {
        throw ApiException.of(ErrorCode.MAP_INVALID_ZIP);
      }
    }

    Path archiveFileName = mapFolder.getFileName();
    if (!Files.isRegularFile(mapFolder.resolve(archiveFileName))) {
      throw ApiException.of(ErrorCode.MAP_MISSING_ARCHIVE_INSIDE_MAP_FOLDER, archiveFileName);
    }

    return mapFolder;
  }

  @SneakyThrows
  private void validateRequiredFiles(Path mapFolder, String[] requiredFiles) {
    try (Stream<Path> mapFileStream = Files.list(mapFolder)) {
      List<String> fileNames = mapFileStream
        .map(Path::toString)
        .collect(Collectors.toList());

      List<Error> errors = Arrays.stream(requiredFiles)
        .filter(requiredEnding -> fileNames.stream().noneMatch(fileName -> fileName.endsWith(requiredEnding)))
        .map(requiredEnding -> new Error(ErrorCode.MAP_FILE_INSIDE_ZIP_MISSING, requiredEnding))
        .collect(Collectors.toList());

      if (!errors.isEmpty()) {
        throw ApiException.of(errors);
      }
    }
  }

  private void validateMapsDetails(List<java.util.Map<String,String>> mapsDetails) {
    if (mapsDetails.isEmpty()) {
      List<Error> errors = new ArrayList<>(List.of(new Error(ErrorCode.MAP_DETAIL_EMPTY)));
      throw ApiException.of(errors);
    }
  }

  @VisibleForTesting
  void validateMapDetails(java.util.Map<String,String> mapDetails, java.util.Map<String,Pair<java.util.regex.Pattern,String>> requiredKeyValueRegexes) {
    List<Error> errors;
    String mapName = mapDetails.getOrDefault("name", "<unknown>");

    errors = requiredKeyValueRegexes.entrySet().stream()
      .filter(requiredEntry -> !mapDetails.containsKey(requiredEntry.getKey()))
      .map(requiredEntry -> new Error(ErrorCode.MAP_DETAIL_MISSING_KEY, mapName, requiredEntry.getKey()))
      .collect(Collectors.toList());
    if (!errors.isEmpty()) {
      throw ApiException.of(errors);
    }

    errors = requiredKeyValueRegexes.entrySet().stream()
      .filter(requiredEntry -> !requiredEntry.getValue().getFirst().matcher(mapDetails.get(requiredEntry.getKey())).find())
      .map(requiredEntry -> new Error(ErrorCode.MAP_DETAIL_BAD_KEY, mapName, requiredEntry.getKey(), mapDetails.get(requiredEntry.getKey()), requiredEntry.getValue().getSecond()))
      .collect(Collectors.toList());
    if (!errors.isEmpty()) {
      throw ApiException.of(errors);
    }
  }

  private void validateAgainstExistingMap(Map existingMap, Player author, String newCrc32) {
    Optional.ofNullable(existingMap.getAuthor())
      .filter(existingMapAuthor -> !Objects.equals(existingMapAuthor, author))
      .ifPresent(existingMapAuthor -> {
        throw ApiException.of(ErrorCode.MAP_NOT_ORIGINAL_AUTHOR, existingMap.getDisplayName());
      });

    if (existingMap.getVersions().stream()
      .anyMatch(mapVersion -> mapVersion.getFilename().split("/")[2].equals(newCrc32))) {
      //throw ApiException.of(ErrorCode.MAP_VERSION_EXISTS, existingMap.getDisplayName(), newCrc32);
    }

    if (existingMap.getVersions().stream()
      .anyMatch(mapVersion -> officialMapArchives.contains(mapVersion.getFilename().split("/")[0]))) {
      throw ApiException.of(ErrorCode.MAP_ARCHIVE_OFFICIAL, existingMap.getDisplayName());
    }
  }

  static private Integer[] getMapSize(String size, String description) {
    for (String s: List.of(size, description)) {
      java.util.regex.Matcher m = MAP_SIZE_PATTERN.matcher(s);
      if (m.find()) {
        try {
          Integer[] result = { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) };
          return result;
        }
        catch (NumberFormatException e) { }
        catch (IndexOutOfBoundsException e) { }
      }
    }

    return new Integer[] {8,8};
  }

  static private int getMaxPlayers(String players) {
    // from all kinds of nasty things eg "2,4,6,8" or "1v1 or 2v2"
    int maxPlayers = java.util.Arrays.stream(players.replaceAll("[^0-9v]+", ",").split(","))
      .map(s -> java.util.Arrays.stream(s.split("v"))
        .map(v -> {
          try { return Integer.parseInt(v); }
          catch (NumberFormatException ex) { return 0; }
        })
        .reduce(0, (a,b) -> a+b))
      .reduce(0, (a,b) -> a>b ? a : b);

    return maxPlayers >= 2 ? maxPlayers : 10;
  }

  private Map updateHibernateMapEntities(String archiveName, java.util.Map<String,String> mapDetails, Optional<Map> existingMapOptional, Player author, boolean isRanked) {
    // mapDetails is supposed to be validated already

    Map map = existingMapOptional
      .orElseGet(() ->
        new Map()
          .setDisplayName(mapDetails.get("name"))
          .setAuthor(author)
      );

    map
      .setMapType("FFA")
      .setBattleType("skirmish");

    Integer[] size = getMapSize(mapDetails.getOrDefault("size", ""), mapDetails.get("description"));

    Optional<MapVersion> version = map.getVersions().stream()
      .filter(v -> v.getCrc().equals(mapDetails.get("crc")))
      .findAny();

    if (!version.isPresent()) {
      int newVersionNumber = 1+map.getVersions().stream()
        .map(v -> v.getVersion())
        .reduce(0, (a, b) -> a>b ? a : b);
      MapVersion newVersion = new MapVersion().setVersion(newVersionNumber);
      map.getVersions().add(newVersion);
      version = Optional.of(newVersion);
    }

    version.get()
      .setDescription(mapDetails.get("description"))
      .setWidth(size[0])
      .setHeight(size[1])
      .setHidden(false)
      .setRanked(isRanked)
      .setMaxPlayers(getMaxPlayers(mapDetails.getOrDefault("players","10")))
      .setMap(map)
      .setFilename(String.format("%s/%s/%s", archiveName, mapDetails.get("name"), mapDetails.get("crc")));

    // this triggers validation
    mapRepository.save(map);

    return map;
  }

  private void zipMapData(Path newMapFolder, Path finalZipPath) throws IOException, ArchiveException {
    Files.createDirectories(finalZipPath.getParent(), FilePermissionUtil.directoryPermissionFileAttributes());
    Zipper
      .of(newMapFolder)
      .to(finalZipPath)
      .zip();
    // TODO if possible, this should be done using umask instead
    FilePermissionUtil.setDefaultFilePermission(finalZipPath);
  }

  private void copyMapArchive(Path newMapArchive, Path finalArchivePath) throws IOException {
    Files.createDirectories(finalArchivePath.getParent(), FilePermissionUtil.directoryPermissionFileAttributes());
    Files.copy(newMapArchive, finalArchivePath);
    // TODO if possible, this should be done using umask instead
    FilePermissionUtil.setDefaultFilePermission(finalArchivePath);
  }

  static class ScenarioMapInfo {
    static final String FILE_ENDING_SCENARIO = "_scenario.lua";
    static final String FILE_ENDING_MAP = ".ufo";
    static final String DIRECTORY_MINIMAPS = "mini";
    static final String FILE_ENDING_SAVE = "_save.lua";
    static final String FILE_ENDING_SCRIPT = "_script.lua";

    static final String[] MANDATORY_FILES = new String[]{
      DIRECTORY_MINIMAPS,
      FILE_ENDING_MAP
    };
  }

  static class MapDetailInfo {
    static final java.util.regex.Pattern REGEX_CRC32 = java.util.regex.Pattern.compile("^[0-9a-f]{8}$");
    static final java.util.regex.Pattern REGEX_NOT_EMPTY = java.util.regex.Pattern.compile(".{1,128}");
    static final java.util.regex.Pattern REGEX_NO_SLASHES = java.util.regex.Pattern.compile("^[^/]{1,64}$");
    static final java.util.regex.Pattern REGEX_HPI_ARCHIVE = java.util.regex.Pattern.compile("^[0-9A-Za-z \\(\\)\\-_+=\\[\\]\\{\\}',.]{1,64}\\.(ccx|ufo|hpi)$");
    static final java.util.Map<String, Pair<java.util.regex.Pattern, String>> MANDATORY_MAP_DETAILS = java.util.Map.of(
      "name", Pair.of(REGEX_NO_SLASHES, "map names contain no slashes ('/') and are between 1 and 64 characters long"),
      "archive", Pair.of(REGEX_HPI_ARCHIVE, "archive filenames have .ccx, .ufo or .hpi extension; contain only characters 0-9, A-Z, a-z, ()[]{}+-=_,. or space; and are between 1 and 64 characters long"),
      "crc", Pair.of(REGEX_CRC32, "crc is exactly 8 characters long and consists of lower-case hexidecimal digits (0-9, a-f) only"),
      "description", Pair.of(REGEX_NOT_EMPTY, "map's description is not empty"));
  }

  private class MapNameBuilder {
    @Getter
    private final String displayName;
    private final String normalizedDisplayName;
    private String folderName;

    private MapNameBuilder(String displayName) {
      this.displayName = displayName;
      this.normalizedDisplayName = NameUtil.normalizeWhitespaces(displayName.toLowerCase());
    }

    String buildFolderNameWithoutVersion() {
      return normalizedDisplayName;
    }

    String buildFolderName(int version) {
      if (folderName == null) {
        folderName = String.format("%s.v%04d", normalizedDisplayName, version);
      }

      return folderName;
    }

    String buildFileName(String fileEnding) {
      return normalizedDisplayName + fileEnding;
    }

    String buildFinalZipName(int version) {
      return buildFolderName(version) + ".zip";
    }

    Path buildFinalZipPath(int version) {
      return fafApiProperties.getMap().getTargetDirectory().resolve(buildFinalZipName(version));
    }
  }
}
