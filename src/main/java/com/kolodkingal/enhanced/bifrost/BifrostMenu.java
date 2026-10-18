package com.kolodkingal.enhanced.bifrost;

import com.kolodkingal.enhanced.network.BifrostActionPayload;
import com.kolodkingal.enhanced.power.BifrostData;

import java.util.ArrayList;
import java.util.List;

/**
 * What the Bifrost screen offers (3.5), worked out with no client class so the tests can read it: the realms first (Asgard,
 * only from outside Asgard and while it is open; then Home, called "Home (Midgard)" in Asgard), then the saved places; and
 * which {@link BifrostActionPayload} each sends, as a journey of one's own or, in Heimdall's Send mode, for the player he
 * sends.
 */
public final class BifrostMenu {
    /** What an entry of the screen is. */
    public enum Kind {
        ASGARD, HOME, PLACE
    }

    /**
     * One entry.
     *
     * @param kind     what it is
     * @param index    the place's index in the list ({@link Kind#PLACE}), else -1
     * @param name     the place's name ({@link Kind#PLACE}), else empty
     * @param labelKey the lang key of its label (a place's label is its name)
     */
    public record Entry(Kind kind, int index, String name, String labelKey) {
        public boolean realm() {
            return kind != Kind.PLACE;
        }
    }

    /** The lang key of Home's button, and of Home's in Asgard. */
    public static final String HOME_KEY = "screen.enhanced.bifrost.home";
    public static final String HOME_MIDGARD_KEY = "message.enhanced.bifrost.home_midgard";
    public static final String ASGARD_KEY = "message.enhanced.bifrost.asgard";

    /**
     * The screen's entries, in order.
     *
     * @param inAsgard      whether whoever goes is in Asgard now (the traveller, or the one Heimdall sends)
     * @param asgardEnabled whether Asgard can be reached ({@code asgard.enabled})
     * @param places        the saved places shown (the traveller's own, or Heimdall's in Send mode)
     */
    public static List<Entry> entries(boolean inAsgard, boolean asgardEnabled, List<BifrostData.Place> places) {
        List<Entry> out = new ArrayList<>();
        if (!inAsgard && asgardEnabled) {
            out.add(new Entry(Kind.ASGARD, -1, "", ASGARD_KEY));
        }
        out.add(new Entry(Kind.HOME, -1, "", inAsgard ? HOME_MIDGARD_KEY : HOME_KEY));
        for (int i = 0; i < places.size(); i++) {
            out.add(new Entry(Kind.PLACE, i, places.get(i).name(), ""));
        }
        return out;
    }

    /**
     * What choosing an entry sends: a journey of one's own, or ({@code sendTarget} ≥ 0, Heimdall's Send mode) the player
     * with that entity id sent there ({@link BifrostActionPayload#SEND} with {@code "asgard"}, {@code "home"} or the place's
     * name).
     */
    public static BifrostActionPayload action(Entry entry, int sendTarget) {
        if (sendTarget >= 0) {
            String where = switch (entry.kind()) {
                case ASGARD -> "asgard";
                case HOME -> "home";
                case PLACE -> entry.name();
            };
            return new BifrostActionPayload(BifrostActionPayload.SEND, sendTarget, where);
        }
        return switch (entry.kind()) {
            case ASGARD -> new BifrostActionPayload(BifrostActionPayload.ASGARD, -1, "");
            case HOME -> new BifrostActionPayload(BifrostActionPayload.HOME, -1, "");
            case PLACE -> new BifrostActionPayload(BifrostActionPayload.TRAVEL, entry.index(), entry.name());
        };
    }

    private BifrostMenu() {
    }
}
