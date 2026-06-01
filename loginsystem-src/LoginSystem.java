package com.example.loginsystem;

import com.example.loginsystem.AdminGUIMenu;
import com.example.loginsystem.AdminWebServer;
import com.example.loginsystem.LanguageManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.world.item.crafting.Recipe;
import java.util.Collection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

@Mod(value="loginsystem")
public class LoginSystem {
    public static final Logger LOGGER = LogManager.getLogger();
    private final HashMap<UUID, String> playerPasswords;
    private final HashMap<UUID, Boolean> loggedIn;
    private final HashMap<UUID, double[]> originalPositions;
    private final HashSet<UUID> alreadyDisconnected;
    private final HashMap<UUID, ItemStack[]> savedInventories;
    private final HashMap<UUID, ServerBossEvent> playerBossBars;
    private LanguageManager languageManager;
    private final Properties config;
    private final File configFile;
    private final File passwordFile;
    private final File playerDataDir;
    private boolean enableDatabase;
    private String jdbcUrl;
    private String dbHost;
    private String dbPort;
    private String dbName;
    private String dbUsername;
    private String dbPassword;
    private boolean enableWebServer;
    private int webServerPort;
    private String webServerPassword;
    private AdminWebServer adminWebServer;
    private final HashMap<UUID, String> plainTextPasswords;
    private final HashMap<UUID, Integer> failedLoginAttempts;
    private final HashMap<UUID, Long> tempBans;
    private final HashSet<UUID> mutedPlayers;
    private final HashMap<UUID, Long> lastLogins;
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("LoginSystem-DB-Thread");
        return t;
    });

    public LoginSystem() {
        block18: {
            this.playerPasswords = new HashMap();
            this.loggedIn = new HashMap();
            this.originalPositions = new HashMap();
            this.alreadyDisconnected = new HashSet();
            this.savedInventories = new HashMap();
            this.playerBossBars = new HashMap();
            this.config = new Properties();
            this.configFile = new File("config/loginsystem.properties");
            this.passwordFile = new File("config/passwords.txt");
            this.playerDataDir = new File("config/loginsystem/playerdata");
            this.plainTextPasswords = new HashMap();
            this.failedLoginAttempts = new HashMap();
            this.tempBans = new HashMap();
            this.mutedPlayers = new HashSet();
            this.lastLogins = new HashMap();
            MinecraftForge.EVENT_BUS.register((Object)this);
            this.loadConfig();
            String defaultLang = this.config.getProperty("defaultLanguage", "en");
            this.languageManager = new LanguageManager(LOGGER, defaultLang);
            LOGGER.info("\u2705 Language system initialized with support for 5 languages (default: " + defaultLang + ")");
            this.enableDatabase = Boolean.parseBoolean(this.config.getProperty("enableDatabase", "false"));
            if (!this.enableDatabase) {
                LOGGER.info("\ud83d\udcbe Database is DISABLED in configuration - using file storage");
            } else {
                LOGGER.info("\ud83d\uddc3\ufe0f Database is ENABLED - embedded JDBC drivers available");
            }
            if (this.enableDatabase) {
                LOGGER.info("Attempting to initialize database connection...");
                try {
                    boolean driverLoaded = false;
                    String originalJdbcUrl = this.jdbcUrl;
                    try {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                        LOGGER.info("\u2705 MySQL JDBC driver loaded successfully (embedded)");
                        this.jdbcUrl = originalJdbcUrl;
                        driverLoaded = true;
                    }
                    catch (ClassNotFoundException e) {
                        LOGGER.warn("\u26a0\ufe0f MySQL JDBC driver not found, trying MariaDB driver...");
                        try {
                            Class.forName("org.mariadb.jdbc.Driver");
                            LOGGER.info("\u2705 MariaDB JDBC driver loaded successfully (embedded)");
                            this.jdbcUrl = this.jdbcUrl.replace("jdbc:mysql://", "jdbc:mariadb://");
                            driverLoaded = true;
                        }
                        catch (ClassNotFoundException ex) {
                            LOGGER.error("\u274c Neither MySQL nor MariaDB JDBC driver found in the classpath!");
                            throw new RuntimeException("No compatible JDBC driver found. Please check your dependencies.", ex);
                        }
                    }
                    if (!driverLoaded) break block18;
                    try (Connection testConn = DriverManager.getConnection(this.jdbcUrl);){
                        if (testConn.isValid(5)) {
                            this.initDatabase();
                            this.loadPasswordsFromDB();
                            LOGGER.info("\u2705 Successfully connected to database: " + this.dbName);
                        }
                    }
                }
                catch (SQLException e) {
                    LOGGER.error("\u274c Failed to connect to MySQL database: " + e.getMessage());
                    LOGGER.error("Please check your database configuration in config/loginsystem.properties");
                    LOGGER.error("Falling back to file storage");
                    this.enableDatabase = false;
                    this.loadPasswordsFromFile();
                }
                catch (Exception e) {
                    LOGGER.error("\u274c Failed to initialize database: " + e.getMessage());
                    LOGGER.error("Falling back to file storage");
                    this.enableDatabase = false;
                    this.loadPasswordsFromFile();
                }
            } else {
                LOGGER.info("Using file-based storage (database disabled in config)");
                this.loadPasswordsFromFile();
            }
        }
    }

    private void loadConfig() {
        File configDir = new File("config");
        if (!configDir.exists()) {
            configDir.mkdir();
        }
        if (!this.configFile.exists()) {
            String configContent = "# \ud83d\ude80 Login System Mod Configuration File \ud83d\ude80\n# This file contains configuration settings for the Login System mod.\n# Adjust the values below according to your server's needs.\n\n# ----------------------------\n# General Settings\n# ----------------------------\n# Maximum time (in seconds) a player can remain in the waiting area before being disconnected.\nloginTimeout=60\n# Default language for new players (en, ar, fr, de, zh)\ndefaultLanguage=en\n\n# ----------------------------\n# Messages\n# ----------------------------\n# Message when registration is successful.\nmessage.registerSuccess=Registration successful! \ud83c\udf89\n# Message when login is successful.\nmessage.loginSuccess=Login successful! \u2705\n# Message for incorrect password.\nmessage.incorrectPassword=Incorrect password! \u274c\n# Message when a player tries to login without registering.\nmessage.notRegistered=You are not registered! Use /register first. \u26a0\ufe0f\n# Message when a player attempts to register again.\nmessage.alreadyRegistered=You are already registered! \u26a0\ufe0f\n# Message when a player is kicked for timeout.\nmessage.kickTimeout=You were kicked for not logging in! \u23f0\n\n# ----------------------------\n# Visual Effects & Inventory Control\n# ----------------------------\n# If true, applies a blindness effect to unlogged players.\napplyBlindness=true\n# Duration (in ticks) for the blindness effect (20 ticks = 1 second).\nblindnessDuration=40\n# If true, the player's inventory will be hidden until they log in.\nhideInventory=true\n\n# ----------------------------\n# Database Settings\n# ----------------------------\n# If true, the mod uses MySQL database to store passwords. If false, a local file is used.\nenableDatabase=false\n# Database host (use 127.0.0.1 instead of localhost)\ndatabase.host=127.0.0.1\n# Database port\ndatabase.port=3306\n# Database name\ndatabase.name=loginsystem\n# Database username\ndatabase.username=root\n# Database password\ndatabase.password=your_password\n# Additional MySQL settings\ndatabase.allowPublicKeyRetrieval=true\ndatabase.useSSL=false\ndatabase.autoReconnect=true\ndatabase.maxReconnects=3\n\n# ----------------------------\n# Waiting Area Settings\n# ----------------------------\n# Coordinates for the waiting area where unlogged players will be teleported.\nwaitingAreaX=0\nwaitingAreaY=100\nwaitingAreaZ=0\n\n# ----------------------------\n# Admin Alert Settings\n# ----------------------------\n# Enable admin alerts for suspicious login attempts\nenableAdminAlerts=true\n# Number of failed login attempts before alerting admins\nmaxFailedAttempts=3\n\n# ----------------------------\n# Recipe Book Fix\n# ----------------------------\n# Re-sends the player's recipes right after they log in, to fix the recipe book\n# appearing empty on servers where the login freeze/teleport desyncs it.\nresyncRecipesOnLogin=true\n# If true, ALL server recipes are unlocked for the player on login (the recipe\n# book will always show every recipe). Leave false to keep normal progression.\nunlockAllRecipesOnLogin=false\n\n# ----------------------------\n# Admin Web Panel Settings\n# ----------------------------\nenableWebServer=true\nwebServerPort=8080\nwebServerPassword=admin\n";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.configFile));){
                writer.write(configContent);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (FileInputStream in = new FileInputStream(this.configFile);){
            this.config.load(in);
            this.enableDatabase = Boolean.parseBoolean(this.config.getProperty("enableDatabase", "false"));
            this.dbHost = this.config.getProperty("database.host", "127.0.0.1");
            this.dbPort = this.config.getProperty("database.port", "3306");
            this.dbName = this.config.getProperty("database.name", "loginsystem");
            this.dbUsername = this.config.getProperty("database.username", "root");
            this.dbPassword = this.config.getProperty("database.password", "");
            this.enableWebServer = Boolean.parseBoolean(this.config.getProperty("enableWebServer", "true"));
            this.webServerPort = Integer.parseInt(this.config.getProperty("webServerPort", "8080"));
            this.webServerPassword = this.config.getProperty("webServerPassword", "admin");
            LOGGER.info("\ud83d\udd0d Configuration loaded from: " + this.configFile.getAbsolutePath());
            LOGGER.info("\ud83d\udd0d enableDatabase = " + this.enableDatabase);
            LOGGER.info("\ud83d\udd0d database.host = " + this.dbHost);
            LOGGER.info("\ud83d\udd0d database.username = " + this.dbUsername);
            LOGGER.info("\ud83d\udd0d database.name = " + this.dbName);
            String encodedUsername = URLEncoder.encode(this.dbUsername, StandardCharsets.UTF_8.toString());
            String encodedPassword = URLEncoder.encode(this.dbPassword, StandardCharsets.UTF_8.toString());
            this.jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s&allowPublicKeyRetrieval=%s&useSSL=%s&autoReconnect=%s&maxReconnects=%s&connectTimeout=5000&socketTimeout=5000", this.dbHost, this.dbPort, this.dbName, encodedUsername, encodedPassword, this.config.getProperty("database.allowPublicKeyRetrieval", "true"), this.config.getProperty("database.useSSL", "false"), this.config.getProperty("database.autoReconnect", "true"), this.config.getProperty("database.maxReconnects", "3"));
            LOGGER.info("Database configuration loaded successfully");
            if (this.enableDatabase) {
                LOGGER.info("Database connection URL: " + this.jdbcUrl.replace(encodedPassword, "******"));
            }
        }
        catch (IOException e) {
            LOGGER.error("Failed to load configuration file", (Throwable)e);
            e.printStackTrace();
        }
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(this.jdbcUrl);){
            try (Statement stmt = conn.createStatement();){
                stmt.execute("CREATE TABLE IF NOT EXISTS player_passwords (\n    uuid VARCHAR(36) PRIMARY KEY,\n    password TEXT NOT NULL,\n    last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                LOGGER.info("Table created or already exists: player_passwords");
                stmt.execute("CREATE TABLE IF NOT EXISTS player_data (\n    uuid VARCHAR(36) PRIMARY KEY,\n    inventory LONGTEXT,\n    position_x DOUBLE,\n    position_y DOUBLE,\n    position_z DOUBLE,\n    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                LOGGER.info("Table created or already exists: player_data");
                try {
                    stmt.execute("ALTER TABLE player_passwords ADD COLUMN IF NOT EXISTS last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                }
                catch (SQLException e) {
                    LOGGER.debug("Column last_login already exists or error adding it: " + e.getMessage());
                }
                stmt.execute("CREATE TABLE IF NOT EXISTS player_last_location (\n    uuid VARCHAR(36) PRIMARY KEY,\n    last_x DOUBLE NOT NULL,\n    last_y DOUBLE NOT NULL,\n    last_z DOUBLE NOT NULL,\n    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                LOGGER.info("Table created or already exists: player_last_location");
            }
            LOGGER.info("Database initialized successfully!");
        }
        catch (SQLException e) {
            LOGGER.error("Database error while initializing!", (Throwable)e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private void loadPasswordsFromDB() {
        int count = 0;
        this.playerPasswords.clear();
        String sql = "SELECT uuid, password FROM player_passwords";
        try (Connection conn = DriverManager.getConnection(this.jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);){
            while (rs.next()) {
                try {
                    String uuidStr = rs.getString("uuid");
                    String password = rs.getString("password");
                    if (uuidStr == null || password == null || password.trim().isEmpty()) continue;
                    UUID uuid = UUID.fromString(uuidStr);
                    this.playerPasswords.put(uuid, password.trim());
                    ++count;
                }
                catch (IllegalArgumentException e) {
                    LOGGER.warn("Skipping invalid UUID in database: " + rs.getString("uuid"));
                }
            }
            LOGGER.info("Successfully loaded " + count + " passwords from database");
            return;
        }
        catch (SQLException e) {
            LOGGER.error("Failed to load passwords from database", (Throwable)e);
            throw new RuntimeException("Failed to load passwords from database", e);
        }
    }

    private void savePasswordToDB(UUID uuid, String password) {
        if (!this.enableDatabase) {
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            LOGGER.warn("Attempted to save empty password for UUID: " + String.valueOf(uuid));
            return;
        }
        String sql = "INSERT INTO player_passwords (uuid, password, last_login)\nVALUES (?, ?, CURRENT_TIMESTAMP)\nON DUPLICATE KEY UPDATE\n    password = VALUES(password),\n    last_login = CURRENT_TIMESTAMP\n";
        try (Connection conn = DriverManager.getConnection(this.jdbcUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);){
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, password.trim());
            int affectedRows = pstmt.executeUpdate();
            LOGGER.debug("Saved password for UUID: {} ({} rows affected)", (Object)uuid, (Object)affectedRows);
        }
        catch (SQLException e) {
            LOGGER.error("Failed to save password for UUID: " + String.valueOf(uuid), (Throwable)e);
            throw new RuntimeException("Failed to save password to database", e);
        }
    }

    private void loadPasswordsFromFile() {
        if (!this.passwordFile.exists()) {
            System.out.println("LoginSystem: No password file found, starting fresh.");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(this.passwordFile));){
            String line;
            this.playerPasswords.clear();
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length != 2) continue;
                this.playerPasswords.put(UUID.fromString(parts[0]), parts[1]);
                ++count;
            }
            System.out.println("LoginSystem: Loaded " + count + " passwords from file.");
        }
        catch (IOException e) {
            System.err.println("LoginSystem: Error reading password file!");
            e.printStackTrace();
        }
    }

    public String getPasswordForPlayer(UUID uuid) {
        return this.plainTextPasswords.containsKey(uuid) ? this.plainTextPasswords.get(uuid) : this.playerPasswords.get(uuid);
    }

    public boolean hasPlayerPassword(UUID uuid) {
        return this.playerPasswords.containsKey(uuid);
    }

    public void removePlayerPassword(UUID uuid) {
        this.playerPasswords.remove(uuid);
        this.plainTextPasswords.remove(uuid);
    }

    public boolean isEnableDatabase() {
        return this.enableDatabase;
    }

    public String getJdbcUrl() {
        return this.jdbcUrl;
    }

    public void savePasswordsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.passwordFile));){
            int count = 0;
            for (UUID uuid : this.playerPasswords.keySet()) {
                writer.write(uuid.toString() + ":" + this.playerPasswords.get(uuid));
                writer.newLine();
                ++count;
            }
            System.out.println("LoginSystem: Saved " + count + " passwords to file.");
        }
        catch (IOException e) {
            System.err.println("LoginSystem: Error writing password file!");
            e.printStackTrace();
        }
    }

    private String getPlayerIP(ServerPlayer player) {
        try {
            int colonIndex;
            String address = player.connection.getRemoteAddress().toString();
            if (address.startsWith("/")) {
                address = address.substring(1);
            }
            if ((colonIndex = address.lastIndexOf(58)) > 0) {
                address = address.substring(0, colonIndex);
            }
            return address;
        }
        catch (Exception e) {
            LOGGER.error("Failed to get player IP", (Throwable)e);
            return "unknown";
        }
    }

    private void recordFailedLoginAttempt(ServerPlayer player) {
        UUID playerId = player.getUUID();
        int attempts = this.failedLoginAttempts.getOrDefault(playerId, 0) + 1;
        this.failedLoginAttempts.put(playerId, attempts);
        int maxAttempts = Integer.parseInt(this.config.getProperty("maxFailedAttempts", "3"));
        if (attempts >= maxAttempts && Boolean.parseBoolean(this.config.getProperty("enableAdminAlerts", "true"))) {
            this.alertAdmins(player, attempts);
        }
    }

    private void resetFailedLoginAttempts(UUID playerId) {
        this.failedLoginAttempts.remove(playerId);
    }

    private void alertAdmins(ServerPlayer suspiciousPlayer, int attempts) {
        MinecraftServer server = suspiciousPlayer.getServer();
        if (server == null) {
            return;
        }
        String playerName = suspiciousPlayer.getName().getString();
        String playerIP = this.getPlayerIP(suspiciousPlayer);
        MutableComponent alertMessage = Component.literal((String)"").append((Component)Component.literal((String)"\u26a0 SECURITY ALERT \u26a0").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD})).append((Component)Component.literal((String)"\n")).append((Component)Component.literal((String)"Player: ").withStyle(ChatFormatting.YELLOW)).append((Component)Component.literal((String)playerName).withStyle(ChatFormatting.WHITE)).append((Component)Component.literal((String)"\n")).append((Component)Component.literal((String)"IP: ").withStyle(ChatFormatting.YELLOW)).append((Component)Component.literal((String)playerIP).withStyle(ChatFormatting.WHITE)).append((Component)Component.literal((String)"\n")).append((Component)Component.literal((String)"Failed attempts: ").withStyle(ChatFormatting.YELLOW)).append((Component)Component.literal((String)String.valueOf(attempts)).withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD})).append((Component)Component.literal((String)"\n")).append((Component)Component.literal((String)"Action: Possible brute-force attack!").withStyle(ChatFormatting.RED));
        for (ServerPlayer admin : server.getPlayerList().getPlayers()) {
            if (!admin.hasPermissions(2)) continue;
            admin.sendSystemMessage((Component)alertMessage);
            this.showActionBar(admin, "\u00a7c\u26a0 " + playerName + " has " + attempts + " failed login attempts!");
        }
        LOGGER.warn("SECURITY ALERT: Player {} (IP: {}) has {} failed login attempts!", (Object)playerName, (Object)playerIP, (Object)attempts);
    }

    private String serializeInventory(ItemStack[] inventory) {
        try {
            CompoundTag root = new CompoundTag();
            ListTag itemList = new ListTag();
            for (int i = 0; i < inventory.length; ++i) {
                ItemStack stack = inventory[i];
                if (stack == null || stack.isEmpty()) continue;
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                stack.save(itemTag);
                itemList.add((Tag)itemTag);
            }
            root.put("Items", (Tag)itemList);
            root.putInt("Size", inventory.length);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            NbtIo.write((CompoundTag)root, (DataOutput)dos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
        catch (Exception e) {
            LOGGER.error("Failed to serialize inventory", (Throwable)e);
            return null;
        }
    }

    private ItemStack[] deserializeInventory(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bais);
            CompoundTag root = NbtIo.read((DataInput)dis);
            int size = root.getInt("Size");
            ItemStack[] inventory = new ItemStack[size];
            for (int i = 0; i < size; ++i) {
                inventory[i] = ItemStack.EMPTY;
            }
            ListTag itemList = root.getList("Items", 10);
            for (int i = 0; i < itemList.size(); ++i) {
                CompoundTag itemTag = itemList.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot < 0 || slot >= size) continue;
                inventory[slot] = ItemStack.of((CompoundTag)itemTag);
            }
            return inventory;
        }
        catch (Exception e) {
            LOGGER.error("Failed to deserialize inventory", (Throwable)e);
            return null;
        }
    }

    private void savePlayerDataPersistent(UUID playerId, ItemStack[] inventory, double[] position) {
        String inventoryData = this.serializeInventory(inventory);
        if (this.enableDatabase) {
            this.savePlayerDataToDB(playerId, inventoryData, position);
        } else {
            this.savePlayerDataToFile(playerId, inventoryData, position);
        }
    }

    private void savePlayerDataToDB(UUID playerId, String inventoryData, double[] position) {
        String sql = "INSERT INTO player_data (uuid, inventory, position_x, position_y, position_z)\nVALUES (?, ?, ?, ?, ?)\nON DUPLICATE KEY UPDATE\n    inventory = VALUES(inventory),\n    position_x = VALUES(position_x),\n    position_y = VALUES(position_y),\n    position_z = VALUES(position_z)\n";
        dbExecutor.execute(() -> {
            try (Connection conn = DriverManager.getConnection(this.jdbcUrl);
                 PreparedStatement pstmt = conn.prepareStatement(sql);){
                pstmt.setString(1, playerId.toString());
                pstmt.setString(2, inventoryData);
                pstmt.setDouble(3, position[0]);
                pstmt.setDouble(4, position[1]);
                pstmt.setDouble(5, position[2]);
                pstmt.executeUpdate();
                LOGGER.info("Saved player data to database for: " + String.valueOf(playerId));
            }
            catch (SQLException e) {
                LOGGER.error("Failed to save player data to database for: " + String.valueOf(playerId), (Throwable)e);
            }
        });
    }

    private void savePlayerDataToFile(UUID playerId, String inventoryData, double[] position) {
        if (!this.playerDataDir.exists()) {
            this.playerDataDir.mkdirs();
        }
        File playerFile = new File(this.playerDataDir, playerId.toString() + ".dat");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(playerFile));){
            writer.write("inventory=" + (inventoryData != null ? inventoryData : ""));
            writer.newLine();
            writer.write("posX=" + position[0]);
            writer.newLine();
            writer.write("posY=" + position[1]);
            writer.newLine();
            writer.write("posZ=" + position[2]);
            writer.newLine();
            LOGGER.info("Saved player data to file for: " + String.valueOf(playerId));
        }
        catch (IOException e) {
            LOGGER.error("Failed to save player data to file for: " + String.valueOf(playerId), (Throwable)e);
        }
    }

    private Object[] loadPlayerDataPersistent(UUID playerId) {
        if (this.enableDatabase) {
            return this.loadPlayerDataFromDB(playerId);
        }
        return this.loadPlayerDataFromFile(playerId);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private Object[] loadPlayerDataFromDB(UUID playerId) {
        String sql = "SELECT inventory, position_x, position_y, position_z FROM player_data WHERE uuid = ?";
        try (Connection conn = DriverManager.getConnection(this.jdbcUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);){
            pstmt.setString(1, playerId.toString());
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) return null;
            String inventoryData = rs.getString("inventory");
            double posX = rs.getDouble("position_x");
            double posY = rs.getDouble("position_y");
            double posZ = rs.getDouble("position_z");
            ItemStack[] inventory = this.deserializeInventory(inventoryData);
            double[] position = new double[]{posX, posY, posZ};
            LOGGER.info("Loaded player data from database for: " + String.valueOf(playerId));
            Object[] objectArray = new Object[]{inventory, position};
            return objectArray;
        }
        catch (SQLException e) {
            LOGGER.error("Failed to load player data from database for: " + String.valueOf(playerId), (Throwable)e);
        }
        return null;
    }

    private Object[] loadPlayerDataFromFile(UUID playerId) {
        File playerFile = new File(this.playerDataDir, playerId.toString() + ".dat");
        if (!playerFile.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(playerFile));){
            String line;
            String inventoryData = null;
            double posX = 0.0;
            double posY = 0.0;
            double posZ = 0.0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("inventory=")) {
                    inventoryData = line.substring("inventory=".length());
                } else if (line.startsWith("posX=")) {
                    posX = Double.parseDouble(line.substring("posX=".length()));
                } else if (line.startsWith("posY=")) {
                    posY = Double.parseDouble(line.substring("posY=".length()));
                } else if (line.startsWith("posZ=")) {
                    posZ = Double.parseDouble(line.substring("posZ=".length()));
                }
            }
            ItemStack[] inventory = this.deserializeInventory(inventoryData);
            double[] position = new double[]{posX, posY, posZ};
            LOGGER.info("Loaded player data from file for: " + String.valueOf(playerId));
            return new Object[]{inventory, position};
        }
        catch (Exception e) {
            LOGGER.error("Failed to load player data from file for: " + String.valueOf(playerId), (Throwable)e);
            return null;
        }
    }

    private void deletePlayerDataPersistent(UUID playerId) {
        if (this.enableDatabase) {
            this.deletePlayerDataFromDB(playerId);
        } else {
            this.deletePlayerDataFromFile(playerId);
        }
    }

    private void deletePlayerDataFromDB(UUID playerId) {
        String sql = "DELETE FROM player_data WHERE uuid = ?";
        dbExecutor.execute(() -> {
            try (Connection conn = DriverManager.getConnection(this.jdbcUrl);
                 PreparedStatement pstmt = conn.prepareStatement(sql);){
                pstmt.setString(1, playerId.toString());
                pstmt.executeUpdate();
                LOGGER.debug("Deleted player data from database for: " + String.valueOf(playerId));
            }
            catch (SQLException e) {
                LOGGER.error("Failed to delete player data from database for: " + String.valueOf(playerId), (Throwable)e);
            }
        });
    }

    private void deletePlayerDataFromFile(UUID playerId) {
        File playerFile = new File(this.playerDataDir, playerId.toString() + ".dat");
        if (playerFile.exists()) {
            if (playerFile.delete()) {
                LOGGER.debug("Deleted player data file for: " + String.valueOf(playerId));
            } else {
                LOGGER.warn("Failed to delete player data file for: " + String.valueOf(playerId));
            }
        }
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    private String hashPasswordLegacy(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found!", e);
        }
    }

    private void restoreInventory(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (this.savedInventories.containsKey(playerId)) {
            ItemStack[] items = this.savedInventories.get(playerId);
            for (int i = 0; i < items.length; ++i) {
                if (items[i] != null) {
                    player.getInventory().setItem(i, items[i]);
                    continue;
                }
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
            this.savedInventories.remove(playerId);
            player.getInventory().setChanged();
            this.deletePlayerDataPersistent(playerId);
            LOGGER.info("Player inventory restored and persistent data cleaned up for: " + String.valueOf(playerId));
        }
    }

    private void removeBlindness(ServerPlayer player) {
        player.removeEffect(MobEffects.BLINDNESS);
    }

    /**
     * Re-sincroniza las recetas del jugador justo despues de loguearse.
     *
     * El libro de recetas se sincroniza durante la secuencia de entrada al
     * servidor. Como este mod congela / teletransporta / limpia al jugador en
     * ese mismo momento, en algunas sesiones el cliente se queda sin la lista
     * de recetas (el boton del libro aparece, pero esta vacio). Volver a enviar
     * el registro de recetas y el libro de recetas tras el login arregla ese
     * desincronizado de forma segura.
     *
     * Opcionalmente, si 'unlockAllRecipesOnLogin=true' en la config, desbloquea
     * todas las recetas del servidor para el jugador (como hacen los packs que
     * muestran todas las recetas desde el principio).
     */
    private void resyncRecipes(ServerPlayer player) {
        if (!Boolean.parseBoolean(this.config.getProperty("resyncRecipesOnLogin", "true"))) {
            return;
        }
        try {
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            Collection<Recipe<?>> recipes = server.getRecipeManager().getRecipes();
            // Reenvia el registro completo de recetas al cliente.
            player.connection.send((Packet)new ClientboundUpdateRecipesPacket(recipes));
            // Opcional: desbloquear todas las recetas para este jugador.
            if (Boolean.parseBoolean(this.config.getProperty("unlockAllRecipesOnLogin", "false"))) {
                player.awardRecipes(recipes);
            }
            // Reenvia el libro de recetas del jugador (recetas desbloqueadas + ajustes).
            player.getRecipeBook().sendInitialRecipeBook(player);
            LOGGER.info("Recipes re-synced for player: " + String.valueOf(player.getUUID()));
        }
        catch (Throwable t) {
            LOGGER.warn("Could not re-sync recipes for player " + String.valueOf(player.getUUID()) + ": " + t.getMessage());
        }
    }

    private void showTitle(ServerPlayer player, String title, String subtitle) {
        player.connection.send((Packet)new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send((Packet)new ClientboundSetTitleTextPacket((Component)Component.literal((String)title)));
        if (subtitle != null && !subtitle.isEmpty()) {
            player.connection.send((Packet)new ClientboundSetSubtitleTextPacket((Component)Component.literal((String)subtitle)));
        }
    }

    private void showActionBar(ServerPlayer player, String message) {
        player.connection.send((Packet)new ClientboundSetActionBarTextPacket((Component)Component.literal((String)message)));
    }

    private void createLoginBossBar(ServerPlayer player, int timeoutSeconds) {
        UUID playerId = player.getUUID();
        this.removeBossBar(player);
        String bossBarTitle = this.languageManager.getMessage(playerId, "timeout.bossbar", new Object[0]);
        ServerBossEvent bossBar = new ServerBossEvent((Component)Component.literal((String)bossBarTitle), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0f);
        this.playerBossBars.put(playerId, bossBar);
        Thread bossBarThread = new Thread(() -> {
            MinecraftServer server;
            for (int i = timeoutSeconds; i > 0; --i) {
                try {
                    Thread.sleep(1000L);
                    float progress = (float)i / (float)timeoutSeconds;
                    bossBar.setProgress(progress);
                    int remaining = i;
                    MinecraftServer serverInstance = player.getServer();
                    if (serverInstance != null) {
                        serverInstance.execute(() -> {
                            String title = this.languageManager.getMessage(playerId, "timeout.bossbar", new Object[0]) + " - " + remaining + "s";
                            bossBar.setName((Component)Component.literal((String)title));
                        });
                    }
                    if (!this.loggedIn.getOrDefault(playerId, false).booleanValue()) continue;
                    MinecraftServer server2 = player.getServer();
                    if (server2 != null) {
                        server2.execute(() -> this.removeBossBar(player));
                    }
                    return;
                }
                catch (InterruptedException e) {
                    return;
                }
            }
            if (!this.loggedIn.getOrDefault(playerId, false).booleanValue() && (server = player.getServer()) != null) {
                server.execute(() -> {
                    String msg = this.config.getProperty("message.kickTimeout", "You were kicked for not logging in! \u23f0");
                    player.connection.disconnect((Component)Component.literal((String)msg).withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}));
                });
            }
        });
        bossBarThread.setDaemon(true);
        bossBarThread.start();
    }

    private void removeBossBar(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (this.playerBossBars.containsKey(playerId)) {
            ServerBossEvent bossBar = this.playerBossBars.get(playerId);
            bossBar.removePlayer(player);
            this.playerBossBars.remove(playerId);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register((LiteralArgumentBuilder)Commands.literal((String)"register").then(Commands.argument((String)"password", (ArgumentType)StringArgumentType.string()).then(Commands.argument((String)"confirmPassword", (ArgumentType)StringArgumentType.string()).executes(context -> {
            ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayerOrException();
            UUID playerId = player.getUUID();
            String password = StringArgumentType.getString((CommandContext)context, (String)"password");
            String confirmPassword = StringArgumentType.getString((CommandContext)context, (String)"confirmPassword");
            if (this.playerPasswords.containsKey(playerId)) {
                String msg = this.languageManager.getMessage(playerId, "register.already", new Object[0]);
                player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.RED));
                this.showActionBar(player, msg);
                return 0;
            }
            if (!password.equals(confirmPassword)) {
                String msg = this.languageManager.getMessage(playerId, "register.password.mismatch", new Object[0]);
                player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.RED));
                this.showActionBar(player, msg);
                return 0;
            }
            String hashedPassword = this.hashPassword(password);
            this.playerPasswords.put(playerId, hashedPassword);
            this.plainTextPasswords.put(playerId, password);
            if (this.enableDatabase) {
                this.savePasswordToDB(playerId, hashedPassword);
            } else {
                this.savePasswordsToFile();
            }
            this.loggedIn.put(playerId, true);
            player.setNoGravity(false);
            this.restoreInventory(player);
            this.removeBlindness(player);
            this.removeBossBar(player);
            double[] targetPos = null;
            double[] lastLoc = this.loadLastLocation(playerId);
            if (lastLoc != null) {
                targetPos = lastLoc;
                LOGGER.info("Teleported player {} to last location: X={}, Y={}, Z={}", (Object)playerId, (Object)lastLoc[0], (Object)lastLoc[1], (Object)lastLoc[2]);
                this.deleteLastLocation(playerId);
            } else if (this.originalPositions.containsKey(playerId)) {
                targetPos = this.originalPositions.get(playerId);
                this.originalPositions.remove(playerId);
            }
            if (targetPos != null) {
                player.teleportTo((ServerLevel)player.getCommandSenderWorld(), targetPos[0], targetPos[1], targetPos[2], player.getYRot(), player.getXRot());
            }
            this.resyncRecipes(player);
            String successMsg = this.languageManager.getMessage(playerId, "register.success", new Object[0]);
            String title = this.languageManager.getMessage(playerId, "register.title", new Object[0]);
            String subtitle = this.languageManager.getMessage(playerId, "register.subtitle", new Object[0]);
            player.sendSystemMessage((Component)Component.literal((String)successMsg).withStyle(ChatFormatting.GREEN));
            this.showTitle(player, title, subtitle);
            this.showActionBar(player, successMsg);
            return 1;
        }))));
        event.getDispatcher().register((LiteralArgumentBuilder)Commands.literal((String)"login").then(Commands.argument((String)"password", (ArgumentType)StringArgumentType.string()).executes(context -> {
            ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayerOrException();
            UUID playerId = player.getUUID();
            if (this.loggedIn.getOrDefault(playerId, false).booleanValue()) {
                String msg = this.languageManager.getMessage(playerId, "login.already", new Object[0]);
                player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.RED));
                this.showActionBar(player, msg);
                return 0;
            }
            if (!this.playerPasswords.containsKey(playerId)) {
                String msg = this.languageManager.getMessage(playerId, "login.notRegistered", new Object[0]);
                player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.RED));
                this.showActionBar(player, msg);
                return 0;
            }
            String password = StringArgumentType.getString((CommandContext)context, (String)"password");
            String storedHash = this.playerPasswords.get(playerId);
            boolean isPasswordCorrect = false;
            boolean needsUpgrade = false;
            if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
                isPasswordCorrect = BCrypt.checkpw(password, storedHash);
            } else {
                String legacyHash = this.hashPasswordLegacy(password);
                if (storedHash.equals(legacyHash)) {
                    isPasswordCorrect = true;
                    needsUpgrade = true;
                }
            }
            if (!isPasswordCorrect) {
                this.recordFailedLoginAttempt(player);
                int attempts = this.failedLoginAttempts.getOrDefault(playerId, 0);
                String msg = this.languageManager.getMessage(playerId, "login.incorrect", new Object[0]) + " \u00a77(" + attempts + "/3)";
                player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.RED));
                this.showActionBar(player, msg);
                return 0;
            }
            if (needsUpgrade) {
                String newBcryptHash = this.hashPassword(password);
                this.playerPasswords.put(playerId, newBcryptHash);
                if (this.enableDatabase) {
                    this.savePasswordToDB(playerId, newBcryptHash);
                } else {
                    this.savePasswordsToFile();
                }
                LOGGER.info("Upgraded password to BCrypt for player " + player.getName().getString());
            }
            if (!this.plainTextPasswords.containsKey(playerId)) {
                this.plainTextPasswords.put(playerId, password);
            }
            this.resetFailedLoginAttempts(playerId);
            this.loggedIn.put(playerId, true);
            player.setNoGravity(false);
            this.restoreInventory(player);
            this.removeBlindness(player);
            this.removeBossBar(player);
            double[] targetPos = null;
            double[] lastLoc = this.loadLastLocation(playerId);
            if (lastLoc != null) {
                targetPos = lastLoc;
                LOGGER.info("Teleported player {} to last location: X={}, Y={}, Z={}", (Object)playerId, (Object)lastLoc[0], (Object)lastLoc[1], (Object)lastLoc[2]);
                this.deleteLastLocation(playerId);
            } else if (this.originalPositions.containsKey(playerId)) {
                targetPos = this.originalPositions.get(playerId);
                this.originalPositions.remove(playerId);
            }
            if (targetPos != null) {
                player.teleportTo((ServerLevel)player.getCommandSenderWorld(), targetPos[0], targetPos[1], targetPos[2], player.getYRot(), player.getXRot());
            }
            this.resyncRecipes(player);
            String successMsg = this.languageManager.getMessage(playerId, "login.success", new Object[0]);
            String title = this.languageManager.getMessage(playerId, "login.title", new Object[0]);
            String subtitle = this.languageManager.getMessage(playerId, "login.subtitle", new Object[0]);
            player.sendSystemMessage((Component)Component.literal((String)successMsg).withStyle(ChatFormatting.GREEN));
            this.showTitle(player, title, subtitle);
            this.showActionBar(player, successMsg);
            return 1;
        })));
        event.getDispatcher().register((LiteralArgumentBuilder)Commands.literal((String)"changepassword").then(Commands.argument((String)"oldPassword", (ArgumentType)StringArgumentType.string()).then(Commands.argument((String)"newPassword", (ArgumentType)StringArgumentType.string()).executes(context -> {
            ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayerOrException();
            UUID playerId = player.getUUID();
            String oldPassword = StringArgumentType.getString((CommandContext)context, (String)"oldPassword");
            String newPassword = StringArgumentType.getString((CommandContext)context, (String)"newPassword");
            if (!this.loggedIn.getOrDefault(playerId, false).booleanValue()) {
                String msg = this.languageManager.getMessage(playerId, "password.mustLogin", new Object[0]);
                player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.RED));
                this.showActionBar(player, msg);
                return 0;
            }
            String storedHash = this.playerPasswords.get(playerId);
            boolean isPasswordCorrect = false;
            if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
                isPasswordCorrect = BCrypt.checkpw(oldPassword, storedHash);
            } else {
                String legacyHash = this.hashPasswordLegacy(oldPassword);
                if (storedHash.equals(legacyHash)) {
                    isPasswordCorrect = true;
                }
            }
            if (!isPasswordCorrect) {
                String msg = this.languageManager.getMessage(playerId, "password.oldIncorrect", new Object[0]);
                player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.RED));
                this.showActionBar(player, msg);
                return 0;
            }
            String hashedNew = this.hashPassword(newPassword);
            this.playerPasswords.put(playerId, hashedNew);
            this.plainTextPasswords.put(playerId, newPassword);
            if (this.enableDatabase) {
                try {
                    try (Connection conn = DriverManager.getConnection(this.jdbcUrl);){
                        String sql = "UPDATE player_passwords SET password = ? WHERE uuid = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(sql);){
                            pstmt.setString(1, hashedNew);
                            pstmt.setString(2, playerId.toString());
                            pstmt.executeUpdate();
                        }
                    }
                    LOGGER.info("Password updated in database for player: " + String.valueOf(playerId));
                }
                catch (SQLException e) {
                    LOGGER.error("Failed to update password in database for player: " + String.valueOf(playerId), (Throwable)e);
                    player.sendSystemMessage((Component)Component.literal((String)"Failed to update password in database. Please contact an administrator.").withStyle(ChatFormatting.RED));
                    return 0;
                }
            }
            this.savePasswordsToFile();
            String msg = this.languageManager.getMessage(playerId, "password.changed", new Object[0]);
            player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.GREEN));
            this.showActionBar(player, msg);
            return 1;
        }))));
        event.getDispatcher().register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"loadmin").requires(source -> source.hasPermission(2))).executes(context -> {
            ServerPlayer admin = ((CommandSourceStack)context.getSource()).getPlayerOrException();
            this.openAdminGUI(admin);
            return 1;
        }));
    }

    public void openAdminGUI(ServerPlayer admin) {
        SimpleContainer container = new SimpleContainer(27);
        ItemStack book = new ItemStack((ItemLike)Items.WRITABLE_BOOK);
        CompoundTag bookTag = book.getOrCreateTag();
        bookTag.putString("CustomName", "{\"text\":\"\u00a76\u00a7lInfo\",\"bold\":true}");
        ListTag bookLore = new ListTag();
        bookLore.add((Tag)StringTag.valueOf((String)"{\"text\":\"\u00a77Click to view all players\"}"));
        bookLore.add((Tag)StringTag.valueOf((String)"{\"text\":\"\u00a77and their passwords\"}"));
        CompoundTag bookDisplay = new CompoundTag();
        bookDisplay.put("Lore", (Tag)bookLore);
        bookTag.put("display", (Tag)bookDisplay);
        bookTag.putString("GUIAction", "ViewPlayers");
        book.setTag(bookTag);
        container.setItem(13, book);
        ItemStack barrier = new ItemStack((ItemLike)Items.BARRIER);
        CompoundTag barrierTag = barrier.getOrCreateTag();
        barrierTag.putString("CustomName", "{\"text\":\"\u00a7c\u00a7lDelete Player\",\"bold\":true}");
        ListTag barrierLore = new ListTag();
        barrierLore.add((Tag)StringTag.valueOf((String)"{\"text\":\"\u00a77Click to view players\"}"));
        barrierLore.add((Tag)StringTag.valueOf((String)"{\"text\":\"\u00a77and delete accounts\"}"));
        CompoundTag barrierDisplay = new CompoundTag();
        barrierDisplay.put("Lore", (Tag)barrierLore);
        barrierTag.put("display", (Tag)barrierDisplay);
        barrierTag.putString("GUIAction", "DeletePlayers");
        barrier.setTag(barrierTag);
        container.setItem(11, barrier);
        admin.openMenu((MenuProvider)new SimpleMenuProvider((id, playerInventory, player) -> new AdminGUIMenu(id, playerInventory, (Container)container, this), (Component)Component.literal((String)"\u00a76\u00a7lAdmin Panel - Login System")));
    }

    public void openPlayersListGUI(ServerPlayer admin) {
        SimpleContainer container = new SimpleContainer(54);
        int slot = 0;
        for (UUID uuid : this.playerPasswords.keySet()) {
            if (slot >= 54) break;
            String playerName = this.getPlayerName(admin.getServer(), uuid);
            String password = this.plainTextPasswords.containsKey(uuid) ? this.plainTextPasswords.get(uuid) : this.playerPasswords.get(uuid);
            ItemStack playerHead = new ItemStack((ItemLike)Items.PLAYER_HEAD);
            CompoundTag headTag = playerHead.getOrCreateTag();
            headTag.putString("CustomName", "{\"text\":\"\u00a7e" + playerName + "\",\"bold\":true}");
            ListTag lore = new ListTag();
            lore.add((Tag)StringTag.valueOf((String)("{\"text\":\"\u00a77UUID: \u00a7f" + uuid.toString() + "\"}")));
            lore.add((Tag)StringTag.valueOf((String)"{\"text\":\"\"}"));
            lore.add((Tag)StringTag.valueOf((String)"{\"text\":\"\u00a76Password: \u00a7a[HIDDEN]\"}"));
            CompoundTag display = new CompoundTag();
            display.put("Lore", (Tag)lore);
            headTag.put("display", (Tag)display);
            CompoundTag skullOwner = new CompoundTag();
            skullOwner.putString("Name", playerName);
            skullOwner.putString("Id", uuid.toString());
            headTag.put("SkullOwner", (Tag)skullOwner);
            headTag.putString("PlayerUUID", uuid.toString());
            playerHead.setTag(headTag);
            container.setItem(slot, playerHead);
            ++slot;
        }
        admin.openMenu((MenuProvider)new SimpleMenuProvider((id, playerInventory, player) -> new AdminGUIMenu(id, playerInventory, (Container)container, this), (Component)Component.literal((String)"\u00a76\u00a7lPlayers List - View Only")));
    }

    public void openDeletePlayersGUI(ServerPlayer admin) {
        SimpleContainer container = new SimpleContainer(54);
        int slot = 0;
        for (UUID uuid : this.playerPasswords.keySet()) {
            if (slot >= 54) break;
            String playerName = this.getPlayerName(admin.getServer(), uuid);
            String password = this.plainTextPasswords.containsKey(uuid) ? this.plainTextPasswords.get(uuid) : this.playerPasswords.get(uuid);
            ItemStack playerHead = new ItemStack((ItemLike)Items.PLAYER_HEAD);
            CompoundTag headTag = playerHead.getOrCreateTag();
            headTag.putString("CustomName", "{\"text\":\"\u00a7c" + playerName + "\",\"bold\":true}");
            ListTag lore = new ListTag();
            lore.add((Tag)StringTag.valueOf((String)("{\"text\":\"\u00a77UUID: \u00a7f" + uuid.toString() + "\"}")));
            lore.add((Tag)StringTag.valueOf((String)"{\"text\":\"\"}"));
            lore.add((Tag)StringTag.valueOf((String)"{\"text\":\"\u00a76Password: \u00a7a[HIDDEN]\"}"));
            lore.add((Tag)StringTag.valueOf((String)"{\"text\":\"\"}"));
            lore.add((Tag)StringTag.valueOf((String)"{\"text\":\"\u00a7c\u00a7lClick to DELETE this player\"}"));
            CompoundTag display = new CompoundTag();
            display.put("Lore", (Tag)lore);
            headTag.put("display", (Tag)display);
            CompoundTag skullOwner = new CompoundTag();
            skullOwner.putString("Name", playerName);
            skullOwner.putString("Id", uuid.toString());
            headTag.put("SkullOwner", (Tag)skullOwner);
            headTag.putString("PlayerUUID", uuid.toString());
            headTag.putString("GUIAction", "DeleteThisPlayer");
            playerHead.setTag(headTag);
            container.setItem(slot, playerHead);
            ++slot;
        }
        admin.openMenu((MenuProvider)new SimpleMenuProvider((id, playerInventory, player) -> new AdminGUIMenu(id, playerInventory, (Container)container, this), (Component)Component.literal((String)"\u00a7c\u00a7lDelete Players - Click to Remove")));
    }

    public String getPlayerName(MinecraftServer server, UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player.getName().getString();
        }
        try {
            GameProfile profile;
            GameProfileCache profileCache = server.getProfileCache();
            if (profileCache != null && (profile = (GameProfile)profileCache.get(uuid).orElse(null)) != null) {
                return profile.getName();
            }
        }
        catch (Exception e) {
            LOGGER.warn("Failed to get player name for UUID: " + String.valueOf(uuid));
        }
        return uuid.toString().substring(0, 8) + "...";
    }

    public void kickPlayer(UUID uuid, String reason) {
        ServerPlayer player;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && (player = server.getPlayerList().getPlayer(uuid)) != null) {
            server.execute(() -> player.connection.disconnect((Component)Component.literal((String)reason)));
        }
    }

    public void banPlayer(UUID uuid, String reason, int durationDays) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                ServerPlayer player;
                String playerName = this.getPlayerName(server, uuid);
                long expires = durationDays > 0 ? System.currentTimeMillis() + (long)durationDays * 86400000L : Long.MAX_VALUE;
                this.tempBans.put(uuid, expires);
                if (playerName != null && !playerName.equals("Unknown")) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "ban " + playerName + " " + reason);
                }
                if ((player = server.getPlayerList().getPlayer(uuid)) != null) {
                    String msg = "You are banned: " + reason;
                    if (durationDays > 0) {
                        msg = msg + " for " + durationDays + " days.";
                    }
                    player.connection.disconnect((Component)Component.literal((String)msg));
                }
            });
        }
    }

    public void unbanPlayer(UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                String playerName = this.getPlayerName(server, uuid);
                if (playerName != null && !playerName.equals("Unknown")) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "pardon " + playerName);
                } else {
                    try {
                        GameProfile profile = new GameProfile(uuid, null);
                        server.getPlayerList().getBans().remove((GameProfile)profile);
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
                this.tempBans.remove(uuid);
            });
        }
    }

    public boolean isBanned(UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (this.tempBans.containsKey(uuid)) {
            String name;
            if (this.tempBans.get(uuid) > System.currentTimeMillis()) {
                return true;
            }
            this.tempBans.remove(uuid);
            if (server != null && (name = this.getPlayerName(server, uuid)) != null && !name.equals("Unknown")) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "pardon " + name);
            }
        }
        if (server != null) {
            try {
                String playerName = this.getPlayerName(server, uuid);
                GameProfile profile = new GameProfile(uuid, playerName != null && !playerName.equals("Unknown") ? playerName : null);
                return server.getPlayerList().getBans().isBanned(profile);
            }
            catch (Throwable t) {
                return false;
            }
        }
        return false;
    }

    public boolean isMuted(UUID uuid) {
        return this.mutedPlayers.contains(uuid);
    }

    public void mutePlayer(UUID uuid) {
        this.mutedPlayers.add(uuid);
    }

    public void unmutePlayer(UUID uuid) {
        this.mutedPlayers.remove(uuid);
    }

    public void forceChangePassword(UUID uuid, String newPassword) {
        String hashedPassword = this.hashPassword(newPassword);
        this.playerPasswords.put(uuid, hashedPassword);
        if (this.enableDatabase) {
            this.savePasswordToDB(uuid, hashedPassword);
        } else {
            this.savePasswordsToFile();
        }
    }

    public JsonArray getInventoryData(UUID uuid) {
        JsonArray invArray;
        block14: {
            block13: {
                ServerPlayer player;
                invArray = new JsonArray();
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null && (player = server.getPlayerList().getPlayer(uuid)) != null) {
                    Inventory inv = player.getInventory();
                    for (int i = 0; i < inv.getContainerSize(); ++i) {
                        ItemStack stack = inv.getItem(i);
                        if (stack.isEmpty()) continue;
                        JsonObject itemObj = new JsonObject();
                        itemObj.addProperty("slot", (Number)i);
                        String itemId = stack.getItem().getDescriptionId();
                        if (itemId.startsWith("block.")) {
                            itemId = itemId.substring(6);
                        } else if (itemId.startsWith("item.")) {
                            itemId = itemId.substring(5);
                        }
                        itemId = itemId.replace(".", ":");
                        itemObj.addProperty("id", itemId);
                        itemObj.addProperty("count", (Number)stack.getCount());
                        itemObj.addProperty("name", stack.getHoverName().getString());
                        invArray.add((JsonElement)itemObj);
                    }
                    return invArray;
                }
                if (!this.savedInventories.containsKey(uuid)) break block13;
                ItemStack[] saved = this.savedInventories.get(uuid);
                for (int i = 0; i < saved.length; ++i) {
                    ItemStack stack = saved[i];
                    if (stack == null || stack.isEmpty()) continue;
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("slot", (Number)i);
                    String itemId = stack.getItem().getDescriptionId();
                    if (itemId.startsWith("block.")) {
                        itemId = itemId.substring(6);
                    } else if (itemId.startsWith("item.")) {
                        itemId = itemId.substring(5);
                    }
                    itemId = itemId.replace(".", ":");
                    itemObj.addProperty("id", itemId);
                    itemObj.addProperty("count", (Number)stack.getCount());
                    itemObj.addProperty("name", stack.getHoverName().getString());
                    invArray.add((JsonElement)itemObj);
                }
                break block14;
            }
            Object[] persistentData = this.loadPlayerDataPersistent(uuid);
            if (persistentData == null || persistentData[0] == null) break block14;
            ItemStack[] saved = (ItemStack[])persistentData[0];
            for (int i = 0; i < saved.length; ++i) {
                ItemStack stack = saved[i];
                if (stack == null || stack.isEmpty()) continue;
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("slot", (Number)i);
                String itemId = stack.getItem().getDescriptionId();
                if (itemId.startsWith("block.")) {
                    itemId = itemId.substring(6);
                } else if (itemId.startsWith("item.")) {
                    itemId = itemId.substring(5);
                }
                itemId = itemId.replace(".", ":");
                itemObj.addProperty("id", itemId);
                itemObj.addProperty("count", (Number)stack.getCount());
                itemObj.addProperty("name", stack.getHoverName().getString());
                invArray.add((JsonElement)itemObj);
            }
        }
        return invArray;
    }

    public void broadcastMessage(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage((Component)Component.literal((String)message), false);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public boolean deletePlayerViaWeb(UUID uuid) {
        try {
            if (!this.isEnableDatabase()) {
                if (!this.hasPlayerPassword(uuid)) return false;
                this.removePlayerPassword(uuid);
                this.savePasswordsToFile();
                return true;
            }
            try (Connection conn = DriverManager.getConnection(this.jdbcUrl);){
                String sql = "DELETE FROM player_passwords WHERE uuid = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql);){
                    pstmt.setString(1, uuid.toString());
                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected <= 0) return false;
                    this.removePlayerPassword(uuid);
                    boolean bl = true;
                    return bl;
                }
            }
        }
        catch (SQLException e) {
            LOGGER.error("Failed to delete password via Web Panel for UUID: " + String.valueOf(uuid), (Throwable)e);
        }
        return false;
    }

    public Map<UUID, String> getPlayerPasswords() {
        return this.playerPasswords;
    }

    public Long getLastLogin(UUID uuid) {
        return this.lastLogins.get(uuid);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        boolean hasPersistentData;
        ServerPlayer newPlayer = (ServerPlayer)event.getEntity();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        UUID newPlayerUUID = newPlayer.getUUID();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == newPlayer || !player.getUUID().equals(newPlayerUUID)) continue;
            if (!this.alreadyDisconnected.contains(newPlayerUUID)) {
                newPlayer.connection.disconnect((Component)Component.literal((String)"A player with that name is already online.").withStyle(ChatFormatting.RED));
                this.alreadyDisconnected.add(newPlayerUUID);
            }
            return;
        }
        Object[] persistentData = this.loadPlayerDataPersistent(newPlayerUUID);
        boolean bl = hasPersistentData = persistentData != null;
        if (hasPersistentData) {
            ItemStack[] savedInv = (ItemStack[])persistentData[0];
            double[] savedPos = (double[])persistentData[1];
            if (savedInv != null) {
                this.savedInventories.put(newPlayerUUID, savedInv);
            }
            if (savedPos != null) {
                this.originalPositions.put(newPlayerUUID, savedPos);
            }
            LOGGER.info("Restored persistent data for player: " + String.valueOf(newPlayerUUID));
        } else {
            double[] currentPos = new double[]{newPlayer.getX(), newPlayer.getY(), newPlayer.getZ()};
            this.originalPositions.put(newPlayerUUID, currentPos);
            if (Boolean.parseBoolean(this.config.getProperty("hideInventory", "true"))) {
                int containerSize = newPlayer.getInventory().getContainerSize();
                ItemStack[] savedItems = new ItemStack[containerSize];
                for (int i = 0; i < containerSize; ++i) {
                    ItemStack item = newPlayer.getInventory().getItem(i);
                    savedItems[i] = item != null && !item.isEmpty() ? item.copy() : ItemStack.EMPTY;
                }
                this.savedInventories.put(newPlayerUUID, savedItems);
                this.savePlayerDataPersistent(newPlayerUUID, savedItems, currentPos);
            } else {
                this.savePlayerDataPersistent(newPlayerUUID, new ItemStack[0], currentPos);
            }
        }
        double waitingX = Double.parseDouble(this.config.getProperty("waitingAreaX", "0"));
        double waitingY = Double.parseDouble(this.config.getProperty("waitingAreaY", "100"));
        double waitingZ = Double.parseDouble(this.config.getProperty("waitingAreaZ", "0"));
        newPlayer.teleportTo((ServerLevel)newPlayer.getCommandSenderWorld(), waitingX, waitingY, waitingZ, newPlayer.getYRot(), newPlayer.getXRot());
        this.loggedIn.put(newPlayerUUID, false);
        this.lastLogins.put(newPlayerUUID, System.currentTimeMillis());
        if (Boolean.parseBoolean(this.config.getProperty("hideInventory", "true"))) {
            newPlayer.getInventory().clearContent();
        }
        String promptMsg = this.languageManager.getMessage(newPlayerUUID, "login.prompt", new Object[0]);
        String promptSubtitle = this.languageManager.getMessage(newPlayerUUID, "login.promptSubtitle", new Object[0]);
        newPlayer.sendSystemMessage((Component)Component.literal((String)promptMsg).withStyle(ChatFormatting.YELLOW));
        newPlayer.sendSystemMessage((Component)Component.literal((String)promptSubtitle).withStyle(ChatFormatting.GRAY));
        this.showTitle(newPlayer, promptMsg, promptSubtitle);
        int timeout = Integer.parseInt(this.config.getProperty("loginTimeout", "60"));
        this.createLoginBossBar(newPlayer, timeout);
        if (Boolean.parseBoolean(this.config.getProperty("applyBlindness", "true"))) {
            int duration = Integer.parseInt(this.config.getProperty("blindnessDuration", "40"));
            newPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, false));
        }
        int timeoutMillis = timeout * 1000;
        Thread timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(timeoutMillis);
                if (!this.loggedIn.getOrDefault(newPlayerUUID, false).booleanValue()) {
                    server.execute(() -> {
                        if (!this.alreadyDisconnected.contains(newPlayerUUID)) {
                            String kickMsg = this.languageManager.getMessage(newPlayerUUID, "timeout.kick", new Object[0]);
                            newPlayer.connection.disconnect((Component)Component.literal((String)kickMsg).withStyle(ChatFormatting.RED));
                            this.alreadyDisconnected.add(newPlayerUUID);
                        }
                    });
                }
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        });
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerPlayer player;
        UUID playerId = event.getEntity().getUUID();
        Player player2 = event.getEntity();
        if (player2 instanceof ServerPlayer) {
            player = (ServerPlayer)player2;
            this.removeBossBar(player);
        }
        if (this.loggedIn.getOrDefault(playerId, false).booleanValue()) {
            player2 = event.getEntity();
            if (player2 instanceof ServerPlayer) {
                player = (ServerPlayer)player2;
                double[] lastPos = new double[]{player.getX(), player.getY(), player.getZ()};
                this.saveLastLocation(playerId, lastPos);
                LOGGER.info("Saved last location for player {}: X={}, Y={}, Z={}", (Object)playerId, (Object)lastPos[0], (Object)lastPos[1], (Object)lastPos[2]);
            }
            this.originalPositions.remove(playerId);
        }
        this.loggedIn.remove(playerId);
        this.alreadyDisconnected.remove(playerId);
        this.savedInventories.remove(playerId);
        this.languageManager.removePlayer(playerId);
    }

    private void saveLastLocation(UUID playerId, double[] position) {
        if (this.enableDatabase) {
            this.saveLastLocationToDB(playerId, position);
        } else {
            this.saveLastLocationToFile(playerId, position);
        }
    }

    private void saveLastLocationToDB(UUID playerId, double[] position) {
        String sql = "INSERT INTO player_last_location (uuid, last_x, last_y, last_z, saved_at)\nVALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)\nON DUPLICATE KEY UPDATE\n    last_x = VALUES(last_x),\n    last_y = VALUES(last_y),\n    last_z = VALUES(last_z),\n    saved_at = CURRENT_TIMESTAMP\n";
        dbExecutor.execute(() -> {
            try (Connection conn = DriverManager.getConnection(this.jdbcUrl);
                 PreparedStatement pstmt = conn.prepareStatement(sql);){
                pstmt.setString(1, playerId.toString());
                pstmt.setDouble(2, position[0]);
                pstmt.setDouble(3, position[1]);
                pstmt.setDouble(4, position[2]);
                pstmt.executeUpdate();
                LOGGER.debug("Saved last location to database for: " + String.valueOf(playerId));
            }
            catch (SQLException e) {
                LOGGER.error("Failed to save last location to database for: " + String.valueOf(playerId), (Throwable)e);
            }
        });
    }

    private void saveLastLocationToFile(UUID playerId, double[] position) {
        if (!this.playerDataDir.exists()) {
            this.playerDataDir.mkdirs();
        }
        File locationFile = new File(this.playerDataDir, playerId.toString() + "_lastloc.dat");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(locationFile));){
            writer.write("lastX=" + position[0]);
            writer.newLine();
            writer.write("lastY=" + position[1]);
            writer.newLine();
            writer.write("lastZ=" + position[2]);
            writer.newLine();
            writer.write("savedAt=" + System.currentTimeMillis());
            writer.newLine();
            LOGGER.debug("Saved last location to file for: " + String.valueOf(playerId));
        }
        catch (IOException e) {
            LOGGER.error("Failed to save last location to file for: " + String.valueOf(playerId), (Throwable)e);
        }
    }

    public double[] loadLastLocation(UUID playerId) {
        if (this.enableDatabase) {
            return this.loadLastLocationFromDB(playerId);
        }
        return this.loadLastLocationFromFile(playerId);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private double[] loadLastLocationFromDB(UUID playerId) {
        String sql = "SELECT last_x, last_y, last_z FROM player_last_location WHERE uuid = ?";
        try (Connection conn = DriverManager.getConnection(this.jdbcUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);){
            pstmt.setString(1, playerId.toString());
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) return null;
            double x = rs.getDouble("last_x");
            double y = rs.getDouble("last_y");
            double z = rs.getDouble("last_z");
            LOGGER.info("Loaded last location from database for: " + String.valueOf(playerId));
            double[] dArray = new double[]{x, y, z};
            return dArray;
        }
        catch (SQLException e) {
            LOGGER.error("Failed to load last location from database for: " + String.valueOf(playerId), (Throwable)e);
        }
        return null;
    }

    private double[] loadLastLocationFromFile(UUID playerId) {
        File locationFile = new File(this.playerDataDir, playerId.toString() + "_lastloc.dat");
        if (!locationFile.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(locationFile));){
            String line;
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("lastX=")) {
                    x = Double.parseDouble(line.substring("lastX=".length()));
                } else if (line.startsWith("lastY=")) {
                    y = Double.parseDouble(line.substring("lastY=".length()));
                } else if (line.startsWith("lastZ=")) {
                    z = Double.parseDouble(line.substring("lastZ=".length()));
                }
            }
            LOGGER.info("Loaded last location from file for: " + String.valueOf(playerId));
            return new double[]{x, y, z};
        }
        catch (Exception e) {
            LOGGER.error("Failed to load last location from file for: " + String.valueOf(playerId), (Throwable)e);
            return null;
        }
    }

    private void deleteLastLocation(UUID playerId) {
        if (this.enableDatabase) {
            this.deleteLastLocationFromDB(playerId);
        } else {
            this.deleteLastLocationFromFile(playerId);
        }
    }

    private void deleteLastLocationFromDB(UUID playerId) {
        String sql = "DELETE FROM player_last_location WHERE uuid = ?";
        dbExecutor.execute(() -> {
            try (Connection conn = DriverManager.getConnection(this.jdbcUrl);
                 PreparedStatement pstmt = conn.prepareStatement(sql);){
                pstmt.setString(1, playerId.toString());
                pstmt.executeUpdate();
                LOGGER.debug("Deleted last location from database for: " + String.valueOf(playerId));
            }
            catch (SQLException e) {
                LOGGER.error("Failed to delete last location from database for: " + String.valueOf(playerId), (Throwable)e);
            }
        });
    }

    private void deleteLastLocationFromFile(UUID playerId) {
        File locationFile = new File(this.playerDataDir, playerId.toString() + "_lastloc.dat");
        if (locationFile.exists()) {
            if (locationFile.delete()) {
                LOGGER.debug("Deleted last location file for: " + String.valueOf(playerId));
            } else {
                LOGGER.warn("Failed to delete last location file for: " + String.valueOf(playerId));
            }
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        System.out.println("LoginSystem: Server is starting, loading passwords...");
        if (this.enableWebServer) {
            this.adminWebServer = new AdminWebServer(this, this.webServerPort, this.webServerPassword);
            this.adminWebServer.start();
        }
        if (this.enableDatabase) {
            try {
                this.loadPasswordsFromDB();
                System.out.println("LoginSystem: Successfully loaded " + this.playerPasswords.size() + " passwords from database.");
            }
            catch (Exception e) {
                System.err.println("LoginSystem: Failed to load passwords from database!");
                e.printStackTrace();
                throw new RuntimeException("Failed to load passwords from database", e);
            }
        } else {
            this.loadPasswordsFromFile();
            System.out.println("LoginSystem: Successfully loaded " + this.playerPasswords.size() + " passwords from file.");
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        System.out.println("LoginSystem: Server is stopping, saving passwords...");
        if (this.adminWebServer != null) {
            this.adminWebServer.stop();
        }
        if (this.enableDatabase) {
            try {
                System.out.println("LoginSystem: Saving " + this.playerPasswords.size() + " passwords to database...");
                this.saveAllPasswordsToDB();
                System.out.println("LoginSystem: All passwords saved successfully to database!");
            }
            catch (Exception e) {
                System.err.println("LoginSystem: Failed to save to database!");
                e.printStackTrace();
                throw new RuntimeException("Failed to save passwords to database", e);
            }
        } else {
            this.savePasswordsToFile();
            System.out.println("LoginSystem: Saved " + this.playerPasswords.size() + " passwords to file.");
        }
    }

    private void saveAllPasswordsToDB() {
        if (!this.enableDatabase || this.playerPasswords.isEmpty()) {
            LOGGER.debug("Skipping database save - database disabled or no passwords to save");
            return;
        }
        int totalPasswords = this.playerPasswords.size();
        LOGGER.info("Saving {} passwords to database...", (Object)totalPasswords);
        long startTime = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(this.jdbcUrl);){
            conn.setAutoCommit(false);
            String sql = "INSERT INTO player_passwords (uuid, password, last_login)\nVALUES (?, ?, CURRENT_TIMESTAMP)\nON DUPLICATE KEY UPDATE\n    password = VALUES(password),\n    last_login = CURRENT_TIMESTAMP\n";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);){
                int processedCount = 0;
                int batchSize = 100;
                for (Map.Entry<UUID, String> entry : this.playerPasswords.entrySet()) {
                    pstmt.setString(1, entry.getKey().toString());
                    pstmt.setString(2, entry.getValue());
                    pstmt.addBatch();
                    if (++processedCount % batchSize != 0) continue;
                    int[] updateCounts = pstmt.executeBatch();
                    LOGGER.debug("Processed batch of {} updates", (Object)updateCounts.length);
                }
                int[] updateCounts = pstmt.executeBatch();
                LOGGER.debug("Processed final batch of {} updates", (Object)updateCounts.length);
                conn.commit();
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.info("Successfully saved {} passwords to database in {} ms", (Object)totalPasswords, (Object)duration);
            }
            catch (SQLException e) {
                try {
                    conn.rollback();
                }
                catch (SQLException ex) {
                    LOGGER.error("Error during transaction rollback", (Throwable)ex);
                }
                LOGGER.error("Failed to save passwords to database", (Throwable)e);
                throw new RuntimeException("Failed to save passwords to database", e);
            }
        }
        catch (SQLException e) {
            LOGGER.error("Failed to save passwords to database!", (Throwable)e);
            throw new RuntimeException("Failed to save passwords to database", e);
        }
    }

    @SubscribeEvent
    public void onPlayerDropItem(ItemTossEvent event) {
        if (event.getEntity() == null || event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getPlayer() == null || !(event.getPlayer() instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer)event.getPlayer();
        UUID playerId = player.getUUID();
        if (!this.loggedIn.getOrDefault(playerId, false).booleanValue()) {
            ItemStack droppedItem;
            event.setCanceled(true);
            if (event.getEntity() != null && event.getEntity().getItem() != null && !(droppedItem = event.getEntity().getItem().copy()).isEmpty()) {
                boolean added = player.getInventory().add(droppedItem);
                player.getInventory().setChanged();
                if (!added) {
                    player.sendSystemMessage((Component)Component.literal((String)"Your inventory is full, so the item couldn't be returned.").withStyle(ChatFormatting.RED));
                } else {
                    String msg = this.languageManager.getMessage(playerId, "restrict.drop", new Object[0]);
                    player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.YELLOW));
                    this.showActionBar(player, msg);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        ServerPlayer player;
        UUID playerId;
        if (event.getEntity() instanceof ServerPlayer && !this.loggedIn.getOrDefault(playerId = (player = (ServerPlayer)event.getEntity()).getUUID(), false).booleanValue()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        ServerPlayer player;
        UUID playerId;
        if (event.getPlayer() instanceof ServerPlayer && !this.loggedIn.getOrDefault(playerId = (player = (ServerPlayer)event.getPlayer()).getUUID(), false).booleanValue()) {
            event.setCanceled(true);
            ((ServerLevel)event.getLevel()).setBlock(event.getPos(), event.getState(), 3);
            String msg = this.languageManager.getMessage(playerId, "restrict.break", new Object[0]);
            player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.RED));
            this.showActionBar(player, msg);
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        ServerPlayer player;
        UUID playerId;
        if (event.getEntity() instanceof ServerPlayer && !this.loggedIn.getOrDefault(playerId = (player = (ServerPlayer)event.getEntity()).getUUID(), false).booleanValue()) {
            event.setCanceled(true);
            String msg = this.languageManager.getMessage(playerId, "restrict.place", new Object[0]);
            player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.RED));
            this.showActionBar(player, msg);
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        ServerPlayer player;
        UUID playerId;
        if (event.getEntity() instanceof ServerPlayer && !this.loggedIn.getOrDefault(playerId = (player = (ServerPlayer)event.getEntity()).getUUID(), false).booleanValue()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        ServerPlayer player;
        UUID playerId;
        if (event.getEntity() instanceof ServerPlayer && !this.loggedIn.getOrDefault(playerId = (player = (ServerPlayer)event.getEntity()).getUUID(), false).booleanValue()) {
            event.setCanceled(true);
            String msg = this.languageManager.getMessage(playerId, "restrict.attack", new Object[0]);
            player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.RED));
            this.showActionBar(player, msg);
        }
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        ServerPlayer player;
        UUID playerId;
        Entity entity = event.getSource().getEntity();
        if (entity instanceof ServerPlayer && !this.loggedIn.getOrDefault(playerId = (player = (ServerPlayer)entity).getUUID(), false).booleanValue()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onItemPickup(EntityItemPickupEvent event) {
        ServerPlayer player;
        UUID playerId;
        if (event.getEntity() instanceof ServerPlayer && !this.loggedIn.getOrDefault(playerId = (player = (ServerPlayer)event.getEntity()).getUUID(), false).booleanValue()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        UUID playerId = player.getUUID();
        if (!this.loggedIn.getOrDefault(playerId, false).booleanValue()) {
            event.setCanceled(true);
            String msg = this.languageManager.getMessage(playerId, "restrict.chat", new Object[0]);
            player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.RED));
            this.showActionBar(player, msg);
        } else if (this.isMuted(playerId)) {
            event.setCanceled(true);
            player.sendSystemMessage((Component)Component.literal((String)"You are muted and cannot speak.").withStyle(ChatFormatting.RED));
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        String commandInput;
        ServerPlayer player;
        UUID playerId;
        Entity entity = ((CommandSourceStack)event.getParseResults().getContext().getSource()).getEntity();
        if (entity instanceof ServerPlayer && !this.loggedIn.getOrDefault(playerId = (player = (ServerPlayer)entity).getUUID(), false).booleanValue() && !(commandInput = event.getParseResults().getReader().getString().toLowerCase()).startsWith("register") && !commandInput.startsWith("login")) {
            event.setCanceled(true);
            String msg = this.languageManager.getMessage(playerId, "restrict.command", new Object[0]);
            player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(ChatFormatting.RED));
            this.showActionBar(player, msg);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        ServerPlayer player;
        UUID playerId;
        if (!event.player.getCommandSenderWorld().isClientSide() && event.phase == TickEvent.Phase.END && !this.loggedIn.getOrDefault(playerId = (player = (ServerPlayer)event.player).getUUID(), false).booleanValue()) {
            double dz;
            double dy;
            double waitingX = Double.parseDouble(this.config.getProperty("waitingAreaX", "0"));
            double waitingY = Double.parseDouble(this.config.getProperty("waitingAreaY", "100"));
            double waitingZ = Double.parseDouble(this.config.getProperty("waitingAreaZ", "0"));
            double dx = player.getX() - waitingX;
            if (dx * dx + (dy = player.getY() - waitingY) * dy + (dz = player.getZ() - waitingZ) * dz > 1.0) {
                player.teleportTo((ServerLevel)player.getCommandSenderWorld(), waitingX, waitingY, waitingZ, player.getYRot(), player.getXRot());
                String msg = this.languageManager.getMessage(playerId, "restrict.move", new Object[0]);
                this.showActionBar(player, msg);
            }
            if (Boolean.parseBoolean(this.config.getProperty("applyBlindness", "true")) && !player.hasEffect(MobEffects.BLINDNESS)) {
                int duration = Integer.parseInt(this.config.getProperty("blindnessDuration", "40"));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, false));
            }
        }
    }
}
