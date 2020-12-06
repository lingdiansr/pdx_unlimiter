package com.crschnick.pdx_unlimiter.app.game;

import com.crschnick.pdx_unlimiter.app.installation.ErrorHandler;
import com.crschnick.pdx_unlimiter.app.savegame.FileImportTarget;
import com.crschnick.pdx_unlimiter.app.util.JsonHelper;
import com.crschnick.pdx_unlimiter.app.util.WatcherHelper;
import com.crschnick.pdx_unlimiter.eu4.data.GameVersion;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class GameInstallation {

    public static Eu4Installation EU4 = null;
    public static Hoi4Installation HOI4 = null;
    public static Ck3Installation CK3 = null;
    public static StellarisInstallation STELLARIS = null;

    protected ObjectProperty<List<FileImportTarget>> savegames = new SimpleObjectProperty<>();
    protected DistributionType distType;
    private Path path;
    private List<GameDlc> dlcs = new ArrayList<>();
    protected List<GameMod> mods = new ArrayList<>();
    protected GameVersion version;

    public GameInstallation(Path path) {
        this.path = path;
    }

    public static void initInstallations() throws Exception {
        if (EU4 != null) {
            EU4.init();
        }

        if (HOI4 != null) {
            HOI4.init();
        }

        if (STELLARIS != null) {
            STELLARIS.init();
        }

        if (CK3 != null) {
            CK3.init();
        }

        initInstallationsOptional();
    }

    public static void initInstallationsOptional() {
        if (EU4 != null) {
            try {
                EU4.initOptional();
            } catch (Exception e) {
                ErrorHandler.handleException(e);
            }
        }

        if (HOI4 != null) {
            try {
                HOI4.initOptional();
            } catch (Exception e) {
                ErrorHandler.handleException(e);
            }
        }

        if (STELLARIS != null) {
            try {
                STELLARIS.initOptional();
            } catch (Exception e) {
                ErrorHandler.handleException(e);
            }
        }

        if (CK3 != null) {
            try {
                CK3.initOptional();
            } catch (Exception e) {
                ErrorHandler.handleException(e);
            }
        }
    }

    private List<Path> getAllSavegameDirectories() {
        List<Path> savegameDirs = new ArrayList<>();
        savegameDirs.add(getSavegamesPath());
        savegameDirs.addAll(getDistType().getSavegamePaths());
        return savegameDirs;
    }

    public void initOptional() throws Exception {
        LoggerFactory.getLogger(getClass()).debug("Initializing optional data");
        loadDlcs();
        loadMods();
        savegames.set(getLatestSavegames());

        List<Path> savegameDirs = getAllSavegameDirectories();

        WatcherHelper.startWatchersInDirectories("Savegame watcher", savegameDirs, (p) -> {
            savegames.set(getLatestSavegames());
        });
        LoggerFactory.getLogger(getClass()).debug("Finished initializing optional data\n");
    }

    public void startLauncher() {
        getDistType().launch();
    }

    public void startDirect() {
        getDistType().launch();
    }

    public void writeDlcLoadFile(List<GameMod> mods, List<GameDlc> dlcs) throws IOException {
        var out = Files.newOutputStream(getUserPath().resolve("dlc_load.json"));
        ObjectNode n = JsonNodeFactory.instance.objectNode();
        n.putArray("enabled_mods").addAll(mods.stream()
                .map(m -> m.getModFile().toString())
                .map(JsonNodeFactory.instance::textNode)
                .collect(Collectors.toList()));
        n.putArray("disabled_dlcs").addAll(this.dlcs.stream()
                .filter(d -> d.isExpansion() && !dlcs.contains(d))
                .map(d -> d.getInfoFilePath().toString())
                .map(JsonNodeFactory.instance::textNode)
                .collect(Collectors.toList()));
        JsonHelper.write(n, out);
    }

    private void loadDlcs() throws IOException {
        Files.list(getDlcPath()).forEach(f -> {
            try {
                GameDlc.fromDirectory(f).ifPresent(d -> dlcs.add(d));
            } catch (Exception e) {
                ErrorHandler.handleException(e);
            }
        });
    }

    private void loadMods() throws IOException {
        if (!Files.isDirectory(getUserPath().resolve("mod"))) {
            return;
        }

        Files.list(getUserPath().resolve("mod")).forEach(f -> {
            try {
                GameMod.fromFile(f).ifPresent(m -> {
                    if (Files.exists(m.getPath())) mods.add(m);
                    LoggerFactory.getLogger(getClass()).debug("Found mod " + m.getName() +
                            " at " + m.getModFile().toString()+ ". Content exists: " + Files.exists(m.getPath()));
                });
            } catch (Exception e) {
                ErrorHandler.handleException(e);
            }
        });
    }

    private List<FileImportTarget> getLatestSavegames() {
        return getAllSavegameDirectories().stream().map(d -> {
                    try {
                        return Files.list(d);
                    } catch (IOException e) {
                        return Collections.<Path>emptyList().stream();
                    }
                })
                .flatMap(Stream::distinct)
                .sorted(Comparator.comparingLong(p -> {
                    try {
                        return Long.MAX_VALUE - Files.getLastModifiedTime(p).toMillis();
                    } catch (IOException e) {
                        ErrorHandler.handleException(e);
                        return 0;
                    }
                }))
                .map(FileImportTarget::create)
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());
    }

    protected Path replaceVariablesInPath(String value) {
        if (SystemUtils.IS_OS_WINDOWS) {
            value = value.replace("%USER_DOCUMENTS%",
                    Paths.get(System.getProperty("user.home"), "Documents").toString());
        } else if (SystemUtils.IS_OS_LINUX) {
            value = value.replace("$LINUX_DATA_HOME",
                    Paths.get(System.getProperty("user.home"), ".local", "share").toString());
        }
        return Path.of(value);
    }

    public Optional<GameDlc> getDlcForName(String name) {
        return dlcs.stream().filter(d -> d.getName().equals(name)).findAny();
    }

    public DistributionType getDistType() {
        return distType;
    }

    public Path getDlcPath() {
        return getPath().resolve("dlc");
    }

    public abstract void writeLaunchConfig(String name, Instant lastPlayed, Path path) throws IOException;

    public abstract Optional<GameMod> getModForName(String name);

    public abstract void start();

    public abstract void init() throws Exception;

    public abstract boolean isValid();

    protected Path getLauncherDataPath() {
        return getPath();
    }

    public Path getPath() {
        return path;
    }

    public abstract Path getExecutable();

    public abstract Path getUserPath();

    public abstract Path getSavegamesPath();

    public List<FileImportTarget> getSavegames() {
        return savegames.get();
    }

    public ObjectProperty<List<FileImportTarget>> savegamesProperty() {
        return savegames;
    }

    public GameVersion getVersion() {
        return version;
    }
}
