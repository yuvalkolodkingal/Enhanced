package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * Where the Stone Locator looks for each stone, realm by realm (3.7, lane L5; the plan's table 8.4). Pure: the realm,
 * the kind and two switches in, a {@link Route} out; the client reads the same table for the HUD's words.
 *
 * <table>
 *   <tr><th>Kind</th><th>Its realm</th><th>Elsewhere</th></tr>
 *   <tr><td>Reality</td><td>Asgard: the vault's pedestal (while the vault is open)</td><td>Overworld: the nearest
 *   Bifrost site ("beyond the Bifrost"; vault shut: "sealed ... until Hela falls", still the Bifrost site)</td></tr>
 *   <tr><td>Mind, Time, Space</td><td>Overworld: a Hydra outpost, a Sanctum, an Avengers Tower</td><td>it spins:
 *   "Not in this realm." (Asgard: "Not in Asgard.")</td></tr>
 *   <tr><td>Power</td><td>the End: an End tunnel</td><td>Overworld: the nearest stronghold ("beyond the End");
 *   elsewhere it spins</td></tr>
 *   <tr><td>Soul</td><td>the Nether: a Vormir Cliff</td><td>Overworld: it spins, "The Soul Stone's energy comes from
 *   the Nether."</td></tr>
 * </table>
 * On the Witches' Road everything spins ("The Road hides everything."). With {@code pointsToGateways} off, the ways to
 * other realms are not shown (it spins, with the same words).
 */
public final class LocatorRoutes {
    /** The realms the table knows. */
    public enum Realm {
        OVERWORLD, NETHER, END, ASGARD, ROAD, OTHER
    }

    /** What the Locator does for a kind in a realm. */
    public enum Type {
        /** Search the stone's own sites here. */
        SEARCH,
        /** Search the way to its realm (a Bifrost site, a stronghold). */
        GATEWAY,
        /** A fixed point (Asgard's vault pedestal). */
        FIXED,
        /** Nothing to point at: it spins. */
        SPIN
    }

    /**
     * A route: its type, the structures searched (SEARCH, GATEWAY), the status it shows when it points (or spins) and
     * the words of the HUD's second line (a lang key with the stone's name as its argument, or null).
     */
    public record Route(Type type, @Nullable TagKey<Structure> sites, byte status, @Nullable String message) {
    }

    public static final String NOT_HERE = "message.enhanced.locator.not_here";
    public static final String NOT_ASGARD = "message.enhanced.locator.not_asgard";
    public static final String SOUL_NETHER = "message.enhanced.locator.soul_nether";
    public static final String ROAD = "message.enhanced.locator.road";
    public static final String BEYOND_BIFROST = "message.enhanced.locator.beyond_bifrost";
    public static final String SEALED = "message.enhanced.locator.sealed";
    public static final String BEYOND_END = "message.enhanced.locator.beyond_end";

    /** The realm of a dimension. */
    public static Realm realm(ResourceKey<Level> dimension) {
        if (dimension == Level.OVERWORLD) {
            return Realm.OVERWORLD;
        }
        if (dimension == Level.NETHER) {
            return Realm.NETHER;
        }
        if (dimension == Level.END) {
            return Realm.END;
        }
        if (dimension == AsgardDimension.KEY) {
            return Realm.ASGARD;
        }
        return dimension == RoadDimension.KEY ? Realm.ROAD : Realm.OTHER;
    }

    /** The route of {@code kind} in {@code realm}, with the vault of Asgard open (the 3-argument form's default). */
    public static Route route(Realm realm, StoneKind kind, boolean gateways) {
        return route(realm, kind, gateways, true);
    }

    /**
     * The route of {@code kind} in {@code realm}: {@code gateways} is {@code [stone_locator] pointsToGateways},
     * {@code vaultOpen} whether Asgard's vault is open ({@code AsgardState.odinAwake}).
     */
    public static Route route(Realm realm, StoneKind kind, boolean gateways, boolean vaultOpen) {
        if (realm == Realm.ROAD) {
            return spin(ROAD);
        }
        if (realm == Realm.ASGARD) {
            if (kind != StoneKind.REALITY) {
                return spin(NOT_ASGARD);
            }
            return vaultOpen ? new Route(Type.FIXED, null, LocatorState.FOUND, null)
                    : new Route(Type.SPIN, null, LocatorState.HIDDEN, SEALED);
        }
        return switch (kind) {
            case MIND, TIME, SPACE -> realm == Realm.OVERWORLD ? search(kind) : spin(NOT_HERE);
            case POWER -> switch (realm) {
                case END -> search(kind);
                case OVERWORLD -> gateway(StructureTags.EYE_OF_ENDER_LOCATED, LocatorState.OTHER_REALM, BEYOND_END,
                        gateways);
                default -> spin(NOT_HERE);
            };
            case SOUL -> switch (realm) {
                case NETHER -> search(kind);
                case OVERWORLD -> spin(SOUL_NETHER);
                default -> spin(NOT_HERE);
            };
            case REALITY -> realm == Realm.OVERWORLD
                    ? gateway(ModTags.BIFROST_SITES, vaultOpen ? LocatorState.OTHER_REALM : LocatorState.HIDDEN,
                    vaultOpen ? BEYOND_BIFROST : SEALED, gateways)
                    : spin(NOT_HERE);
        };
    }

    /**
     * The HUD's second line for a reading (a lang key, the stone's name its argument), or null: the route's words for
     * the realm and kind; a sealed vault's words for {@code HIDDEN}.
     */
    @Nullable
    public static String message(Realm realm, StoneKind kind, byte status) {
        if (status == LocatorState.HIDDEN) {
            return SEALED;
        }
        if (status != LocatorState.OTHER_REALM) {
            return null;
        }
        return route(realm, kind, true).message();
    }

    private static Route search(StoneKind kind) {
        return new Route(Type.SEARCH, SpentSites.sites(kind), LocatorState.FOUND, null);
    }

    private static Route gateway(TagKey<Structure> way, byte status, String message, boolean gateways) {
        return gateways ? new Route(Type.GATEWAY, way, status, message) : new Route(Type.SPIN, null, status, message);
    }

    private static Route spin(String message) {
        return new Route(Type.SPIN, null, LocatorState.OTHER_REALM, message);
    }

    private LocatorRoutes() {
    }
}
