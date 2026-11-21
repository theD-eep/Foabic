package clienttest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ClientTest implements ClientModInitializer {
    private static boolean joined;
    private static boolean connecting;
    private static int ticks;

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            joined = true;
            ticks = 0;
            System.out.println("FOABIC_CLIENT_JOIN");
        });
        ClientTickEvents.START_CLIENT_TICK.register(ClientTest::maybeConnect);
        ClientTickEvents.END_CLIENT_TICK.register(ClientTest::tick);
    }

    private static void maybeConnect(Minecraft client) {
        if (connecting || joined || client.player != null) return;
        if (!(client.screen instanceof TitleScreen)) return;
        connecting = true;
        ServerAddress address = ServerAddress.parseString("127.0.0.1:25598");
        ServerData data = new ServerData("Foabic", "127.0.0.1:25598", ServerData.Type.OTHER);
        System.out.println("FOABIC_CLIENT_CONNECTING");
        ConnectScreen.startConnecting(new TitleScreen(), client, address, data, false, null);
    }

    private static void tick(Minecraft client) {
        if (!joined || client.player == null || client.level == null) return;
        ticks++;
        if (ticks == 80) {
            BlockPos pos = client.player.blockPosition().offset(2, 1, 0);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
            System.out.println("FOABIC_CLIENT_USE=" + result);
        }
        if (ticks == 90) {
            BlockPos pos = client.player.blockPosition().offset(2, 1, 0);
            double dx = pos.getX() + 0.5 - client.player.getX();
            double dz = pos.getZ() + 0.5 - client.player.getZ();
            client.player.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
            client.player.setYHeadRot(client.player.getYRot());
            client.player.setXRot(0.0f);
            System.out.println("FOABIC_CLIENT_VIEW pos=" + pos
                    + " yaw=" + client.player.getYRot());
        }
        if (ticks == 100) {
            BlockPos pos = client.player.blockPosition().offset(2, 1, 0);
            System.out.println("FOABIC_CLIENT_BLOCK="
                    + client.level.getBlockState(pos).getBlock());
        }
        if (ticks == 130) {
            Screenshot.grab(client.gameDirectory, client.getMainRenderTarget(),
                    message -> System.out.println("FOABIC_CLIENT_SCREENSHOT=" + message.getString()));
        }
    }
}