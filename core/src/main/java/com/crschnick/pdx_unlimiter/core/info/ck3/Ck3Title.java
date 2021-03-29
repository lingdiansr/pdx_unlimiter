package com.crschnick.pdx_unlimiter.core.info.ck3;

import com.crschnick.pdx_unlimiter.core.info.GameColor;
import com.crschnick.pdx_unlimiter.core.node.ColorNode;
import com.crschnick.pdx_unlimiter.core.node.Node;
import com.crschnick.pdx_unlimiter.core.node.ValueNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Ck3Title {

    private long id;
    private String key;
    private String name;
    private GameColor color;
    private Ck3CoatOfArms coatOfArms;

    public Ck3Title() {
    }

    public Ck3Title(long id, String key, String name, GameColor color, Ck3CoatOfArms coatOfArms) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.color = color;
        this.coatOfArms = coatOfArms;
    }

    public static Map<Long, Ck3Title> createTitleMap(Node node, Map<Long, Ck3CoatOfArms> coaMap) {
        var tts = node.getNodeForKey("landed_titles").getNodeForKey("landed_titles");
        var map = new HashMap<Long, Ck3Title>();
        tts.forEach((k, v) -> {
            var id = Long.parseLong(k);
            fromNode(id, v, coaMap).ifPresent(t -> {
                map.put(id, t);
            });
        });
        return map;
    }

    private static Optional<Ck3Title> fromNode(long id, Node n, Map<Long, Ck3CoatOfArms> coaMap) {
        // If node is "none"
        if (n instanceof ValueNode) {
            return Optional.empty();
        }

        var name = n.getNodeForKey("name").getString();
        var key = n.getNodeForKey("key").getString();
        var coaId = n.getNodeForKey("coat_of_arms_id").getLong();
        var color = n.getNodeForKeyIfExistent("color")
                .map(Node::getColorNode)
                .map(GameColor::fromColorNode)
                .orElse(null);
        var coatOfArms = coaMap.get(coaId);
        return Optional.of(new Ck3Title(id, key, name, color, coatOfArms));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ck3Title ck3Title = (Ck3Title) o;
        return id == ck3Title.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String getName() {
        return name;
    }

    public Ck3CoatOfArms getCoatOfArms() {
        return coatOfArms;
    }

    public Optional<GameColor> getColor() {
        return Optional.ofNullable(color);
    }

    public String getKey() {
        return key;
    }

    public long getId() {
        return id;
    }
}
