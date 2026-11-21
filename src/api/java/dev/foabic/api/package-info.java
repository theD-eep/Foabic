/**
 * Foabic's public modding API for Minecraft 26.3.
 *
 * <p>Content registration is platform-neutral:
 * {@link dev.foabic.api.registry.FoabicRegistries#BLOCKS},
 * {@link dev.foabic.api.registry.FoabicRegistries#ITEMS},
 * {@link dev.foabic.api.registry.FoabicRegistries#BLOCK_ENTITY_TYPES} and
 * {@link dev.foabic.api.registry.FoabicRegistries#SOUND_EVENTS} work
 * identically on Fabric and NeoForge. The active adapter binds the concrete
 * views before mods are initialized.
 *
 * <p>Signals are plain data on the shared synchronous event bus. Platform
 * adapters translate their native events into
 * {@link dev.foabic.api.signal.PlayerInteractBlockSignal},
 * {@link dev.foabic.api.signal.PlayerBreakBlockSignal},
 * {@link dev.foabic.api.signal.ServerTickSignal} and the other signals.
 */
package dev.foabic.api;
