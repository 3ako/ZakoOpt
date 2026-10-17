package remodik.opt.bench;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

// the benchmark scene as server commands; each level adds a row to every zone, past ROWS rows the rows go up a glass storey
// so the whole scene stays within entity render distance
@UtilityClass
public class Scene {
	// over the mountains of the generated terrain under the scene
	public final int Y = 250;
	public final int LEVELS = 60;
	private final int ROWS = 10;
	private final int STOREY = 4;
	// player-model stand-ins for the player LOD, from 1.21.9 on
	public final boolean MANNEQUIN = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet().stream().anyMatch(id -> id.getPath().equals("mannequin"));
	private final String[] KINDS = {"zombie", "skeleton", "husk", "stray", "piglin", "vindicator"};
	private final String[] ITEMS = {"diamond", "iron_ingot", "gold_ingot", "emerald", "cobblestone", "oak_log", "redstone", "bone", "arrow", "rotten_flesh",
			"diamond_sword", "golden_apple", "ender_pearl", "torch", "oak_planks", "glass", "string", "gunpowder", "apple", "bread"};
	private final String[] BLOCKS = {"chest[type=left,facing=north]", "chest[type=right,facing=north]", "oak_sign[rotation=8]", "campfire[lit=true]", "player_head", "white_banner"};

	public record View(String key, int x, int y, int z, int yaw, int pitch) {
	}

	public final View OVERVIEW = new View("overview", 60, Y + 40, -25, 0, 40);
	// a little over the heads at the crowd's corner, turned so both the crowd and the distance ruler beside it are in sight
	public final View LOD = new View("lod", -1, Y + 2, -6, 0, 8);
	// low over the near edge of the item pile, close enough to tell a stack of copies from a single item
	public final View ITEMS_CLOSE = new View("items_close", 48, Y + 2, 1, 0, 45);
	// over the near edge of the spawner field, far enough right of the scene that it stays out of the picture
	public final View SPAWNERS = new View("spawners", 232, Y + 3, -6, 0, 15);
	// over the edge of a planted forest, high enough to see its canopy from right below out to its far side
	public final View FOREST = new View("forest", 32, Y + 16, -40, 180, 25);
	// over the land out to the render distance; set over the world spawn, which the game always puts on land, once the world is in
	public View terrain = new View("terrain", 60, Y - 60, -40, 180, 10);

	public void terrainAt(int x, int y, int z) {
		terrain = new View("terrain", x, y + 40, z, 180, 10);
	}

	public List<View> views() {
		return List.of(terrain, FOREST, new View("crowd", 14, Y + 16, -10, 0, 35), new View("items", 48, Y + 16, -10, 0, 35),
				new View("blocks", 76, Y + 16, -10, 0, 35), new View("pistons", 123, Y + 16, -10, 0, 35), new View("holograms", 104, Y + 16, -10, 0, 30), OVERVIEW);
	}

	public List<String> base() {
		List<String> c = new ArrayList<>();
		// both gamerule spellings: 1.21.11 renamed them, the unknown one just fails
		for (String rule : List.of("doDaylightCycle false", "advance_time false", "doWeatherCycle false", "advance_weather false", "doMobSpawning false",
				"spawn_mobs false", "randomTickSpeed 0", "random_tick_speed 0", "commandBlockOutput false", "command_block_output false")) {
			c.add("gamerule " + rule);
		}
		c.add("time set 6000");
		c.add("weather clear");
		c.add("gamemode creative @a");
		c.add(f("fill -4 %d -8 130 %d 4 minecraft:smooth_stone", Y - 1, Y - 1));
		// the pistons' clock: #p runs 0..3 with the game time, every row's command blocks power its pistons on half of it;
		// it stands by the pistons, since command blocks run only within the simulation distance
		c.add("scoreboard objectives add zakoopt_t dummy");
		c.add("scoreboard players set #4 zakoopt_t 4");
		c.addAll(zone(OVERVIEW));
		c.add(f("setblock 130 %d -6 minecraft:repeating_command_block[facing=up]{auto:1b,Command:\"execute store result score #p zakoopt_t run time query gametime\"}", Y - 6));
		c.add(f("setblock 130 %d -6 minecraft:chain_command_block[facing=up]{auto:1b,Command:\"scoreboard players operation #p zakoopt_t %%= #4 zakoopt_t\"}", Y - 5));
		for (int i = 0; i < 3; i++) {
			c.add(f("bossbar add zakoopt:b%d %s", i, text("Boss " + i)));
			c.add(f("bossbar set zakoopt:b%d value %d", i, 30 + i * 25));
			c.add(f("bossbar set zakoopt:b%d players @a", i));
		}
		c.add("scoreboard objectives add zakoopt dummy " + text("ZakoOpt"));
		c.add("scoreboard objectives setdisplay sidebar zakoopt");
		for (int i = 0; i < 15; i++) {
			c.add(f("scoreboard players set line%d zakoopt %d", i, i * 7));
		}
		for (int i = 0; i < 20; i++) {
			c.add("tellraw @a " + text("Chat line " + i + ": ZakoOpt benchmark scene"));
		}
		return c;
	}

	// for the LOD pick only: an armoured mob and a player stand-in every 4 blocks from the LOD camera out to 64, about where
	// the game stops drawing mobs at all, with the distance written over every second pair; the crowd alone may end at 20
	public List<String> ruler(String blocksLabel) {
		List<String> c = new ArrayList<>();
		c.add(f("fill -11 %d -2 -5 %d 60 minecraft:smooth_stone", Y - 1, Y - 1));
		for (int d = 8; d <= 64; d += 4) {
			int z = LOD.z() + d;
			String armour = BenchWorld.LEGACY_SNBT
					? "ArmorItems:[{id:\"diamond_boots\",count:1},{id:\"diamond_leggings\",count:1},{id:\"diamond_chestplate\",count:1},{id:\"diamond_helmet\",count:1}],HandItems:[{id:\"diamond_sword\",count:1},{}]"
					: "equipment:{head:{id:\"diamond_helmet\",count:1},chest:{id:\"diamond_chestplate\",count:1},legs:{id:\"diamond_leggings\",count:1},feet:{id:\"diamond_boots\",count:1},mainhand:{id:\"diamond_sword\",count:1}}";
			c.add(f("summon minecraft:zombie -7 %d %d {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"%s\"],Rotation:[180f,0f],%s}", Y, z, RULER, armour));
			c.add(f("summon minecraft:%s -9 %d %d {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"%s\"],Rotation:[180f,0f],%s}", MANNEQUIN ? "mannequin" : "skeleton", Y, z, RULER, armour));
			if (d % 8 == 0) {
				c.add(f("summon minecraft:text_display -8 %d.6 %d {billboard:\"center\",alignment:\"center\",Tags:[\"%s\"],text:%s}", Y + 2, z, RULER, nbtText(blocksLabel.formatted(d))));
			}
		}
		return c;
	}

	// for the spawner pick only: the field grows by 4-row strips of 25 spawners with their mobs, from 8 to 68 blocks off the camera,
	// with the distance written beside every 8 blocks
	public final int SPAWNER_STRIPS = 15;

	public List<String> spawnerLabels(String blocksLabel) {
		List<String> c = new ArrayList<>();
		c.add(f("fill 216 %d -8 248 %d 64 minecraft:smooth_stone", Y - 1, Y - 1));
		for (int d = 8; d <= 64; d += 8) {
			c.add(f("summon minecraft:text_display 218 %d.6 %d {billboard:\"center\",alignment:\"center\",Tags:[\"%s\"],text:%s}", Y + 1, SPAWNERS.z() + d, RULER, nbtText(blocksLabel.formatted(d))));
		}
		return c;
	}

	public List<String> spawnerStrip(int strip) {
		int z = SPAWNERS.z() + 8 + strip * 4;
		return List.of(f("fill 220 %d %d 244 %d %d minecraft:spawner{SpawnData:{entity:{id:\"minecraft:%s\"}},Delay:32767s,MinSpawnDelay:32767s,MaxSpawnDelay:32767s,RequiredPlayerRange:128s}",
				Y, z, Y, z + 3, KINDS[strip % KINDS.length]));
	}

	public List<String> clearSpawnerField() {
		return List.of(f("fill 220 %d %d 244 %d %d minecraft:air", Y, SPAWNERS.z() + 8, Y, SPAWNERS.z() + 8 + SPAWNER_STRIPS * 4));
	}

	// the pistons and the particles run only where they are the subject or the whole scene is in sight: their block updates and
	// chunk rebuilds would otherwise compete with the multithreaded switches for the cores on every other view
	public List<String> zone(View view) {
		boolean all = view == OVERVIEW;
		return List.of(f("scoreboard players set #pistons zakoopt_t %d", all || view.key().equals("pistons") ? 1 : 0),
				f("scoreboard players set #particles zakoopt_t %d", all || view.key().equals("holograms") ? 1 : 0));
	}

	// a low render distance leaves the far part unloaded, and fill then places nothing at all
	public final int FOREST_X0 = -10, FOREST_Z0 = -130, FOREST_X1 = 74, FOREST_Z1 = -46;

	public List<String> forestLoaded(boolean loaded) {
		return List.of(f("forceload %s %d %d %d %d", loaded ? "add" : "remove", FOREST_X0, FOREST_Z0, FOREST_X1, FOREST_Z1));
	}

	// for the far leaves pick only: dark oaks with some oaks on grass behind the scene, out of every other camera's sight
	public List<String> forest() {
		Random random = new Random(42);
		List<String> c = new ArrayList<>();
		c.add(f("fill %d %d %d %d %d %d minecraft:grass_block", FOREST_X0, Y - 1, FOREST_Z0, FOREST_X1, Y - 1, FOREST_Z1));
		for (int x = -8; x <= 72; x += 5) {
			for (int z = -128; z <= -50; z += 5) {
				c.add(f("place feature minecraft:%s %d %d %d", random.nextInt(4) == 0 ? "oak" : "dark_oak", x + random.nextInt(3), Y, z + random.nextInt(3)));
			}
		}
		return c;
	}

	public final String RULER = "zakoopt_ruler";
	public final String PILE = "zakoopt_pile";

	// for the stack-copies pick only: full stacks right under the close items camera, where each draws its most copies
	public List<String> pile(int batch) {
		Random random = new Random(1000 + batch);
		List<String> c = new ArrayList<>();
		for (int i = 0; i < 200; i++) {
			c.add(f("summon minecraft:item %.2f %.1f %.2f {Item:{id:\"%s\",count:64},PickupDelay:32767,Age:-32768,Tags:[\"%s\"]}",
					ITEMS_CLOSE.x() - 8 + random.nextDouble() * 16, Y + 0.2, ITEMS_CLOSE.z() + 5 + random.nextDouble() * 8, ITEMS[random.nextInt(ITEMS.length)], PILE));
		}
		return c;
	}

	public List<String> level(int lv) {
		Random random = new Random(lv);
		List<String> c = new ArrayList<>();
		int st = lv / ROWS, z = 5 + lv % ROWS * 3, y = Y + st * STOREY;
		c.add(f("fill -4 %d %d 130 %d %d minecraft:%s", y - 1, z, y - 1, z + 2, st == 0 ? "smooth_stone" : "glass"));
		// A: armoured mobs in two staggered sub-rows, every third named, every fourth a mannequin where there are any
		for (int i = 0; i < 28; i++) {
			int n = lv * 28 + i;
			String name = n % 3 == 0 ? f(",CustomName:%s,CustomNameVisible:1b", nbtText("Mob_" + n)) : "";
			c.add(f("summon minecraft:%s %.1f %d %.1f {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[%df,0f]%s,%s}", MANNEQUIN && i % 4 == 3 ? "mannequin" : KINDS[(i % 14 + lv) % 6],
					i % 14 * 2.2 + i / 14 * 1.1, y, z + 0.7 + i / 14 * 1.5, random.nextInt(360), name, BenchWorld.LEGACY_SNBT
							? "ArmorItems:[{id:\"diamond_boots\",count:1},{id:\"diamond_leggings\",count:1},{id:\"diamond_chestplate\",count:1},{id:\"diamond_helmet\",count:1}],HandItems:[{id:\"diamond_sword\",count:1},{}]"
							: "equipment:{head:{id:\"diamond_helmet\",count:1},chest:{id:\"diamond_chestplate\",count:1},legs:{id:\"diamond_leggings\",count:1},feet:{id:\"diamond_boots\",count:1},mainhand:{id:\"diamond_sword\",count:1}}"));
		}
		// B: dropped items that never despawn or get picked up
		for (int i = 0; i < 60; i++) {
			c.add(f("summon minecraft:item %.2f %.1f %.2f {Item:{id:\"%s\",count:%d},PickupDelay:32767,Age:-32768}", 36 + random.nextDouble() * 24, y + 0.2,
					z + random.nextDouble() * 3, ITEMS[random.nextInt(ITEMS.length)], 1 + random.nextInt(64)));
		}
		// C: block entities
		for (int i = 0; i < 24; i++) {
			String b = BLOCKS[i % 6];
			String nbt = switch (b) {
				case "oak_sign[rotation=8]" -> f("{front_text:{messages:[%s,%s,%s,%s]}}", nbtText("Sign " + lv + "." + i), nbtText("ZakoOpt"), nbtText("benchmark"), nbtText("line 4"));
				case "player_head" -> "{profile:{name:\"Notch\"}}";
				default -> "";
			};
			c.add(f("setblock %d %d %d minecraft:%s%s", 64 + i % 12 * 2, y, z + i / 12 * 2, b, nbt));
		}
		for (int i = 0; i < 4; i++) {
			c.add(f("setblock %d %d %d minecraft:spawner{SpawnData:{entity:{id:\"minecraft:%s\"}},Delay:32767s,MinSpawnDelay:32767s,MaxSpawnDelay:32767s,RequiredPlayerRange:128s}", 88 + i * 2, y, z + 1,
					KINDS[(lv + i) % 6]));
		}
		// E: pistons pushing stone up and down every 2 ticks, powered from the cells under them by the row's two command blocks
		for (int i = 0; i < 6; i++) {
			c.add(f("setblock %d %d %d minecraft:air", 118 + i * 2, y - 1, z + 1));
			c.add(f("setblock %d %d %d minecraft:piston[facing=up]", 118 + i * 2, y, z + 1));
			c.add(f("setblock %d %d %d minecraft:stone", 118 + i * 2, y + 1, z + 1));
		}
		c.add(f("setblock 120 %d %d minecraft:repeating_command_block{auto:1b,Command:\"execute if score #pistons zakoopt_t matches 1 if score #p zakoopt_t matches 0..1 run fill 118 %d %d 128 %d %d minecraft:redstone_block replace minecraft:air\"}",
				Y - 2 - st, z + 1, y - 1, z + 1, y - 1, z + 1));
		c.add(f("setblock 124 %d %d minecraft:repeating_command_block{auto:1b,Command:\"execute if score #p zakoopt_t matches 2..3 run fill 118 %d %d 128 %d %d minecraft:air replace minecraft:redstone_block\"}",
				Y - 2 - st, z + 1, y - 1, z + 1, y - 1, z + 1));
		// D: holograms, a particle emitter every other row
		for (int i = 0; i < 6; i++) {
			c.add(f("summon minecraft:text_display %d %.1f %d {billboard:\"center\",alignment:\"center\",text:%s}", 94 + i * 4, y + 2.5, z + 1,
					nbtText("Hologram " + lv + "." + i + "\nZakoOpt benchmark\nline three")));
		}
		if (lv % 2 == 0) {
			c.add(f("setblock 104 %d %d minecraft:repeating_command_block{auto:1b,Command:\"execute if score #particles zakoopt_t matches 1 run particle minecraft:end_rod 104 %d %d 6 1 1 0.02 15\"}", Y - 2 - st, z + 1, y + 3, z + 1));
			c.add(f("setblock 112 %d %d minecraft:repeating_command_block{auto:1b,Command:\"execute if score #particles zakoopt_t matches 1 run particle minecraft:flame 112 %d %d 4 0.5 1 0.01 10\"}", Y - 2 - st, z + 1, y + 1, z + 1));
		}
		return c;
	}

	// commands parse '.' decimals whatever the player's locale
	private String f(String format, Object... args) {
		return String.format(Locale.ROOT, format, args);
	}

	// a plain text component as a command argument: a JSON string before 1.21.5, an SNBT string after, the same quoted form
	public String text(String s) {
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
	}

	// a text component stored in NBT: before 1.21.5 it is JSON inside an NBT string
	private String nbtText(String s) {
		return BenchWorld.LEGACY_SNBT ? "'" + text(s).replace("\\", "\\\\").replace("'", "\\'") + "'" : text(s);
	}
}
