import java.io.Serializable;

enum Operation {
    CREATE_DIR,
    WRITE_FILE,  
    DELETE
}

public class SyncOperation implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Operation operation;
    public final String relativePath;
    public final byte[] data;

    public SyncOperation(Operation operation, String relativePath, byte[] data) {
        this.operation = operation;
        this.relativePath = relativePath;
        this.data = data;
    }
}