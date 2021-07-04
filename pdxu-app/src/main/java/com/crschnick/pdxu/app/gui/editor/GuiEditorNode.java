package com.crschnick.pdxu.app.gui.editor;

import com.crschnick.pdxu.app.editor.*;
import com.crschnick.pdxu.app.editor.adapter.EditorSavegameAdapter;
import com.crschnick.pdxu.app.gui.GuiTooltips;
import com.crschnick.pdxu.app.util.ColorHelper;
import com.crschnick.pdxu.io.node.Node;
import com.crschnick.pdxu.io.node.NodeWriter;
import com.crschnick.pdxu.model.GameColor;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXTextField;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class GuiEditorNode {

    static Region createValueDisplay(EditorNode n, EditorState state) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.setFillHeight(true);
        if (n.isReal() && EditorSettings.getInstance().enableNodeTags.getValue()) {
            var tag = EditorSavegameAdapter.ALL.get(state.getFileContext().getGame())
                    .createNodeTag(state, (EditorSimpleNode) n);
            if (tag != null) {
                box.getChildren().add(tag);
            }
        }

        if (n.isReal() && ((EditorSimpleNode) n).getBackingNode().isValue()) {
            var tf = new JFXTextField(((EditorSimpleNode) n).getBackingNode().getString());
            tf.setAlignment(Pos.CENTER);
            tf.textProperty().addListener((c, o, ne) -> {
                ((EditorSimpleNode) n).updateText(ne);
                state.onTextChanged();
            });
            box.getChildren().add(tf);
            HBox.setHgrow(tf, Priority.ALWAYS);
        } else if (n.isReal() && ((EditorSimpleNode) n).getBackingNode().isTagged() &&
                ((EditorSimpleNode) n).getBackingNode().describe().getValueType().equals(Node.ValueType.COLOR)) {
            var picker = new JFXColorPicker(ColorHelper.fromGameColor(GameColor.fromColorNode(
                    ((EditorSimpleNode) n).getBackingNode())));
            picker.valueProperty().addListener((c, o, ne) -> {
                ((EditorSimpleNode) n).updateColor(ne);
                state.onColorChanged();
            });
            box.getChildren().add(picker);
            box.setAlignment(Pos.CENTER);
            HBox.setHgrow(picker, Priority.ALWAYS);
        } else {
            var ar = createArrayDisplay(n, state);
            box.getChildren().add(ar);
            HBox.setHgrow(ar, Priority.ALWAYS);
        }
        return box;
    }

    private static Region createArrayDisplay(EditorNode n, EditorState state) {
        var box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.setSpacing(5);

        {
            int length = n.isReal() ? ((EditorSimpleNode) n).getBackingNode().getNodeArray().size() :
                    ((EditorCollectorNode) n).getNodes().size();
            var btn = new JFXButton("List (" + length + ")");
            var icon = new FontIcon();
            GuiTooltips.install(icon, "Expand node");
            btn.getStyleClass().add("list-expand-button");
            btn.setGraphic(icon);
            btn.setAlignment(Pos.CENTER);
            btn.setOnAction(e -> state.getNavHistory().navigateTo(n));
            box.getChildren().add(btn);
        }

        {
            var preview = new Label();
            preview.getStyleClass().add("preview");
            preview.setGraphic(new FontIcon());
            preview.setOnMouseEntered(e -> {
                var tt = GuiTooltips.createTooltip(NodeWriter.writeToString(
                        n.toWritableNode(),
                        EditorSettings.getInstance().maxTooltipLines.getMax(),
                        EditorSettings.getInstance().indentation.getValue()));
                tt.setWrapText(false);
                tt.setShowDelay(Duration.ZERO);
                Tooltip.install(preview, tt);
            });
            box.getChildren().add(preview);
        }

        return box;
    }
}
