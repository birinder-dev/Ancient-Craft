package dev.birinder.ac.worldgen;

import dev.birinder.ac.AncientCraft;
import dev.birinder.ac.block.ModBlocks;
import dev.birinder.ac.block.TavernBenchBlock;
import dev.birinder.ac.entity.ModEntities;
import dev.birinder.ac.entity.custom.GamblerEntity;
import dev.birinder.ac.entity.custom.GuardEntity;
import dev.birinder.ac.entity.custom.TavernVillagerEntity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;

/**
 * Builds an authentic medieval Market Alley structure:
 * - 25 blocks long, 15 blocks wide
 * - Worn cobblestone, stone brick, and gravel alleyway
 * - 3 distinct artisan shops on Left side (Bakery/Tavern, Blacksmith/Armory, Apothecary)
 * - 3 distinct artisan shops on Right side (Gambling Parlor, Fishmonger, Jeweler/Antiquities)
 * - Striped awnings extending over the street
 * - Overhead crossbeams with hanging lanterns
 * - Integrated Ancient Craft benches, tables, gambling tables, and stools
 * - Shopkeepers and guards populated inside
 */
public class MarketAlleyBuilder {

    public static final int LENGTH = 25; // Z: 0 to 24
    public static final int HALF_WIDTH = 7; // X: -7 to +7 (Width = 15)
    public static final int HEIGHT = 7; // Y: 0 to 6

    public static int build(World world, BlockPos origin, boolean spawnNpcs) {
        Random random = new Random(origin.asLong());
        int placedCount = 0;

        // Clear air space for the entire alley (from Y = 1 to 6)
        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
            for (int z = 0; z < LENGTH; z++) {
                for (int y = 1; y <= HEIGHT; y++) {
                    world.setBlockState(origin.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                }
            }
        }

        // ==========================================
        // 1. STREET PAVEMENT (X: -2 to +2, Z: 0 to 24)
        // ==========================================
        for (int x = -2; x <= 2; x++) {
            for (int z = 0; z < LENGTH; z++) {
                BlockState streetBlock;
                int r = random.nextInt(10);
                if (r < 4) {
                    streetBlock = Blocks.COBBLESTONE.getDefaultState();
                } else if (r < 7) {
                    streetBlock = Blocks.STONE_BRICKS.getDefaultState();
                } else if (r < 9) {
                    streetBlock = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
                } else {
                    streetBlock = Blocks.GRAVEL.getDefaultState();
                }
                world.setBlockState(origin.add(x, 0, z), streetBlock, 2);
                placedCount++;
            }
        }

        // ==========================================
        // 2. ENTRANCE & EXIT ARCHWAYS (Z = 0 and Z = 24)
        // ==========================================
        buildArchway(world, origin, 0);
        buildArchway(world, origin, LENGTH - 1);

        // ==========================================
        // 3. OVERHEAD CROSSBEAMS & STREET LANTERNS
        // ==========================================
        int[] beamZ = {6, 12, 18};
        for (int z : beamZ) {
            // High wooden crossbeam spanning the street at Y = 5
            for (int x = -3; x <= 3; x++) {
                world.setBlockState(origin.add(x, 5, z), Blocks.SPRUCE_PLANKS.getDefaultState(), 2);
            }
            // Hanging chain & lantern in the center of the street
            world.setBlockState(origin.add(0, 4, z), Blocks.CHAIN.getDefaultState(), 2);
            world.setBlockState(origin.add(0, 3, z), Blocks.LANTERN.getDefaultState(), 2);
        }

        // ==========================================
        // 4. STREET SEATING & REST AREA (Z = 12)
        // ==========================================
        // Planter boxes with flowers on the sides of the street
        world.setBlockState(origin.add(-2, 1, 9), Blocks.DIRT.getDefaultState(), 2);
        world.setBlockState(origin.add(-2, 2, 9), Blocks.POPPY.getDefaultState(), 2);
        world.setBlockState(origin.add(2, 1, 9), Blocks.DIRT.getDefaultState(), 2);
        world.setBlockState(origin.add(2, 2, 9), Blocks.CORNFLOWER.getDefaultState(), 2);

        // Center tavern benches & table facing each other along the street
        world.setBlockState(origin.add(-1, 1, 12), ModBlocks.TAVERN_BENCH.getDefaultState().with(TavernBenchBlock.FACING, Direction.EAST), 2);
        world.setBlockState(origin.add(0, 1, 12), ModBlocks.TAVERN_TABLE.getDefaultState(), 2);
        world.setBlockState(origin.add(1, 1, 12), ModBlocks.TAVERN_BENCH.getDefaultState().with(TavernBenchBlock.FACING, Direction.WEST), 2);

        // ==========================================
        // 5. LEFT SIDE SHOPS (X: -7 to -3)
        // ==========================================
        // Shop 1: Bakery & Tavern (Z: 1 to 7)
        buildShop(world, origin, -7, -3, 1, 7,
                Blocks.OAK_PLANKS.getDefaultState(), Blocks.STRIPPED_SPRUCE_LOG.getDefaultState(),
                Blocks.RED_WOOL.getDefaultState(), Blocks.WHITE_WOOL.getDefaultState(),
                Direction.EAST);
        decorateBakeryShop(world, origin);

        // Shop 2: Blacksmith & Armory (Z: 9 to 15)
        buildShop(world, origin, -7, -3, 9, 15,
                Blocks.STONE_BRICKS.getDefaultState(), Blocks.COBBLESTONE.getDefaultState(),
                Blocks.ORANGE_WOOL.getDefaultState(), Blocks.BLACK_WOOL.getDefaultState(),
                Direction.EAST);
        decorateBlacksmithShop(world, origin);

        // Shop 3: Alchemist & Apothecary (Z: 17 to 23)
        buildShop(world, origin, -7, -3, 17, 23,
                Blocks.SPRUCE_PLANKS.getDefaultState(), Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState(),
                Blocks.PURPLE_WOOL.getDefaultState(), Blocks.YELLOW_WOOL.getDefaultState(),
                Direction.EAST);
        decorateApothecaryShop(world, origin);

        // ==========================================
        // 6. RIGHT SIDE SHOPS (X: +3 to +7)
        // ==========================================
        // Shop 4: The Lucky Roll - Dice Parlor (Z: 1 to 7)
        buildShop(world, origin, 3, 7, 1, 7,
                Blocks.DARK_OAK_PLANKS.getDefaultState(), Blocks.STRIPPED_SPRUCE_LOG.getDefaultState(),
                Blocks.GREEN_WOOL.getDefaultState(), Blocks.YELLOW_WOOL.getDefaultState(),
                Direction.WEST);
        decorateGamblingShop(world, origin);

        // Shop 5: Fishmonger & Smoker (Z: 9 to 15)
        buildShop(world, origin, 3, 7, 9, 15,
                Blocks.BIRCH_PLANKS.getDefaultState(), Blocks.STRIPPED_BIRCH_LOG.getDefaultState(),
                Blocks.CYAN_WOOL.getDefaultState(), Blocks.WHITE_WOOL.getDefaultState(),
                Direction.WEST);
        decorateFishmongerShop(world, origin);

        // Shop 6: Jeweler & Antiquities (Z: 17 to 23)
        buildShop(world, origin, 3, 7, 17, 23,
                Blocks.POLISHED_ANDESITE.getDefaultState(), Blocks.CHISELED_STONE_BRICKS.getDefaultState(),
                Blocks.MAGENTA_WOOL.getDefaultState(), Blocks.WHITE_WOOL.getDefaultState(),
                Direction.WEST);
        decorateJewelerShop(world, origin);

        // ==========================================
        // 7. SPAWN NPCS
        // ==========================================
        if (spawnNpcs && !world.isClient()) {
            spawnShopkeeper(world, origin.add(-5, 1, 4), "Baker Tobias", Direction.EAST);
            spawnShopkeeper(world, origin.add(-5, 1, 12), "Smith Ulfric", Direction.EAST);
            spawnShopkeeper(world, origin.add(-5, 1, 20), "Alchemist Miriam", Direction.EAST);

            spawnGambler(world, origin.add(5, 1, 4), Direction.WEST);
            spawnShopkeeper(world, origin.add(5, 1, 12), "Fisher Finnegan", Direction.WEST);
            spawnShopkeeper(world, origin.add(5, 1, 20), "Jeweler Lysander", Direction.WEST);

            spawnGuard(world, origin.add(0, 1, 1), "Market Gate Guard", Direction.SOUTH);
            spawnGuard(world, origin.add(0, 1, 23), "Market Watchman", Direction.NORTH);
        }

        return placedCount;
    }

    private static void buildArchway(World world, BlockPos origin, int z) {
        // Pillars on both street curb sides
        for (int y = 1; y <= 4; y++) {
            world.setBlockState(origin.add(-3, y, z), Blocks.STRIPPED_SPRUCE_LOG.getDefaultState(), 2);
            world.setBlockState(origin.add(3, y, z), Blocks.STRIPPED_SPRUCE_LOG.getDefaultState(), 2);
        }
        // Top lintel
        for (int x = -3; x <= 3; x++) {
            world.setBlockState(origin.add(x, 5, z), Blocks.SPRUCE_PLANKS.getDefaultState(), 2);
        }
        // Lanterns hanging under the arch pillars
        world.setBlockState(origin.add(-2, 4, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.HALF, BlockHalf.TOP).with(StairsBlock.FACING, Direction.EAST), 2);
        world.setBlockState(origin.add(2, 4, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.HALF, BlockHalf.TOP).with(StairsBlock.FACING, Direction.WEST), 2);
        world.setBlockState(origin.add(0, 4, z), Blocks.LANTERN.getDefaultState(), 2);
    }

    private static void buildShop(World world, BlockPos origin, int minX, int maxX, int minZ, int maxZ,
                                  BlockState wallBlock, BlockState pillarBlock,
                                  BlockState woolColor1, BlockState woolColor2,
                                  Direction facingStreet) {
        // Floor
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockState(origin.add(x, 0, z), wallBlock, 2);
            }
        }

        // Corner Pillars (Y = 1 to 4)
        for (int y = 1; y <= 4; y++) {
            world.setBlockState(origin.add(minX, y, minZ), pillarBlock, 2);
            world.setBlockState(origin.add(minX, y, maxZ), pillarBlock, 2);
            world.setBlockState(origin.add(maxX, y, minZ), pillarBlock, 2);
            world.setBlockState(origin.add(maxX, y, maxZ), pillarBlock, 2);
        }

        // Back and Side Walls
        boolean isLeft = (facingStreet == Direction.EAST);
        int backX = isLeft ? minX : maxX;
        int frontX = isLeft ? maxX : minX;
        int awningExtendX = isLeft ? (frontX + 1) : (frontX - 1);

        // Back wall
        for (int z = minZ + 1; z < maxZ; z++) {
            for (int y = 1; y <= 3; y++) {
                world.setBlockState(origin.add(backX, y, z), wallBlock, 2);
            }
        }
        // Side walls
        for (int x = Math.min(minX, maxX) + 1; x < Math.max(minX, maxX); x++) {
            for (int y = 1; y <= 3; y++) {
                world.setBlockState(origin.add(x, y, minZ), wallBlock, 2);
                world.setBlockState(origin.add(x, y, maxZ), wallBlock, 2);
            }
        }

        // Roof ceiling at Y = 4
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockState(origin.add(x, 4, z), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
            }
        }

        // Front Counter at Y = 1 (Opening on side for merchant)
        for (int z = minZ + 1; z <= maxZ - 2; z++) {
            world.setBlockState(origin.add(frontX, 1, z), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
        }
        // Merchant entrance flap (trapdoor)
        world.setBlockState(origin.add(frontX, 1, maxZ - 1), Blocks.SPRUCE_TRAPDOOR.getDefaultState(), 2);

        // Striped Awning at Y = 3 over the counter and extending 1 block into the street
        for (int z = minZ; z <= maxZ; z++) {
            BlockState stripe = ((z % 2) == 0) ? woolColor1 : woolColor2;
            world.setBlockState(origin.add(frontX, 3, z), stripe, 2);
            world.setBlockState(origin.add(awningExtendX, 3, z), stripe, 2);
        }

        // Interior lantern
        int midX = (minX + maxX) / 2;
        int midZ = (minZ + maxZ) / 2;
        world.setBlockState(origin.add(midX, 3, midZ), Blocks.LANTERN.getDefaultState(), 2);
    }

    private static void decorateBakeryShop(World world, BlockPos origin) {
        // Cake on counter
        world.setBlockState(origin.add(-4, 2, 3), Blocks.CAKE.getDefaultState(), 2);
        // Barrels and crafting bench in back
        world.setBlockState(origin.add(-6, 1, 2), Blocks.BARREL.getDefaultState(), 2);
        world.setBlockState(origin.add(-6, 2, 2), Blocks.BARREL.getDefaultState(), 2);
        world.setBlockState(origin.add(-6, 1, 3), Blocks.CRAFTING_TABLE.getDefaultState(), 2);
        // Customer tavern stools outside along the street counter
        world.setBlockState(origin.add(-2, 1, 2), ModBlocks.TAVERN_STOOL.getDefaultState(), 2);
        world.setBlockState(origin.add(-2, 1, 4), ModBlocks.TAVERN_STOOL.getDefaultState(), 2);
    }

    private static void decorateBlacksmithShop(World world, BlockPos origin) {
        // Anvil & Blast furnace
        world.setBlockState(origin.add(-6, 1, 10), Blocks.BLAST_FURNACE.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST), 2);
        world.setBlockState(origin.add(-6, 1, 11), Blocks.ANVIL.getDefaultState(), 2);
        world.setBlockState(origin.add(-6, 1, 13), Blocks.GRINDSTONE.getDefaultState(), 2);
        world.setBlockState(origin.add(-6, 1, 14), Blocks.SMITHING_TABLE.getDefaultState(), 2);
        // Iron bars on front window
        world.setBlockState(origin.add(-3, 2, 11), Blocks.IRON_BARS.getDefaultState(), 2);
        world.setBlockState(origin.add(-3, 2, 12), Blocks.IRON_BARS.getDefaultState(), 2);
        // Water cauldron
        world.setBlockState(origin.add(-5, 1, 10), Blocks.CAULDRON.getDefaultState(), 2);
    }

    private static void decorateApothecaryShop(World world, BlockPos origin) {
        // Brewing stand on table
        world.setBlockState(origin.add(-6, 1, 18), Blocks.BOOKSHELF.getDefaultState(), 2);
        world.setBlockState(origin.add(-6, 2, 18), Blocks.BOOKSHELF.getDefaultState(), 2);
        world.setBlockState(origin.add(-6, 1, 19), Blocks.BREWING_STAND.getDefaultState(), 2);
        world.setBlockState(origin.add(-6, 1, 21), Blocks.CAULDRON.getDefaultState(), 2);
        world.setBlockState(origin.add(-6, 1, 22), Blocks.CHEST.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST), 2);
        // Potted flower on counter
        world.setBlockState(origin.add(-4, 2, 19), Blocks.POTTED_BLUE_ORCHID.getDefaultState(), 2);
    }

    private static void decorateGamblingShop(World world, BlockPos origin) {
        // CENTERPIECE: Mod's custom Gambling Table and Stools!
        world.setBlockState(origin.add(5, 1, 3), ModBlocks.GAMBLING_TABLE.getDefaultState(), 2);
        world.setBlockState(origin.add(5, 1, 2), ModBlocks.GAMBLING_STOOL.getDefaultState(), 2);
        world.setBlockState(origin.add(5, 1, 4), ModBlocks.GAMBLING_STOOL.getDefaultState(), 2);
        // Gold and barrel accents
        world.setBlockState(origin.add(6, 1, 5), Blocks.GOLD_BLOCK.getDefaultState(), 2);
        world.setBlockState(origin.add(6, 1, 6), Blocks.BARREL.getDefaultState(), 2);
        // Green carpet
        world.setBlockState(origin.add(4, 1, 5), Blocks.GREEN_CARPET.getDefaultState(), 2);
    }

    private static void decorateFishmongerShop(World world, BlockPos origin) {
        // Smoker and ice blocks
        world.setBlockState(origin.add(6, 1, 10), Blocks.SMOKER.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST), 2);
        world.setBlockState(origin.add(6, 1, 11), Blocks.BARREL.getDefaultState(), 2);
        world.setBlockState(origin.add(6, 2, 11), Blocks.BARREL.getDefaultState(), 2);
        world.setBlockState(origin.add(6, 1, 13), Blocks.PACKED_ICE.getDefaultState(), 2);
        world.setBlockState(origin.add(6, 1, 14), Blocks.CAULDRON.getDefaultState(), 2);
        // Counter fish box
        world.setBlockState(origin.add(4, 1, 11), Blocks.PACKED_ICE.getDefaultState(), 2);
    }

    private static void decorateJewelerShop(World world, BlockPos origin) {
        // Decorated pots, Amethyst and gold showcase
        world.setBlockState(origin.add(6, 1, 18), Blocks.DECORATED_POT.getDefaultState(), 2);
        world.setBlockState(origin.add(6, 1, 19), Blocks.AMETHYST_BLOCK.getDefaultState(), 2);
        world.setBlockState(origin.add(6, 2, 19), Blocks.AMETHYST_CLUSTER.getDefaultState(), 2);
        world.setBlockState(origin.add(6, 1, 21), Blocks.CHEST.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST), 2);
        world.setBlockState(origin.add(6, 1, 22), Blocks.GOLD_BLOCK.getDefaultState(), 2);
    }

    private static void spawnShopkeeper(World world, BlockPos pos, String customName, Direction facing) {
        TavernVillagerEntity villager = new TavernVillagerEntity(ModEntities.TAVERN_VILLAGER, world);
        villager.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        villager.setCustomName(net.minecraft.text.Text.literal(customName));
        villager.setCustomNameVisible(true);
        villager.setYaw(facing.asRotation());
        villager.setHeadYaw(facing.asRotation());
        villager.setBodyYaw(facing.asRotation());
        world.spawnEntity(villager);
    }

    private static void spawnGambler(World world, BlockPos pos, Direction facing) {
        GamblerEntity gambler = new GamblerEntity(ModEntities.GAMBLER, world);
        gambler.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        gambler.setCustomName(net.minecraft.text.Text.literal("Dice Master Joram"));
        gambler.setCustomNameVisible(true);
        gambler.setYaw(facing.asRotation());
        gambler.setHeadYaw(facing.asRotation());
        gambler.setBodyYaw(facing.asRotation());
        world.spawnEntity(gambler);
    }

    private static void spawnGuard(World world, BlockPos pos, String customName, Direction facing) {
        GuardEntity guard = new GuardEntity(ModEntities.GUARD, world);
        guard.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        guard.setCustomName(net.minecraft.text.Text.literal(customName));
        guard.setCustomNameVisible(true);
        guard.setYaw(facing.asRotation());
        guard.setHeadYaw(facing.asRotation());
        guard.setBodyYaw(facing.asRotation());
        world.spawnEntity(guard);
    }

    /**
     * Captures the built in-world alleyway and exports it to a compressed .nbt StructureTemplate file.
     */
    public static boolean exportToNbt(ServerWorld world, BlockPos origin, File outputFile) {
        try {
            BlockPos minPos = origin.add(-HALF_WIDTH, 0, 0);
            net.minecraft.util.math.Vec3i size = new net.minecraft.util.math.Vec3i(HALF_WIDTH * 2 + 1, HEIGHT + 1, LENGTH);

            StructureTemplate template = new StructureTemplate();
            template.saveFromWorld(world, minPos, size, true, Blocks.STRUCTURE_VOID);

            NbtCompound nbt = template.writeNbt(new NbtCompound());

            outputFile.getParentFile().mkdirs();
            try (OutputStream os = new FileOutputStream(outputFile)) {
                NbtIo.writeCompressed(nbt, os);
            }
            AncientCraft.LOGGER.info("Successfully exported Market Alley structure to: {}", outputFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            AncientCraft.LOGGER.error("Failed to export Market Alley structure to NBT", e);
            return false;
        }
    }
}
