package com.fantasticterraform.network;

import com.fantasticterraform.FantasticTerraform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Canal de red del mod. Todos los handlers C->S validan {@code hasPermission(4)}
 * (ver cada packet). El wireframe NO viaja por aqui frame a frame: solo se envia un
 * {@link SelectionUpdatePacket} cuando los puntos cambian.
 */
public final class PacketHandler {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(FantasticTerraform.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private PacketHandler() {
    }

    public static void register() {
        int id = 0;

        // --- CORE / editor state ---
        CHANNEL.registerMessage(id++, EnterEditorModePacket.class,
                EnterEditorModePacket::encode, EnterEditorModePacket::decode, EnterEditorModePacket::handle);
        CHANNEL.registerMessage(id++, ExitEditorModePacket.class,
                ExitEditorModePacket::encode, ExitEditorModePacket::decode, ExitEditorModePacket::handle);
        CHANNEL.registerMessage(id++, EditorStatePacket.class,
                EditorStatePacket::encode, EditorStatePacket::decode, EditorStatePacket::handle);

        // --- SELECTION ---
        CHANNEL.registerMessage(id++, SetSelectionModePacket.class,
                SetSelectionModePacket::encode, SetSelectionModePacket::decode, SetSelectionModePacket::handle);
        CHANNEL.registerMessage(id++, SetSelectionPointPacket.class,
                SetSelectionPointPacket::encode, SetSelectionPointPacket::decode, SetSelectionPointPacket::handle);
        CHANNEL.registerMessage(id++, SelectionUpdatePacket.class,
                SelectionUpdatePacket::encode, SelectionUpdatePacket::decode, SelectionUpdatePacket::handle);
        CHANNEL.registerMessage(id++, ClearSelectionPacket.class,
                ClearSelectionPacket::encode, ClearSelectionPacket::decode, ClearSelectionPacket::handle);
        CHANNEL.registerMessage(id++, SetCylinderHeightPacket.class,
                SetCylinderHeightPacket::encode, SetCylinderHeightPacket::decode, SetCylinderHeightPacket::handle);

        // --- EDITING ---
        CHANNEL.registerMessage(id++, EditOperationPacket.class,
                EditOperationPacket::encode, EditOperationPacket::decode, EditOperationPacket::handle);
        CHANNEL.registerMessage(id++, EditProgressPacket.class,
                EditProgressPacket::encode, EditProgressPacket::decode, EditProgressPacket::handle);

        // --- BRUSHES ---
        CHANNEL.registerMessage(id++, BrushApplyPacket.class,
                BrushApplyPacket::encode, BrushApplyPacket::decode, BrushApplyPacket::handle);

        // --- TERRAIN ---
        CHANNEL.registerMessage(id++, TerrainOperationPacket.class,
                TerrainOperationPacket::encode, TerrainOperationPacket::decode, TerrainOperationPacket::handle);

        // --- MASKS ---
        CHANNEL.registerMessage(id++, MaskUpdatePacket.class,
                MaskUpdatePacket::encode, MaskUpdatePacket::decode, MaskUpdatePacket::handle);

        // --- HISTORY ---
        CHANNEL.registerMessage(id++, UndoRedoPacket.class,
                UndoRedoPacket::encode, UndoRedoPacket::decode, UndoRedoPacket::handle);

        // --- SCHEMATICS ---
        CHANNEL.registerMessage(id++, SaveSchematicPacket.class,
                SaveSchematicPacket::encode, SaveSchematicPacket::decode, SaveSchematicPacket::handle);
        CHANNEL.registerMessage(id++, LoadSchematicPacket.class,
                LoadSchematicPacket::encode, LoadSchematicPacket::decode, LoadSchematicPacket::handle);
        CHANNEL.registerMessage(id++, PasteSchematicPacket.class,
                PasteSchematicPacket::encode, PasteSchematicPacket::decode, PasteSchematicPacket::handle);
        CHANNEL.registerMessage(id++, SchematicListRequestPacket.class,
                SchematicListRequestPacket::encode, SchematicListRequestPacket::decode, SchematicListRequestPacket::handle);
        CHANNEL.registerMessage(id++, SchematicListPacket.class,
                SchematicListPacket::encode, SchematicListPacket::decode, SchematicListPacket::handle);

        // --- PARTICLES ---
        CHANNEL.registerMessage(id++, CreateParticleEmitterPacket.class,
                CreateParticleEmitterPacket::encode, CreateParticleEmitterPacket::decode, CreateParticleEmitterPacket::handle);
        CHANNEL.registerMessage(id++, ParticleEmitterDefinitionPacket.class,
                ParticleEmitterDefinitionPacket::encode, ParticleEmitterDefinitionPacket::decode, ParticleEmitterDefinitionPacket::handle);
        CHANNEL.registerMessage(id++, RemoveParticleEmitterPacket.class,
                RemoveParticleEmitterPacket::encode, RemoveParticleEmitterPacket::decode, RemoveParticleEmitterPacket::handle);

        // --- AMBIENCE ---
        CHANNEL.registerMessage(id++, CreateAmbienceZonePacket.class,
                CreateAmbienceZonePacket::encode, CreateAmbienceZonePacket::decode, CreateAmbienceZonePacket::handle);
        CHANNEL.registerMessage(id++, AmbienceTriggerPacket.class,
                AmbienceTriggerPacket::encode, AmbienceTriggerPacket::decode, AmbienceTriggerPacket::handle);

        // --- INTELLIGENT GENERATION ---
        CHANNEL.registerMessage(id++, GenerateBiomeTerrainPacket.class,
                GenerateBiomeTerrainPacket::encode, GenerateBiomeTerrainPacket::decode, GenerateBiomeTerrainPacket::handle);
        CHANNEL.registerMessage(id++, PopulateSelectionPacket.class,
                PopulateSelectionPacket::encode, PopulateSelectionPacket::decode, PopulateSelectionPacket::handle);
        CHANNEL.registerMessage(id++, ValidateDungeonSelectionPacket.class,
                ValidateDungeonSelectionPacket::encode, ValidateDungeonSelectionPacket::decode, ValidateDungeonSelectionPacket::handle);
        CHANNEL.registerMessage(id++, DungeonSelectionValidationResultPacket.class,
                DungeonSelectionValidationResultPacket::encode, DungeonSelectionValidationResultPacket::decode, DungeonSelectionValidationResultPacket::handle);
        CHANNEL.registerMessage(id++, GenerateDungeonPacket.class,
                GenerateDungeonPacket::encode, GenerateDungeonPacket::decode, GenerateDungeonPacket::handle);
        CHANNEL.registerMessage(id++, GenerationProgressPacket.class,
                GenerationProgressPacket::encode, GenerationProgressPacket::decode, GenerationProgressPacket::handle);

        // --- SELECTION (transformaciones y smart) ---
        CHANNEL.registerMessage(id++, SelectionTransformPacket.class,
                SelectionTransformPacket::encode, SelectionTransformPacket::decode, SelectionTransformPacket::handle);
        CHANNEL.registerMessage(id++, SmartSelectPacket.class,
                SmartSelectPacket::encode, SmartSelectPacket::decode, SmartSelectPacket::handle);

        // --- SCHEMATICS (vista previa fantasma) ---
        CHANNEL.registerMessage(id++, ClipboardPreviewPacket.class,
                ClipboardPreviewPacket::encode, ClipboardPreviewPacket::decode, ClipboardPreviewPacket::handle);

        // --- HISTORY (visual) ---
        CHANNEL.registerMessage(id++, HistoryRequestPacket.class,
                HistoryRequestPacket::encode, HistoryRequestPacket::decode, HistoryRequestPacket::handle);
        CHANNEL.registerMessage(id++, HistoryListPacket.class,
                HistoryListPacket::encode, HistoryListPacket::decode, HistoryListPacket::handle);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToClient(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /**
     * Devuelve el jugador remitente solo si es operador (nivel 4). Todos los handlers
     * C->S deben pasar por aqui: ninguna logica de edicion se ejecuta sin OP.
     */
    public static net.minecraft.server.level.ServerPlayer requireOp(
            net.minecraftforge.network.NetworkEvent.Context ctx) {
        net.minecraft.server.level.ServerPlayer player = ctx.getSender();
        if (player == null || !player.hasPermissions(4)) {
            return null;
        }
        return player;
    }
}
