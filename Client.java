import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class Client {

    private static Path localDir;
    private static ObjectOutputStream out;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java Client <local_folder_path> <server_ip> [port]");
            return;
        }
        localDir = Paths.get(args[0]).toAbsolutePath().normalize();
        String host = args[1];
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 9999;

        if (!Files.isDirectory(localDir)) {
            System.err.println("Local folder does not exist!");
            return;
        }

        Socket socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("Connected to server " + host + ":" + port);

        initialSync();

        watchDirectory();
    }

    private static void initialSync() throws IOException {
        System.out.println("Starting initial synchronization...");
        try (var stream = Files.walk(localDir)) {
            stream.forEach(path -> {
                try {
                    String rel = localDir.relativize(path).toString();
                    if (rel.isEmpty()) return;

                    if (Files.isDirectory(path)) {
                        send(new SyncOperation(Operation.CREATE_DIR, rel, null));
                    } else {
                        byte[] data = Files.readAllBytes(path);
                        send(new SyncOperation(Operation.WRITE_FILE, rel, data));
                    }
                } catch (Exception e) {
                    System.err.println("Initial sync error on " + path);
                }
            });
        }
        System.out.println("Initial sync completed!");
    }

    private static void watchDirectory() throws Exception {
        WatchService watcher = FileSystems.getDefault().newWatchService();
        Map<WatchKey, Path> keyToDir = new HashMap<>();

        registerAll(localDir, watcher, keyToDir);

        System.out.println("Watching for changes... (press Ctrl+C to stop)");

        while (true) {
            WatchKey key = watcher.take();
            Path dir = keyToDir.get(key);

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                Path name = (Path) event.context();
                Path fullPath = dir.resolve(name);

                String relative = localDir.relativize(fullPath).toString();

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    if (Files.isDirectory(fullPath)) {
                        registerAll(fullPath, watcher, keyToDir);
                        send(new SyncOperation(Operation.CREATE_DIR, relative, null));
                    } else {
                        byte[] data = Files.readAllBytes(fullPath);
                        send(new SyncOperation(Operation.WRITE_FILE, relative, data));
                    }
                } 
                else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    if (!Files.isDirectory(fullPath)) {
                        byte[] data = Files.readAllBytes(fullPath);
                        send(new SyncOperation(Operation.WRITE_FILE, relative, data));
                    }
                } 
                else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    send(new SyncOperation(Operation.DELETE, relative, null));
                }
            }
            key.reset();
        }
    }

    private static void registerAll(Path start, WatchService watcher, Map<WatchKey, Path> keyToDir) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
                keyToDir.put(key, dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void send(SyncOperation op) throws IOException {
        out.writeObject(op);
        out.flush();
        System.out.println("Sent: " + op.operation + " -> " + op.relativePath);
    }
}