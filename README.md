# RepoSync

RepoSync is a simple Java-based tool for synchronizing files and directories from a local client machine to a remote server. 

## Components

The project consists of three main Java classes:

- `Server.java`: Listens for incoming connections from clients and applies file operations (create, write, delete) to a specified remote directory.
- `Client.java`: Monitors a specified local directory for changes using the Java WatchService API and sends these changes to the server. It also performs an initial full synchronization on startup.
- `SyncOperation.java`: A serializable data structure that encapsulates the data transferred between the client and the server, such as the operation type, file path, and file contents.

## Compilation

To compile the project, run the following command in the project directory:

```bash
javac *.java
```

## Usage

### 1. Start the Server

Run the server by providing a path to the target directory where the synchronized files will be stored. You can also provide an optional port number (defaults to 9999):

```bash
java Server <remote_folder_path> [port]
```

Example:
```bash
java Server ./server_data 9999
```

### 2. Start the Client

Run the client by providing the path to the local directory you want to synchronize, the server IP address, and an optional port number (defaults to 9999):

```bash
java Client <local_folder_path> <server_ip> [port]
```

Example:
```bash
java Client ./client_data localhost 9999
```

## How It Works

1. When the client starts, it connects to the server and performs an initial synchronization, sending all existing files and directories from the local folder.
2. The client then sets up a watcher to monitor the local folder for any new files, modified files, or deleted files.
3. Every detected change is sent to the server and replicated in the target remote folder.
