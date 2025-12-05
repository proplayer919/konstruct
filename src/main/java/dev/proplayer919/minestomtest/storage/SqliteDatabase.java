package dev.proplayer919.minestomtest.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class SqliteDatabase {
    private final Path dbPath;
    private Connection connection;
    private final ExecutorService dbWorker;
    private final Object connLock = new Object();

    public SqliteDatabase(Path dbPath) {
        this.dbPath = dbPath;
        this.dbWorker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "sqlite-db-worker");
            t.setDaemon(true);
            return t;
        });
    }

    public CompletableFuture<Void> connectAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                connect();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, dbWorker);
    }

    public void connect() throws Exception {
        synchronized (connLock) {
            if (connection != null && !connection.isClosed()) return;
            Files.createDirectories(dbPath.getParent());
            String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
            connection = DriverManager.getConnection(url);
            try (Statement s = connection.createStatement()) {
                // Enable WAL for better concurrency
                s.execute("PRAGMA journal_mode=WAL;");
                s.execute("PRAGMA synchronous=NORMAL;");
                s.execute("PRAGMA foreign_keys=ON;");
            }
            ensureSchema();
        }
    }

    public void close() {
        // Shutdown worker after closing connection
        Future<?> future = dbWorker.submit(() -> {
            synchronized (connLock) {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException ignored) {
                    }
                }
            }
        });
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        dbWorker.shutdown();
        try {
            if (!dbWorker.awaitTermination(2, TimeUnit.SECONDS)) dbWorker.shutdownNow();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void ensureSchema() throws SQLException {
        // Run schema creation on the connection thread to ensure ordering
        try (Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY);");
            s.execute("CREATE TABLE IF NOT EXISTS player_permissions (player_uuid TEXT NOT NULL, permission_node TEXT NOT NULL, PRIMARY KEY(player_uuid, permission_node), FOREIGN KEY(player_uuid) REFERENCES players(uuid) ON DELETE CASCADE);");
            s.execute("CREATE INDEX IF NOT EXISTS idx_permission_node ON player_permissions(permission_node);");
        }
    }

    // Run an update (INSERT/UPDATE/DELETE) asynchronously
    public CompletableFuture<Integer> runAsyncUpdate(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = prepareStatementSync(sql, params)) {
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, dbWorker);
    }

    // Run query asynchronously and return list of rows as maps
    public CompletableFuture<List<Map<String, Object>>> runAsyncQuery(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = prepareStatementSync(sql, params);
                 ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(md.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, dbWorker);
    }

    // Prepare a statement and set parameters (must be called on DB thread)
    private PreparedStatement prepareStatementSync(String sql, Object... params) throws SQLException {
        synchronized (connLock) {
            if (connection == null) throw new SQLException("Connection not established");
            PreparedStatement ps = connection.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            return ps;
        }
    }

    // Run a synchronous transaction (blocking) - will run on DB thread and block until completion
    public void runInTransaction(Consumer<Connection> consumer) throws Exception {
        Future<?> f = dbWorker.submit(() -> {
            synchronized (connLock) {
                try {
                    boolean oldAuto = connection.getAutoCommit();
                    connection.setAutoCommit(false);
                    try {
                        consumer.accept(connection);
                        connection.commit();
                    } catch (Throwable t) {
                        try {
                            connection.rollback();
                        } catch (SQLException ignored) {
                        }
                        throw new CompletionException(t);
                    } finally {
                        connection.setAutoCommit(oldAuto);
                    }
                } catch (SQLException e) {
                    throw new CompletionException(e);
                }
            }
        });
        try {
            f.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CompletionException && cause.getCause() != null) throw new Exception(cause.getCause());
            throw new Exception(cause);
        }
    }

    // Synchronous convenience wrappers (blocks on async)
    public boolean playerHasPermissionSync(UUID playerUuid, String permissionNode) {
        List<Map<String, Object>> rows = runAsyncQuery("SELECT 1 FROM player_permissions WHERE player_uuid = ? AND permission_node = ? LIMIT 1", playerUuid.toString(), permissionNode).join();
        return !rows.isEmpty();
    }

    public void insertPlayerPermissionSync(UUID playerUuid, String permissionNode) {
        runAsyncUpdate("INSERT OR IGNORE INTO players(uuid) VALUES (?)", playerUuid.toString()).join();
        runAsyncUpdate("INSERT OR IGNORE INTO player_permissions(player_uuid, permission_node) VALUES (?, ?)", playerUuid.toString(), permissionNode).join();
    }

    public void removePlayerPermissionSync(UUID playerUuid, String permissionNode) {
        runAsyncUpdate("DELETE FROM player_permissions WHERE player_uuid = ? AND permission_node = ?", playerUuid.toString(), permissionNode).join();
    }
}

