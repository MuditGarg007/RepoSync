import java.io.*;
import java.net.*;
import java.nio.file.*;

public class Server {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java Server <remote_folder_path> [port]");
            return;
        }
        Path remoteDir = Paths.get(args[0]).toAbsolutePath().normalize();
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9999;

        Files.createDirectories(remoteDir);
        System.out.println("Server started. Remote folder: " + remoteDir);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port " + port + "...");

            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
                while (true) {
                    SyncOperation op = (SyncOperation) in.readObject();
                    processOperation(remoteDir, op);
                }
            }
        }
    }

    private static void processOperation(Path baseDir, SyncOperation op) {
        Path target = baseDir.resolve(op.relativePath).normalize();

        try {
            switch (op.operation) {
                case CREATE_DIR:
                    Files.createDirectories(target);
                    System.out.println("Created directory: " + target);
                    break;

                case WRITE_FILE:
                    // Make sure parent folders exist
                    Files.createDirectories(target.getParent());
                    Files.write(target, op.data);
                    System.out.println("Written file: " + target + " (" + op.data.length + " bytes)");
                    break;

                case DELETE:
                    if (Files.exists(target)) {
                        if (Files.isDirectory(target)) {
                            deleteRecursive(target);
                        } else {
                            Files.delete(target);
                        }
                        System.out.println("Deleted: " + target);
                    }
                    break;
            }
        } catch (IOException e) {
            System.err.println("Error processing " + op.relativePath + ": " + e.getMessage());
        }
    }

    private static void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.walk(path)) {
                stream.sorted((a, b) -> b.compareTo(a))  // delete children first
                      .forEach(p -> {
                          try { Files.deleteIfExists(p); }
                          catch (IOException ignored) {}
                      });
            }
        } else {
            Files.deleteIfExists(path);
        }
    }
}