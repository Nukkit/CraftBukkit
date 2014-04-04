package fr.ribesg.nukkit;

import net.minecraft.server.Block;
import net.minecraft.server.Blocks;
import net.minecraft.server.World;

/**
 * Based on the work of lishid and the Spigot integration.
 * Simplified because I don't need all the stuff.
 *
 * @author lishid
 * @author Ribesg
 */
public class Orebfuscator {

    /**
     * Orebfuscator instance
     */
    public static Orebfuscator instance;

    /**
     * Initialize Orebfuscator
     */
    public static void init(final boolean[] replacedBlocks, final byte[] replacedWith) {
        instance = new Orebfuscator(replacedBlocks, replacedWith);
    }

    /**
     * Blocks hidden
     */
    private final boolean[] replacedBlocks;

    /**
     * Blocks that will replace hidden blocks
     */
    private final byte[] replacedWith;

    private Orebfuscator(final boolean[] replacedBlocks, final byte[] replacedWith) {
        this.replacedBlocks = replacedBlocks;
        this.replacedWith = replacedWith;
    }

    /**
     * Removes all non exposed ores from the provided chunk buffer.
     */
    public void obfuscate(final int chunkX, final int chunkY, final int bitmask, final byte[] buffer, final World world) {
        final int initialRadius = 1;
        int index = 0;
        int randomOre = 0;

        final int startX = chunkX << 4;
        final int startZ = chunkY << 4;

        for (int i = 0; i < 16; i++) {
            if ((bitmask & 1 << i) != 0) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            if (index >= buffer.length) {
                                index++;
                                continue;
                            }
                            final int blockId = buffer[index] & 0xFF;
                            if (replacedBlocks[blockId]) {
                                if (!isLoaded(world, startX + x, (i << 4) + y, startZ + z, initialRadius)) {
                                    index++;
                                    continue;
                                }
                                if (!hasTransparentBlockAdjacent(world, startX + x, (i << 4) + y, startZ + z, initialRadius)) {
                                    if (randomOre >= replacedWith.length) {
                                        randomOre = 0;
                                    }
                                    buffer[index] = replacedWith[randomOre++];
                                }
                            }
                            index++;
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates all blocks within the set radius of the given coordinate, revealing them if they are hidden ores.
     */
    public void updateNearbyBlocks(final World world, final int x, final int y, final int z) {
        updateNearbyBlocks(world, x, y, z, 2, false);
    }

    private void updateNearbyBlocks(final World world, final int x, final int y, final int z, final int radius, final boolean updateSelf) {
        if (world.isLoaded(x, y, z)) {
            final Block block = world.getType(x, y, z);

            if (updateSelf && replacedBlocks[Block.b(block)]) {
                world.notify(x, y, z);
            }

            if (radius > 0) {
                updateNearbyBlocks(world, x + 1, y, z, radius - 1, true);
                updateNearbyBlocks(world, x - 1, y, z, radius - 1, true);
                updateNearbyBlocks(world, x, y + 1, z, radius - 1, true);
                updateNearbyBlocks(world, x, y - 1, z, radius - 1, true);
                updateNearbyBlocks(world, x, y, z + 1, radius - 1, true);
                updateNearbyBlocks(world, x, y, z - 1, radius - 1, true);
            }
        }
    }

    private boolean isLoaded(final World world, final int x, final int y, final int z, final int radius) {
        return world.isLoaded(x, y, z) &&
                (radius == 0 ||
                        (isLoaded(world, x + 1, y, z, radius - 1) &&
                                isLoaded(world, x - 1, y, z, radius - 1) &&
                                isLoaded(world, x, y + 1, z, radius - 1) &&
                                isLoaded(world, x, y - 1, z, radius - 1) &&
                                isLoaded(world, x, y, z + 1, radius - 1) &&
                                isLoaded(world, x, y, z - 1, radius - 1)));
    }

    private boolean hasTransparentBlockAdjacent(final World world, final int x, final int y, final int z, final int radius) {
        return !isSolidBlock(world.getType(x, y, z)) ||
                (radius > 0 && (hasTransparentBlockAdjacent(world, x + 1, y, z, radius - 1) ||
                        hasTransparentBlockAdjacent(world, x - 1, y, z, radius - 1) ||
                        hasTransparentBlockAdjacent(world, x, y + 1, z, radius - 1) ||
                        hasTransparentBlockAdjacent(world, x, y - 1, z, radius - 1) ||
                        hasTransparentBlockAdjacent(world, x, y, z + 1, radius - 1) ||
                        hasTransparentBlockAdjacent(world, x, y, z - 1, radius - 1)));
    }

    private boolean isSolidBlock(final Block block) {
        return block.r() && block != Blocks.MOB_SPAWNER;
    }
}
