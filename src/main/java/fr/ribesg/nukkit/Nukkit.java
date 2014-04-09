package fr.ribesg.nukkit;

import fr.ribesg.nukkit.command.HeapDumpCommand;
import fr.ribesg.nukkit.command.TicksPerSecondCommand;
import net.minecraft.server.Block;
import net.minecraft.util.gnu.trove.set.TByteSet;
import net.minecraft.util.gnu.trove.set.hash.TByteHashSet;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;

import java.io.*;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

public class Nukkit {

    /**
     * Nukkit instance
     */
    public static Nukkit instance;

    /* * * * * * * * * * * * *
     * Configuration values  *
     * * * * * * * * * * * * */

    /**
     * Orebfuscator - blocks hidden
     */
    private final boolean[] replacedBlocks = new boolean[Short.MAX_VALUE];

    /**
     * Orebfuscator - blocks that will replace hidden blocks
     */
    private byte[] replacedWith;

    /**
     * ChunkTick - chunks to grow per tick
     */
    public int chunksPerTick;

    /**
     * ChunkTick - clear tick list
     */
    public boolean clearTickList;

    /**
     * Player shuffling
     */
    public int playerShuffling;

    /* * * * * * * * * *
     * Static methods  *
     * * * * * * * * * */

    /**
     * Creates the instance of Nukkit.
     *
     * @param server the CraftServer
     */
    public static void init(final CraftServer server) {
        instance = new Nukkit(server);

        server.getCommandMap().register("tps", "Nukkit", new TicksPerSecondCommand());
        server.getCommandMap().register("heapdump", "Nukkit", new HeapDumpCommand());

        Orebfuscator.init(instance.replacedBlocks, instance.replacedWith);
    }

    /* * * * * * * *
     * Non-statics *
     * * * * * * * */

    /**
     * The CraftServer
     */
    private final CraftServer server;

    /**
     * Nukkit constructor.
     *
     * @param server the CraftServer
     */
    private Nukkit(final CraftServer server) {
        this.server = server;

        final File bukkitConfigFile = server.getConfigFile();
        final String pathPrefix = bukkitConfigFile.getParentFile() == null ? "" : bukkitConfigFile.getParentFile().getPath()
                + File.pathSeparatorChar;
        final File nukkitConfigFile = new File(pathPrefix + "nukkit.yml");
        if (!nukkitConfigFile.exists()) {
            createConfig(nukkitConfigFile);
        } else {
            loadConfig(nukkitConfigFile);
        }
    }

    /**
     * Creates and populate the nukkit.yml file.
     *
     * @param nukkitConfigFile the nukkit.yml file
     */
    private void createConfig(final File nukkitConfigFile) {
        // Set default values
        for (int i : new int[]{1, 13, 14, 15, 21, 56, 73, 129}) {
            this.replacedBlocks[i] = true;
        }

        this.replacedWith = new byte[]{56};

        this.chunksPerTick = 650;

        this.clearTickList = false;

        this.playerShuffling = 0;

        // Write file
        try {
            if (!nukkitConfigFile.createNewFile()) {
                throw new IllegalStateException("nukkit.yml shouldn't exist but was found");
            } else {
                final Writer writer = new FileWriter(nukkitConfigFile);
                writer.write(getConfigString());
                writer.flush();
                writer.close();
                this.server.getLogger().info("Nukkit config not found, created default nukkit.yml");
            }
        } catch (final IOException e) {
            this.server.getLogger().log(Level.SEVERE, "Unable to create nukkit.yml!", e);
        }
    }

    /**
     * Creates the String which will be written as the Configuration content.
     *
     * @return the Configuration content
     */
    private String getConfigString() {
        final StringBuilder builder = new StringBuilder();

        // Header
        builder.append("# Nukkit configuration file\n\n");

        // Orebfuscator - Hidden blocks
        builder.append("# List of block ids hidden/replaced:\n");
        builder.append("replacedBlocks:\n");
        for (int i = 0; i < this.replacedBlocks.length; i++) {
            if (this.replacedBlocks[i]) {
                builder.append("- ").append(i).append('\n');
            }
        }
        builder.append("\n");

        // Orebfuscator - Replacing blocks
        builder.append("# List of block ids which will replace hidden ones:\n");
        builder.append("replacedWith:\n");
        for (byte id : this.replacedWith) {
            builder.append("- ").append(0xFF & id).append('\n');
        }
        builder.append("\n");

        // ChunkTick - Chunks to grow per tick
        builder.append("# Number of chunks to grow per tick:\n");
        builder.append("chunksPerTick: ").append(this.chunksPerTick).append("\n\n");

        // ChunkTick - clear tick list
        builder.append("# Clear tick list ?:\n");
        builder.append("clearTickList: ").append(this.clearTickList).append("\n\n");

        // Player shuffling
        builder.append("# Player shuffling - prevents players from 'gaming' the server, and strategically relogging to increase their position in the tick order:\n");
        builder.append("playerShuffling: ").append(this.playerShuffling).append("\n\n");

        return builder.toString();
    }

    /**
     * Loads the configuration from the nukkit.yml file.
     *
     * @param nukkitConfigFile the nukkit.yml file
     */
    private void loadConfig(final File nukkitConfigFile) {
        final YamlConfiguration config = new YamlConfiguration();

        // Read file
        try {
            if (!nukkitConfigFile.exists()) {
                throw new IllegalStateException("nukkit.yml should exist but was not found");
            } else {
                final String content = new Scanner(nukkitConfigFile).useDelimiter("\\Z").next();
                config.loadFromString(content);
            }
        } catch (final FileNotFoundException e) {
            this.server.getLogger().log(Level.SEVERE, "nukkit.yml should exist but was not found", e);
        } catch (final InvalidConfigurationException e) {
            this.server.getLogger().log(Level.SEVERE, "nukkit.yml is not valid", e);
        }

        // Set config values

        // Orebfuscator - Hidden blocks
        if (config.isList("replacedBlocks")) {
            for (int i = 0; i < this.replacedBlocks.length; i++) {
                this.replacedBlocks[i] = false;
            }
            final List<Integer> replacedBlocksList = config.getIntegerList("replacedBlocks");
            for (final Integer i : replacedBlocksList) {
                this.replacedBlocks[i] = true;
            }
        }

        // Orebfuscator - Replacing blocks
        if (config.isList("replacedWith")) {
            final List<Integer> replacedWithList = config.getIntegerList("replacedWith");
            final TByteSet bytes = new TByteHashSet(replacedWithList.size());
            for (final Integer i : replacedWithList) {
                final Block b = Block.e(i);
                if (b != null && !b.isTileEntity()) {
                    bytes.add((byte)(int)i);
                }
            }
            this.replacedWith = bytes.toArray();
        }

        // ChunkTick - Chunks to grow per tick
        if (config.isInt("chunksPerTick")) {
            this.chunksPerTick = config.getInt("chunksPerTick", this.chunksPerTick);
        }

        // ChunkTick - clear tick list
        if (config.isBoolean("clearTickList")) {
            this.clearTickList = config.getBoolean("clearTickList", this.clearTickList);
        }

        // Player shuffling
        if (config.isInt("playerShuffling")) {
            this.playerShuffling = config.getInt("playerShuffling", this.playerShuffling);
        }
    }

}
