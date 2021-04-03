package com.crschnick.pdx_unlimiter.app.savegame;

import com.crschnick.pdx_unlimiter.app.core.FileWatchManager;
import com.crschnick.pdx_unlimiter.app.installation.Game;
import com.crschnick.pdx_unlimiter.app.installation.GameInstallation;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SavegameWatcher {

    public static final BidiMap<Game, SavegameWatcher> ALL = new DualHashBidiMap<>();

    private final GameInstallation install;
    private final ListProperty<FileImportTarget> savegames = new SimpleListProperty<>(
            FXCollections.observableArrayList());

    private SavegameWatcher(GameInstallation install) {
        this.install = install;
    }

    public static void init() {
        for (var g : Game.values()) {
            var install = GameInstallation.ALL.get(g);
            if (install != null) {
                ALL.put(g, new SavegameWatcher(install));
                ALL.get(g).initSavegames();
            }
        }
    }

    public static void reset() {
        ALL.clear();
    }

    private void initSavegames() {
        savegames.get().setAll(getLatestSavegames());

        List<Path> savegameDirs = install.getAllSavegameDirectories();
        FileWatchManager.getInstance().startWatchersInDirectories(savegameDirs, (p, k) -> {
            savegames.get().setAll(getLatestSavegames());
        });
    }

    private List<FileImportTarget> getLatestSavegames() {
        return install.getAllSavegameDirectories().stream()
                .map(Path::toString)
                .map(FileImportTarget::createTargets)
                .map(List::stream)
                .flatMap(Stream::distinct)
                .sorted(Comparator.<FileImportTarget, Instant>comparing(t -> t.getLastModified()).reversed())
                .collect(Collectors.toList());
    }

    public List<FileImportTarget> getSavegames() {
        return savegames.get();
    }

    public ListProperty<FileImportTarget> savegamesProperty() {
        return savegames;
    }
}
