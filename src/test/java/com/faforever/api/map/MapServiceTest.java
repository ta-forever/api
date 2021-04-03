package com.faforever.api.map;

import com.faforever.api.config.FafApiProperties;
import com.faforever.api.config.FafApiProperties.Map;
import com.faforever.api.content.ContentService;
import com.faforever.api.data.domain.BanInfo;
import com.faforever.api.data.domain.BanLevel;
import com.faforever.api.data.domain.MapVersion;
import com.faforever.api.data.domain.Player;
import com.faforever.api.error.ApiException;
import com.faforever.api.error.Error;
import com.faforever.api.error.ErrorCode;
import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.thymeleaf.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.faforever.api.error.ApiExceptionMatcher.hasErrorCode;
import static com.faforever.api.error.ErrorCode.MAP_NAME_DOES_NOT_START_WITH_LETTER;
import static com.faforever.api.error.ErrorCode.MAP_NAME_INVALID_CHARACTER;
import static com.faforever.api.error.ErrorCode.MAP_NAME_INVALID_MINUS_OCCURENCE;
import static com.faforever.api.error.ErrorCode.MAP_NAME_TOO_LONG;
import static com.faforever.api.error.ErrorCode.MAP_NAME_TOO_SHORT;
import static com.faforever.api.error.ErrorCode.MAP_SCRIPT_LINE_MISSING;
import static com.faforever.api.map.MapService.MapDetailInfo.MANDATORY_MAP_DETAILS;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MapServiceTest {
  private Path temporaryDirectory;
  private Path finalDirectory;

  @Mock
  private MapRepository mapRepository;
  @Mock
  private FafApiProperties fafApiProperties;
  @Mock
  private ContentService contentService;
  @Mock
  private Player author;

  private MapService instance;
  private Map mapProperties;

  @BeforeEach
  void beforeEach() {
    instance = new MapService(fafApiProperties, mapRepository, contentService);
  }

  private String loadMapAsString(String filename) throws IOException {
    return new String(loadMapAsBytes(filename), StandardCharsets.UTF_8);
  }

  private InputStream loadMapAsInputSteam(String filename) {
    return MapServiceTest.class.getResourceAsStream("/maps/" + filename);
  }

  private byte[] loadMapAsBytes(String filename) throws IOException {
    try (InputStream inputStream = MapServiceTest.class.getResourceAsStream("/maps/" + filename)) {
      return ByteStreams.toByteArray(inputStream);
    }
  }

  @Nested
  class Validation {

    @ParameterizedTest
    @CsvSource(value={
      "[NLJ] 10th Anniversary/[NLJ]-Maps-Pack-2019.ufo/cbefbd4f/16 x 16 map - wind +20-30e -tidal +25e - map by \"nlj\" 1v1, 2v2, 3v3, 4v4, 5v5",
      "[NLJ] 20th Anniversary/[NLJ]-Maps-Pack-2019.ufo/e0a11b0a/19 x 19 map wind +20-30e -tidal +20e - map by \\\"nlj\\\" 1v1, 2v2, 3v3, 4v4, 5v5"
    }, delimiterString="/")
    void testMapDetailCrcValid(String name, String archive, String crc, String description) {
      java.util.Map<String,String> mapDetails = java.util.Map.of("name",name,"archive", archive, "crc", crc, "description", description);
      instance.validateMapDetails(mapDetails, MANDATORY_MAP_DETAILS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "Map1",
      "map-2",
      "A very but overall not really too long map name",
      "Three - dashes - are - allowed"
    })
    void testMapNameValid(String name) {
      instance.validateMapName(name);
    }

    @Test
    void testMapNameMultiErrorsButNoScenarioValidation() {
      String mapName = "123Invalid-in$-many-ways-atOnce" + StringUtils.repeat("x", 50);
      ApiException result = assertThrows(ApiException.class, () -> instance.validateMapName(mapName));
      List<ErrorCode> errorCodes = Arrays.stream(result.getErrors()).map(Error::getErrorCode).collect(Collectors.toList());
      assertThat(errorCodes, hasItems(MAP_NAME_INVALID_CHARACTER, MAP_NAME_TOO_LONG, MAP_NAME_INVALID_MINUS_OCCURENCE));
      assertThat(errorCodes, not(contains(MAP_SCRIPT_LINE_MISSING)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "Map.With.Dots",
      "Map/With/Slashes",
      "Map,With,Commas",
      "Map|With|Pipes",
      "SomeMore:",
      "SomeMore(",
      "SomeMore)",
      "SomeMore[",
      "SomeMore]",
      "SomeMore?",
      "SomeMore$",
    })
    void testMapNameInvalidChars(String name) {
      ApiException result = assertThrows(ApiException.class, () -> instance.validateMapName(name));
      assertThat(result, hasErrorCode(MAP_NAME_INVALID_CHARACTER));
    }

    @Test
    void testMapNameTooManyDashes() {
      ApiException result = assertThrows(ApiException.class, () -> instance.validateMapName("More-than-three-dashes-invalid"));
      assertThat(result, hasErrorCode(MAP_NAME_INVALID_MINUS_OCCURENCE));
    }

    @Test
    void testMapNameToShort() {
      ApiException result = assertThrows(ApiException.class, () -> instance.validateMapName("x"));
      assertThat(result, hasErrorCode(MAP_NAME_TOO_SHORT));
    }

    @Test
    void testMapNameToLong() {
      ApiException result = assertThrows(ApiException.class, () -> instance.validateMapName(StringUtils.repeat("x", 51)));
      assertThat(result, hasErrorCode(MAP_NAME_TOO_LONG));
    }

    @Test
    void testMapNameStartsInvalid() {
      ApiException result = assertThrows(ApiException.class, () -> instance.validateMapName("123x"));
      assertThat(result, hasErrorCode(MAP_NAME_DOES_NOT_START_WITH_LETTER));
    }

    @Test
    void authorBannedFromVault() {
      when(author.getActiveBanOf(BanLevel.VAULT)).thenReturn(Optional.of(
        new BanInfo()
          .setLevel(BanLevel.VAULT)
      ));

      InputStream mapData = loadMapAsInputSteam("Beta Tropics (Coasts).tar");
      assertThrows(Forbidden.class, () -> instance.uploadMap(mapData, author, true, List.of(java.util.Map.of(
        "name", "Beta Tropics (Coasts)", "description", "a map", "crc", "deadbeef", "archive", "Beta Tropics (Coasts).ufo"))));
      verify(mapRepository, never()).save(any(com.faforever.api.data.domain.Map.class));
    }
  }

  @Nested
  class WithTempDir {
    @TempDir
    Path baseTemporaryDirectory;

    @BeforeEach
    void setUp() throws Exception {
      temporaryDirectory = Files.createDirectory(baseTemporaryDirectory.resolve("temp"));
      finalDirectory = Files.createDirectory(baseTemporaryDirectory.resolve("final"));

      mapProperties = new Map()
        .setTargetDirectory(finalDirectory)
        .setDirectoryPreviewPath(finalDirectory.resolve("mini"));
      when(contentService.createTempDir()).thenReturn(temporaryDirectory);
    }

    @ParameterizedTest(name = "Expecting ErrorCode.{0} with file ''{1}''")
    @CsvSource(value = {
      "MAP_ARCHIVE_OFFICIAL,map_archive_official.tar,valid",
      "MAP_MISSING_ARCHIVE_INSIDE_MAP_FOLDER,map_file_inside_zip_missing.tar,valid",
      "MAP_MISSING_MAP_FOLDER_INSIDE_ZIP,new_text_document.tar,valid",
      "MAP_DETAIL_EMPTY,Beta Tropics (Coasts).tar,empty",
      "MAP_DETAIL_MISSING_KEY,Beta Tropics (Coasts).tar,noname",
      "MAP_DETAIL_MISSING_KEY,Beta Tropics (Coasts).tar,nodescription",
      "MAP_DETAIL_MISSING_KEY,Beta Tropics (Coasts).tar,nocrc",
      "MAP_DETAIL_MISSING_KEY,Beta Tropics (Coasts).tar,noarchive",
      "MAP_DETAIL_BAD_KEY,Beta Tropics (Coasts).tar,badname",
      "MAP_DETAIL_BAD_KEY,Beta Tropics (Coasts).tar,baddescription",
      "MAP_DETAIL_BAD_KEY,Beta Tropics (Coasts).tar,badcrc1",
      "MAP_DETAIL_BAD_KEY,Beta Tropics (Coasts).tar,badcrc2",
      "MAP_DETAIL_BAD_KEY,Beta Tropics (Coasts).tar,badarchive",
      "MAP_DETAIL_ARCHIVE_NAME_MISMATCH,Beta Tropics (Coasts).tar,wrongarchive",
      "MAP_MISSING_PREVIEW,wrong_preview_filename.tar,valid"
    })
    void uploadFails(String errorCodeEnumValue, String fileName, String detailsKey) {
      when(fafApiProperties.getMap()).thenReturn(mapProperties);
      java.util.Map<String,String> mapDetails = new java.util.HashMap<>();
      switch(detailsKey) {
        case "empty":
          uploadFails(ErrorCode.valueOf(errorCodeEnumValue), fileName, List.of());
          return;
        case "valid":
          mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "a map", "crc", "deadbeef", "archive", "Beta Tropics (Coasts).ufo");
          break;
        case "noname":
          mapDetails = java.util.Map.of("description", "a map", "crc", "deadbeef", "archive", "Beta Tropics (Coasts).ufo");
          break;
        case "nodescription":
          mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "crc", "deadbeef", "archive", "Beta Tropics (Coasts).ufo");
          break;
        case "nocrc":
          mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "a map", "archive", "Beta Tropics (Coasts).ufo");
          break;
        case "noarchive":
          mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "a map", "crc", "deadbeef");
          break;
        case "badname":
          mapDetails = java.util.Map.of("name", "Beta Tropics / Coasts", "description", "a map", "crc", "deadbeef", "archive", "Beta Tropics (Coasts).ufo");
          break;
        case "baddescription":
          mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "", "crc", "deadbeef", "archive", "Beta Tropics (Coasts).ufo");
          break;
        case "badcrc1":
          mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "a map", "crc", "uberl337", "archive", "Beta Tropics (Coasts).ufo");
          break;
        case "badcrc2":
          mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "a map", "crc", "123456789", "archive", "Beta Tropics (Coasts).ufo");
          break;
        case "badarchive":
          mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "a map", "crc", "deadbeef", "archive", "Beta Tropics (Coasts).zip");
          break;
        case "wrongarchive":
          mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "a map", "crc", "deadbeef", "archive", "Comet Catcher.ufo");
          break;
      }

      uploadFails(ErrorCode.valueOf(errorCodeEnumValue), fileName, List.of(mapDetails));
    }

    void uploadFails(ErrorCode expectedErrorCode, String fileName, List<java.util.Map<String,String>> mapsDetails) {
      InputStream mapData = loadMapAsInputSteam(fileName);
      ApiException result = assertThrows(ApiException.class, () -> instance.uploadMap(mapData, author, true, mapsDetails));
      assertThat(result, hasErrorCode(expectedErrorCode));
      verify(mapRepository, never()).save(any(com.faforever.api.data.domain.Map.class));
    }

    @Test
    void archiveAlreadyExists() throws IOException {
      when(fafApiProperties.getMap()).thenReturn(mapProperties);
      Path clashedMap = finalDirectory.resolve("Beta Tropics (Coasts).ufo");
      assertTrue(clashedMap.toFile().createNewFile());

      java.util.Map<String,String> mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "a map", "crc", "deadbeef", "archive", "Beta Tropics (Coasts).ufo");
      uploadFails(ErrorCode.MAP_NAME_CONFLICT, "Beta Tropics (Coasts).tar", List.of(mapDetails));
    }

    @Test
    void notCorrectAuthor() {
      when(fafApiProperties.getMap()).thenReturn(mapProperties);

      Player me = new Player();
      me.setId(1);
      Player bob = new Player();
      bob.setId(2);

      com.faforever.api.data.domain.Map map = new com.faforever.api.data.domain.Map().setAuthor(bob);
      when(mapRepository.findOneByDisplayName(any())).thenReturn(Optional.of(map));

      java.util.Map<String,String> mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "a map", "crc", "deadbeef", "archive", "Beta Tropics (Coasts).ufo");
      uploadFails(ErrorCode.MAP_NOT_ORIGINAL_AUTHOR, "Beta Tropics (Coasts).tar", List.of(mapDetails));
    }

    @Test
    void versionExistsAlready() {
      when(fafApiProperties.getMap()).thenReturn(mapProperties);

      com.faforever.api.data.domain.Map map = new com.faforever.api.data.domain.Map()
        .setDisplayName("Beta Tropics (Coasts)")
        .setAuthor(author)
        .setVersions(Collections.singletonList(new MapVersion()
          .setCrc("deadbeef")
          .setDescription("some map")
          .setFilename("original.ufo/Beta Tropics (Coasts)/deadbeef")
          .setHeight(10)
          .setWidth(10)
          .setHidden(false)
          .setMaxPlayers(8)
          .setName("Beta Tropics (Coasts)")
          .setRanked(true)
          .setVersion(13)
        ));
      InputStream mapData = loadMapAsInputSteam("Beta Tropics (Coasts).tar");

      when(mapRepository.findOneByDisplayName(any())).thenReturn(Optional.of(map));

      java.util.Map<String,String> mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "a map", "crc", "deadbeef", "archive", "Beta Tropics (Coasts).ufo");
      instance.uploadMap(mapData, author, true, List.of(mapDetails));

      ArgumentCaptor<com.faforever.api.data.domain.Map> mapCaptor = ArgumentCaptor.forClass(com.faforever.api.data.domain.Map.class);
      verify(mapRepository).save(mapCaptor.capture());
      assertEquals(1, mapCaptor.getValue().getVersions().size());
      assertEquals(13, mapCaptor.getValue().getVersions().get(0).getVersion());
      assertEquals("a map", mapCaptor.getValue().getVersions().get(0).getDescription());
      assertEquals("Beta Tropics (Coasts).ufo/Beta Tropics (Coasts)/deadbeef", mapCaptor.getValue().getVersions().get(0).getFilename());
    }

    @Test
    void newVersion() {
      when(fafApiProperties.getMap()).thenReturn(mapProperties);

      com.faforever.api.data.domain.Map map = new com.faforever.api.data.domain.Map()
        .setDisplayName("Beta Tropics (Coasts)")
        .setAuthor(author)
        .setVersions(new ArrayList<>());

      map.getVersions().add(new MapVersion()
          .setCrc("deadbeef")
          .setDescription("some map")
          .setFilename("original.ufo/Beta Tropics (Coasts)/deadbeef")
          .setHeight(10)
          .setWidth(10)
          .setHidden(false)
          .setMaxPlayers(8)
          .setName("Beta Tropics (Coasts)")
          .setRanked(true)
          .setVersion(13)
          );

      InputStream mapData = loadMapAsInputSteam("Beta Tropics (Coasts).tar");

      when(mapRepository.findOneByDisplayName(any())).thenReturn(Optional.of(map));

      java.util.Map<String,String> mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "a map", "crc", "deedbeef", "archive", "Beta Tropics (Coasts).ufo");
      instance.uploadMap(mapData, author, true, List.of(mapDetails));

      ArgumentCaptor<com.faforever.api.data.domain.Map> mapCaptor = ArgumentCaptor.forClass(com.faforever.api.data.domain.Map.class);
      verify(mapRepository).save(mapCaptor.capture());
      assertEquals(2, mapCaptor.getValue().getVersions().size());
      assertEquals(14, mapCaptor.getValue().getVersions().get(1).getVersion());
      assertEquals("a map", mapCaptor.getValue().getVersions().get(1).getDescription());
      assertEquals("Beta Tropics (Coasts).ufo/Beta Tropics (Coasts)/deedbeef", mapCaptor.getValue().getVersions().get(1).getFilename());
    }

    @Test
    void positiveUploadTest() throws Exception {
      String zipFilename = "Beta Tropics (Coasts).tar";
      when(fafApiProperties.getMap()).thenReturn(mapProperties);
      when(mapRepository.findOneByDisplayName(any())).thenReturn(Optional.empty());
      InputStream mapData = loadMapAsInputSteam(zipFilename);

      Path tmpDir = temporaryDirectory;
      java.util.Map<String,String> mapDetails = java.util.Map.of("name", "Beta Tropics (Coasts)", "description", "a map", "crc", "deadbeef", "archive", "Beta Tropics (Coasts).ufo");
      instance.uploadMap(mapData, author, true, List.of(mapDetails));

      ArgumentCaptor<com.faforever.api.data.domain.Map> mapCaptor = ArgumentCaptor.forClass(com.faforever.api.data.domain.Map.class);
      verify(mapRepository).save(mapCaptor.capture());
      assertEquals("Beta Tropics (Coasts)", mapCaptor.getValue().getDisplayName());
      assertEquals(1, mapCaptor.getValue().getVersions().size());

      MapVersion mapVersion = mapCaptor.getValue().getVersions().get(0);
      assertEquals("a map", mapVersion.getDescription());
      assertEquals(8, mapVersion.getHeight());
      assertEquals(8, mapVersion.getWidth());
      assertEquals(10, mapVersion.getMaxPlayers());
      assertEquals("Beta Tropics (Coasts).ufo/Beta Tropics (Coasts)/deadbeef", mapVersion.getFilename());

      assertFalse(Files.exists(tmpDir));

      Path finalArchive = finalDirectory.resolve("Beta Tropics (Coasts).ufo");
      assertTrue(Files.exists(finalArchive));

      assertTrue(Files.exists(mapProperties.getDirectoryPreviewPath().resolve("Beta Tropics (Coasts).png")));
    }
  }
}
