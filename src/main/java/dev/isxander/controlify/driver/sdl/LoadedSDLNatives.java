package dev.isxander.controlify.driver.sdl;

import dev.isxander.controlify.utils.CUtil;
import dev.isxander.controlify.utils.log.ControlifyLogger;
import dev.isxander.sdl3java.api.version.SdlVersionRecord;

import static dev.isxander.sdl3java.api.SdlInit.*;
import static dev.isxander.sdl3java.api.SdlSubSystemConst.*;
import static dev.isxander.sdl3java.api.error.SdlError.*;
import static dev.isxander.sdl3java.api.hints.SdlHintConsts.*;
import static dev.isxander.sdl3java.api.hints.SdlHints.*;
import static dev.isxander.sdl3java.api.version.SdlVersion.*;

/**
 * This class is only constructed in a state where SDL natives are loaded
 * and can be used.
 */
public class LoadedSDLNatives {
    private static final ControlifyLogger logger = CUtil.LOGGER.createSubLogger("LoadedSDLNatives");

    private boolean hasAttemptedLoad = false;

    private boolean hasAudio = false;

    void startSDL3() {
        if (hasAttemptedLoad) {
            logger.warn("SDL3 has already been started, not starting again");
            return;
        }
        hasAttemptedLoad = true;

        SdlVersionRecord nativesVersion = SdlVersionRecord.fromPacked(SDL_GetVersion());
        SdlVersionRecord javaVersion = SDL_GetJavaBindingsVersion();
        logger.log("Loading SDL3 version: {}. Java bindings targeting: {}", nativesVersion, javaVersion);
        if (!nativesVersion.equals(javaVersion)) {
            String nativeStr = nativesVersion.toString();
            String javaStr = javaVersion.toString();
            String nativeMajorMinor = nativeStr.substring(0, nativeStr.lastIndexOf('.'));
            String javaMajorMinor = javaStr.substring(0, javaStr.lastIndexOf('.'));
            if (!nativeMajorMinor.equals(javaMajorMinor)) {
                throw new RuntimeException("SDL3 native library version mismatch: native is "
                        + nativesVersion + " but Java bindings target " + javaVersion
                        + ". Major/minor version difference is not ABI-compatible. SDL cannot be used.");
            } else {
                logger.warn("SDL3 native library micro-version differs: native is {} but Java bindings target {}. "
                        + "This is usually safe (SDL3 guarantees ABI compatibility within the same major.minor), "
                        + "but may cause issues on non-standard builds.", nativesVersion, javaVersion);
            }
        }

        SDL_SetHint(SDL_HINT_JOYSTICK_HIDAPI, "1");
        // Enhanced reports can trigger unaligned-memory access on Android SDL builds (e.g. ZalithLauncher, PojavLauncher)
        if (!CUtil.IS_ANDROID) {
            SDL_SetHint(SDL_HINT_JOYSTICK_ENHANCED_REPORTS, "1");
        }
        SDL_SetHint(SDL_HINT_JOYSTICK_HIDAPI_STEAM, "1");
        SDL_SetHint(SDL_HINT_JOYSTICK_ROG_CHAKRAM, "1");
        SDL_SetHint(SDL_HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS, "1");
        SDL_SetHint(SDL_HINT_JOYSTICK_LINUX_DEADZONES, "1");

        // initialise SDL with just joystick and gamecontroller subsystems
        if (!SDL_Init(SDL_INIT_JOYSTICK | SDL_INIT_GAMEPAD | SDL_INIT_EVENTS)) {
            logger.error("Failed to initialise SDL3: {}", SDL_GetError());
            throw new RuntimeException("Failed to initialise SDL3: " + SDL_GetError());
        }

        if (!SDL_InitSubSystem(SDL_INIT_AUDIO)) {
            logger.warn("Failed to initialise SDL3's audio subsystem, continuing without audio: {}", SDL_GetError());
            hasAudio = false;
        } else {
            hasAudio = true;
        }

        logger.log("Successfully initialised SDL subsystems");
    }

    public boolean hasAudio() {
        return hasAudio;
    }
}
