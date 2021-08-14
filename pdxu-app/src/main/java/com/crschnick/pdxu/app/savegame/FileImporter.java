package com.crschnick.pdxu.app.savegame;

import com.crschnick.pdxu.app.core.*;
import com.crschnick.pdxu.app.core.settings.Settings;
import com.crschnick.pdxu.app.gui.dialog.GuiImporter;
import com.crschnick.pdxu.io.savegame.SavegameParseResult;
import javafx.application.Platform;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class FileImporter {

    private static final Logger logger = LoggerFactory.getLogger(FileImporter.class);


    public static void init() throws IOException {
        var path = PdxuInstallation.getInstance().getImportQueueLocation();
        FileUtils.forceMkdir(path.toFile());

        BiConsumer<Path, WatchEvent.Kind<Path>> importFunc = (p, k) -> {
            if (!Files.exists(p)) {
                return;
            }

            importFromQueue(p);
        };

        Files.list(path).forEach(FileImporter::importFromQueue);
        FileWatchManager.getInstance().startWatchersInDirectories(List.of(path), importFunc);
    }

    private static void importFromQueue(Path queueFile) {
        // Only accept files that were generated by the file importer
        try {
            UUID.fromString(queueFile.getFileName().toString());
        } catch (Exception ex) {
            return;
        }

        String input;
        try {
            input = Files.readString(queueFile);
        } catch (IOException e) {
            ErrorHandler.handleException(e);
            return;
        }

        logger.debug("Starting to import " + input + " from queue file " + queueFile);
        var targets = FileImportTarget.createTargets(input);
        if (targets.size() == 0) {
            logger.debug("No targets to import.");

        } else {
            for (FileImportTarget t : targets) {
                logger.debug("Starting to import target " + t.getName() + " from " + input);
                t.importTarget(s -> {
                    if (Settings.getInstance().deleteOnImport.getValue()) {
                        logger.debug("Deleting import target " + t.getName());
                        t.delete();
                    }
                });
            }
        }

        logger.debug("Deleting queue file " + queueFile);
        FileUtils.deleteQuietly(queueFile.toFile());
    }

    public static void onFileDrop(List<File> files) {
        for (var file : files) {
            EditorProvider.get().openExternalDataFile(file.toPath());
        }

        var importTargets = files.stream()
                .map(File::toString)
                .map(FileImportTarget::createTargets)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        FileImporter.importTargets(importTargets);
    }

    public static void importTargets(Collection<? extends FileImportTarget> targets) {
        Map<FileImportTarget, SavegameParseResult> statusMap = new HashMap<>();
        targets.forEach(t -> t.importTarget(s -> {
            // Only save non success results
            s.ifPresent(result -> {
                statusMap.put(t, result);
            });

            if (Settings.getInstance().deleteOnImport.getValue()) {
                logger.debug("Deleting import target " + t.getName());
                t.delete();
            }
        }));
        TaskExecutor.getInstance().submitTask(
                () -> {
                    // Report errors
                    statusMap.entrySet().stream()
                            .filter(e -> e.getValue() instanceof SavegameParseResult.Error)
                            .findFirst()
                            .ifPresent(e -> {
                                ErrorHandler.handleException(
                                        ((SavegameParseResult.Error) e.getValue()).error,
                                        null,
                                        e.getKey().getPath());
                            });

                    Platform.runLater(() -> GuiImporter.showResultDialog(statusMap));
                }, false);
    }

    public static void addToImportQueue(String toImport) {
        try {
            FileUtils.forceMkdir(PdxuInstallation.getInstance().getImportQueueLocation().toFile());
        } catch (IOException e) {
            ErrorHandler.handleException(e);
            return;
        }

        // Check if the target is a valid file
        try {
            Path p = Path.of(toImport);
            if (!Files.exists(p)) {
                return;
            }
        } catch (Exception ex) {
            return;
        }

        var path = PdxuInstallation.getInstance().getImportQueueLocation()
                .resolve(UUID.randomUUID().toString());

        logger.debug("Creating queue file at " + path + " for import target " + toImport);
        try {
            Files.writeString(path, toImport);
        } catch (IOException e) {
            ErrorHandler.handleException(e);
        }
    }
}
