package fr.ribesg.nukkit.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * This command will dump a Heap snapshot into a file.
 * This is useful to debug memory leaks even on a production server.
 *
 * @author Ribesg
 */
public class HeapDumpCommand extends Command {

    private static volatile Object hotspotMBean;

    /**
     * Dumps a heap snapshot into a file.
     *
     * @param fileName name of the heap dump file
     * @param live     flag that tells whether to dump only the live objects
     */
    @SuppressWarnings("unchecked")
    private static void dumpHeap(final String fileName, final boolean live) throws Exception {
        final Class clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");

        // Initialize the HotSpot MBean if it isn't
        if (hotspotMBean == null) {
            synchronized (HeapDumpCommand.class) {
                if (hotspotMBean == null) {
                    final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                    hotspotMBean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", clazz);
                }
            }
        }

        // Call hotspotMBean#dumpHeap(String fileName, boolean live);
        final Method m = clazz.getMethod("dumpHeap", String.class, boolean.class);
        m.invoke(hotspotMBean, fileName, live);
    }

    public HeapDumpCommand() {
        super("heapdump");
        this.description = "Dump a heap snapshot. Use '/heapdump all' to get more than live objects.";
        this.usageMessage = "/heapdump [all]";
        this.setPermission("nukkit.command.heapdump");
        this.setAliases(Arrays.asList("hdump", "hd"));
    }

    @Override
    public boolean execute(final CommandSender sender, final String currentAlias, final String[] args) {
        if (!testPermission(sender)) {
            return true;
        }

        // Find a free fileName
        int i = 0;
        String fileName;
        File file;
        do {
            fileName = "heap." + i++ + ".bin";
            file = new File(fileName);
        } while (file.exists());

        // DUMP ALL THE THINGZ
        try {
            Bukkit.broadcastMessage(ChatColor.GOLD + "Heap dump...");
            final long start = System.nanoTime();
            dumpHeap(fileName, !(args.length > 0 && "all".equalsIgnoreCase(args[0])));
            final long end = System.nanoTime();
            sender.sendMessage(ChatColor.GREEN + "Heap dumped in " + format(start, end));
        } catch (final Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to dump heap, see console for details");
            throw new RuntimeException("Failed to dump heap", e);
        }

        return true;
    }

    private String format(final long start, final long end) {
        final long diff = end - start;
        if (diff > /* 1min */ 60000000000L) {
            final long min = diff / 60000000000L;
            return min + " minutes" + (diff - min > 1000000000L ? " and " + diff % 60000000000L + " seconds" : "");
        } else if (diff > /* 1s */ 1000000000L) {
            return (diff / 1000000000L) + " seconds";
        } else if (diff > /* 1ms */ 1000000L) {
            return (diff / 1000000L) + " milliseconds (ms)";
        } else if (diff > /* 1µs */ 1000L) {
            return (diff / 1000L) + " microseconds (µs)";
        } else {
            return diff + " nanoseconds (ns)";
        }
    }
}
