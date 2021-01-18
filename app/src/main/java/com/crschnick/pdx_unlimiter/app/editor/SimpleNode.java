package com.crschnick.pdx_unlimiter.app.editor;

import com.crschnick.pdx_unlimiter.core.parser.ArrayNode;
import com.crschnick.pdx_unlimiter.core.parser.KeyValueNode;
import com.crschnick.pdx_unlimiter.core.parser.Node;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class SimpleNode extends EditorNode {

    private int keyIndex;
    private Node backingNode;

    public SimpleNode(EditorNode parent, String keyName, int keyIndex, Node backingNode) {
        super(parent, keyName);
        this.keyIndex = keyIndex;
        this.backingNode = backingNode;
    }

    @Override
    public boolean filterKey(Predicate<String> filter) {
        if (getKeyName().isPresent() && filter.test(getKeyName().get())) {
            return true;
        }

        if (filter.test(String.valueOf(keyIndex))) {
            return true;
        }

        return false;
    }

    @Override
    public String displayKeyName() {
        return getKeyName().orElse("[" + keyIndex + "]");
    }

    @Override
    public String navigationName() {
        return getKeyName().orElseGet(() -> getDirectParent().navigationName() + "[" + keyIndex + "]");
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    public SimpleNode getRealParent() {
        return getDirectParent().isReal() ? (SimpleNode) getDirectParent() : getDirectParent().getRealParent();
    }

    @Override
    public List<EditorNode> open() {
        return EditorNode.create(this, backingNode.getNodeArray());
    }

    public Node toWritableNode() {
        return backingNode;
    }

    public void update(ArrayNode newNode) {
        Node nodeToUse = backingNode instanceof ArrayNode ? newNode : newNode.getNodes().get(0);
        getKeyName().ifPresentOrElse(s -> {
            getRealParent().getBackingNode().getNodeArray().set(getKeyIndex(),
                    KeyValueNode.create(s, nodeToUse));
        }, () -> {
            getRealParent().getBackingNode().getNodeArray().set(getKeyIndex(), nodeToUse);
        });
        this.backingNode = nodeToUse;
    }

    public int getKeyIndex() {
        return keyIndex;
    }

    public Node getBackingNode() {
        return backingNode;
    }
}
