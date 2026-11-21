package dev.foabic.fabric.game;

import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

public final class FabricPermissions {

    private FabricPermissions() {
    }

    public static boolean has(PermissionSet permissions, int requestedLevel) {
        if (requestedLevel <= 0) return true;
        int level = Math.min(requestedLevel, 4);
        if (permissions instanceof LevelBasedPermissionSet levelBased) {
            return levelBased.level().id() >= level;
        }
        if (permissions == PermissionSet.ALL_PERMISSIONS) return true;
        if (permissions == PermissionSet.NO_PERMISSIONS) return false;
        return switch (level) {
            case 1 -> permissions.hasPermission(Permissions.COMMANDS_MODERATOR);
            case 2 -> permissions.hasPermission(Permissions.COMMANDS_GAMEMASTER);
            case 3 -> permissions.hasPermission(Permissions.COMMANDS_ADMIN);
            default -> permissions.hasPermission(Permissions.COMMANDS_OWNER);
        };
    }

    public static int level(PermissionSet permissions) {
        if (permissions instanceof LevelBasedPermissionSet levelBased) {
            return levelBased.level().id();
        }
        if (permissions == PermissionSet.ALL_PERMISSIONS) return 4;
        if (permissions == PermissionSet.NO_PERMISSIONS) return 0;
        if (permissions.hasPermission(Permissions.COMMANDS_OWNER)) return 4;
        if (permissions.hasPermission(Permissions.COMMANDS_ADMIN)) return 3;
        if (permissions.hasPermission(Permissions.COMMANDS_GAMEMASTER)) return 2;
        if (permissions.hasPermission(Permissions.COMMANDS_MODERATOR)) return 1;
        return 0;
    }
}
