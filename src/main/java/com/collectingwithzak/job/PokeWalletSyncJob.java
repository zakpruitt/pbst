package com.collectingwithzak.job;

import com.collectingwithzak.client.PokeWalletClient;
import com.collectingwithzak.mapper.PokemonCardMapper;
import com.collectingwithzak.repository.PokemonCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PokeWalletSyncJob {

    private static final List<String> SYNC_SETS_A = List.of(
            "24146", // 10th Movie Commemoration Set
            "24145", // 11th Movie Commemoration Set
            "24197", // 64 Mario Stadium Best Photo Contest
            "24129", // ADV Expansion Pack
            "24140", // ADV-P Promotional Cards
            "24122", // Aqua Deck Kit
            "24054", // Arceus LV.X Deck: Grass & Fire
            "24055", // Arceus LV.X Deck: Lightning & Psychic
            "23720", // Awakening Legends
            "23730", // Base Expansion Pack
            "24047", // Bastiodon the Defender Half Deck
            "23944", // Battle Gift Set: Thundurus vs Tornadus
            "24199", // Battle Road
            "24056", // Battle Starter Deck (Blastoise)
            "24057", // Battle Starter Deck (Magmortar)
            "24058", // Battle Starter Deck (Raichu)
            "24059", // Battle Starter Deck (Torterra)
            "23934", // Battle Theme Deck: Victini
            "23952", // BK: Cobalion Battle Strength Deck
            "23951", // BK: Terrakion Battle Strength Deck
            "23950", // BK: Virizion Battle Strength Deck
            "23946", // BKB: Black Kyurem-EX Battle Strength Deck
            "23935", // BKR: Reshiram-EX Battle Strength Deck
            "23947", // BKW: White Kyurem-EX Battle Strength Deck
            "23936", // BKZ: Zekrom-EX Battle Strength Deck
            "24134", // Black Deck Kit
            "23938", // Blastoise + Kyurem-EX Combo Deck
            "24342", // BW-P Promotional cards
            "24514", // BW: Extra Regulation Box
            "23893", // BW1: Black Collection
            "23894", // BW1: White Collection
            "23895", // BW2: Red Collection
            "23897", // BW3: Hail Blizzard
            "23896", // BW3: Psycho Drive
            "23898", // BW4: Dark Rush
            "23900", // BW5: Dragon Blade
            "23899", // BW5: Dragon Blast
            "23902", // BW6: Cold Flare
            "23901", // BW6: Freeze Bolt
            "23903", // BW7: Plasma Gale
            "23904", // BW8: Spiral Force
            "23905", // BW8: Thunder Knuckle
            "23906", // BW9: Megalo Cannon
            "23726", // Challenge from the Darkness
            "24201", // Champion Road
            "24175", // City Gym Decks
            "24114", // Clash of the Blue Sky
            "24194", // CoroCoro Promotional Cards
            "23728", // Crossing the Ruins...
            "23933", // CS1: Journey Partners Collection Sheet
            "23729", // Darkness, and to Light...
            "24115", // Deoxys Constructed Starter Deck
            "-204", // Deoxys/Rayquaza Constructed Starter Deck
            "24050", // Dialga LV.X Constructed Standard Deck
            "-80", // Diamond & Pearl Promos
            "24137", // DP-P Promotional Cards
            "23973", // DP1: Space-Time Creation
            "23974", // DP2: Secret of the Lakes
            "-78", // DP3 Dialga Deck
            "-79", // DP3 Palkia Deck
            "23975", // DP3: Shining Darkness
            "23976", // DP4: Dawn Dash
            "23977", // DP4: Moonlit Pursuit
            "23979", // DP5: Cry from the Mysterious
            "23978", // DP5: Temple of Anger
            "24035", // DPt Gift Box (Chimchar)
            "24036", // DPt Gift Box (Pikachu)
            "24037", // DPt Gift Box (Piplup)
            "24038", // DPt Gift Box (Turtwig)
            "24136", // DPt-P Promotional Cards
            "23910", // Dragon Selection
            "24082", // Earth's Groudon ex Constructed Starter Deck
            "-223", // Earth's Groudon ex Deck
            "24196", // Elementary School Competition
            "-208", // Emerald Gift Box
            "24127", // Emerald Gift Box (Deoxys)
            "24130", // Emerald Gift Box (Rayquaza)
            "24040", // Entry Pack
            "24041", // Entry Pack '08
            "-82", // Entry Pack DPt
            "24042", // Entry Pack DPt (Dialga)
            "24043", // Entry Pack DPt (Giratina)
            "24044", // Entry Pack DPt (Palkia)
            "23939", // Everyone's Exciting Battle
            "23912", // EX Battle Boost
            "23721", // Expansion Pack
            "23740", // Expansion Pack (No Rarity)
            "24104", // Feraligatr Constructed Starter Deck
            "24107", // Fighting Quick Construction Pack
            "24108", // Fire Quick Construction Pack
            "24117", // Flight of Legends
            "24125", // Flygon Constructed Starter Deck
            "23942", // Garchomp Half Deck
            "24033", // Garchomp vs Charizard SP Deck Kit (Charizard)
            "24032", // Garchomp vs Charizard SP Deck Kit (Garchomp)
            "24120", // Gift Box (Latias)
            "-202", // Gift Box (Latias/Latios)
            "24121", // Gift Box (Latios)
            "24095", // Gift Box Mew - Lucario (Crawdaunt Quarter Deck)
            "24094", // Gift Box Mew - Lucario (Lucario Quarter Deck)
            "24093", // Gift Box Mew - Lucario (Mew Quarter Deck)
            "24096", // Gift Box Mew - Lucario (Mightyena Quarter Deck)
            "24098", // Gift Box Mew - Lucario (Pokemon Star)
            "-83", // Giratina vs Dialga Deck Kit
            "24045", // Giratina vs Dialga Deck Kit (Dialga)
            "24046", // Giratina vs Dialga Deck Kit (Giratina)
            "23727", // Gold, Silver, to a New World...
            "24103", // Golden Sky, Silvery Ocean
            "24109", // Grass Quick Construction Pack
            "24039", // Heatran vs Regigigas Deck Kit
            "24084", // Holon Phantom
            "24085", // Holon Research Tower
            "24086", // Holon Research Tower Fire Quarter Deck
            "24087", // Holon Research Tower Lightning Quarter Deck
            "24088", // Holon Research Tower Water Quarter Deck
            "24195", // How I Became a Pokemon Card
            "23943", // Hydreigon Half Deck
            "24091", // Imprison, Gardevoir ex Constructed Standard Deck
            "-221", // Imprison! Gardevoir ex Deck
            "24031", // Infernape vs Gallade SP Deck Kit (Gallade)
            "24030", // Infernape vs Gallade SP Deck Kit (Infernape)
            "24168", // Intro Pack (Bulbasaur)
            "24169", // Intro Pack (Squirtle)
            "24160", // Intro Pack Neo (Chikorita)
            "24161", // Intro Pack Neo (Totodile)
            "24142", // J Promotional Cards
            "23941", // Keldeo Battle Strength Deck
            "24023", // L-P: Legends Promos
            "24025", // L1: HeartGold Collection
            "24026", // L1: SoulSilver Collection
            "24021", // L2: Revival Legends
            "24080", // L2: Steelix Constructed Standard Deck
            "24081", // L2: Tyranitar Constructed Standard Deck
            "24024", // L3: Clash at the Summit
            "23725", // Leaders' Stadium
            "24028", // Leafeon vs Metagross Expert Deck (Leafeon)
            "24029", // Leafeon vs Metagross Expert Deck (Metagross)
            "24110", // Lightning Quick Construction Pack
            "24022", // LL: Lost Link
            "24123", // Magma Deck Kit
            "24124", // Magma VS Aqua: Two Ambitions
            "24052", // Magmortar vs Electivire Deck Kit
            "23940", // Master Deck Build Box EX
            "24100", // Master Kit (Bulbasaur)
            "24159", // Master Kit (Side Deck)
            "24158", // Master Kit (Torchic)
            "24156", // McDonald's Pokémon-e Minimum Pack
            "24105", // Meganium Constructed Starter Deck
            "24151", // Melee, Pokemon Scramble
            "24118", // Metagross Constructed Starter Deck
            "-218", // Mew Constructed Starter Deck
            "23955", // Mewtwo vs Genesect Deck Kit (Genesect)
            "23937", // Mewtwo vs Genesect Deck Kit (Mewtwo)
            "24099", // Miracle Crystal
            "24139", // Miracle of the Desert
            "24101", // Mirage Forest
            "24102", // Mirage's Mew Constructed Starter Deck
            "24144", // Movie Commemoration Random Pack
            "24149", // Movie Commemoration VS Pack
            "24148", // Movie Commemoration VS Pack: Aura's Lucario
            "24147", // Movie Commemoration VS Pack: Sea's Manaphy
            "24503", // Movie Commemoration VS Pack: Sky-Splitting Deoxys
            "24131", // Mudkip Constructed Starter Deck
            "23734", // Mysterious Mountains
            "23723", // Mystery of the Fossils
            "24013", // Neo Premium File 1
            "24017", // Neo Premium File 2
            "24018", // Neo Premium File 3
            "24083", // Ocean's Kyogre ex Constructed Starter Deck
            "-224", // Ocean's Kyogre ex Deck
            "24090", // Offense and Defense of the Furthest Ends
            "24143", // P Promotional Cards
            "24051", // Palkia LV.X Constructed Standard Deck
            "24138", // PCG-P Promotional Cards
            "-84", // Platinum Promos
            "24529", // Player Placement Trainer Promos
            "24203", // Pokemon Card Design Contest
            "24204", // Pokemon Card Fan Club
            "24198", // Pokemon Card Information Promotional cards
            "23722", // Pokemon Jungle
            "24180", // Pokemon VS
            "24141", // Pokemon Web
            "24167", // Pokemon-e Starter Deck
            "24154", // PokePark Forest
            "24111", // Psychic Quick Construction Pack
            "24002", // Pt: Arceus LV.X Deck: Grass & Fire
            "24003", // Pt: Arceus LV.X Deck: Lightning & Psychic
            "23998", // Pt1: Galactic's Conquest
            "23999", // Pt2: Bonds to the End of Time
            "24000", // Pt3: Beat of the Frontier
            "24001", // Pt4: Advent of Arceus
            "24004", // PtM: Mewtwo LV.X Collection Pack
            "24005", // PtR: Regigigas LV.X Collection Pack
            "24006", // PtS: Shaymin LV.X Collection Pack
            "-214", // Quick Construction Pack (Fighting)
            "-210", // Quick Construction Pack (Fire)
            "-209", // Quick Construction Pack (Grass)
            "-212", // Quick Construction Pack (Lightning)
            "-213", // Quick Construction Pack (Psychic)
            "-211", // Quick Construction Pack (Water)
            "24048", // Rampardos the Attacker Constructed Half Deck
            "24116", // Rayquaza Constructed Starter Deck
            "23724", // Rocket Gang
            "24135", // Rocket Gang Strikes Back
            "24128", // Rulers of the Heavens
            "24126", // Salamence Constructed Starter Deck
            "23911", // Shiny Collection
            "24092", // Shockwave, Tyranitar ex Constructed Standard Deck
            "-222", // Shockwave! Tyranitar ex Deck
            "24113", // Silver Deck Kit
            "24019", // Southern Island
            "23733", // Split Earth
            "24157", // T Promotional cards
            "23949", // Team Plasma Battle Gift Set
            "23945", // Team Plasma's Powered Half Deck
            "23731", // The Town on No Map
            "24150", // Theater Limited VS Pack
            "24132", // Torchic Constructed Starter Deck
            "24133", // Treecko Constructed Starter Deck
            "24106", // Typhlosion Constructed Starter Deck
            "24119", // Undone Seal
            "24172", // Unnumbered Promotional cards
            "24206", // Vending Machine cards Series 1 (Blue)
            "24207", // Vending Machine cards Series 2 (Red)
            "24208", // Vending Machine cards Series 3 (Green)
            "24112", // Water Quick Construction Pack
            "23732", // Wind from the Sea
            "24200", // World Hobby Fair
            "24027", // Intense Fight in the Destroyed Sky
            "24489" // Limited Collection Master Battle Set
    );

    private static final List<String> SYNC_SETS_B = List.of(
            "23822", // Battle Academy
            "-228", // Battle Academy
            "-172", // Black Star Promos
            "23963", // BREAK Starter Pack
            "23964", // CP1: Magma Gang vs Aqua Gang: Double Crisis
            "23966", // CP2: Legendary Shine Collection
            "23970", // CP3: PokeKyun Collection
            "23972", // CP4: Premium Champion Pack
            "23981", // CP5: Mythical & Legendary Dream Shine Collection
            "23982", // CP6: Expansion Pack 20th Anniversary
            "23985", // M Master Deck Build Box Power Style
            "23986", // M Master Deck Build Box Speed Style
            "24423", // M-P Promotional Cards
            "24399", // m1L: Mega Brave
            "24400", // m1S: Mega Symphonia
            "24459", // M2: Inferno X
            "24600", // M3: Nihil Zero
            "24653", // M4: Ninja Spinner
            "24445", // MBD: MEGA Starter Set Mega Diancie ex
            "24444", // MBG: MEGA Starter Set Mega Gengar ex
            "24499", // MEGA Dream ex
            "23817", // Pokemon TCG Classic: Blastoise
            "23818", // Pokemon TCG Classic: Charizard
            "23819", // Pokemon TCG Classic: Venusaur
            "23876", // S-P: Sword & Shield Promos
            "23830", // s0: Charizard VSTAR vs Rayquaza VMAX Special Deck Set
            "23640", // S10a: Dark Phantasma
            "23641", // S10b: Pokemon GO
            "23629", // S10D: Time Gazer
            "23630", // S10P: Space Juggler
            "23631", // S11: Lost Abyss
            "23642", // S11a: Incandescent Arcana
            "23632", // S12: Paradigm Trigger
            "23645", // S12a: VSTAR Universe
            "23633", // S1a: VMAX Rising
            "23617", // S1H: Shield
            "23616", // S1W: Sword
            "23618", // S2: Rebellion Crash
            "23634", // S2a: Explosive Walker
            "23619", // S3: Infinity Zone
            "23635", // S3a: Legendary Heartbeat
            "23620", // S4: Amazing Volt Tackle
            "23643", // S4a: Shiny Star V
            "23636", // S5a: Peerless Fighters
            "23621", // S5I: Single Strike Master
            "23622", // S5R: Rapid Strike Master
            "23637", // S6a: Eevee Heroes
            "23623", // S6H: Silver Lance
            "23624", // S6K: Jet-Black Spirit
            "23625", // S7D: Skyscraping Perfection
            "23626", // S7R: Blue Sky Stream
            "23627", // S8: Fusion Arts
            "23838", // s8a-G: 25th Anniversary Golden Box
            "23847", // s8a-P: Promo Card Pack 25th Anniversary Edition
            "23638", // S8a: 25th Anniversary Collection
            "23644", // S8b: VMAX Climax
            "23628", // S9: Star Birth
            "23639", // S9a: Battle Region
            "23857", // sA: Fighting Starter Set V
            "23858", // sA: Fire Starter Set V
            "23860", // sA: Grass Starter Set V
            "23861", // sA: Lightning Starter Set V
            "23862", // sA: Water Starter Set V
            "23863", // sB: Sword & Shield Premium Trainer Box
            "23854", // sC: Charizard Starter Set VMAX
            "23855", // sC: Grimmsnarl Starter Set VMAX
            "23843", // sC2: Charizard Starter Set VMAX 2
            "-168", // Scarlet & Violet Chinese/Japanese Promos
            "-169", // Scarlet & Violet Indonesian Promos
            "-170", // Scarlet & Violet Thai Promos
            "23853", // sD: V Starter Decks
            "23844", // sEF: Venusaur Starter Set VMAX
            "23842", // sEK: Blastoise Starter Set VMAX
            "23864", // sF: Single Strike & Rapid Strike Premium Trainer Boxes
            "23865", // sH: Sword & Shield Family Pokemon Card Game
            "23859", // SI: Start Deck 100
            "23839", // sJ: Zacian & Zamazenta vs Eternatus Special Deck Set
            "23837", // sK: VSTAR Premium Trainer Box
            "23866", // sLD: Darkrai Starter Set VSTAR
            "23836", // sLL: Lucario Starter Set VSTAR
            "23881", // SM-P: Sun & Moon Promos
            "23887", // SM: The Best of XY
            "23674", // SM0: Pikachu's New Friends
            "23880", // sm1+: Enhanced Expansion Pack Sun & Moon
            "23692", // SM1+: Sun & Moon
            "23689", // SM10: Double Blaze
            "23703", // SM10a: GG End
            "23704", // SM10b: Sky Legend
            "23690", // SM11: Miracle Twin
            "23705", // SM11a: Remix Bout
            "23706", // SM11b: Dream League
            "23691", // SM12: Alter Genesis
            "23709", // SM12a: TAG TEAM GX: Tag All Stars
            "23676", // SM1M: Collection Moon
            "23675", // SM1S: Collection Sun
            "23693", // SM2+: Facing a New Trial
            "23677", // SM2K: Islands Await You
            "23678", // SM2L: Alolan Moonlight
            "23694", // SM3+: Shining Legends
            "23679", // SM3H: To Have Seen the Battle Rainbow
            "23680", // SM3N: Darkness that Consumes Light
            "23707", // SM4+: GX Battle Boost
            "23682", // SM4A: Ultradimensional Beasts
            "23681", // SM4S: Awakened Heroes
            "23695", // SM5+: Ultra Force
            "23684", // SM5M: Ultra Moon
            "23683", // SM5S: Ultra Sun
            "23685", // SM6: Forbidden Light
            "23696", // SM6a: Dragon Storm
            "23697", // SM6b: Champion Road
            "23686", // SM7: Sky-Splitting Charisma
            "23698", // SM7a: Thunderclap Spark
            "23699", // SM7b: Fairy Rise
            "23687", // SM8: Super-Burst Impact
            "23700", // SM8a: Dark Order
            "23708", // SM8b: GX Ultra Shiny
            "23688", // SM9: Tag Bolt
            "23701", // SM9a: Night Unison
            "23702", // SM9b: Full Metal Wall
            "23879", // smA: Sun & Moon Starter Set
            "23878", // smB: Premium Trainer Box
            "23882", // smC: Tapu Bulu-GX Enhanced Starter Set
            "23883", // smD: Ash vs Team Rocket Deck Kit
            "23884", // smE: Solgaleo-GX & Lunala-GX Legendary Starter Set
            "23885", // smF: Ultra Sun & Ultra Moon Premium Trainer Box
            "23886", // smG: Ultra Sun & Ultra Moon Deck Build Boxes
            "23888", // smH: GX Starter Decks
            "23875", // smI: Flareon-GX, Vaporeon-GX & Jolteon-GX Starter Sets
            "23873", // smJ: Tag Team GX Premium Trainer Box
            "23872", // smK: Trainer Battle Decks
            "23870", // smL: Sun & Moon Family Pokemon Card Game
            "23868", // smM: Tag Team GX Starter Sets
            "23871", // smN: Tag Team GX Deck Build Box
            "23877", // smP1: Rockruff Full Power Deck
            "23869", // smP2: Great Detective Pikachu
            "23835", // sN: Start Deck 100 CoroCoro Comic Version
            "23987", // SNP: Noivern BREAK Evolution Pack
            "23988", // SNP: Raichu BREAK Evolution Pack
            "23856", // sp1: Zacian + Zamazenta Box
            "23852", // sp2: VMAX Special Set
            "23841", // sp4: Eevee Heroes VMAX Special Set
            "23840", // sp5: V-UNION Special Card Sets
            "23831", // sp6: VSTAR Special Set
            "23832", // sPD: Deoxys VSTAR & VMAX High-Class Deck
            "24425", // Special Box Collections
            "23833", // sPZ: Zeraora VSTAR & VMAX High-Class Deck
            "23826", // SS: Gengar VMAX High-Class Deck
            "23827", // SS: Inteleon VMAX High-Class Deck
            "23829", // SS: Silver Lance & Jet-Black Spirit Jumbo Pack Set
            "-227", // SV Starter Set Lucario
            "-229", // SV Starter Set Mimikyu
            "23779", // SV-P Promotional Cards
            "23810", // SV: Ancient Koraidon ex Starter Deck & Build Set
            "23793", // SV: Ceruledge ex Stellar Tera Type Starter Set
            "23795", // SV: Chien-Pao ex Battle Master Deck
            "23804", // SV: ex Special Set
            "23803", // SV: ex Start Decks
            "23797", // SV: ex Starter Deck & Build Set
            "23809", // SV: ex Starter Set Fuecoco & Ampharos ex
            "24231", // SV: ex Starter Set Marnie's Morpeko & Grimmsnarl ex
            "23805", // SV: ex Starter Set Pikachu ex & Pawmot
            "23808", // SV: ex Starter Set Quaxly & Mimikyu ex
            "23807", // SV: ex Starter Set Sprigatito & Lucario ex
            "24230", // SV: ex Starter Set Steven's Beldum & Metagross ex
            "23811", // SV: Future Miraidon ex Starter Deck & Build Set
            "23799", // SV: Mewtwo ex Terastal Starter Set
            "23806", // SV: Premium Trainer Box ex
            "23801", // SV: Ruler of the Black Flame Deck Build Box
            "23800", // SV: Skeledirge ex Terastal Starter Set
            "23794", // SV: Stellar Miracle Deck Build Box
            "23798", // SV: Sylveon ex Stellar Tera Type Starter Set
            "23796", // SV: Terastal Charizard ex Battle Master Deck
            "23812", // SV: Venusaur, Charizard & Blastoise Special Deck Set
            "24310", // SV10: The Glory of Team Rocket
            "24349", // SV11B: Black Bolt
            "24350", // SV11W: White Flare
            "23598", // SV1a: Triplet Beat
            "23605", // SV1S: Scarlet ex
            "23606", // SV1V: Violet ex
            "23599", // SV2a: Pokemon Card 151
            "23608", // SV2D: Clay Burst
            "23607", // SV2P: Snow Hazard
            "23609", // SV3: Ruler of the Black Flame
            "23600", // SV3a: Raging Surf
            "23601", // SV4a: Shiny Treasure ex
            "23610", // SV4K: Ancient Roar
            "23611", // SV4M: Future Flash
            "23602", // SV5a: Crimson Haze
            "23612", // SV5K: Wild Force
            "23613", // SV5M: Cyber Judge
            "23614", // SV6: Transformation Mask
            "23603", // SV6a: Night Wanderer
            "23615", // SV7: Stellar Miracle
            "23604", // SV7a: Paradise Dragona
            "23777", // SV8: Super Electric Breaker
            "23909", // SV8a: Terastal Fest ex
            "24173", // SV9: Battle Partners
            "24260", // SV9a: Heat Wave Arena
            "24324", // SVM: Generations Start Decks
            "24174", // SVN: Battle Partners Deck Build Box
            "-163", // Sword & Shield Chinese/Japanese Promos
            "24468", // Triple Starter Deck Set VMAX
            "23802", // WCS23: 2023 World Championships Yokohama Deck: Pikachu
            "23980", // Xerneas Half Deck
            "23983", // XY Beginning Set
            "-194", // XY Promotional Cards
            "23914", // XY-Bx: Collection X
            "23915", // XY-By: Collection Y
            "23908", // XY-P: XY Promos
            "23913", // XY10: Awakening Psychic King
            "23916", // XY11-Bb: Fever-Burst Fighter
            "23917", // XY11-Br: Cruel Traitor
            "23918", // XY2: Wild Blaze
            "23919", // XY3: Rising Fist
            "23920", // XY4: Phantom Gate
            "23921", // XY5-Bg: Gaia Volcano
            "23922", // XY5-Bt: Tidal Storm
            "23923", // XY6: Emerald Break
            "23924", // XY7: Bandit Ring
            "23925", // XY8-Bb: Blue Shock
            "23926", // XY8-Br: Red Flash
            "23927", // XY9: Rage of the Broken Heavens
            "23958", // XYA: M Charizard-EX Mega Battle Deck
            "23959", // XYB: Dialga-EX + Aegislash-EX Hyper Metal Chain Deck
            "23960", // XYC: Super Legend Set: Xerneas-EX & Yveltal-EX
            "23961", // XYD: M Rayquaza-EX Mega Battle Deck
            "23962", // XYE: Emboar-EX vs Togekiss-EX Deck Kit
            "23965", // XYF: Golduck BREAK + Palkia-EX Combo Deck
            "23968", // XYG: Zygarde-EX Perfect Battle Deck
            "23969", // XYH: M Audino-EX Mega Battle Deck
            "23971" // Yveltal Half Deck
    );

    private final PokeWalletClient pokeWalletClient;
    private final PokemonCardRepository cardRepo;
    private final PokemonCardMapper cardMapper;

    @Autowired
    @Lazy
    private PokeWalletSyncJob self;

    @Scheduled(fixedRate = 43_200_000)
    public void sync() {
        if (!pokeWalletClient.isConfigured()) {
            log.info("PokeWallet sync skipped: credentials not configured");
            return;
        }

        var allSets = new java.util.ArrayList<>(SYNC_SETS_A);
        allSets.addAll(SYNC_SETS_B);
        log.info("PokeWallet sync started: {} sets", allSets.size());
        for (String setCode : allSets) {
            try {
                self.syncSet(setCode);
            } catch (Exception e) {
                log.error("Sync failed for set {}", setCode, e);
            }
        }
    }

    @Transactional
    public void syncSet(String setCode) {
        var cards = pokeWalletClient.fetchAllCards(setCode);
        if (cards.isEmpty()) {
            log.info("No cards returned for set: {}", setCode);
            return;
        }

        var entities = cards.stream()
                .filter(c -> c.getCardInfo() != null && c.getCardInfo().getCardNumber() != null)
                .map(cardMapper::fromPokeWallet)
                .toList();
        cardRepo.saveAll(entities);
        log.info("Set synced: {} — {} cards", setCode, entities.size());
    }
}
